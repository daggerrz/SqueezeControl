package com.squeezecontrol.image;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

/**
 * Aggregates one or more image stores and tries to fetch images from them
 * in a serial fashion until one returns a non-null value.
 * 
 * @author liodden
 *
 */
public class AggregatingImageStore implements ImageStore {

	private List<ImageStore> targetStores = new ArrayList<ImageStore>();
	
	/**
	 * Adds an {@link ImageStore} to the store chain. Stores will be called
	 * in the order they were added.
	 * 
	 * @param store the store to add
	 * @return
	 */
	public AggregatingImageStore addStore(ImageStore store) {
		targetStores.add(store);
		return this;
	}

	/**
	 * @see ImageStore#getImage(String)
	 */
	public Bitmap getImage(String name) {
		for (ImageStore store : targetStores){
			Bitmap i = store.getImage(name);
			if (i != null) return i;
		}			
		return null;
	}

}
