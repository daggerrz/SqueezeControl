/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.io;

import com.squeezecontrol.model.Song;

public interface SqueezePlayerListener {

    void onSongChanged(Song newSong);

    void onPlayerStateChanged();
}
