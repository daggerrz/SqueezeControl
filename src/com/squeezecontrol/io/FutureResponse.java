/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

/**
 *
 */
package com.squeezecontrol.io;


class FutureResponse<T> {
    private T mResponse;
    private ResponseCallback<T> mCallback;

    public FutureResponse(ResponseCallback<T> callback) {
        mCallback = callback;
    }

    void setResponse(T value) {
        synchronized (this) {
            mResponse = value;
            notifyAll();
        }
        if (mCallback != null) {
            mCallback.handleResponse(value);
        }
    }

    T getResponse() throws InterruptedException {
        synchronized (this) {
            if (mResponse == null) {
                wait();
            }
        }
        return mResponse;
    }

    public static abstract class ResponseCallback<T> {
        private String mCommandPrefix;

        public ResponseCallback(String commandPrefix) {
            mCommandPrefix = commandPrefix;
        }

        public String getCommandPrefix() {
            return mCommandPrefix;
        }

        public abstract void handleResponse(T response);
    }

}