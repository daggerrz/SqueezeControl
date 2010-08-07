package com.squeezecontrol.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.net.Uri;
import android.util.Log;

import com.squeezecontrol.BrowseLoadResult;
import com.squeezecontrol.model.Album;
import com.squeezecontrol.model.Artist;
import com.squeezecontrol.model.Favorite;
import com.squeezecontrol.model.FolderItem;
import com.squeezecontrol.model.Genre;
import com.squeezecontrol.model.Playlist;
import com.squeezecontrol.model.RadioStation;
import com.squeezecontrol.model.Song;

public class MusicBrowser {
	private SqueezeBroker mBroker;

	public MusicBrowser(SqueezeBroker broker) {
		mBroker = broker;
	}

	private int count(String command) {
		if (mBroker == null)
			return 0;

		SqueezeCommand res;
		try {
			res = mBroker.sendRequest(command);
			String countString = res.getParameterMap().get("count");
			return Integer.parseInt(countString);
		} catch (IOException e) {
			return 0;
		}

	}

	public int getAlbumCount(String searchString, String artistId) {
		String command = "albums 0 0";
		if (searchString != null && !"".equals(searchString))
			command += " search%3A" + Uri.encode(searchString);
		if (artistId != null)
			command += " artist_id%3A" + artistId;
		return count(command);
	}

	public BrowseLoadResult<Album> getAlbums(String searchString,
			String artistId, int startIndex, int pageSize, String sortMode) {
		ArrayList<Album> albums = new ArrayList<Album>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"albums " + startIndex + " " + pageSize);
			if (searchString != null && !"".equals(searchString))
				command.addTag("search", Uri.encode(searchString));
			command.addTag("artist_id", artistId);
			command.addTag("tags", "alj");
			command.addTag("sort", sortMode == null ? "album" : sortMode);
			SqueezeCommand res = mBroker.sendRequest(command.toString());
			Album album = null;
			for (ArrayList<String> albumRow : res.splitParameters("id")) {
				Map<String, String> albumMap = SqueezeCommand
						.splitToParameterMap(albumRow);
				album = new Album();
				album.id = albumMap.get("id");
				album.name = albumMap.get("album");
				album.artistName = albumMap.get("artist");
				album.artwork_track_id = albumMap.get("artwork_track_id");
				if (albumMap.containsKey("count"))
					count = Integer.parseInt(albumMap.get("count"));
				albums.add(album);
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<Album>(count, startIndex, albums);
	}

	public int getSongCount(String searchString, String albumId) {
		String command = "songs 0 0";
		if (searchString != null && !"".equals(searchString))
			command += " search%3A" + Uri.encode(searchString);
		if (albumId != null)
			command += " album_id%3A" + albumId;
		return count(command);
	}

	public BrowseLoadResult<Song> getSongs(String searchString, String albumId,
			String artistId, int startIndex, int pageSize) {
		ArrayList<Song> songs = new ArrayList<Song>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"songs " + startIndex + " " + pageSize);
			if (searchString != null && !"".equals(searchString))
				command.addTag("search", Uri.encode(searchString));
			command.addTag("album_id", albumId);
			// Sort by track num if filtered on album
			if (albumId != null)
				command.addTag("sort", "tracknum");
			command.addTag("artist_id", artistId);

