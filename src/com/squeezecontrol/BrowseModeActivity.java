/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.squeezecontrol.util.VolumeKeyHandler;

import java.util.ArrayList;

public class BrowseModeActivity extends ListActivity {
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // TODO Make the ordering user configurable
        ArrayList<Action> modes = new ArrayList<Action>();
        modes.add(new Action("New music", R.drawable.newmusic));
        modes.add(new Action("Artists", R.drawable.artists));
        modes.add(new Action("Albums", R.drawable.albums));
        modes.add(new Action("Songs", R.drawable.songs));
        modes.add(new Action("Genres", R.drawable.genres));
        modes.add(new Action("Internet radio", R.drawable.radio));
        modes.add(new Action("Favorites", R.drawable.favorites));
        modes.add(new Action("Playlists", R.drawable.playlists));
        modes.add(new Action("Apps", R.drawable.plugin));
        modes.add(new Action("Music folder", R.drawable.musicfolder));

        setListAdapter(new ArrayAdapter<Action>(this,
                R.layout.list_item_with_icon, modes) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View view;

                if (convertView == null) {
                    view = mInflater.inflate(R.layout.list_item_with_icon,
                            parent, false);
                } else {
                    view = convertView;
                }

                ImageView icon = (ImageView) view.findViewById(R.id.icon);
                icon.setImageResource(getItem(position).mIcon);

                TextView actionName = (TextView) view
                        .findViewById(R.id.action_name);
                actionName.setText(getItem(position).mName);

                return view;
            }

        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = null;
        switch (position) {
            case 1:
                intent = new Intent(this, ArtistBrowserActivity.class);
                break;
            case 2:
                intent = new Intent(this, AlbumBrowserActivity.class);
                break;
            case 3:
                intent = new Intent(this, SongBrowserActivity.class);
                break;
            case 9:
                intent = new Intent(this, MusicFolderBrowserActivity.class);
                break;
            case 5:
                intent = new Intent(this, AppsAndRadioRootsBrowserActivity.class);
                intent.putExtra(AppsAndRadioRootsBrowserActivity.EXTRA_BROWSER_TYPE,
                		AppsAndRadioRootsBrowserActivity.RADIOS);
                break;
            case 8:
                intent = new Intent(this, AppsAndRadioRootsBrowserActivity.class);
                intent.putExtra(AppsAndRadioRootsBrowserActivity.EXTRA_BROWSER_TYPE,
                		AppsAndRadioRootsBrowserActivity.APPS);
                break;
            case 4:
                intent = new Intent(this, GenreBrowserActivity.class);
                break;
            case 0:
                intent = new Intent(this, AlbumBrowserActivity.class);
                intent.putExtra(AlbumBrowserActivity.EXTRA_SORT_MODE,
                		AlbumBrowserActivity.SORT_MODE_NEW_MUSIC);
                break;
            case 7:
                intent = new Intent(this, PlaylistBrowserActivity.class);
                break;
            case 6:
                intent = new Intent(this, XmlBrowserActivity.class);
                intent.putExtra(XmlBrowserActivity.EXTRA_BROWSER_COMMAND_COMMAND, "favorites");
                intent.putExtra(XmlBrowserActivity.EXTRA_BROWSER_TITLE, "Favorites");
                break;
        }
        if (intent != null)
            startActivity(intent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (VolumeKeyHandler.dispatchKeyEvent(event)) return true;
        else return super.dispatchKeyEvent(event);
    }

    class Action {
        String mName;
        int mIcon;

        Action(String name, int icon) {
            mName = name;
            mIcon = icon;
        }

        void perform() {

        }
    }

}
