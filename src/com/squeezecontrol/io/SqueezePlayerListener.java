package com.squeezecontrol.io;

import com.squeezecontrol.model.Song;

public interface SqueezePlayerListener {
	
	void onSongChanged(Song newSong);
	void onPlayerStateChanged();
}
