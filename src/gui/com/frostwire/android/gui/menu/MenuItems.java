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

package com.frostwire.android.gui.menu;

/**
 * Class to ensure uniqueness of menu items ids.
 * 
 * @author aldenml
 *
 */
/*
 * aldenml: I'm well aware of the bad patterns that could arise with the direct use
 * of the orginal, but the root of this problem is the way android link the dialog with the
 * activity/fragment.
 */
public enum MenuItems {

    CHANGE_NICKNAME, BROWSE_PEER;

    private static final MenuItems values[] = values();

    public static MenuItems fromInt(int ordinal) {
        return values[ordinal];
    }
}
