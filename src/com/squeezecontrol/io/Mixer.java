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
