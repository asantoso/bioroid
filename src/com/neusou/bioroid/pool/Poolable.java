package com.neusou.bioroid.pool;
/**
 * @hide
 */
public interface Poolable<T> {
    void setNextPoolable(T element);
    T getNextPoolable();
}
