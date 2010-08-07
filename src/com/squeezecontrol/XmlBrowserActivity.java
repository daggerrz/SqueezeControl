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

import com.squeezecontrol.io.SqueezeCommand;
import com.squeezecontrol.io.SqueezeTaggedRequestBuilder;
import com.squeezecontrol.model.Album;
import com.squeezecontrol.model.RadioStation;
import com.squeezecontrol.model.XmlBrowserEntry;
import com.squeezecontrol.view.BrowseableAdapter;

public class XmlBrowserActivity extends
		AbstractMusicBrowserActivity<XmlBrowserEntry> {

	public static final String EXTRA_BROWSER_COMMAND_COMMAND = "browser_command";
	public static final String EXTRA_ITEM_ID = "browser_item_id";
	public static final String EXTRA_BROWSER_TITLE = "browser_title";
	private String mBrowserCommand;
	private String mItemId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "radio station";
		setContentView(R.layout.generic_list);

		mBrowserCommand = getIntent().getStringExtra(
				EXTRA_BROWSER_COMMAND_COMMAND);
		mItemId = getIntent().getStringExtra(EXTRA_ITEM_ID);

		super.init();

	}

	@Override
	protected BrowseableAdapter<XmlBrowserEntry> createListAdapter() {
		return new BrowseableAdapter<XmlBrowserEntry>(this, R.layout.xmlbrowser_list_item) {
			@Override
			protected void bindView(int position, View view) {
				XmlBrowserEntry a = getItem(position);

				ImageView icon = (ImageView) view
						.findViewById(R.id.icon);
				TextView name = (TextView) findViewById(R.id.name);
				if (a == null) {
					name.setText(R.string.loading_progress);
					icon.setImageResource(R.drawable.unknown_album_cover);
				} else {
//					if (a.artwork_track_id == null) {
//						coverImage
//								.setImageResource(R.drawable.unknown_album_cover);
//					} else {
//						Bitmap image = mCoverImageService.getFromCache(a);
//						if (image != null) {
//							coverImage.setImageBitmap(image);
//						} else {
//							coverImage
//									.setImageResource(R.drawable.unknown_album_cover);
//							if (mLoadArt) {
//								mCoverImageService.loadImage(a, mImageCallback);
//							}
//
//						}
//					}
//					albumName.setText(a.getName());
//					if (getQueryPattern() != null) {
//						Linkify.addLinks(albumName, getQueryPattern(), null);
//					}
//					artistName.setText(a.artistName);
				}
			}
		};	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		XmlBrowserEntry item = getSelectedItem();
		if ("link".equals(item.type)) {
			Intent i = new Intent(this, XmlBrowserActivity.class);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_COMMAND_COMMAND,
					mBrowserCommand);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_TITLE, item.title);
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
				entry.title = m.get("title");
				entry.name = m.get("name");
				entry.type = m.get("type");
				entry.hasItems = "1".equals(m.get("hasitems"));
				entry.url = m.get("url");
				entries.add(entry);

				if (m.containsKey("count"))
					count = Integer.parseInt(m.get("count"));
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<XmlBrowserEntry>(count, startIndex, entries);
	}
}
