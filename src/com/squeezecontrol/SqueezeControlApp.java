/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol;

import android.app.Application;

public class SqueezeControlApp extends Application {

    public void onCreate() {
        SqueezeService.createService(this).start();
    }

    ;

    @Override
    public void onTerminate() {
        SqueezeService.getInstance().stop();
    }
}
