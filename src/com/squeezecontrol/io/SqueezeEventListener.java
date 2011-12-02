/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.io;

import java.io.IOException;

public interface SqueezeEventListener {

    public void onConnect(SqueezeBroker broker);

    public void onDisconnect(SqueezeBroker broker);

    public void onConnectionError(SqueezeBroker broker, IOException cause);

}
