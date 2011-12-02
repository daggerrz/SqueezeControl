/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.squeezecontrol.model.Song;

public class MusicDownloadNotificationManager {

    public static final int NOTIFICATION_DOWNLOAD_PROGRESS = 1;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private PendingIntent mIntent;

    private Context mContext;

    public MusicDownloadNotificationManager(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        mNotification = new Notification(R.drawable.squeeze_control,
                "Downloading music", System.currentTimeMillis());

        Intent downloadManagerIntent = new Intent(context,
                PlayerActivity.class);

        mIntent = PendingIntent.getActivity(context, 0, downloadManagerIntent,
                0);

    }

    public void onDownloadProgress(Song song, int songsInQueue, long downloaded, long total) {
        mNotification.setLatestEventInfo(mContext.getApplicationContext(),
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
