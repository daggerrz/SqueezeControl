package com.squeezecontrol;

import java.io.IOException;

import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.squeezecontrol.io.SqueezePlayer;
import com.squeezecontrol.io.SqueezePlayerListener;
import com.squeezecontrol.model.Playlist;
import com.squeezecontrol.model.Song;
import com.squeezecontrol.view.BrowseableAdapter;

public class CurrentPlaylistBrowserActivity extends
		AbstractMusicBrowserActivity<Song> implements SqueezePlayerListener {

	private Handler mHandler = new Handler();
	private int mCurrentPosition;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTitle = "playlist item";

		setContentView(R.layout.current_playlist);
		super.init();

	}

	@Override
	protected BrowseableAdapter<Song> createListAdapter() {
		return new BrowseableAdapter<Song>(this, R.layout.current_playlist_item) {
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
					if (position == getSqueezeService().getPlayer()
							.getSongIndexInPlaylist()) {
						songName.setText("* " + s.getName());
					} else {
						songName.setText(s.getName());
					}
					artistName.setText(s.artist);
					albumName.setText(s.album);
				}
			}
		};
	}

	@Override
	protected void onServiceBound(SqueezeService service) {
		service.getPlayer().addListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		SqueezePlayer player = getPlayer();
		if (player != null)
			player.removeListener(this);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position < 0)
			return;
		getPlayer().setSongIndexInPlayList(position);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.current_playlist_list_menu, menu);

		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		mCurrentPosition = mi.position;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.remove) {
			getPlayer().removeFromPlaylist(mCurrentPosition);
			getListAdapter().remove(mCurrentPosition);
		}
		return true;
	}

	@Override
	protected BrowseLoadResult<Song> loadItems(int startIndex, int count)
			throws IOException {
		return getPlayer().getSongsInCurrentPlaylist(
				startIndex, count);
	}

	@Override
	protected void onInitialResultLoaded(BrowseLoadResult<Song> result) {
		if (getPlayer().getSongIndexInPlaylist() > 0) {
			getListView().setSelection(getPlayer().getSongIndexInPlaylist());
		}
	}

	@Override
	public void onSongChanged(Song newSong) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				getListAdapter().notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onPlayerStateChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void addToPlaylist(Song selectedItem) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void play(Song selectedItem, int index) {
		// TODO Auto-generated method stub

	}

}