			// a:artistname, l:album name, r:bitrate, d:duration, o:type,
			// x:remote
			command.addTag("tags", "alrdox");
			SqueezeCommand res = mBroker.sendRequest(command.toString());
			Song song = null;
			for (String p : res.getParameters()) {
				if (p.startsWith("title%3A")) {
					String title = SqueezeCommand.decode(p.substring("title%3A"
							.length()));
					song.title = title;
				} else if (p.startsWith("album%3A")) {
					String album = SqueezeCommand.decode(p.substring("title%3A"
							.length()));
					song.album = album;
				} else if (p.startsWith("artist%3A")) {
					String artist = SqueezeCommand.decode(p
							.substring("artist%3A".length()));
					song.artist = artist;
				} else if (p.startsWith("id%3A")) {
					song = Song.forName(null);
					song.setId(p.substring("id%3A".length()));
					songs.add(song);
				} else if (p.startsWith("bitrate%3A")) {
					song.bitrate = SqueezeCommand.decode(p
							.substring("bitrate%3A".length()));
				} else if (p.startsWith("duration%3A")) {
					String durationString = SqueezeCommand.decode(p
							.substring("duration%3A".length()));
					// Duration in seconds with decimals. Skip decimals
					song.duration = SqueezeCommand.parseTime(durationString);
				} else if (p.startsWith("remote%3a1")) { // 1 -> Remote = true
					song.remote = true;
				} else if (p.startsWith("type%3A")) {
					song.type = p.substring("type%3A".length());
				} else if (p.startsWith("count%3A")) {
					count = Integer.parseInt(p.substring("count%3A".length()));
				}
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<Song>(count, startIndex, songs);
	}

	public int getAristCount(String searchString) {
		String command = "artists 0 0";
		if (searchString != null && !"".equals(searchString))
			command += " search%3A" + Uri.encode(searchString);
		return count(command);
	}

	public BrowseLoadResult<Artist> getArtists(String searchString,
			String genreId, int startIndex, int pageSize) {
		ArrayList<Artist> artists = new ArrayList<Artist>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"artists " + startIndex + " " + pageSize);

			if (searchString != null && !"".equals(searchString))
				command.addTag("search", Uri.encode(searchString));
			if (genreId != null)
				command.addTag("genre_id", genreId);

			SqueezeCommand res = mBroker.sendRequest(command.toString());
			Artist artist = null;
			for (String p : res.getParameters()) {
				if (p.startsWith("artist%3A")) {
					String title = SqueezeCommand.decode(p
							.substring("artist%3A".length()));
					artist.setName(title);
				} else if (p.startsWith("id%3A")) {
					artist = new Artist();
					artist.setId(p.substring("id%3A".length()));
					artists.add(artist);
				} else if (p.startsWith("count%3A")) {
					count = Integer.parseInt(p.substring("count%3A".length()));
				}
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<Artist>(count, startIndex, artists);
	}

	public BrowseLoadResult<Genre> getGenres(String searchString,
			int startIndex, int pageSize) {
		ArrayList<Genre> artists = new ArrayList<Genre>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"genres " + startIndex + " " + pageSize);

			if (searchString != null && !"".equals(searchString))
				command.addTag("search", Uri.encode(searchString));

			SqueezeCommand res = mBroker.sendRequest(command.toString());
			Genre artist = null;
			for (String p : res.getParameters()) {
				if (p.startsWith("genre%3A")) {
					String title = SqueezeCommand.decode(p.substring("genre%3A"
							.length()));
					artist.setName(title);
				} else if (p.startsWith("id%3A")) {
					artist = new Genre();
					artist.setId(p.substring("id%3A".length()));
					artists.add(artist);
				} else if (p.startsWith("count%3A")) {
					count = Integer.parseInt(p.substring("count%3A".length()));
				}
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<Genre>(count, startIndex, artists);
	}

	public int getPlaylistCount(String searchString) {
		String command = "playlists 0 0";
		if (searchString != null && !"".equals(searchString))
			command += " search%3A" + Uri.encode(searchString);
		return count(command);
	}

	public BrowseLoadResult<Playlist> getPlaylists(String searchString,
			int startIndex, int pageSize) {
		ArrayList<Playlist> lists = new ArrayList<Playlist>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"playlists " + startIndex + " " + pageSize);

			if (searchString != null && !"".equals(searchString))
				command.addTag("search", Uri.encode(searchString));

