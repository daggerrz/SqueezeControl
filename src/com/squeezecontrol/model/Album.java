/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.model;

public class Album implements Browsable {
    public String id;
    public String name;
    public String artistName;
    public String artwork_track_id;

    public String getName() {
        return name;
    }


    public static Album forName(String name) {
        Album a = new Album();
        a.name = name;
        return a;
    }

    @Override
    public String toString() {
        return name;
    }

}
