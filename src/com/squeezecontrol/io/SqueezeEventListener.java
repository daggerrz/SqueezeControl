package com.squeezecontrol.io;

import java.io.IOException;

public interface SqueezeEventListener {
	
	public void onConnect(SqueezeBroker broker);
	public void onDisconnect(SqueezeBroker broker);
	public void onConnectionError(SqueezeBroker broker, IOException cause);
	
}
