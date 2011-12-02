/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.download;

import android.os.Environment;
import android.util.Log;
import com.squeezecontrol.BrowseLoadResult;
import com.squeezecontrol.io.MusicBrowser;
import com.squeezecontrol.model.Album;
import com.squeezecontrol.model.Song;
import com.squeezecontrol.util.PathUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class MusicDownloadService {

    private static final int BUFFER_SIZE = 8096 * 4;
    private static final int PROGRESS_THRESHOLD = 1000 * 200;

    private static final String TAG = "SongDownloadService";

    private SongDownloadThread mDownloadThread;
    private AlbumDownloadThread mAlbumDownloadThread;

    private LinkedBlockingQueue<Song> mSongQueue = new LinkedBlockingQueue<Song>();
    private LinkedBlockingQueue<Album> mAlbumQueue = new LinkedBlockingQueue<Album>();

    private DefaultHttpClient mClient;
    private String mBaseUrl;
    private String mSongFolder;

    private MusicBrowser mMusicBrowser;

    public MusicDownloadService(MusicBrowser musicBrowser, String baseUrl,
                                String username, String password) {
        this.mMusicBrowser = musicBrowser;
        this.mBaseUrl = baseUrl;
        this.mSongFolder = Environment.getExternalStorageDirectory()
                + "/Music/";

        mClient = new DefaultHttpClient();
        if (username != null && !"".equals(username)) {
            Credentials defaultcreds = new UsernamePasswordCredentials("dag",
                    "test");
            mClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
                    defaultcreds);
        }
    }

    public void queueSongForDownload(Song song) {
        mSongQueue.add(song);
    }

    public synchronized void start() {
        mDownloadThread = new SongDownloadThread();
        mDownloadThread.start();
        mAlbumDownloadThread = new AlbumDownloadThread();
        mAlbumDownloadThread.start();
    }

    public synchronized void stop() {
        if (mDownloadThread != null) {
            mDownloadThread.mStopped = true;
            mDownloadThread.interrupt();
        }
        if (mAlbumDownloadThread != null) {
            mAlbumDownloadThread.mStopped = true;
            mAlbumDownloadThread.interrupt();
        }
    }

    protected void onDownloadStarted(Song song, int songsInQueue) {

    }

    protected void onDownloadProgress(Song song, int songsInQueue,
                                      long bytesTransferred, long totalBytes) {

    }

    protected void onDownloadCompleted(Song song, int songsInQuee) {

    }

    protected void onDownloadError(Song song, String errorMessage) {

    }

    public void queueAlbumForDownload(Album album) {
        mAlbumQueue.add(album);
    }

    private void downloadSong(Song song) {
        HttpEntity entity = null;
        InputStream in = null;
        try {
            onDownloadStarted(song, mSongQueue.size());
            String fileName = createFilenamePath(song);
            HttpGet get = new HttpGet(mBaseUrl + song.id + "/download");
            HttpResponse response = (BasicHttpResponse) mClient.execute(get);
            entity = response.getEntity();
            long length = response.getEntity().getContentLength();
            in = response.getEntity().getContent();
            FileOutputStream out = new FileOutputStream(fileName);

            byte[] buf = new byte[BUFFER_SIZE];
            long bytesTransferred = 0;
            long bytesTransferredSinceThreshold = 0;
            int read = 0;
            while ((read = in.read(buf, 0, BUFFER_SIZE)) != -1) {
                out.write(buf, 0, read);
                bytesTransferred += read;
                bytesTransferredSinceThreshold += read;
                if (bytesTransferredSinceThreshold > PROGRESS_THRESHOLD) {
                    onDownloadProgress(song, mSongQueue.size(),
                            bytesTransferred, length);
                    bytesTransferredSinceThreshold = 0;
                }
            }
            out.close();
            in.close();
            response.getEntity().consumeContent();
            onDownloadCompleted(song, mSongQueue.size());
        } catch (IOException e) {
            Log.e(TAG, "Error downloading song: " + e.getMessage());
            onDownloadError(song, e.getMessage());
        } finally {
            if (entity != null) {
                try {
                    entity.consumeContent();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    private String createFilenamePath(Song song) throws IOException {
        File songFolder = new File(mSongFolder + "/"
                + PathUtils.escapeInvalidPathCharacters(song.artist) + "/"
                + PathUtils.escapeInvalidPathCharacters(song.album));
        if (!songFolder.exists()) {
            if (!songFolder.mkdirs()) {
                throw new IOException(
                        "Error creading album folder. Is the SD card mounted?");
            }
        }
        return songFolder.getAbsolutePath() + "/" + PathUtils.escapeInvalidPathCharacters(song.title) + "."
                + song.type;
    }

    /**
     * Looks up song information for an album and queues the songs individually.
     *
     * @author daggerrz
     */
    class AlbumDownloadThread extends Thread {
        private boolean mStopped = false;

        public AlbumDownloadThread() {
            setName("Album download thread");
        }

        @Override
        public void run() {
            while (!mStopped) {
                Album a;
                try {
                    a = mAlbumQueue.take();
                    int songCount = mMusicBrowser.getSongCount(null, a.id);
                    BrowseLoadResult<Song> songs = mMusicBrowser.getSongs(null,
                            a.id, null, 0, songCount);
                    for (Song song : songs.getResults())
                        mSongQueue.add(song);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    class SongDownloadThread extends Thread {

        private boolean mStopped = false;

        public SongDownloadThread() {
            setName("Song download thread");
        }

        @Override
        public void run() {
            while (!mStopped) {
                Song song;
                try {
                    song = mSongQueue.take();
                    downloadSong(song);
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
