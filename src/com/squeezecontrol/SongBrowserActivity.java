/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.os.Bundle;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.squeezecontrol.model.Song;
import com.squeezecontrol.view.BrowseableAdapter;

import java.io.IOException;

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

        setContentView(R.layout.default_browser_list);

        super.init();
    }

    @Override
    protected BrowseableAdapter<Song> createListAdapter() {
        return new BrowseableAdapter<Song>(this, R.layout.song_list_item) {
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
    protected void addContextMenuItems(ContextMenu menu) {
    	Song selectedItem = (Song) getSelectedItem();
		menu.add(0, ARTIST_CTX_MENU_ITEM, 1, "Artist: " + selectedItem.artist);
		menu.add(0, ALBUM_CTX_MENU_ITEM, 1, "Album: " + selectedItem.album);
		menu.add(0, DOWNLOAD_CTX_MENU_ITEM, 1, "Download to device");
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (position < 0)
            return;
        Song item = (Song) l.getItemAtPosition(position);
        if (item == null)
            return;
        addToPlaylist(item);
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

    @Override
    protected void play(Song selectedItem, int index) {
        getPlayer().playNow(selectedItem);
    }

    @Override
    protected BrowseLoadResult<Song> loadItems(int startIndex, int count)
            throws IOException {
        return getMusicBrowser().getSongs(getQueryString(), mAlbumId,
                mArtistId, startIndex, count);
    }

}
