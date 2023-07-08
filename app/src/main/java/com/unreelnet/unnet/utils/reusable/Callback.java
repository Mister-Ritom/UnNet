package com.unreelnet.unnet.utils.reusable;

@FunctionalInterface
public interface Callback<T> {
    void onCall(T item);
}
