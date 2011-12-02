/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.*;
import android.widget.AbsListView.OnScrollListener;
import com.squeezecontrol.image.ImageLoaderService;
import com.squeezecontrol.model.Album;
import com.squeezecontrol.view.BrowseableAdapter;

import java.io.IOException;

public class AlbumBrowserActivity extends AbstractMusicBrowserActivity<Album>
        implements ListView.OnScrollListener {

    public static final String EXTRA_ARTIST_ID = "artist_id";
    public static final String EXTRA_SORT_MODE = "sort_mode";

    public static final String SORT_MODE_ALBUM = "album";
    public static final String SORT_MODE_NEW_MUSIC = "new";

    private String mArtistId;
    private String mSortMode = SORT_MODE_ALBUM;
    private Callback<Bitmap> mImageCallback;
    private ImageLoaderService mCoverImageService;
    private boolean mLoadArt = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTitle = "album";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mArtistId = extras.getString(EXTRA_ARTIST_ID);
            mSortMode = extras.getString(EXTRA_SORT_MODE);
            if (mSortMode == null)
                mSortMode = SORT_MODE_ALBUM;
        }

        setContentView(R.layout.default_browser_list);

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

        getListView().setOnScrollListener(this);

        super.init();
    }

    @Override
    protected void onServiceBound(SqueezeService service) {
        super.onServiceBound(service);
        mCoverImageService = service.getCoverImageService();
    }

    @Override
    protected BrowseableAdapter<Album> createListAdapter() {
        return new BrowseableAdapter<Album>(this, R.layout.album_list_item) {
            @Override
            protected void bindView(int position, View view) {
                Album a = getItem(position);

                TextView albumName = (TextView) view
                        .findViewById(R.id.album_name);
                TextView artistName = (TextView) view
                        .findViewById(R.id.album_artist_name);
                ImageView coverImage = (ImageView) view
                        .findViewById(R.id.album_image);

                if (a == null) {
                    albumName.setText(R.string.loading_progress);
                    artistName.setText("");
                    coverImage.setImageResource(R.drawable.unknown_album_cover);
                } else {
                    if (a.artwork_track_id == null) {
                        coverImage
                                .setImageResource(R.drawable.unknown_album_cover);
                    } else {
                        Bitmap image = mCoverImageService.getFromCache(a);
                        if (image != null) {
                            coverImage.setImageBitmap(image);
                        } else {
                            coverImage
                                    .setImageResource(R.drawable.unknown_album_cover);
                            if (mLoadArt) {
                                mCoverImageService.loadImage(a, mImageCallback);
                            }

                        }
                    }
                    albumName.setText(a.getName());
                    if (getQueryPattern() != null) {
                        Linkify.addLinks(albumName, getQueryPattern(), null);
                    }
                    artistName.setText(a.artistName);
                }
            }
        };
    }


    @Override
    protected int getMenuResource() {
        return R.menu.browse_menu_with_download;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (position < 0)
            return;
        Album selectedItem = (Album) getListAdapter().getItem(position);
        if (selectedItem == null) return;

        Intent intent = new Intent(this, SongBrowserActivity.class);
        intent.putExtra(SongBrowserActivity.EXTRA_ALBUM_ID, selectedItem.id);
        intent.putExtra(SongBrowserActivity.EXTRA_ALBUM_NAME, selectedItem
                .getName());
        if (mArtistId != null)
            intent.putExtra(SongBrowserActivity.EXTRA_ARTIST_ID, mArtistId);
        startActivity(intent);
    }

    @Override
    protected void addToPlaylist(Album selectedItem) {
        getPlayer().sendCommand(
                "playlist addtracks album.id=" + selectedItem.id);
        Toast.makeText(this, "Added to playlist:\n" + selectedItem.getName(),
                Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void download(Album selectedItem) {
        getDownloadService().queueAlbumForDownload(selectedItem);
    }

    @Override
    protected void play(Album selectedItem, int index) {
        getPlayer().playNow(selectedItem);
    }

    protected int getItemCount() {
        return getMusicBrowser().getAlbumCount(getQueryString(), mArtistId);
    }

    @Override
    protected BrowseLoadResult<Album> loadItems(int startIndex, int count)
            throws IOException {

        return getMusicBrowser().getAlbums(getQueryString(), mArtistId,
                startIndex, count, mSortMode);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        super.onScrollStateChanged(view, scrollState);
        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                mLoadArt = true;
                getListView().invalidate();
                break;
            default:
                mLoadArt = false;
                break;
        }
    }
}
