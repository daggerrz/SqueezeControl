package com.squeezecontrol.io;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class SqueezeDiagnostics {

	public static class Result {
		private boolean success = false;
		private String result = null;
		
		Result(boolean succes, String result) {
			this.success = succes;
			this.result = result;
		}
		
		public boolean isSuccess() {
			return success;
		}
		public String getResult() {
			return result;
		}
		
	}
	
	public static Result testCLI(String host, int port, String username, String password) {
		SqueezeConnection conn = new SqueezeConnection(host, port, username, password);
		try {
			conn.open();
			conn.sendCommand("version ?");
			String response = conn.readCommand();
			
			if (response.startsWith("version"))
				return new Result(true, "Successfully connected to SqueezeCenter v"
						+ response.substring("version".length()));
			else
				return new Result(false, "Something is responding at "
						+ host
						+ ":"
						+ port
						+ ", but it does not seem to be a SqueezeCenter installation.");
		} catch (IOException e) {
			return new Result(false, "Unable to connect to CLI on " + host + ":" + port + ". ("
					+ e.getMessage() + ")");
		} finally {
			if (conn != null)
				conn.close();
		}
	}

	public static String testHTTP(String host, int port) {
		String base = "http://" + host + ":" + port;
		try {
			URLConnection conn = new URL(base
					+ "/music/0/cover.png").openConnection();
			String contentType = conn.getHeaderField("Content-Type");
			if ("image/png".equals(contentType)) {
				return "Successfully connected to SqueezeCenter!";
			} else {
				return "Something is responding at "
				+ base
				+ ", but it does not seem to be a SqueezeCenter installation.";
			}
		} catch (Exception ioe) {
			return "Unable to connect to SqueezeCenter at " + base + ". (" + ioe.getMessage() + ")";
		}
	}
}
