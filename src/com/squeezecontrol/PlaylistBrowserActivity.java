package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.model.Artist;
import com.squeezecontrol.model.Playlist;
import com.squeezecontrol.view.BrowseableAdapter;

public class PlaylistBrowserActivity extends AbstractMusicBrowserActivity<Playlist> {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "playlist";
		setContentView(R.layout.default_browser_list);
		super.init();
	}
	
	@Override
	protected BrowseableAdapter<Playlist> createListAdapter() {
		return new BrowseableAdapter<Playlist>(this, android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position < 0)
			return;
		/*Artist selectedItem = (Artist) getListAdapter().getItem(position);
		Intent intent = new Intent(this, AlbumBrowserActivity.class);
		intent.putExtra(AlbumBrowserActivity.EXTRA_ARTIST_ID, selectedItem.getId());
		startActivity(intent);*/		
	}
	
	@Override
	protected void addToPlaylist(Playlist selectedItem) {
		getPlayer().addToPlaylist(selectedItem);
	}
	
	@Override
	protected void play(Playlist selectedItem, int index) {
		getPlayer().playNow(selectedItem);
	}

	
	@Override
	protected BrowseLoadResult<Playlist> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getPlaylists(getQueryString(), startIndex, count);
	}
}
