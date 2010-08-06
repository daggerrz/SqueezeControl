package com.squeezecontrol.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.squeezecontrol.io.FutureResponse.ResponseCallback;
import com.squeezecontrol.io.SqueezeConnection.InvalidCredentialsException;

/**
 * Manages a {@link SqueezeConnection} and read / write queues and routes
 * notifications to the players.
 * 
 * @author liodden
 * 
 */
public class SqueezeBroker {

	private static final String TAG = "SqueezeBroker";
	private static final boolean DEBUG = false;
	private static final String REQUEST_TAG = "_SC_REQ_";

	private SqueezeConnection mConnection;
	private CopyOnWriteArraySet<SqueezeEventListener> mEventListeners = new CopyOnWriteArraySet<SqueezeEventListener>();

	private ReaderThread readerThread;
	private WriterThread writerThread;

	private Map<String, SqueezePlayer> players = new ConcurrentHashMap<String, SqueezePlayer>();
	private Map<String, FutureResponse<SqueezeCommand>> responseMap = new ConcurrentHashMap<String, FutureResponse<SqueezeCommand>>();

	private ArrayList<ResponseCallback<SqueezeCommand>> mPrefixCallbacks = new ArrayList<ResponseCallback<SqueezeCommand>>();

	private AtomicInteger requestIdGenerator = new AtomicInteger();
	private int mCliPort;
	private String mHost;
	private MusicBrowser mMusicBrowser;
	private String mUsername;
	private String mPassword;

	public SqueezeBroker(String host, int cliPort, String username,
			String password, SqueezeEventListener listener) {
		mHost = host;
		mCliPort = cliPort;
		mUsername = username;
		mPassword = password;
		mEventListeners.add(listener);
		mMusicBrowser = new MusicBrowser(this);
	}

	public int getPort() {
		return mCliPort;
	}

	public String getHost() {
		return mHost;
	}

	public SqueezePlayer getPlayer(String id) {
		SqueezePlayer p = players.get(id);
		if (p == null) {
			p = new SqueezePlayer(id, this);
			players.put(id, p);
			try {
				p.requestPlayerState();
			} catch (IOException e) {
			}
		}
		return p;
	}

	public synchronized void connect() {
		if (mConnection != null)
			return;
		mConnection = new SqueezeConnection(mHost, mCliPort, mUsername,
				mPassword);
		try {
			mConnection.open();
			readerThread = new ReaderThread();
			readerThread.start();
			writerThread = new WriterThread();
			writerThread.start();
			for (SqueezeEventListener l : mEventListeners)
				l.onConnect(this);
			postCommand("listen 1");
			for (SqueezePlayer p : players.values())
				p.requestPlayerState();
		} catch (IOException e) {
			disconnect();
			for (SqueezeEventListener l : mEventListeners)
				l.onConnectionError(this, e);
		}
	}

	public synchronized void disconnect() {
		if (mConnection == null)
			return;

		if (readerThread != null) {
			readerThread.cancel();
			readerThread = null;
		}
		if (writerThread != null) {
			writerThread.cancel();
			writerThread = null;
		}
		final SqueezeConnection conn = mConnection;

		// A bug in InputStream.close causes it to hang for a long time, so
		// disconnect in a separate thread.
		new Thread("Connection closer thread") {
			@Override
			public void run() {
				conn.close();
			}
		}.start();
		mConnection = null;
		for (SqueezeEventListener l : mEventListeners)
			l.onDisconnect(this);
	}

	public void postCommand(String command) {
		if (writerThread != null)
			writerThread.enqueue(command);
	}

	public void postCommand(String command,
			ResponseCallback<SqueezeCommand> callback) {
		synchronized (mPrefixCallbacks) {
			mPrefixCallbacks.add(callback);
		}
		if (writerThread != null)
			writerThread.enqueue(command);
	}

	public SqueezeCommand sendRequest(String command) throws IOException {
		FutureResponse<SqueezeCommand> respHolder = sendRequest(command, null);
		try {
			return respHolder.getResponse();
		} catch (InterruptedException e) {
			throw new IOException("Interrupted while waiting for response");
		}
	}

	public FutureResponse<SqueezeCommand> sendRequest(String command,
			ResponseCallback<SqueezeCommand> callback) {
		String reqId = String.valueOf(requestIdGenerator.addAndGet(1));
		FutureResponse<SqueezeCommand> respHolder = new FutureResponse<SqueezeCommand>(
				callback);
		responseMap.put(reqId, respHolder);
		postCommand(command + " " + REQUEST_TAG + reqId);
		return respHolder;
	}

