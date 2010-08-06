package com.squeezecontrol.model;

public class RadioStation implements Browsable {
	public String id;
	public String name;

	public String getName() {
		return name;
	}


	public static RadioStation forName(String name) {
		RadioStation a = new RadioStation();
		a.name = name;
		return a;
	}

	@Override
	public String toString() {
		return name;
	}

}
