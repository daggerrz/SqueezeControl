package com.squeezecontrol.util;

public class FormatUtils {
	public static String formatAsTime(long millis) {

		int minutes = (int) (millis / 60000);
		int seconds = (int) ((millis - (minutes * 60000)) / 1000);
		return (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":"
				+ (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
	}
}
