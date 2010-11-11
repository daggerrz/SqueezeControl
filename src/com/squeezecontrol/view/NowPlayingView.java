package com.squeezecontrol.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.squeezecontrol.PlayerActivity;
import com.squeezecontrol.R;
import com.squeezecontrol.SqueezeService;
import com.squeezecontrol.io.SqueezePlayerListener;
import com.squeezecontrol.model.Song;

/**
 * TODO: Listen to player changes
 * @author liodden
 *
 */
public class NowPlayingView implements SqueezePlayerListener {

	private SqueezeService mService;
	private Activity mActivity;
	private View mNowPlayingView;
	private TextView mTitle;
	private TextView mArtist;

	public NowPlayingView(Activity a, SqueezeService service) {
		mService = service;
		mActivity = a;
		mNowPlayingView = a.findViewById(R.id.nowplaying);
		mTitle = (TextView) mNowPlayingView.findViewById(R.id.title);
		mArtist = (TextView) mNowPlayingView.findViewById(R.id.artist);

		mNowPlayingView.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Context c = v.getContext();
				c.startActivity(new Intent(c, PlayerActivity.class));
			}
		});

		if (service.getPlayer() != null) {
			service.getPlayer().addListener(this);
			setSong(service.getPlayer().getCurrentSong());
		}
	}

	protected void setSong(Song newSong) {
		if (mNowPlayingView == null) {
			return;
		}
		Song song = mService.getPlayer().getCurrentSong();
		if (song != null) {
			mTitle.setText(song.title);
			mArtist.setText(song.artist);
			mNowPlayingView.setVisibility(View.VISIBLE);
			return;
		} else {
			mNowPlayingView.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onSongChanged(final Song newSong) {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setSong(newSong);
			}
		});
	}

	@Override
	public void onPlayerStateChanged() {
		// TODO Auto-generated method stub

	}

}
