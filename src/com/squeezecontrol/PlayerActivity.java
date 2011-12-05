/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.squeezecontrol.io.SqueezeBroker;
import com.squeezecontrol.io.SqueezeEventListener;
import com.squeezecontrol.io.SqueezePlayer;
import com.squeezecontrol.io.SqueezePlayer.ShuffleMode;
import com.squeezecontrol.io.SqueezePlayerListener;
import com.squeezecontrol.model.Song;
import com.squeezecontrol.util.VolumeKeyHandler;
import com.squeezecontrol.view.PlayerControlsView;

import java.io.IOException;

public class PlayerActivity extends Activity implements View.OnTouchListener,
        View.OnLongClickListener, SqueezePlayerListener, SqueezeEventListener {

    private static final String TAG = "PlayerActivity";

    private static final int MENU_PLAYERS = 0;
    private static final int MENU_LIBRARY = 1;
    private static final int MENU_CONNECT = 3;
    private static final int MENU_ADD_FAVORITE = 4;
    private static final int MENU_PREFERENCES = 5;
    private static final int MENU_POWER_ON_OFF = 6;
    private static final int MENU_DOWNLOAD_SONG = 7;
    private static final int MENU_SHUFFLE = 8;

    public static final String EXTRA_PLAYER_ID = "playerId";
    public static final int PICK_PLAYER = 1;

    private SqueezeBroker mBroker;
    private SqueezeService mService;
    private PlayerControlsView playerControls;
    private SqueezePlayer mPlayer;
    private Handler guiHandler;
    private View mSongInfoContainer;
    private TextView mSongDescription;
    private TextView mArtistName;
    private ImageView mCoverImageView;
    private TextView mAlbumName;

    private Animation mFadeOut;
    private Animation mFadeIn;

    private Song mCurrentSong;

    private Runnable mPlayerStateUpdater;

    private MenuItem mMenuPower;
    private MenuItem mMenuDownload;
    private MenuItem mMenuFavorite;

    private MenuItem mMenuShuffle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Settings.isConfigured(this)) {
            Toast.makeText(
                    this,
                    "Please start by configuring your SqueezeCenter Server settings!",
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        } else {

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.main);

            guiHandler = new Handler();
            mPlayerStateUpdater = new Runnable() {
                @Override
                public void run() {
                    updatePlayerControlsView();
                }
            };
            ServiceUtils.requireValidNetworkOrFinish(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Return from settings with faulty settings?
        if (!Settings.isConfigured(this)) {
            finish();
        } else {
            ServiceUtils.bindToService(this, new SqueezeServiceConnection() {

                @Override
                public void onServiceConnected(SqueezeService service) {
                    mService = service;
                    setBroker(service.getBroker());
                    init();
                    setPlayer(service.getPlayer());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null)
            mPlayer.removeListener(this);
        if (mBroker != null)
            mBroker.removeEventListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ServiceUtils.unbindFromService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Settings.isNewVersionInstalled(this)) {
            showChangeLog();
        }
        if (mBroker != null) {
            mBroker.addEventListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBroker != null) {
            mBroker.removeEventListener(this);
        }
    }

    protected void init() {
        // Animation
        mFadeOut = AnimationUtils.loadAnimation(PlayerActivity.this,
                android.R.anim.fade_out);
        mFadeIn = AnimationUtils.loadAnimation(PlayerActivity.this,
                android.R.anim.fade_in);

        ImageButton incVolumeButton = (ImageButton) findViewById(R.id.IncVolumeButton);
        ImageButton decVolumeButton = (ImageButton) findViewById(R.id.DecVolumeButton);

        ImageButton browseButton = (ImageButton) findViewById(R.id.browse);

        mCoverImageView = (ImageView) findViewById(R.id.CoverImage);

        browseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PlayerActivity.this,
                        BrowseModeActivity.class));
            }
        });

        ImageButton playlistButton = (ImageButton) findViewById(R.id.playlist);
        playlistButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PlayerActivity.this,
                        CurrentPlaylistBrowserActivity.class));
            }
        });

        incVolumeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.getMixer().increaseVolume();
            }
        });

        decVolumeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.getMixer().decreaseVolume();
            }
        });

        playerControls = (PlayerControlsView) findViewById(R.id.PlayerControls);

        mSongInfoContainer = findViewById(R.id.SongInfo);
        mSongDescription = (TextView) findViewById(R.id.SongDescription);
        mArtistName = (TextView) findViewById(R.id.ArtistName);
        mAlbumName = (TextView) findViewById(R.id.AlbumName);

        View v = (View) mSongDescription.getParent();
        v.setOnTouchListener(this);
        // FIXME We're not doing anything with this long click...
        v.setOnLongClickListener(this);

        v = (View) mArtistName.getParent();
        v.setOnTouchListener(this);
        // TODO Consider having a click do the same thing.  Might break drag scrolling...
        v.setOnLongClickListener(this);

        v = (View) mAlbumName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);
    }

    private void setBroker(SqueezeBroker broker) {
        mBroker = broker;
        broker.addEventListener(this);
    }

    public void onConnect(SqueezeBroker broker) {
        playerControls.setEnabled(true);
        SqueezeService.showConnectionNotification(this, broker);
    }

    @Override
    public void onConnectionError(SqueezeBroker broker, IOException cause) {
        SqueezeService.showConnectionError(this, broker, cause);
        playerControls.setEnabled(false);
    }

    @Override
    public void onDisconnect(SqueezeBroker broker) {
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return ServiceUtils.createWaitScreen(this);
    }

    private void setPlayer(SqueezePlayer player) {
        if (mPlayer != null) {
            mPlayer.removeListener(this);
        }
        mPlayer = player;
        if (mPlayer != null) {
            mPlayer.addListener(this);
            playerControls.setPlayer(mPlayer);

            setSong(mPlayer.getCurrentSong());
        }
    }

    @Override
    public void onSongChanged(final Song newSong) {
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                setSong(newSong);
            }
        });
    }

    @Override
    public void onPlayerStateChanged() {
        guiHandler.post(mPlayerStateUpdater);
    }

    protected void updatePlayerControlsView() {
        playerControls.updatePlayerState();
    }

    protected void setSong(final Song newSong) {

        // Same song?
        if ((mCurrentSong != null) && (mCurrentSong.id != null)
                && (newSong != null) && mCurrentSong.id.equals(newSong.id))
            return;

        // Don't animate if we don't have a song already
        if (mCurrentSong == null) {
            updateSongFields(newSong);
        } else {
            mFadeOut.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    updateSongFields(newSong);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

                @Override
                public void onAnimationStart(Animation animation) {

                }
            });
            mSongInfoContainer.startAnimation(mFadeOut);
        }
        loadCoverImage();

    }

    protected void updateSongFields(Song newSong) {
        if (newSong == null) {
            newSong = Song.EMPTY;
        }
        mCurrentSong = newSong;
        mSongDescription.setText(newSong.title);
        mArtistName.setText(newSong.artist);
        mAlbumName.setText(newSong.album);
        mSongDescription.startAnimation(mFadeIn);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // menu.add(0, MENU_LIBRARY, 0, "Library");
        mMenuFavorite = menu.add(0, MENU_ADD_FAVORITE, 0, "Mark as favorite")
                .setIcon(R.drawable.ic_menu_favorite);

        mMenuShuffle = menu.add(0, MENU_SHUFFLE, 0, "Shuffle mode").setIcon(
                R.drawable.ic_menu_shuffle);

        mMenuDownload = menu.add(0, MENU_DOWNLOAD_SONG, 0, "Save to phone")
                .setIcon(android.R.drawable.ic_menu_save);

        menu.add(0, MENU_PLAYERS, 0, "Select player").setIcon(
                android.R.drawable.ic_menu_agenda);
        mMenuPower = menu.add(0, MENU_POWER_ON_OFF, 0, "Power on / off")
                .setIcon(android.R.drawable.ic_lock_power_off);
        menu.add(0, MENU_PREFERENCES, 0, "Preferences").setIcon(
                android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean hasPlayer = mPlayer != null;
        mMenuPower.setEnabled(hasPlayer);
        mMenuPower.setEnabled(hasPlayer);
        mMenuShuffle.setEnabled(hasPlayer);

        boolean hasSong = hasPlayer && mCurrentSong != null
                && mCurrentSong != Song.EMPTY;
        mMenuDownload.setEnabled(hasSong);
        mMenuFavorite.setEnabled(hasSong);

        if (hasPlayer) {
            if (mPlayer.isPowerOn())
                mMenuPower.setTitle("Power off");
            else
                mMenuPower.setTitle("Power on");

        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case MENU_PLAYERS:
                selectPlayer();
                return true;
            case MENU_LIBRARY:
                startActivity(new Intent(this, LibraryInfoActivity.class));
                return true;
            case MENU_POWER_ON_OFF:
                mPlayer.togglePower();
                return true;
            case MENU_CONNECT:
                if (mBroker.isConnected())
                    mBroker.disconnect();
                else
                    mBroker.connect();
                return true;
            case MENU_ADD_FAVORITE:
                try {
                    Song song = mPlayer.getCurrentSong();
                    mBroker.getMusicBrowser().flagAsFavorite(song);
                    Toast.makeText(PlayerActivity.this,
                            "Added as favorite:\n" + song.title, Toast.LENGTH_SHORT)
                            .show();
                } catch (IOException e) {
                }
                return true;
            case MENU_DOWNLOAD_SONG:
                mService.getDownloadService().queueSongForDownload(mCurrentSong);
                return true;
            case MENU_PREFERENCES:
                setup();
                return true;
            case MENU_SHUFFLE:
                selectShuffleMode();
                return true;
            default:
                return false;
        }
    }

    private void selectShuffleMode() {
        final CharSequence[] items = {"None", "By song", "By album"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shuffle mode");
        ShuffleMode currentMode = mPlayer.getShuffleMode();
        builder.setSingleChoiceItems(items, currentMode == null ? -1
                : currentMode.ordinal(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ShuffleMode m = null;
                switch (item) {
                    case 0:
                        m = ShuffleMode.NONE;
                        break;
                    case 1:
                        m = ShuffleMode.BY_SONG;
                        break;
                    case 2:
                        m = ShuffleMode.BY_ALBUM;
                        break;
                }
                mPlayer.setShuffleMode(m);
                dialog.dismiss();
                Toast.makeText(getApplicationContext(),
                        "Shuffle mode: " + items[item], Toast.LENGTH_SHORT)
                        .show();
            }
        });
        AlertDialog dlg = builder.create();
        dlg.show();
    }

    private void setup() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void selectPlayer() {
        Intent intent = new Intent(this, BrowsePlayersActivity.class);
        startActivityForResult(intent, PICK_PLAYER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_PLAYER:
                if (resultCode == Activity.RESULT_OK && mBroker != null) {
                    String playerId = data.getExtras().getString(EXTRA_PLAYER_ID);
                    SqueezePlayer newPlayer = mBroker.getPlayer(playerId);
                    setPlayer(newPlayer);
                    mService.setPlayer(newPlayer);
                }
                break;
        }
    }

    protected void loadCoverImage() {
        final Song currentSong = mPlayer.getCurrentSong();
        if (currentSong != Song.EMPTY && currentSong != null) {
            mService.getCoverImageService().loadImage(currentSong,
                    mCoverImageView);
        }
    }

    // / From MediaPlaybackActivity

    int mInitialX = -1;
    int mLastX = -1;
    int SLOP = ViewConfiguration.getTouchSlop();
    int mTextWidth = 0;
    int mViewWidth = 0;
    boolean mDraggingLabel = false;

    TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.ArtistName);
        if (vv != null)
            return (TextView) vv;
        vv = v.findViewById(R.id.AlbumName);
        if (vv != null)
            return (TextView) vv;
        vv = v.findViewById(R.id.SongDescription);
        if (vv != null)
            return (TextView) vv;
        return null;
    }

    Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            v.setBackgroundColor(0xff606060);
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
        } else if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(0);
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the
                        // right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
                }
                return true;
            }
            int delta = mInitialX - (int) event.getX();
            if (Math.abs(delta) > SLOP) {
                // start moving
                mLabelScroller.removeMessages(0, tv);

                // Only turn ellipsizing off when it's not already off, because
                // it
                // causes the scroll position to be reset to 0.
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                // layout might be null if the text just changed, or ellipsizing
                // was just turned off
                if (ll == null) {
                    return false;
                }
                // get the non-ellipsized line width, to determine whether
                // scrolling
                // should even be allowed
                mTextWidth = (int) tv.getLayout().getLineWidth(0);
                mViewWidth = tv.getWidth();
                if (mViewWidth > mTextWidth) {
                    tv.setEllipsize(TruncateAt.END);
                    v.cancelLongPress();
                    return false;
                }
                mDraggingLabel = true;
                tv.setHorizontalFadingEdgeEnabled(true);
                v.cancelLongPress();
                return true;
            }
        }
        return false;
    }

    public boolean onLongClick(View view) {
        Song currentSong = mCurrentSong;
        if (view.equals(mArtistName.getParent()) && currentSong != null
                && currentSong.artistId != null) {
            Intent intent = new Intent(this, AlbumBrowserActivity.class);
            intent.putExtra(AlbumBrowserActivity.EXTRA_ARTIST_ID,
                    mPlayer.getCurrentSong().artistId);
            startActivity(intent);
        } else if (view.equals(mAlbumName.getParent()) && currentSong != null
                && currentSong.albumId != null) {
            Intent intent = new Intent(this, SongBrowserActivity.class);
            intent.putExtra(SongBrowserActivity.EXTRA_ALBUM_ID,
                    mPlayer.getCurrentSong().albumId);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (VolumeKeyHandler.dispatchKeyEvent(event)) return true;
        else return super.dispatchKeyEvent(event);
    }

    private void showChangeLog() {
        WebView wv = new WebView(this);

        WebSettings webSettings = wv.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);

        wv.loadUrl("file:///android_asset/changelog.html");
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setView(wv);
        b.setPositiveButton("Close", null);
        b.create().show();

    }
}