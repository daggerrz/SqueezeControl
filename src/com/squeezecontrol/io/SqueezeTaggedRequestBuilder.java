package com.squeezecontrol.io;

public class SqueezeTaggedRequestBuilder {
	private String mCommand;

	public SqueezeTaggedRequestBuilder(String command) {
		mCommand = command;
	}
	
	public SqueezeTaggedRequestBuilder addTag(String tagName, String value) {
		if (value != null) {
			mCommand += " " + tagName + "%3A" + value;
		}
		return this;
	}
	
	@Override
	public String toString() {
		return mCommand;
	}
}
