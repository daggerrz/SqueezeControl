package com.squeezecontrol.image;

import android.graphics.Bitmap;

/**
 * An ImageStore is a strategy for fetching images.
 * 
 * @author liodden
 *
 */
public interface ImageStore {
	/**
	 * Gets an image by the given name.
	 * @param name the name of the image
	 * @return the image or null if none could be found
	 */
	public Bitmap getImage(String name);
}
