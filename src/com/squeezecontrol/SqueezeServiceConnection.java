package com.squeezecontrol;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public abstract class SqueezeServiceConnection implements ServiceConnection {

	@Override
	public final void onServiceConnected(ComponentName name, IBinder service) {
		onServiceConnected(((SqueezeService.SqueezeServiceBinder) service).getService());
	}
	
	public abstract void onServiceConnected(SqueezeService service);

	@Override
	public void onServiceDisconnected(ComponentName name) {
		
	}

}
