/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.squeezecontrol.model.Favorite;
import com.squeezecontrol.view.BrowseableAdapter;

import java.io.IOException;

public class FavoriteBrowserActivity extends AbstractMusicBrowserActivity<Favorite> {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle = "favorite";
        setContentView(R.layout.default_browser_list);
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
        Favorite item = (Favorite) getListAdapter().getItem(position);
        if (item != null) ;
        addToPlaylist(item);
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
