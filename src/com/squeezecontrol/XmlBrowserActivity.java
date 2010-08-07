package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squeezecontrol.image.ImageLoaderService;
import com.squeezecontrol.io.SqueezeCommand;
import com.squeezecontrol.io.SqueezeTaggedRequestBuilder;
import com.squeezecontrol.model.Album;
import com.squeezecontrol.model.XmlBrowser;
import com.squeezecontrol.model.XmlBrowserEntry;
import com.squeezecontrol.view.BrowseableAdapter;

public class XmlBrowserActivity extends
		AbstractMusicBrowserActivity<XmlBrowserEntry> {

	public static final String EXTRA_BROWSER_COMMAND_COMMAND = "browser_command";
	public static final String EXTRA_ITEM_ID = "browser_item_id";
	public static final String EXTRA_BROWSER_TITLE = "browser_title";
	private String mBrowserCommand;
	private String mItemId;
	private Callback<Bitmap> mImageCallback;
	private ImageLoaderService mImageLoaderService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "Loading...";
		setContentView(R.layout.generic_list);

		mBrowserCommand = getIntent().getStringExtra(
				EXTRA_BROWSER_COMMAND_COMMAND);
		mItemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
		String title = getIntent().getStringExtra(EXTRA_BROWSER_TITLE);
		if (title != null) mTitle = title;

		final Runnable notifyChanges = new Runnable() {
			public void run() {
				getListAdapter().notifyDataSetChanged();
			};
		};
		mImageCallback = new Callback<Bitmap>() {
			public void handle(Bitmap value) {
				getListView().post(notifyChanges);
			};
		};

		super.init();

	}

	@Override
	protected void onServiceBound(SqueezeService service) {
		super.onServiceBound(service);
		mImageLoaderService = service.getGenericImageService();
	}

	@Override
	protected BrowseableAdapter<XmlBrowserEntry> createListAdapter() {
		return new BrowseableAdapter<XmlBrowserEntry>(this,
				R.layout.xmlbrowser_list_item) {
			@Override
			protected void bindView(int position, View view) {
				XmlBrowserEntry a = getItem(position);

				ImageView icon = (ImageView) view.findViewById(R.id.icon);
				TextView name = (TextView) view.findViewById(R.id.name);
				if (a == null) {
					name.setText(R.string.loading_progress);
					icon.setImageResource(R.drawable.unknown_album_cover);
				} else {
					if (a.icon == null) {
						if (a.hasItems)
							icon.setImageResource(R.drawable.musicfolder);
						else if (a.isAudio)
							icon.setImageResource(R.drawable.tuneinurl);
						else 
							icon.setImageResource(R.drawable.unknown_album_cover);
					} else {
						Bitmap image = mImageLoaderService.getFromCache(a.icon);
						if (image != null) {
							icon.setImageBitmap(image);
						} else {
							icon.setImageResource(R.drawable.unknown_album_cover);
							mImageLoaderService
									.loadImage(a.icon, mImageCallback);

						}
					}
					name.setText(a.name);
				}
			}
		};
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		XmlBrowserEntry item = (XmlBrowserEntry) l.getItemAtPosition(position);
		if (item.hasItems) {
			Intent i = new Intent(this, XmlBrowserActivity.class);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_COMMAND_COMMAND,
					mBrowserCommand);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_TITLE, item.title == null ? item.name : item.title);
			i.putExtra(XmlBrowserActivity.EXTRA_ITEM_ID, item.id);
			startActivity(i);
		} else {
			getPlayer().sendCommand(
					new SqueezeTaggedRequestBuilder(mBrowserCommand
							+ " playlist play").addTag("item_id", item.id)
							.toString());
		}
	}
	
	@Override
	protected CharSequence getTitle(int totalCount) {
		return mTitle;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// No popup
	}

	@Override
	protected void addToPlaylist(XmlBrowserEntry item) {
		// Not in use
	}

	@Override
	protected void play(XmlBrowserEntry item, int index) {
		// Not in use
	}

	@Override
	protected BrowseLoadResult<XmlBrowserEntry> loadItems(int startIndex,
			int pageSize) throws IOException {
		ArrayList<XmlBrowserEntry> entries = new ArrayList<XmlBrowserEntry>(
				pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					mBrowserCommand + " items " + startIndex + " " + pageSize);
			if (mItemId != null)
				command.addTag("item_id", mItemId);
			SqueezeCommand res = getPlayer().sendRequest(command.toString());

			XmlBrowserEntry entry = null;
			List<Map<String, String>> maps = res.splitToMap("id");
			for (Map<String, String> m : maps) {
				entry = new XmlBrowserEntry();
				entry.id = m.get("id");
				entry.name = m.get("name");
				entry.title = m.get("title");
				entry.type = m.get("type");
				entry.hasItems = "1".equals(m.get("hasitems"));
				entry.isAudio = "1".equals(m.get("isaudio"));
				entry.url = m.get("url");
				entry.icon = m.get("image");
				
				entries.add(entry);

				if (m.containsKey("count"))
					count = Integer.parseInt(m.get("count"));
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<XmlBrowserEntry>(count, startIndex, entries);
	}
}
