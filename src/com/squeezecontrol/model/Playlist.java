package com.squeezecontrol.model;

public class Playlist implements Browsable {
	public String id;
	public String name;

	public Playlist() {
		
	}
	
	public Playlist(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
