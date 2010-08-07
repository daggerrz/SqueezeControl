package com.squeezecontrol;

public interface Callback<T> {

	public void handle(T value);
}
