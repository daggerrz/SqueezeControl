package com.squeezecontrol.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;

import android.net.Uri;
import android.webkit.URLUtil;

public class SqueezeCommand {
	private String mPlayerId;
	private String mCommand;
	private String[] mParameters;

	public SqueezeCommand(String playerId, String command, String... parameters) {
		this.mPlayerId = playerId;
		this.mCommand = command;
		this.mParameters = parameters;
	}

	public String getPlayerId() {
		return mPlayerId;
	}

	public String getCommand() {
		return mCommand;
	}

	public String[] getParameters() {
		return mParameters;
	}
	
	public ArrayList<ArrayList<String>> splitParameters(String expression) {
		ArrayList<ArrayList<String>> parameterRows = new ArrayList<ArrayList<String>>();
		
		// Params on this first row should not be added. I.e all params up until the first
		// split will be ignored.
		ArrayList<String> currentRow = new ArrayList<String>();
		for (String param : mParameters) {
			if (param.startsWith(expression)) {
				currentRow = new ArrayList<String>();
				parameterRows.add(currentRow);
			}
			currentRow.add(param);
		}
		return parameterRows;
	}
	
	public String getUnescapedParameter(int index) {
		return decode(mParameters[index]);
	}

	public String getLastParameter() {
		return mParameters.length == 0 ? null
				: mParameters[mParameters.length - 1];
	}

	public String getFirstParameter() {
		return mParameters.length == 0 ? null : mParameters[0];
	}

	/**
	 * For tagged parameters. Returns a map with the tagged parameters as key
	 * and the value as value
	 * 
	 * @return
	 */
	public Map<String, String> getParameterMap() {
		return splitToParameterMap(mParameters);
	}
	
	public static Map<String, String> splitToParameterMap(String[] params) {
		Map<String, String> map = new HashMap<String, String>();
		for (String p : params) {
			p = decode(p);
			String[] parts = p.split(":", 2);
			if (parts.length == 2) {
				map.put(parts[0], parts[1]);
			}
		}
		return map;
	}

	public static Map<String, String> splitToParameterMap(Iterable<String> params) {
		Map<String, String> map = new HashMap<String, String>();
		for (String p : params) {
			p = decode(p);
			String[] parts = p.split(":", 2);
			if (parts.length == 2) {
				map.put(parts[0], parts[1]);
			}
		}
		return map;
	}
	
	public static String decode(String string) {
		return Uri.decode(string);
	}
	
	public static String encode(String string) {
		return Uri.encode(string);
	}
	
	/**
	 * Parses a timestring from SC (SECONDS.FRACTION) to milliseconds.
	 * This implementation just discards the fraction and returns the result
	 * int integral seconds.
	 * 
	 * @param timeString
	 * @return
	 */
	public static Long parseTime(String timeString) {
		if (timeString == null) return null;
		int decimalPointPos = timeString.indexOf('.');
		if (decimalPointPos != -1) {
			return (long) Integer.parseInt(timeString.substring(0, decimalPointPos)) * 1000;
		} else {
			return null;
		}
	}

	public List<Map<String, String>> splitToMap(String expression) {
		ArrayList<ArrayList<String>> lines = splitParameters(expression);
		List<Map<String, String>> maps = new ArrayList<Map<String,String>>();
		for (ArrayList<String> line : lines) {
			maps.add(splitToParameterMap(line));
		}
		return maps;
	}
	
}
