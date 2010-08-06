package com.squeezecontrol.model;


public class Song implements Browsable {

	public static final Song EMPTY = Song.forName("-");
	static {
		EMPTY.artist = "-";
		EMPTY.album = "-";
		EMPTY.genre = "-";
	}
	public String id;
	public String title;
	public  String genre;
	public String artist;
	public String artistId;
	public String album;
	public String albumId;
	public String path;
	public int rating = -1;
	public String bitrate = null;
	public Long duration = null; // Duration in milliseconds
	public String type; // Content type / extension
	public boolean remote = false;

	public Song(String path) {
		this.path = path;
	}

	public Song() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return title + " (" + artist + ")";
	}

	@Override
	public final String getName() {
		return title;
	}


	public static Song forName(String name) {
		Song s = new Song(null);
		s.title = name;
		return s;
	}

	@Override
	public String toString() {
		return title;
	}
}
