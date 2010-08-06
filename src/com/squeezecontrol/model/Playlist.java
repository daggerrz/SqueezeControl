package com.squeezecontrol.model;

public class Playlist implements Browsable {
	public String id;
	public String name;

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
