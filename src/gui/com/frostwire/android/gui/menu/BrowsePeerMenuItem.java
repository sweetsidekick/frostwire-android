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

import android.content.Context;
import android.content.Intent;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.activities.BrowsePeerActivity;
import com.frostwire.android.gui.dialogs.MenuDialog.MenuItem;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class BrowsePeerMenuItem extends MenuItem {

    public BrowsePeerMenuItem() {
        super(MenuItems.BROWSE_PEER.id(), R.string.browse_peer, R.drawable.contextmenu_icon_user);
    }

    public static void onClick(Context context, Peer peer) {
        Intent i = new Intent(context, BrowsePeerActivity.class);
        i.putExtra(Constants.EXTRA_PEER_UUID, peer.getKey());
        context.startActivity(i);
    }
}