			SqueezeCommand res = mBroker.sendRequest(command.toString());
			Playlist pl = null;
			for (String p : res.getParameters()) {
				if (p.startsWith("playlist%3A")) {
					String title = SqueezeCommand.decode(p
							.substring("playlist%3A".length()));
					pl.name = title;
				} else if (p.startsWith("id%3A")) {
					pl = new Playlist();
					pl.id = p.substring("id%3A".length());
					lists.add(pl);
				} else if (p.startsWith("count%3A")) {
					count = Integer.parseInt(p.substring("count%3A".length()));
				}
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<Playlist>(count, startIndex, lists);
	}

	public BrowseLoadResult<FolderItem> getFolderContents(String queryString,
			String folderId, int startIndex, int pageSize) {
		ArrayList<FolderItem> items = new ArrayList<FolderItem>();
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"musicfolder " + startIndex + " " + pageSize);

			if (folderId != null)
				command.addTag("folder_id", folderId);

			if (queryString != null && !"".equals(queryString))
				command.addTag("search", Uri.encode(queryString));

			SqueezeCommand res = mBroker.sendRequest(command.toString());
			FolderItem item = new FolderItem();
			for (String p : res.getParameters()) {
				if (p.startsWith("filename%3A")) {
					item.name = SqueezeCommand.decode(p.substring("filename%3A"
							.length()));
				} else if (p.startsWith("id%3A")) {
					item = new FolderItem();
					items.add(item);
					item.id = p.substring("id%3A".length());
				} else if (p.startsWith("type%3A")) {
					item.type = p.substring("type%3A".length());
				} else if (p.startsWith("count%3A")) {
					count = Integer.parseInt(p.substring("count%3A".length()));
				}
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<FolderItem>(count, startIndex, items);
	}

	public BrowseLoadResult<RadioStation> getRadios(String searchString,
			int startIndex, int pageSize) {
		ArrayList<RadioStation> radios = new ArrayList<RadioStation>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"radios " + startIndex + " " + pageSize);
			if (searchString != null && !"".equals(searchString))
				command.addTag("search", Uri.encode(searchString));
			SqueezeCommand res = mBroker.sendRequest(command.toString());

			// Count comes first here
			for (String c : res.getParameters()) {
				if (c.startsWith("count%3A")) {
					count = Integer.parseInt(c.substring("count%3A".length()));
					break;
				}
			}
			
			RadioStation radio = null;
			List<Map<String, String>> maps = res.splitToMap("icon");
			for (Map<String, String> m : maps) {
				radio = new RadioStation();
				radio.title = m.get("title");
				radio.icon = m.get("icon");
				radio.name = m.get("name");
				radio.id = m.get("cmd");
				radio.type = m.get("type");
				radios.add(radio);
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<RadioStation>(count, startIndex, radios);
	}

	public BrowseLoadResult<Favorite> getFavorites(String searchString,
			int startIndex, int pageSize) {
		ArrayList<Favorite> favorites = new ArrayList<Favorite>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					"favorites items " + startIndex + " " + pageSize);
			if (searchString != null && !"".equals(searchString))
				command.addTag("search", Uri.encode(searchString));
			command.addTag("want_url", "1");
			SqueezeCommand res = mBroker.sendRequest(command.toString());
			Favorite fav = null;
			for (ArrayList<String> favRow : res.splitParameters("id")) {
				Map<String, String> favMap = SqueezeCommand
						.splitToParameterMap(favRow);
				fav = new Favorite();
				fav.id = favMap.get("id");
				fav.name = favMap.get("name");
				fav.url = favMap.get("url");
				if (favMap.containsKey("count"))
					count = Integer.parseInt(favMap.get("count"));
				favorites.add(fav);
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<Favorite>(count, startIndex, favorites);
	}

	public void flagAsFavorite(Song currentSong) throws IOException {
		SqueezeTaggedRequestBuilder bld = new SqueezeTaggedRequestBuilder(
				"favorites add");
		bld.addTag("url", currentSong.path);
		bld.addTag("title", Uri.encode(currentSong.title));
		mBroker.sendRequest(bld.toString());
	}

}
