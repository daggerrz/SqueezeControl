package com.squeezecontrol;

import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.model.FolderItem;
import com.squeezecontrol.model.Playlist;
import com.squeezecontrol.model.Song;
import com.squeezecontrol.view.BrowseableAdapter;

public class MusicFolderBrowserActivity extends
		AbstractMusicBrowserActivity<FolderItem> {

	public static final String EXTRA_FOLDER_ID = "folder_id";
	private String mFolderId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTitle = "item";
		setContentView(R.layout.default_browser_list);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mFolderId = extras.getString(EXTRA_FOLDER_ID);
		}
		super.init();
	}

	@Override
	protected BrowseableAdapter<FolderItem> createListAdapter() {
		return new BrowseableAdapter<FolderItem>(this,
				android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position < 0)
			return;
		FolderItem selectedItem = (FolderItem) getListAdapter().getItem(
				position);
		if (selectedItem == null) return;
		
		if (FolderItem.FOLDER.equals(selectedItem.type)) {
		Intent intent = new Intent(this, MusicFolderBrowserActivity.class);
		intent.putExtra(MusicFolderBrowserActivity.EXTRA_FOLDER_ID,
				selectedItem.getId());
		startActivity(intent);
		} else {
			addToPlaylist(selectedItem);
		}
	}

	@Override
	protected void addToPlaylist(FolderItem selectedItem) {
		String t = selectedItem.type;
		if (FolderItem.FOLDER.equals(t)) {
			getPlayer().sendCommand(
					"playlistcontrol cmd:add folder_id%3A"
							+ selectedItem.getId());
		} else if (FolderItem.TRACK.equals(t)) {
			getPlayer().addToPlaylist(new Song(selectedItem.id));
		} else if (FolderItem.PLAYLIST.equals(t)) {
			getPlayer().addToPlaylist(new Playlist(selectedItem.id));
		} else {
			Toast.makeText(this,
					"Don't know how to add unknown type to playlist",
					Toast.LENGTH_SHORT);
			return;
		}
		PlayerToasts.addedToPlayList(this, selectedItem);
	}

	@Override
	protected void play(FolderItem selectedItem, int index) {
		String t = selectedItem.type;
		if (FolderItem.FOLDER.equals(t))
			getPlayer().sendCommand(
					"playlistcontrol cmd:load folder_id%3A"
							+ selectedItem.getId());
		else if (FolderItem.TRACK.equals(t)) {
			getPlayer().playNow(new Song(selectedItem.id));
		} else if (FolderItem.PLAYLIST.equals(t)) {
			getPlayer().playNow(new Playlist(selectedItem.id));
		} else
			Toast.makeText(this,
					"Don't know how to play unknown type",
					Toast.LENGTH_SHORT);

	}

	@Override
	protected BrowseLoadResult<FolderItem> loadItems(int startIndex, int count)
			throws IOException {
		return getMusicBrowser().getFolderContents(getQueryString(), mFolderId,
				startIndex, count);
	}
}
