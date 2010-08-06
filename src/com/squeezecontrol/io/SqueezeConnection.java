package com.squeezecontrol.io;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import android.net.Uri;
import android.util.Log;

public class SqueezeConnection {

	private static final boolean DEBUG = true;
	public static String TAG = "SqueezeConnection";

	private String mHost;
	private int mPort;
	private Socket mSocket;
	private BufferedReader mInputReader;
	private BufferedOutputStream mOuputStream;
	private String mUsername;
	private String mPassword;
	private boolean mLoggingIn = false;

	public SqueezeConnection(String host, int port, String username,
			String password) {
		this.mHost = host;
		this.mPort = port;

		// Always send a username and password even if not set. This makes
		// Handling the responses easier.
		if (username == null || "".equals(username)) {
			mUsername = "dummy";
		} else
			mUsername = username;
		if (password == null || "".equals(password)) {
			mPassword = "dummy";
		} else
			mPassword = password;
	}

	public synchronized void open() throws IOException {
		// Log.d(TAG, "Opening socket");
		mSocket = new Socket(mHost, mPort);
		mInputReader = new BufferedReader(new InputStreamReader(mSocket
				.getInputStream()));
		mOuputStream = new BufferedOutputStream(mSocket.getOutputStream());

		mLoggingIn = true;
		sendCommand("login " + Uri.encode(mUsername) + " "
				+ Uri.encode(mPassword));
		// Log.d(TAG, "Connected");
	}

	public void close() {
		try {
			if (mOuputStream != null) {
				sendCommand("exit");
			}
		} catch (IOException e) {

		}
		try {
			if (mSocket != null)
				mSocket.close();
		} catch (IOException e) {
		}
		try {
			if (mInputReader != null)
				mInputReader.close();
		} catch (IOException e) {
		}
		try {
			if (mOuputStream != null)
				mOuputStream.close();
		} catch (IOException e) {
		}
		mInputReader = null;
		mOuputStream = null;
		mSocket = null;
	}

	public void sendCommand(String command) throws IOException {
		if (DEBUG) Log.d(TAG, "Sending command " + command);
		byte[] data = (command + "\n\r").getBytes("UTF-8");
		mOuputStream.write(data);
		mOuputStream.flush();
	}

	public String readCommand() throws IOException {
		BufferedReader reader = mInputReader;
		if (reader == null) throw new IOException("Connection closed.");
		String line = null;
		// Eat empty lines
		while ("".equals(line = reader.readLine()))
			;

		// Login response?
		if (line != null && mLoggingIn && line.startsWith("login")) {
			mLoggingIn = false;
			// Read next line
			while ("".equals(line = reader.readLine()))
				;
			// Connection closed after login response?
			if (line == null)
				throw new InvalidCredentialsException("Invalid username or password. Check your settings!");
		}

		if (line == null)
			throw new IOException("Connection closed.");

		if (DEBUG) Log.d(TAG, "Rcv: " + line);
		return line;
	}

	public String getHost() {
		return mHost;
	}

	public int getPort() {
		return mPort;
	}

	public boolean isConnected() {
		return mSocket != null;
	}
	
	public static class InvalidCredentialsException extends IOException {
		public InvalidCredentialsException(String message) {
			super(message);
		}
	}

}
