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

import android.app.FragmentManager;

import com.frostwire.android.R;
import com.frostwire.android.gui.dialogs.ChangeNicknameDialog;
import com.frostwire.android.gui.dialogs.MenuDialog.MenuItem;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class ChangeNicknameMenuItem extends MenuItem {

    public ChangeNicknameMenuItem() {
        super(MenuItems.CHANGE_NICKNAME.id(), R.string.change_my_nickname, R.drawable.contextmenu_icon_user);
    }

    public static void onClick(FragmentManager fm) {

        ChangeNicknameDialog dlg = new ChangeNicknameDialog();
        dlg.show(fm);

        /*
        if (adapter != null) {
            List<Peer> peers = adapter.getList();
            int size = peers.size();
            for (int i = 0; i < size; i++) {
                Peer p = peers.get(i);
                if (p != null && p.isLocalHost()) {
                    p.setNickname(newNick);
                    break;
                }
            }
            PeerManager.instance().getLocalPeer().setNickname(newNick);
            PeerManager.instance().updateLocalPeer();
            adapter.notifyDataSetChanged();
        }*/
    }
}