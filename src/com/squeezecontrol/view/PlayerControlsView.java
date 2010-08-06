package com.squeezecontrol.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squeezecontrol.R;
import com.squeezecontrol.io.SqueezePlayer;
import com.squeezecontrol.model.Song;

public class PlayerControlsView extends LinearLayout {

	private static final int MAX_SEEK_SECONDS = 30;
	private static final long SEEK_DELAY = 250L;
	
	private SqueezePlayer mPlayer;
	private ImageButton mPauseButton;
	private TextView mCurrentTime;

	public PlayerControlsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		build();
	}

	public PlayerControlsView(Context context) {
		super(context);
		build();
	}

	protected void build() {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.player_controls, this, true);
		
		mCurrentTime = (TextView) findViewById(R.id.CurrentTime);

		mPauseButton = (ImageButton) findViewById(R.id.PauseButton);
		mPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mPlayer.isPaused()) {
					mPlayer.play();
					mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
				} else {
					mPlayer.pause();
					mPauseButton.setImageResource(android.R.drawable.ic_media_play);
				}
			}
		});

		RepeatingImageButton nextButton = (RepeatingImageButton) findViewById(R.id.NextButton);
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.nextSong();
			}
			
		});
		nextButton.setRepeatListener(new RepeatingImageButton.RepeatListener() {
			@Override
			public void onRepeat(View v, long duration, int repeatcount) {
				if (!checkSeeking(repeatcount)) return;
				if (repeatcount == -1) return; // Last
				int seconds = Math.min(MAX_SEEK_SECONDS, repeatcount);
				mPlayer.skip(seconds);
			}
		}, SEEK_DELAY);

		RepeatingImageButton prevButton = (RepeatingImageButton) findViewById(R.id.PreviousButton);
		prevButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.previousSong();
			}
		});
		prevButton.setRepeatListener(new RepeatingImageButton.RepeatListener() {
			@Override
			public void onRepeat(View v, long duration, int repeatcount) {
				if (!checkSeeking(repeatcount)) return;
				if (repeatcount == -1) return; // Last
				int seconds = Math.min(MAX_SEEK_SECONDS, repeatcount);
				mPlayer.skip(-seconds);
			}
		}, SEEK_DELAY);

	}
	
	protected boolean checkSeeking(int repeatcount) {
		Song currentSong = mPlayer.getCurrentSong();
		if (repeatcount == 1 && currentSong.remote) {
			Toast.makeText(getContext(), "Cannot scan in remote streams", Toast.LENGTH_SHORT);
			return false;
		}
		return true;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).setEnabled(enabled);
		}
	}

	public void setPlayer(SqueezePlayer player) {
		this.mPlayer = player;
		updatePlayerState();
	}

	public void updatePlayerState() {
		SqueezePlayer player = mPlayer;
		if (player != null) {
			if (player.isPaused()) {
				mPauseButton.setImageResource(android.R.drawable.ic_media_play);
			} else {
				mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
			}
			// TODO:
			//Long currentTimeInMillis = player.getCurrentTimeInMillis();
			//mCurrentTime.setText(currentTimeInMillis == null ? "-" : FormatUtils.formatAsTime(currentTimeInMillis));
		}
	}

}
