/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.BrowsePeerFragment.OnRefreshSharedListener;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.Refreshable;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class BrowsePeerActivity extends AbstractActivity {

    private TextView textNickname;
    private TextView textTitle;
    private BrowsePeerFragment browsePeerFragment;

    private Peer peer;

    public BrowsePeerActivity() {
        super(R.layout.activity_browse_peer, 1);
    }

    @Override
    protected void initComponents() {
        textNickname = findView(R.id.activity_browse_peer_text_nickname);
        textNickname.setText("");
        textTitle = findView(R.id.activity_browse_peer_text_title);
        textTitle.setText("");
        browsePeerFragment = (BrowsePeerFragment) getSupportFragmentManager().findFragmentById(R.id.activity_browse_peer_fragment);

        peer = browsePeerFragment.getPeer();
        if (peer == null) { // save move
            finish();
            return;
        }

        browsePeerFragment.setOnRefreshSharedListener(new OnRefreshSharedListener() {
            @Override
            public void onRefresh(Fragment f, byte fileType, int numShared) {
                updateTitle(fileType, numShared);
            }
        });

        if (peer.isLocalHost()) {
            textNickname.setText(R.string.me);
        } else {
            textNickname.setText(peer.getNickname());
        }

        ImageButton buttonBack = findView(R.id.activity_browse_peer_button_back);
        buttonBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addRefreshable((Refreshable) findView(R.id.activity_browse_peer_player_notifier));
    }

    private void updateTitle(byte fileType, int numShared) {
        String title = UIUtils.getFileTypeAsString(getResources(), fileType);
        title += " (" + numShared + ")";
        textTitle.setText(title);
    }
}