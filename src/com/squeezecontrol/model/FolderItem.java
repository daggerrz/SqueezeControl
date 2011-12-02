/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.model;

/**
 * Item in folder. Only get the id no matter what type of item it is, so
 * no need for different classes. Just use the type field.
 *
 * @author daggerrz
 */
public class FolderItem implements Browsable {

    public static final String FOLDER = "folder";
    public static final String TRACK = "track";
    public static final String PLAYLIST = "playlist";
    public static final String UNKNOWN = "unknown";


    public String id;
    public String name;
    public String type;

    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
