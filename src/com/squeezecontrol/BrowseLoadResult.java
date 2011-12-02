/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

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
