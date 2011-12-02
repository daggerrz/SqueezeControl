/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.io;

public class Mixer {

    private SqueezePlayer player;

    public Mixer(SqueezePlayer squeezePlayer) {
        this.player = squeezePlayer;
    }

    public void increaseVolume() {
        player.sendCommand("mixer volume +5");
    }

    public void decreaseVolume() {
        player.sendCommand("mixer volume -5");
    }

}
