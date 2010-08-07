package com.squeezecontrol.model;

public class RadioStation implements Browsable {
	public String id;
	public String name;
	public String type;
	public String icon;
	public String title;

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
