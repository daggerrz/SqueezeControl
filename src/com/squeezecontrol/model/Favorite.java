/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.model;


public class Favorite implements Browsable {

    public String id;
    public String name;
    public String url;

    public Favorite() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
