/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.image;

import android.graphics.Bitmap;

/**
 * An ImageStore is a strategy for fetching images.
 *
 * @author daggerrz
 */
public interface ImageStore {
    /**
     * Gets an image by the given name.
     *
     * @param name the name of the image
     * @return the image or null if none could be found
     */
    public Bitmap getImage(String name);
}
