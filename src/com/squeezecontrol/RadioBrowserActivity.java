package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.model.Artist;
import com.squeezecontrol.model.RadioStation;
import com.squeezecontrol.view.BrowseableAdapter;

public class RadioBrowserActivity extends
		AbstractMusicBrowserActivity<RadioStation> {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "radio station";
		setContentView(R.layout.artist_list);
		super.init();
	}

	@Override
	protected BrowseableAdapter<RadioStation> createListAdapter() {
		return new BrowseableAdapter<RadioStation>(this,
				android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		RadioStation item = getSelectedItem();
		if ("xmlbrowser".equals(item.type)) {
			Intent i = new Intent(this, XmlBrowserActivity.class);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_COMMAND_COMMAND, item.id);
			i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_TITLE, item.title);
			startActivity(i);
		}
	}

	@Override
	protected void addToPlaylist(RadioStation selectedItem) {
	}

	@Override
	protected void play(RadioStation selectedItem, int index) {
	}

	@Override
	protected BrowseLoadResult<RadioStation> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getRadios(getQueryString(), startIndex, count);
	}
}
