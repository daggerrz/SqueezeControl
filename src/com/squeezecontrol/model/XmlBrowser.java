package com.squeezecontrol.model;

public class XmlBrowser implements Browsable {
	public String id;
	public String name;
	public String type;
	public String icon;

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
