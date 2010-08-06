package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.model.Artist;
import com.squeezecontrol.view.BrowseableAdapter;

public class ArtistBrowserActivity extends AbstractMusicBrowserActivity<Artist> {

	public static final String EXTRA_GENRE_ID = "genre_id";
	private String mGenreId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "artist";

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mGenreId = extras.getString(EXTRA_GENRE_ID);
		}

		setContentView(R.layout.artist_list);
		super.init();
	}
	
	@Override
	protected BrowseableAdapter<Artist> createListAdapter() {
		return new BrowseableAdapter<Artist>(this, android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position < 0)
			return;
		Artist selectedItem = (Artist) getListAdapter().getItem(position);
		Intent intent = new Intent(this, AlbumBrowserActivity.class);
		intent.putExtra(AlbumBrowserActivity.EXTRA_ARTIST_ID, selectedItem.getId());
		startActivity(intent);		
	}
	
	@Override
	protected void addToPlaylist(Artist selectedItem) {
		getSqueezeService().getPlayer().sendCommand(
				"playlist addtracks contributor.id=" + selectedItem.getId());
		Toast.makeText(this, "Added to playlist:\n" + selectedItem.getName(),
				Toast.LENGTH_SHORT).show();	
	}
	
	@Override
	protected void play(Artist selectedItem, int index) {
		getSqueezeService().getPlayer().sendCommand(
				"playlist loadtracks contributor.id=" + selectedItem.getId());
	}
	
	@Override
	protected BrowseLoadResult<Artist> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getArtists(getQueryString(), mGenreId, startIndex, count);
	}
}
