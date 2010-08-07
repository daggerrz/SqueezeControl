package com.squeezecontrol;

import java.util.ArrayList;

public class BrowseLoadResult<T> {
	private int mTotalCount = 0;
	private ArrayList<T> mResults;
	private int mStartIndex;
	private long mQueryVersion;

	public BrowseLoadResult(int totalCount, int startIndex,
			ArrayList<T> results) {
		mTotalCount = totalCount;
		mStartIndex = startIndex;
		mResults = results;
	}

	public long getQueryVersion() {
		return mQueryVersion;
	}

	public void setQueryVersion(long queryVersion) {
		mQueryVersion = queryVersion;
	}

	public int getStartIndex() {
		return mStartIndex;
	}

	public int getTotalCount() {
		return mTotalCount;
	}

	public ArrayList<T> getResults() {
		return mResults;
	}


}
