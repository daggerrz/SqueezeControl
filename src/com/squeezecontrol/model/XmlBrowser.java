/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.model;

public class XmlBrowser implements Browsable {
    public String name;
    public String type;
    public String icon;
    public String cmd;

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
