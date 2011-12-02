/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.model;

public class XmlBrowserEntry implements Browsable {

    public String id;
    public String name;
    public String type;
    public boolean hasItems;
    public String url;
    public String icon;
    public String title;
    public boolean isAudio;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return type + ":" + name;
    }

}
