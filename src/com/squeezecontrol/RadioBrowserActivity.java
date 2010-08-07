package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.model.Artist;
import com.squeezecontrol.model.XmlBrowser;
import com.squeezecontrol.view.BrowseableAdapter;

public class RadioBrowserActivity extends
		AbstractMusicBrowserActivity<XmlBrowser> {

	public static final String EXTRA_BROWSER_TYPE = "browser_type";
	
	public static final String RADIOS = "radios";
	public static final String APPS = "apps";

	private String mBrowserType;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "radio station";
		
		mBrowserType = getIntent().getStringExtra(EXTRA_BROWSER_TYPE);
		
		setContentView(R.layout.artist_list);
		super.init();
	}

	@Override
	protected BrowseableAdapter<XmlBrowser> createListAdapter() {
		return new BrowseableAdapter<XmlBrowser>(this,
				android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		XmlBrowser item = (XmlBrowser) l.getItemAtPosition(position);
		if ("xmlbrowser".equals(item.type)) {
			Intent i = new Intent(this, XmlBrowserActivity.class);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_COMMAND_COMMAND, item.id);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_TITLE, item.name);
			startActivity(i);
		}
	}

	@Override
	protected void addToPlaylist(XmlBrowser selectedItem) {
	}

	@Override
	protected void play(XmlBrowser selectedItem, int index) {
	}

	@Override
	protected BrowseLoadResult<XmlBrowser> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getBrowsers(mBrowserType, startIndex, count);
	}
}
