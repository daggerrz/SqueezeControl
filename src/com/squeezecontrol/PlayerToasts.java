package com.squeezecontrol;

import android.content.Context;
import android.widget.Toast;

import com.squeezecontrol.model.Browsable;

public class PlayerToasts {
	
	public static void addedToPlayList(Context context, Browsable browsable) {
		Toast.makeText(context, "Added to playlist:\n" + browsable.getName(),
				Toast.LENGTH_SHORT).show();
		
	}
}
