/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.views.preference;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class StoragePreference extends ListPreference {

	public StoragePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public StoragePreference(Context context) {
		this(context, null);
	}

	private void init() {
		List<StorageMount> mounts = new ArrayList<StorageMount>();// StorageUtils.getStorageMounts();
		mounts.add(new StorageMount("a", "b"));
		mounts.add(new StorageMount("c", "d"));
		int count = mounts.size();

		CharSequence[] entries = new CharSequence[count];
		CharSequence[] values = new CharSequence[count];

		for (int i = 0; i < count; i++) {
			StorageMount sm = mounts.get(i);
			entries[i] = sm.getLabel();
			values[i] = sm.getPath();
		}

		setEntries(entries);
		setEntryValues(values);
	}
	
	private static final class StorageMount {

	    private final String label;
	    private final String path;

	    public StorageMount(String label, String path) {
	        this.label = label;
	        this.path = path;
	    }

	    public String getLabel() {
	        return label;
	    }

	    public String getPath() {
	        return path;
	    }
	}
}
