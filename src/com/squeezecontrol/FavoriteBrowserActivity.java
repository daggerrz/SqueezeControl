package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.model.Artist;
import com.squeezecontrol.model.Favorite;
import com.squeezecontrol.model.RadioStation;
import com.squeezecontrol.view.BrowseableAdapter;

public class FavoriteBrowserActivity extends AbstractMusicBrowserActivity<Favorite> {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "favorite";
		setContentView(R.layout.favorite_list);
		super.init();
	}
	
	@Override
	protected BrowseableAdapter<Favorite> createListAdapter() {
		return new BrowseableAdapter<Favorite>(this, android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position < 0)
			return;
		addToPlaylist((Favorite) getListAdapter().getItem(position));
	}
	
	@Override
	protected void addToPlaylist(Favorite selectedItem) {
		getPlayer().addToPlaylist(selectedItem);
		PlayerToasts.addedToPlayList(this, selectedItem);
	}
	
	@Override
	protected void play(Favorite selectedItem, int index) {
		getPlayer().playNow(selectedItem);
	}
	
	@Override
	protected BrowseLoadResult<Favorite> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getFavorites(getQueryString(), startIndex, count);
	}
}