	private void handleResponse(SqueezeCommand command, String requestIdString) {
		String reqId = requestIdString.substring(REQUEST_TAG.length());
		FutureResponse<SqueezeCommand> respHolder = responseMap.get(reqId);
		// if (DEBUG)
		// Log.d(TAG, "Response for " + reqId);
		if (respHolder != null) {
			respHolder.setResponse(command);
		}
	}

	public boolean isConnected() {
		return mConnection != null && mConnection.isConnected();
	}

	public ArrayList<SqueezePlayer> getPlayers() throws IOException {
		SqueezeCommand response = sendRequest("players 0 100");
		ArrayList<SqueezePlayer> players = new ArrayList<SqueezePlayer>();
		ArrayList<ArrayList<String>> rows = response
				.splitParameters("playerindex%3A");
		for (ArrayList<String> pArray : rows) {
			Map<String, String> pMap = SqueezeCommand
					.splitToParameterMap(pArray);
			SqueezePlayer p = getPlayer(pMap.get("playerid"));
			p.name = pMap.get("name");
			p.ipAddress = pMap.get("ip");
			p.model = pMap.get("model");
			players.add(p);
		}
		return players;
	}

	class WriterThread extends Thread {
		private boolean cancelled = false;
		LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<String>();

		public WriterThread() {
			setName("Slim Writer Thread");
		}

		void enqueue(String command) {
			commandQueue.add(command);
		}

		void cancel() {
			cancelled = true;
			interrupt();
		}

		@Override
		public void run() {
			while (!cancelled) {
				try {
					String command = commandQueue.poll(1, TimeUnit.SECONDS);
					if (command != null)
						mConnection.sendCommand(command);
				} catch (InvalidCredentialsException e) {

				} catch (IOException e) {
					if (!cancelled) {
						// Log.i(TAG, "Error during send: " + e.getMessage());
						// Let the reader thread disconnect
					}
					break;
				} catch (InterruptedException e) {
				}
			}
		}
	}

	class ReaderThread extends Thread {
		private boolean cancelled = false;

		public ReaderThread() {
			setName("Slim Reader Thread");
		}

		void cancel() {
			cancelled = true;
			interrupt();
		}

		@Override
		public void run() {
			while (!cancelled) {
				String commandString;
				try {
					commandString = mConnection.readCommand();
					// if (DEBUG)
					// Log.d(TAG, "Recv: " + commandString);
					SqueezeCommand command = CommandParser.parse(commandString);
					if (command != null) {
						// if (DEBUG)
						// Log.d(TAG, "Parsed command successfully");

						boolean handled = false;
						handled = executePrefixCallbacks(commandString, command);
						if (!handled)
							handled = executeResponseHolders(command);
						if (!handled)
							executePlayerNotification(command);

					}
				} catch (SqueezeConnection.InvalidCredentialsException e) {
					for (SqueezeEventListener l : mEventListeners)
						l.onConnectionError(SqueezeBroker.this, e);
					disconnect();
				} catch (IOException e) {
					// Are we disconnected
					if (!cancelled) {
						// Log.i(TAG, "Error during read: " + e.getMessage());
						disconnect();
					}
					break;
				}
			}
		}

		private final boolean executePlayerNotification(SqueezeCommand command) {
			if (players.containsKey(command.getPlayerId())) {
				getPlayer(command.getPlayerId()).handleNotification(command);
				return true;
			} else
				return false;
		}

		private final boolean executeResponseHolders(SqueezeCommand command) {
			// Request response?
			boolean response = false;
			for (String p : command.getParameters()) {
				if (p.startsWith(REQUEST_TAG)) {
					handleResponse(command, p);
				}
			}
			return response;
		}

		@SuppressWarnings("unchecked")
		private final boolean executePrefixCallbacks(String commandString,
				SqueezeCommand response) {

			ArrayList<FutureResponse.ResponseCallback<SqueezeCommand>> callbacks = new ArrayList<ResponseCallback<SqueezeCommand>>();
			synchronized (mPrefixCallbacks) {
				for (Iterator<ResponseCallback<SqueezeCommand>> iter = mPrefixCallbacks
						.iterator(); iter.hasNext();) {
					ResponseCallback<SqueezeCommand> cb = iter.next();
					if (commandString.startsWith(cb.getCommandPrefix())) {
						callbacks.add(cb);
						iter.remove();
					}
				}
			}
			for (ResponseCallback callback : callbacks) {
				callback.handleResponse(response);
			}
			return callbacks.size() > 0;
		}
	}

	public MusicBrowser getMusicBrowser() {
		return mMusicBrowser;
	}

	public void addEventListener(SqueezeEventListener listener) {
		mEventListeners.add(listener);
	}

	public void removeEventListener(SqueezeEventListener listener) {
		mEventListeners.remove(listener);
	}
}
