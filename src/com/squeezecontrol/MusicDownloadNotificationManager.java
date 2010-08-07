package com.squeezecontrol;

import com.squeezecontrol.model.Song;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class MusicDownloadNotificationManager {

	public static final int NOTIFICATION_DOWNLOAD_PROGRESS = 1;

	private SqueezeService mService;

	private NotificationManager mNotificationManager;

	private Notification mNotification;

	private PendingIntent mIntent;

	public MusicDownloadNotificationManager(SqueezeService service) {
		mNotificationManager = (NotificationManager) service
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mService = service;
		mNotification = new Notification(R.drawable.squeeze_control,
				"Downloading music", System.currentTimeMillis());

		Intent downloadManagerIntent = new Intent(mService,
				PlayerActivity.class);

		mIntent = PendingIntent.getActivity(mService, 0, downloadManagerIntent,
				0);

	}

	public void onDownloadProgress(Song song, int songsInQueue, long downloaded, long total) {
		mNotification.setLatestEventInfo(mService.getApplicationContext(),
				"Downloading music (" + songsInQueue + " in queue)", "Song " + song.title + " (" + downloaded
						/ 1000 + " / " + total / 1000 + "kb)", mIntent);
		mNotificationManager.notify(NOTIFICATION_DOWNLOAD_PROGRESS,
				mNotification);

	}

	public void onDownloadStarted(Song song, int songsInQueue) {

	}

	public void onDownloadError(Song song) {
		mNotificationManager.cancel(NOTIFICATION_DOWNLOAD_PROGRESS);
	}

	public void onDownloadCompleted(Song song, int songsInQueue) {
		if (songsInQueue == 0) {
			mNotificationManager.cancel(NOTIFICATION_DOWNLOAD_PROGRESS);
		}
	}
}
