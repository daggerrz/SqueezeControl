package com.squeezecontrol.io;

import java.util.Arrays;

import android.webkit.URLUtil;

public class CommandParser {
	public static SqueezeCommand parse(String commandString) {
		if (commandString == null) return null;
		String parts[]  = commandString.split(" ");
		if (parts.length < 3) return null;
		// <Player> <Command> <Param1> <Param2>
		String[] params = (String[]) Arrays.asList(parts).subList(2, parts.length).toArray(new String[parts.length - 2]);
		int count = params.length;
		return new SqueezeCommand(SqueezeCommand.decode(parts[0]), parts[1], params);
	}
}
