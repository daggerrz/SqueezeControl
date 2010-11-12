package com.squeezecontrol.util;

import android.view.KeyEvent;

import com.squeezecontrol.SqueezeService;

public class VolumeKeyHandler {
	public static boolean dispatchKeyEvent(KeyEvent event) {
		SqueezeService s = SqueezeService.getInstance();
		if (s == null || s.getPlayer() == null) return false;
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_UP || event.getRepeatCount() > 0) {
				s.getPlayer().getMixer().increaseVolume();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_UP || event.getRepeatCount() > 0) {
				s.getPlayer().getMixer().decreaseVolume();
			}
			return true;
		default:
			return false;
		}		
	}
}
