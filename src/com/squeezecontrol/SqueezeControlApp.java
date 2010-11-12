package com.squeezecontrol;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;

public class SqueezeControlApp extends Application {

	public void onCreate() {
		SqueezeService.createService(this).start();
	};
	
	@Override
	public void onTerminate() {
		SqueezeService.getInstance().stop();
	}
}
