package com.squeezecontrol;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public abstract class SqueezeServiceConnection {

	
	public abstract void onServiceConnected(SqueezeService service);

}
