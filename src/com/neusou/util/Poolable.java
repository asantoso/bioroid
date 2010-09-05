package com.neusou.util;

/**
 * @hide
 */
public interface Poolable<T> {
    void setNextPoolable(T element);
    T getNextPoolable();
}
