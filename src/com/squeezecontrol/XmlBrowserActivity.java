/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.squeezecontrol.image.ImageLoaderService;
import com.squeezecontrol.io.SqueezeCommand;
import com.squeezecontrol.io.SqueezeTaggedRequestBuilder;
import com.squeezecontrol.model.XmlBrowserEntry;
import com.squeezecontrol.view.BrowseableAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XmlBrowserActivity extends
        AbstractMusicBrowserActivity<XmlBrowserEntry> {

    public static final String EXTRA_BROWSER_COMMAND_COMMAND = "browser_command";
    public static final String EXTRA_ITEM_ID = "browser_item_id";
    public static final String EXTRA_BROWSER_TITLE = "browser_title";
    public static final String EXTRA_SEARCH_MODE = "browser_search";

    private String mBrowserCommand;
    private String mItemId;
    private Callback<Bitmap> mImageCallback;
    private ImageLoaderService mImageLoaderService;
    private boolean mSearchMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle = "Loading...";
        setContentView(R.layout.default_browser_list);

        mBrowserCommand = getIntent().getStringExtra(
                EXTRA_BROWSER_COMMAND_COMMAND);
        mItemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        mSearchMode = getIntent().getBooleanExtra(EXTRA_SEARCH_MODE, false);

        String title = getIntent().getStringExtra(EXTRA_BROWSER_TITLE);
        if (title != null)
            mTitle = title;

        final Runnable notifyChanges = new Runnable() {
            public void run() {
                getListAdapter().notifyDataSetChanged();
            }

            ;
        };
        mImageCallback = new Callback<Bitmap>() {
            public void handle(Bitmap value) {
                getListView().post(notifyChanges);
            }

            ;
        };

        super.init();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onServiceBound(SqueezeService service) {
        super.onServiceBound(service);
        mImageLoaderService = service.getGenericImageService();
    }

    @Override
    protected BrowseableAdapter<XmlBrowserEntry> createListAdapter() {
        return new BrowseableAdapter<XmlBrowserEntry>(this,
                R.layout.xmlbrowser_list_item) {
            @Override
            protected void bindView(int position, View view) {
                XmlBrowserEntry a = getItem(position);

                ImageView icon = (ImageView) view.findViewById(R.id.icon);
                TextView name = (TextView) view.findViewById(R.id.name);
                icon.setVisibility(View.VISIBLE);
                if (a == null) {
                    name.setText(R.string.loading_progress);
                    icon.setImageResource(R.drawable.unknown_album_cover);
                } else {
                    if (a.icon == null) {
                        if (a.hasItems)
                            icon.setImageResource(R.drawable.musicfolder);
                        else if ("link".equals(a.type))
                            icon.setImageResource(R.drawable.tuneinurl);
                        else if ("text".equals(a.type))
                            icon.setVisibility(View.GONE);
                        else
                            icon.setImageResource(R.drawable.unknown_album_cover);
                    } else {
                        Bitmap image = mImageLoaderService.getFromCache(a.icon);
                        if (image != null) {
                            icon.setImageBitmap(image);
                        } else {
                            icon.setImageResource(R.drawable.unknown_album_cover);
                            mImageLoaderService.loadImage(a.icon,
                                    mImageCallback);

                        }
                    }
                    name.setText(a.name);
                }
            }
        };
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        XmlBrowserEntry item = (XmlBrowserEntry) l.getItemAtPosition(position);
        if (item == null)
            return;

        if (item.hasItems) {

            Intent i = new Intent(this, XmlBrowserActivity.class);
            i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_COMMAND_COMMAND,
                    mBrowserCommand);
            i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_TITLE,
                    item.title == null ? item.name : item.title);
            i.putExtra(XmlBrowserActivity.EXTRA_ITEM_ID, item.id);
            i.putExtra(XmlBrowserActivity.EXTRA_SEARCH_MODE,
                    "search".equals(item.type));
            startActivity(i);
        } else {
            getPlayer().sendCommand(
                    new SqueezeTaggedRequestBuilder(mBrowserCommand
                            + " playlist play").addTag("item_id", item.id)
                            .toString());
        }
    }

    @Override
    protected CharSequence getTitle(int totalCount) {
        return mTitle;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        // No popup
    }

    @Override
    protected void addToPlaylist(XmlBrowserEntry item) {
        // Not in use
    }

    @Override
    protected void play(XmlBrowserEntry item, int index) {
        // Not in use
    }

    @Override
    protected BrowseLoadResult<XmlBrowserEntry> loadItems(int startIndex,
                                                          int pageSize) throws IOException {
        ArrayList<XmlBrowserEntry> entries = new ArrayList<XmlBrowserEntry>(
                pageSize);
        int count = 0;
        try {
            SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
                    mBrowserCommand + " items " + startIndex + " " + pageSize);
            if (mItemId != null)
                command.addTag("item_id", mItemId);
            boolean hasQuery = getQueryString() != null
                    && !"".equals(getQueryString());
            if (mSearchMode) {
                if (!hasQuery) {
                    XmlBrowserEntry e = new XmlBrowserEntry();
                    e.type = "text";
                    e.name = "Enter search criteria";
                    entries.add(e);
                    count = 1;
                    return new BrowseLoadResult<XmlBrowserEntry>(count,
                            startIndex, entries);
                }
            }

            // All items have search, but search-services _need_ a query (hence
            // the check above)
            if (hasQuery) {
                command.addTag("search", getQueryString());
            }

            SqueezeCommand res = getPlayer().sendRequest(command.toString());

            XmlBrowserEntry entry = null;
            List<Map<String, String>> maps = res.splitToMap("id");
            for (Map<String, String> m : maps) {
                entry = new XmlBrowserEntry();
                entry.id = m.get("id");
                entry.name = m.get("name");
                entry.title = m.get("title");
                entry.type = m.get("type");
                entry.hasItems = "1".equals(m.get("hasitems"));
                entry.isAudio = "1".equals(m.get("isaudio"));
                entry.url = m.get("url");
                entry.icon = m.get("image");

                entries.add(entry);

                if (m.containsKey("count"))
                    count = Integer.parseInt(m.get("count"));
            }
        } catch (IOException e) {
        }
        return new BrowseLoadResult<XmlBrowserEntry>(count, startIndex, entries);
    }
}
