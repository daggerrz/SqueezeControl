package com.squeezecontrol.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.squeezecontrol.R;
import com.squeezecontrol.model.Browsable;

public class BrowseableAdapter<T extends Browsable> extends BaseAdapter
		implements Filterable {

	/**
	 * The number of items in this list. Since lists are virtual, this is not
	 * the same as mObject.size().
	 */
	private int mCount = 0;

	/**
	 * Contains the list of objects that represent the data of this
	 * ArrayAdapter. The content of this list is referred to as "the array" in
	 * the documentation.
	 */
	private Object[] mObjects;

	/**
	 * The resource indicating what views to inflate to display the content of
	 * this array adapter.
	 */
	private int mResource;

	/**
	 * The resource indicating what views to inflate to display the content of
	 * this array adapter in a drop down widget.
	 */
	private int mDropDownResource;

	/**
	 * If the inflated resource is not a TextView, {@link #mFieldId} is used to
	 * find a TextView inside the inflated views hierarchy. This field must
	 * contain the identifier that matches the one defined in the resource file.
	 */
	private int mFieldId = 0;

	/**
	 * Indicates whether or not {@link #notifyDataSetChanged()} must be called
	 * whenever {@link #mObjects} is modified.
	 */
	private boolean mNotifyOnChange = true;

	private Context mContext;

	private Filter mFilter;

	private LayoutInflater mInflater;

	/**
	 * Constructor
	 * 
	 * @param context
	 *            The current context.
	 * @param textViewResourceId
	 *            The resource ID for a layout file containing a TextView to use
	 *            when instantiating views.
	 */
	public BrowseableAdapter(Context context, int textViewResourceId) {
		init(context, textViewResourceId, 0, new ArrayList<T>());
	}

	public void setCount(int count) {
		if (mCount != count) {
			mCount = count;
			mObjects = new Object[count];
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}
	}

	public void set(Collection<T> objects, int startIndex) {
		int i = startIndex;
		for (T o : objects) {
			mObjects[i++] = o;
		}
		if (mNotifyOnChange)
			notifyDataSetChanged();
	}
	
	public void remove(int index) {
		int count = mObjects.length;
		Object[] newList = new Object[count - 1];
		for (int i = 0; i < index; i++) newList[i] = mObjects[i];
		for (int i = index + 1; i < count; i++) newList[i-1] = mObjects[i];
		mCount = count -1;
		mObjects = newList;
		notifyDataSetChanged();
	}
	

	/**
	 * Remove all elements from the list.
	 */
	public void clear() {
		mObjects = new Object[0];
		mCount = 0;
		if (mNotifyOnChange)
			notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		mNotifyOnChange = true;
	}

	/**
	 * Control whether methods that change the list ({@link #add},
	 * {@link #insert}, {@link #remove}, {@link #clear}) automatically call
	 * {@link #notifyDataSetChanged}. If set to false, caller must manually call
	 * notifyDataSetChanged() to have the changes reflected in the attached
	 * view.
	 * 
	 * The default is true, and calling notifyDataSetChanged() resets the flag
	 * to true.
	 * 
	 * @param notifyOnChange
	 *            if true, modifications to the list will automatically call
	 *            {@link #notifyDataSetChanged}
	 */
	public void setNotifyOnChange(boolean notifyOnChange) {
		mNotifyOnChange = notifyOnChange;
	}

	private void init(Context context, int resource, int textViewResourceId,
			ArrayList<T> objects) {
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mResource = mDropDownResource = resource;
		mFieldId = textViewResourceId;

		String alphabetString = "\u0020ABCDEFGHIJKLMNOPQRSTUVWXYZ∆ÿ≈";
		char[] alphabet = new char[alphabetString.length()];
		for (int i = 0; i < alphabet.length; i++) {
			alphabet[i] = alphabetString.charAt(i);
		}
	}

	/**
	 * Returns the context associated with this array adapter. The context is
	 * used to create views from the resource passed to the constructor.
	 * 
	 * @return The Context associated with this adapter.
	 */
	public Context getContext() {
		return mContext;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getCount() {
		return mCount;
	}

	/**
	 * {@inheritDoc}
	 */
	public T getItem(int position) {
		if (position >= mObjects.length)
			return null;
		else
			return (T) mObjects[position];
	}

	/**
	 * Returns the position of the specified item in the array.
	 * 
	 * @param item
	 *            The item to retrieve the position of.
	 * 
	 * @return The position of the specified item.
	 */
	public int getPosition(T item) {
		for (int i = 0; i < mObjects.length; i++) {
			if (mObjects[i] == item) return i;
		}
		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = createViewFromResource(position, convertView, parent,
				mResource);
		bindView(position, v);
		return v;
	}

	protected void bindView(int position, View view) {
		TextView text = null;
		try {
			if (mFieldId == 0) {
				// If no custom field is assigned, assume the whole resource is
				// a TextView
				text = (TextView) view;
			} else {
				// Otherwise, find the TextView field within the layout
				text = (TextView) view.findViewById(mFieldId);
			}
		} catch (ClassCastException e) {
			//Log.e("BrowseableAdapter", "You must supply a resource ID for a TextView");
			throw new IllegalStateException(
					"BrowseableAdapter requires the resource ID to be a TextView or that you override bindView()",
					e);
		}

		Object item = getItem(position);
		if (item == null)
			text.setText(R.string.loading_progress);
		else
			text.setText(item.toString());
	}

	private View createViewFromResource(int position, View convertView,
			ViewGroup parent, int resource) {
		View view;

		if (convertView == null) {
			view = mInflater.inflate(resource, parent, false);
		} else {
			view = convertView;
		}
		return view;
	}

	/**
	 * <p>
	 * Sets the layout resource to create the drop down views.
	 * </p>
	 * 
	 * @param resource
	 *            the layout resource defining the drop down views
	 * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
	 */
	public void setDropDownViewResource(int resource) {
		this.mDropDownResource = resource;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent,
				mDropDownResource);
	}

	/**
	 * Creates a new ArrayAdapter from external resources. The content of the
	 * array is obtained through
	 * {@link android.content.res.Resources#getTextArray(int)}.
	 * 
	 * @param context
	 *            The application's environment.
	 * @param textArrayResId
	 *            The identifier of the array to use as the data source.
	 * @param textViewResId
	 *            The identifier of the layout used to create views.
	 * 
	 * @return An ArrayAdapter<CharSequence>.
	 */
	public static ArrayAdapter<CharSequence> createFromResource(
			Context context, int textArrayResId, int textViewResId) {
		CharSequence[] strings = context.getResources().getTextArray(
				textArrayResId);
		return new ArrayAdapter<CharSequence>(context, textViewResId, strings);
	}

	public void setFilter(Filter filter) {
		mFilter = filter;
	}

	/**
	 * {@inheritDoc}
	 */
	public Filter getFilter() {
		return mFilter;
	}


}
