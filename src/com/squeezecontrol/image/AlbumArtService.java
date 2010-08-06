package com.squeezecontrol.image;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.squeezecontrol.Callback;
import com.squeezecontrol.model.Album;
import com.squeezecontrol.model.Song;

public class AlbumArtService implements Runnable {

	private static final String LOG_TAG = "AlbumArtService";

	private static int MAX_PENDING_LOAD_REQUESTS = 10;

	private ImageStore mTargetStore;
	private Thread mLoaderThread = null;

	private LinkedBlockingQueue<ImageRequest> mLoadQueue = new LinkedBlockingQueue<ImageRequest>();
	private ConcurrentHashMap<String, Bitmap> mImageCache = new ConcurrentHashMap<String, Bitmap>();

	public AlbumArtService(ImageStore target) {
		mTargetStore = target;
	}

	public final Bitmap getFromCache(String name) {
		return mImageCache.get(name);
	}

	private static String nameFor(Song song) {
		return song.id + "/cover.png";
	}

	private static String nameFor(Album album) {
		return album.artwork_track_id + "/cover_50x50_o";
	}

	public Bitmap getFromCache(Album album) {
		return getFromCache(nameFor(album));
	}

	public final void loadImage(Song song, ImageView coverImageView) {
		loadImage(nameFor(song), coverImageView);
	}

	public final void loadImage(String name, ImageView coverImageView) {
		final WeakReference<ImageView> viewRef = new WeakReference<ImageView>(
				coverImageView);
		Callback<Bitmap> callback = new Callback<Bitmap>() {
			@Override
			public void handle(final Bitmap value) {
				final ImageView view = viewRef.get();
				if (value != null && view != null) {

					view.post(new Runnable() {
						@Override
						public void run() {
							view.setImageBitmap(value);

						}
					});
				}
			}
		};
		loadImage(name, callback);
	}

	public final void loadImage(Song song, Callback<Bitmap> callback) {
		loadImage(nameFor(song), callback);
	}

	public void loadImage(Album album, Callback<Bitmap> imageCallback) {
		loadImage(nameFor(album), imageCallback);

	}

	public final void loadImage(String name, Callback<Bitmap> callback) {
		ImageRequest req = new ImageRequest();
		req.name = name;
		req.callback = callback;

		Bitmap cached = getFromCache(req.name);
		if (cached != null) {
			callback.handle(cached);
		} else {
			schedule(req);
		}

	}

	private void schedule(ImageRequest request) {
		//Log.i(LOG_TAG, "Scheduling " + request.name);
		mLoadQueue.add(request);
		synchronized (this) {
			if (mLoaderThread == null) {
				mLoaderThread = new Thread(this, "Album Art Loader");
				mLoaderThread.start();
			}
		}
	}

	@Override
	public void run() {
		ImageRequest req = null;
		while (true) {
			try {

				// Drain any excessive requests
				while (mLoadQueue.size() > MAX_PENDING_LOAD_REQUESTS)
					mLoadQueue.take();

				// Get next
				req = mLoadQueue.poll(2, TimeUnit.SECONDS);
				if (req != null) {
					// Check the cache again. Multiple requests for the same
					// image might occur
					Bitmap bitmap = getFromCache(req.name);
					if (bitmap == null) {
						// Nope, load it
						//Log.d(LOG_TAG, "Getting " + req.name);
						try {
						bitmap = mTargetStore.getImage(req.name);
						} catch (Throwable e) {
							//Log.e(LOG_TAG, "Error getting " + req.name + ": " + e.getMessage());
							bitmap = null;
						}
						if (bitmap != null)
							mImageCache.put(req.name, bitmap);
						//else
						//	Log.w(LOG_TAG, req.name + " returned null");
					}
					req.callback.handle(bitmap);
				} else {
					// Time to stop?
					synchronized (this) {
						if (mLoaderThread != null) {
							mLoaderThread = null;
							break;
						}
					}
				}
			} catch (InterruptedException e) {
			}
		}
	}

	private static class ImageRequest {
		String name;
		Callback<Bitmap> callback;
	}

}
