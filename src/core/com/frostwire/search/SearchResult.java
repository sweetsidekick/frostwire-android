package com.frostwire.search;

public class SearchResult<T> {

    private final T value;

    public SearchResult(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
