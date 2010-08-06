package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Bundle;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.squeezecontrol.model.Song;
import com.squeezecontrol.view.BrowseableAdapter;

public class SongBrowserActivity extends AbstractMusicBrowserActivity<Song> {

	public static String EXTRA_ALBUM_ID = "album_id";
	public static String EXTRA_ALBUM_NAME = "album_name";
	public static String EXTRA_ARTIST_ID = "artist_id";

	private String mAlbumId = null;
	private String mArtistId = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "song";			

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mAlbumId = extras.getString(EXTRA_ALBUM_ID);
			mArtistId = extras.getString(EXTRA_ARTIST_ID);
		}

		setContentView(R.layout.song_list);
		
		super.init();
	}
	
	@Override
	protected BrowseableAdapter<Song> createListAdapter() {
		return new BrowseableAdapter<Song>(this,
				R.layout.song_list_item) {
			@Override
			protected void bindView(int position, View view) {
				Song s = getItem(position);

				TextView songName = (TextView) view.findViewById(R.id.name);
				TextView artistName = (TextView) view
						.findViewById(R.id.artist_name);
				TextView albumName = (TextView) view
						.findViewById(R.id.album_name);

				if (s == null) {
					songName.setText(R.string.loading_progress);
					artistName.setText("");
					albumName.setText("");
				} else {
					songName.setText(s.getName());
					if (getQueryPattern() != null) {
						Linkify.addLinks(songName, getQueryPattern(), null);
					}
					artistName.setText(s.artist);
					albumName.setText(s.album);
				}

			}
		};
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position < 0)
			return;
		addToPlaylist((Song) l.getItemAtPosition(position));
	}

	@Override
	protected void addToPlaylist(Song selectedItem) {
		getPlayer().addToPlaylist(selectedItem);
		PlayerToasts.addedToPlayList(this, selectedItem);
	}

	@Override
	protected void download(Song selectedItem) {
		getDownloadService().queueSongForDownload(selectedItem);
	}
	
	protected int getMenuResource() {
		return R.menu.browse_menu_with_download;
	}

	@Override
	protected void play(Song selectedItem, int index) {
		getPlayer().playNow(selectedItem);
	}


	@Override
	protected BrowseLoadResult<Song> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getSongs(getQueryString(), mAlbumId, mArtistId,
				startIndex, count);
	}
	

}
