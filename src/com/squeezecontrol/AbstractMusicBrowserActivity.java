/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.squeezecontrol.download.MusicDownloadService;
import com.squeezecontrol.io.MusicBrowser;
import com.squeezecontrol.io.SqueezePlayer;
import com.squeezecontrol.model.Browsable;
import com.squeezecontrol.model.Album;
import com.squeezecontrol.model.Song;
import com.squeezecontrol.util.VolumeKeyHandler;
import com.squeezecontrol.view.BrowseableAdapter;
import com.squeezecontrol.view.NowPlayingView;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public abstract class AbstractMusicBrowserActivity<T extends Browsable> extends
        ListActivity implements OnScrollListener {

    private final int MENU_SEARCH = 1;

    protected String mTitle = "Items";

    private static int MAX_PENDING_LOAD_REQUESTS = 2;
    private static int PAGE_SIZE = 15;

	private static final int ADD_TO_PLAYLIST_CTX_MENU_ITEM = 100;
	
	private static final int PLAY_CTX_MENU_ITEM = 200;

	protected static final int DOWNLOAD_CTX_MENU_ITEM = 300;
	
	protected static final int ARTIST_CTX_MENU_ITEM = 400;
	
	protected static final int ALBUM_CTX_MENU_ITEM = 500;
	
    private Handler guiHandler = new Handler();

    private SqueezeService mService;
    private MusicBrowser mBrowser;

    private Filter mFilter;

    private String mQueryString = null;
    // This is used for detecting when the mQueryString changes.
    // The loader thread will discard loaded pages if the query version has
    // changed.
    private long mQueryVersion = 0;

    private boolean mHasData = false;

    private int mCurrentPosition;
    private LoaderThread mLoaderThread = null;
    private LinkedBlockingQueue<PageLoadCommand> mPageLoadQueue = new LinkedBlockingQueue<PageLoadCommand>();

    private CopyOnWriteArraySet<Integer> mLoadedPages = new CopyOnWriteArraySet<Integer>();
    private Pattern mSearchQueryPattern;

    private InputMethodManager mInputMethodManager;

    private NowPlayingView mNowPlayingView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void onServiceBound(SqueezeService service) {
        mNowPlayingView = new NowPlayingView(this, service);
        mNowPlayingView.startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mNowPlayingView) {
        	mNowPlayingView.stopListening();
        }
    }

    protected abstract BrowseableAdapter<T> createListAdapter();

    protected BrowseableAdapter<T> getCachedAdapter() {
        return (BrowseableAdapter<T>) getLastNonConfigurationInstance();
    }

    public void init() {
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCachedAdapter() != null) {
            setListAdapter(getCachedAdapter());
        } else {
            setListAdapter(createListAdapter());
        }

        setTitle("Loading...");
        ServiceUtils.bindToService(this, new SqueezeServiceConnection() {
            @Override
            public void onServiceConnected(SqueezeService service) {
                mService = service;
                mBrowser = service.getBroker().getMusicBrowser();
                startLoaderThread();
                onServiceBound(service);
            }

        });

        final ListView list = getListView();
        list.setFocusable(true);
        list.setItemsCanFocus(true);
        list.setTextFilterEnabled(true);
        list.setOnScrollListener(this);
        registerForContextMenu(list);

        mFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                setQueryString(constraint.toString());
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {

            }
        };

        getListAdapter().setFilter(mFilter);
    }

    private void setQueryString(String query) {
        synchronized (mLoaderThread) {
            mQueryVersion++;
            mLoadedPages = new CopyOnWriteArraySet<Integer>();
            mPageLoadQueue = new LinkedBlockingQueue<PageLoadCommand>();
        }
        mQueryString = query;
        if (query != null && query.length() > 0)
            mSearchQueryPattern = Pattern.compile(query, Pattern.LITERAL
                    | Pattern.CASE_INSENSITIVE);
        else
            mSearchQueryPattern = null;

        mPageLoadQueue.add(new PageLoadCommand(mQueryVersion, 0)); // Load first
        // page
        mLoaderThread.interrupt();

    }

    protected void onInitialResultLoaded(BrowseLoadResult<T> result) {

    }

    protected String getQueryString() {
        return mQueryString;
    }

    protected final Pattern getQueryPattern() {
        return mSearchQueryPattern;
    }

    @Override
    protected void onDestroy() {
        ServiceUtils.unbindFromService(this);
        if (mLoaderThread != null) {
            mLoaderThread.stopLoading();
        }
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
		// Figure out what list item we're getting context for
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		mCurrentPosition = mi.position;

		// Build the header
    	LayoutInflater mInflater = getLayoutInflater();
    	View hview = mInflater.inflate(R.layout.context_header, null);
    	TextView firstLine = (TextView) hview.findViewById(R.id.first_line);
    	firstLine.setText(getSelectedItem().getName());
    	menu.setHeaderView(hview);
		
    	/* XXX??? Maybe I should inflate a resource and then call
    	 * addContextMenuItems(menu)?  Not sure if that works... */
    	menu.add(0, PLAY_CTX_MENU_ITEM, 0, "Play now");
		menu.add(0, ADD_TO_PLAYLIST_CTX_MENU_ITEM, 1, "Add to playlist");
		// Allow subclass to add menu items (artist, album, download)
		addContextMenuItems(menu);
    }

    /**
     * Add context menu items appropriate to the subclass' activity, such
     * as "Download to device" or "Browse albums by this artist"
     * 
     * @param menu The menu to add items to.
     */
    protected void addContextMenuItems(ContextMenu menu) {
    	// Nothing to see here in most subclasses.
    }

	@Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == ADD_TO_PLAYLIST_CTX_MENU_ITEM) {
            addToPlaylist(getSelectedItem());
        } else if (itemId == PLAY_CTX_MENU_ITEM) {
            play(getSelectedItem(), mCurrentPosition);
        } else if (itemId == DOWNLOAD_CTX_MENU_ITEM) {
        	downloadIfSdCardIsPresent(getSelectedItem());
        } else if (itemId == ARTIST_CTX_MENU_ITEM) {
        	// Browse albums by the artist of the selected list item
        	Intent intent = new Intent(this, AlbumBrowserActivity.class);
        	Object selectedItem = getSelectedItem();
        	String artistId;
        	if (selectedItem instanceof Song) {
        		artistId = ((Song) selectedItem).getArtistId();
        	} else {
        		/* The Squeeze CLI won't return artist_id for an album, so we
        		 * have to go to some trouble to get it from the artist's name. */
        		artistId = getMusicBrowser().getArtistIdFromArtistName(
        						((Album) selectedItem).artistName);
        	}
        	intent.putExtra(AlbumBrowserActivity.EXTRA_ARTIST_ID, artistId);
            startActivity(intent);
        } else if (itemId == ALBUM_CTX_MENU_ITEM) {
        	// Browse the album the selected list item (a Song) is from
	        Intent intent = new Intent(this, SongBrowserActivity.class);
	        Song selectedItem = (Song) getSelectedItem();
	        intent.putExtra(SongBrowserActivity.EXTRA_ALBUM_ID,
	        		selectedItem.getAlbumId());
        	startActivity(intent);
        }
        return true;
    }

    private void downloadIfSdCardIsPresent(final T selectedItem) {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("Download to device?");
            b.setMessage("Are you sure you want to download this to your device?");
            b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    download(selectedItem);
                }
            });
            b.setNegativeButton("No", null);
            b.create().show();
        } else {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("No SD-card");
            b.setMessage("SD-card / external storage is not available. Please check that your device has an SD-card and that it is not mounted, then try again!");
            b.setPositiveButton("OK", null);
            b.create().show();
        }
    }

    protected T getSelectedItem() {
        return (T) getListView().getItemAtPosition(mCurrentPosition);
    }

    protected abstract void addToPlaylist(T selectedItem);

    protected abstract void play(T selectedItem, int index);

    protected void download(T selectedItem) {

    }

    protected MusicBrowser getMusicBrowser() {
        return mBrowser;
    }

    protected SqueezeService getSqueezeService() {
        return mService;
    }

    protected SqueezePlayer getPlayer() {
        return mService == null ? null : mService.getPlayer();
    }

    protected MusicDownloadService getDownloadService() {
        return mService == null ? null : mService.getDownloadService();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        int startPage = firstVisibleItem / PAGE_SIZE;
        int endPage = (firstVisibleItem + visibleItemCount) / PAGE_SIZE;
        if (!mLoadedPages.contains(startPage))
            try {
                mPageLoadQueue
                        .put(new PageLoadCommand(mQueryVersion, startPage));
            } catch (InterruptedException e) {
            }
        if (startPage != endPage && !mLoadedPages.contains(endPage)) {
            try {
                mPageLoadQueue.put(new PageLoadCommand(mQueryVersion, endPage));
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    protected final void startLoaderThread() {
        mLoaderThread = new LoaderThread();
        mLoaderThread.start();
    }

    @Override
    public BrowseableAdapter getListAdapter() {
        return (BrowseableAdapter) super.getListAdapter();
    }

    protected abstract BrowseLoadResult<T> loadItems(int startIndex, int count)
            throws IOException;

    protected void updateGUI(final BrowseLoadResult<T> result) {
        guiHandler.post(new Runnable() {

            public void run() {
                // We're now back on the gui thread. Check if
                // the query version has changed
                if (result.getQueryVersion() != mQueryVersion) {
                    return;
                }

                BrowseableAdapter<T> ad = (BrowseableAdapter<T>) getListAdapter();
                int totalCount = result.getTotalCount();
                ad.setNotifyOnChange(false);
                ad.setCount(totalCount);
                ad.setNotifyOnChange(true);
                ad.set(result.getResults(), result.getStartIndex());
                AbstractMusicBrowserActivity.this
                        .setTitle(getTitle(totalCount));
                if (!mHasData) {
                    onInitialResultLoaded(result);
                    mHasData = true;
                }
            }

            ;
        });
    }

    protected CharSequence getTitle(int totalCount) {
        return "Showing " + totalCount + " " + mTitle
                + (totalCount == 1 ? "" : "s");
    }

    class LoaderThread extends Thread {
        private volatile boolean mStopped = false;

        public LoaderThread() {
            super("Loader for "
                    + AbstractMusicBrowserActivity.this.getClass()
                    .getSimpleName());
        }

        protected synchronized void stopLoading() {
            mStopped = true;
            LoaderThread.this.interrupt();
        }

        @Override
        public void run() {
            try {
                while (!mStopped) {
                    try {
                        // Retrieve the current command queue and loaded page
                        // info, this will be changed by
                        // the GUI thread when the query changes, but we
                        // will be interrupted and stopped first in that case.
                        LinkedBlockingQueue<PageLoadCommand> commandQueue;
                        CopyOnWriteArraySet<Integer> loadedPages;
                        synchronized (this) {
                            commandQueue = mPageLoadQueue;
                            loadedPages = mLoadedPages;
                        }

                        // Drain the queue to appropriate size
                        while (commandQueue.size() > MAX_PENDING_LOAD_REQUESTS)
                            commandQueue.take();
                        PageLoadCommand command = commandQueue.take();
                        // Log.d(LOG_TAG, "Got page " + page);

                        int startIndex = command.pageNumber * PAGE_SIZE;
                        int itemsToLoad = PAGE_SIZE;
                        if (!loadedPages.contains(command.pageNumber)) {
                            /*
                                    * Log.d(LOG_TAG,
                                    * "Requesting items starting at position " +
                                    * startIndex);
                                    */

                            BrowseLoadResult<T> items = loadItems(startIndex,
                                    itemsToLoad);
                            // Set the query version that was used
                            // so out-of-date results can be discarded.
                            items.setQueryVersion(command.queryVersion);
                            // Log.d(LOG_TAG, "Updating view with page "
                            // + page);
                            loadedPages.add(command.pageNumber);
                            // Log.d(LOG_TAG, "Updating gui with items from "
                            // + startIndex);

                            updateGUI(items);
                        }
                    } catch (InterruptedException e) {
                        // Log.d(LOG_TAG, "Interrupted!");
                    }

                }
            } catch (IOException e) {
                final String message = "Error loading items: " + e.getMessage();
                guiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AbstractMusicBrowserActivity.this,
                                message, Toast.LENGTH_LONG);
                        finish();
                    }
                });
            }
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return getListAdapter();
    }

    @Override
    public boolean onSearchRequested() {
        mInputMethodManager.toggleSoftInput(0, 0);
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SEARCH, 0, "Show keyboard").setIcon(
                android.R.drawable.ic_menu_search);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SEARCH:
                onSearchRequested();
                break;
            default:
                onContextItemSelected(item);
                break;
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (VolumeKeyHandler.dispatchKeyEvent(event)) return true;
        else return super.dispatchKeyEvent(event);
    }

    class PageLoadCommand {
        int pageNumber;
        long queryVersion;

        public PageLoadCommand(long queryVersion, int pageNumber) {
            this.queryVersion = queryVersion;
            this.pageNumber = pageNumber;
        }
    }
}
