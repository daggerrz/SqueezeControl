package com.squeezecontrol;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.squeezecontrol.download.MusicDownloadService;
import com.squeezecontrol.io.MusicBrowser;
import com.squeezecontrol.io.SqueezePlayer;
import com.squeezecontrol.model.Browsable;
import com.squeezecontrol.view.BrowseableAdapter;

@SuppressWarnings("unchecked")
public abstract class AbstractMusicBrowserActivity<T extends Browsable> extends
		ListActivity implements OnScrollListener {

	private final int MENU_SEARCH = 1;

	protected String mTitle = "Items";

	private static int MAX_PENDING_LOAD_REQUESTS = 2;
	private static int PAGE_SIZE = 15;

	private static final String LOG_TAG = "AbstractMusicBrowser";

	private Handler guiHandler = new Handler();

	private SqueezeService mService;
	private MusicBrowser mBrowser;

	private Filter mFilter;

	private String mQueryString = null;
	// This is used for detecting when the mQueryString changes.
	// The loader thread will discard loaded pages if the query version has
	// changed.
	private long mQueryVersion = 0;

	private int mCurrentPosition;
	private LoaderThread mLoaderThread = null;
	private LinkedBlockingQueue<PageLoadCommand> mPageLoadQueue = new LinkedBlockingQueue<PageLoadCommand>();

	private CopyOnWriteArraySet<Integer> mLoadedPages = new CopyOnWriteArraySet<Integer>();
	private Pattern mSearchQueryPattern;

	private InputMethodManager mInputMethodManager;

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

	@Override
	protected void onPause() {
		super.onPause();
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

	protected void onServiceBound(SqueezeService service) {
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
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(getMenuResource(), menu);

		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		mCurrentPosition = mi.position;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.browser_add_to_playlist) {
			addToPlaylist(getSelectedItem());
		} else if (itemId == R.id.browser_play) {
			play(getSelectedItem(), mCurrentPosition);
		} else {
			downloadIfSdCardIsPresent(getSelectedItem());
		}
		return true;
	}

	private void downloadIfSdCardIsPresent(final T selectedItem) {
		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setTitle("Download to device?");
			b
					.setMessage("Are you sure you want to download this to your device?");
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
			b
					.setMessage("SD-card / external storage is not available. Please check that your device has an SD-card and that it is not mounted, then try again!");
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

				BrowseableAdapter ad = (BrowseableAdapter) getListAdapter();
				int totalCount = result.getTotalCount();
				if (totalCount != ad.getCount()) {
					ad.setNotifyOnChange(false);
					ad.setCount(totalCount);
				}
				ad.setNotifyOnChange(true);
				ad.set(result.getResults(), result.getStartIndex());
				AbstractMusicBrowserActivity.this.setTitle("Showing "
						+ totalCount + " " + mTitle
						+ (totalCount == 1 ? "" : "s"));
			};
		});
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

	protected int getMenuResource() {
		return R.menu.browser_menu;
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

	class PageLoadCommand {
		int pageNumber;
		long queryVersion;

		public PageLoadCommand(long queryVersion, int pageNumber) {
			this.queryVersion = queryVersion;
			this.pageNumber = pageNumber;
		}
	}
}
