package com.squeezecontrol;

import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.model.Genre;
import com.squeezecontrol.view.BrowseableAdapter;

public class GenreBrowserActivity extends AbstractMusicBrowserActivity<Genre> {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "genre";

		setContentView(R.layout.generic_list);
		super.init();
	}
	
	@Override
	protected BrowseableAdapter<Genre> createListAdapter() {
		return new BrowseableAdapter<Genre>(this, android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position < 0)
			return;
		Genre selectedItem = (Genre) getListAdapter().getItem(position);
		if (selectedItem == null) return;
		
		Intent intent = new Intent(this, ArtistBrowserActivity.class);
		intent.putExtra(ArtistBrowserActivity.EXTRA_GENRE_ID, selectedItem.getId());
		startActivity(intent);		
	}
	
	@Override
	protected void addToPlaylist(Genre selectedItem) {
		getSqueezeService().getPlayer().sendCommand(
				"playlist addtracks genre.id=" + selectedItem.getId());
		Toast.makeText(this, "Added to playlist:\n" + selectedItem.getName(),
				Toast.LENGTH_SHORT).show();	
	}
	
	@Override
	protected void play(Genre selectedItem, int index) {
		getSqueezeService().getPlayer().sendCommand(
				"playlist loadtracks genre.id=" + selectedItem.getId());
	}
	
	@Override
	protected BrowseLoadResult<Genre> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getGenres(getQueryString(), startIndex, count);
	}
}
