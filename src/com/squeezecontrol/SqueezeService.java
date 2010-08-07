package com.squeezecontrol;

import java.io.IOException;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.squeezecontrol.download.MusicDownloadService;
import com.squeezecontrol.image.ImageLoaderService;
import com.squeezecontrol.image.HttpFetchingImageStore;
import com.squeezecontrol.io.SqueezeBroker;
import com.squeezecontrol.io.SqueezeEventListener;
import com.squeezecontrol.io.SqueezePlayer;
import com.squeezecontrol.model.Song;

public class SqueezeService extends Service implements SqueezeEventListener {

	private final SqueezeServiceBinder binder = new SqueezeServiceBinder();
	private Handler mHandler = new Handler();
	private SqueezeBroker mBroker;
	private SqueezePlayer mPlayer;
	private boolean started = false;

	private ConnectivityManager mConnectivityManager;
	private BroadcastReceiver mConnectivityStateReceiver;
	
	private HttpFetchingImageStore mCoverImageStore;
	private ImageLoaderService mCoverImageService;
	
	private HttpFetchingImageStore mGenericImageStore;
	private ImageLoaderService mGenericImageService;

	private MusicDownloadService mDownloadService;

	@Override
	public void onCreate() {
		super.onCreate();
		initialize();
		IntentFilter filter = new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(mConnectivityStateReceiver, filter);
	}

	public void initialize() {
		if (mBroker != null) {
			mBroker.disconnect();
		}
		String username = Settings.getUsername(this);
		String password = Settings.getPassword(this);
		mBroker = new SqueezeBroker(Settings.getHost(this), Settings
				.getCLIPort(this), username, password, this);

		final MusicDownloadNotificationManager notManager = new MusicDownloadNotificationManager(
				this);
		mDownloadService = new MusicDownloadService(mBroker.getMusicBrowser(),
				"http://" + mBroker.getHost() + ":"
						+ Settings.getHTTPPort(this) + "/music/", username,
				password) {
			@Override
			protected void onDownloadStarted(Song song, int songsInQueue) {
				super.onDownloadStarted(song, songsInQueue);
				notManager.onDownloadStarted(song, songsInQueue);
			}

			@Override
			protected void onDownloadProgress(Song song, int songsInQueue, long bytesTransferred,
					long totalBytes) {
				super.onDownloadProgress(song, songsInQueue, bytesTransferred, totalBytes);
				notManager.onDownloadProgress(song, songsInQueue, bytesTransferred, totalBytes);
			}

			@Override
			protected void onDownloadCompleted(Song song, int songsInQueue) {
				super.onDownloadCompleted(song, songsInQueue);
				notManager.onDownloadCompleted(song, songsInQueue);
				// Let the media scanner pick it up
				if (songsInQueue == 0) {
					postToast("Music download completed", Toast.LENGTH_SHORT);
					sendBroadcast(new Intent(
							Intent.ACTION_MEDIA_MOUNTED,
							Uri
									.parse("file://"
											+ Environment
													.getExternalStorageDirectory())));

				}
			}

			@Override
			protected void onDownloadError(Song song, String errorMessage) {
				super.onDownloadError(song, errorMessage);
				notManager.onDownloadError(song);
				postToast("Download of song " + song.title + " failed. ("
						+ errorMessage + ")", Toast.LENGTH_LONG);
			}
		};
		mDownloadService.start();

		mCoverImageStore = new HttpFetchingImageStore("http://" + mBroker.getHost()
				+ ":" + Settings.getHTTPPort(this) + "/music/", username,
				password);
		mCoverImageService = new ImageLoaderService(mCoverImageStore);
		
		// For XMLBrowser
		mGenericImageStore = new HttpFetchingImageStore(null, null, null);
		mGenericImageService = new ImageLoaderService(mGenericImageStore);
		

		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mConnectivityStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				startIfWifiAccess();
			}
		};
	}

	protected void postToast(final String message, final int length) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(SqueezeService.this, message, length).show();
			}
		});
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		startIfWifiAccess();
	}

	private void startIfWifiAccess() {
		if (!Settings.isConfigured(this))
			return;

		NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		if (activeNetwork != null
				&& activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
			synchronized (this) {
				if (!started) {
					started = true;
				}
				mBroker.connect();
			}
		}
	}

	public void setPlayer(SqueezePlayer player) {
		mPlayer = player;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mBroker.disconnect();
		if (mDownloadService != null)
			mDownloadService.stop();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class SqueezeServiceBinder extends Binder {
		SqueezeService getService() {
			return SqueezeService.this;
		}
	}

	@Override
	public void onConnect(final SqueezeBroker broker) {
	}

	@Override
	public void onConnectionError(SqueezeBroker broker, final IOException cause) {
	}

	@Override
	public void onDisconnect(final SqueezeBroker broker) {
		/*
		 * mHandler.post(new Runnable() {
		 * 
		 * @Override public void run() { Toast.makeText(SqueezeService.this,
		 * "Disconnected from " + broker.getHost(), Toast.LENGTH_SHORT).show();
		 * } });
		 */
	}

	public SqueezePlayer getPlayer() {
		if (mPlayer == null) {
			String playerId = Settings.getPlayerId(this);
			if (playerId != null) {
				mPlayer = mBroker.getPlayer(playerId);
			}
		}
		return mPlayer;
	}

	public SqueezeBroker getBroker() {
		return mBroker;
	}

	public ImageLoaderService getCoverImageService() {
		return mCoverImageService;
	}

	public ImageLoaderService getGenericImageService() {
		return mGenericImageService;
	}
	
	public MusicDownloadService getDownloadService() {
		return mDownloadService;
	}

	public static void showConnectionNotification(final Activity context,
			final SqueezeBroker broker) {
		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context,
						"SqueezeControl: Connected to " + broker.getHost(),
						Toast.LENGTH_SHORT).show();
			}
		});

	}

	public static void showConnectionError(final Activity context,
			SqueezeBroker broker, final IOException cause) {
		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(
						context,
						"SqueezeControl: Connection error: "
								+ cause.getMessage(), Toast.LENGTH_SHORT)
						.show();
			}
		});

	}

}
