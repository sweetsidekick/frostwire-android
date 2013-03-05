/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.search;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class SearchResult<T> {

    private final T value;
    private final boolean deeper;

    public SearchResult(T value, boolean deeper) {
        this.value = value;
        this.deeper = deeper;
    }

    public SearchResult(T value) {
        this(value, false);
    }

    public T getValue() {
        return value;
    }

    public boolean isDeeper() {
        return deeper;
    }
}
