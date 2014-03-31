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

package com.frostwire.android.gui.adapters;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.SoftwareUpdater;
import com.frostwire.android.gui.activities.BrowsePeerActivity;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.dialogs.MenuDialog;
import com.frostwire.android.gui.dialogs.MenuDialog.MenuItem;
import com.frostwire.android.gui.dialogs.ShareIndicationDialog;
import com.frostwire.android.gui.menu.BrowsePeerMenuItem;
import com.frostwire.android.gui.menu.ChangeNicknameMenuItem;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractAdapter;
import com.frostwire.android.gui.views.ClickAdapter;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class PeerListAdapter2 extends AbstractAdapter<Peer> {

    private final String menuDialogId;
    private final OnClickListener howtoShareClickListener;

    public PeerListAdapter2(final Activity activity, List<Peer> peers, String menuDialogId) {
        super(activity, R.layout.view_peer_list_item, peers);

        this.menuDialogId = menuDialogId;
        howtoShareClickListener = new HowtoShareClickListener(activity);
    }

    @Override
    protected void populateView(View view, Peer peer) {
        Context ctx = view.getContext();

        TextView title = findView(view, R.id.view_peer_list_item_title);
        title.setText(peer.isLocalHost() ? view.getResources().getString(R.string.my_files) : peer.getNickname());

        TextView version = findView(view, R.id.view_peer_list_item_version);
        version.setText("v. " + peer.getClientVersion());
        version.setTextColor(0xffcccccc);

        ImageView icon = findView(view, R.id.view_peer_list_item_icon);
        populateIcon(icon, peer);

        ImageView howtoShareButton = findView(view, R.id.view_peer_list_item_button_how_to_share);
        howtoShareButton.setOnClickListener(howtoShareClickListener);

        if (!peer.isLocalHost()) {
            howtoShareButton.setVisibility(View.INVISIBLE);
            title.setTextColor(0xff3b3b3b);
        } else {
            title.setTextColor(0xff54afe4);

            // show my version in red If I'm old to encourage user to update.
            if (SoftwareUpdater.instance().isOldVersion()) {
                version.setTextColor(Color.RED);
                version.setText(ctx.getString(R.string.please_update_to_v, SoftwareUpdater.instance().getLatestVersion()));
            }

            howtoShareButton.setVisibility(View.VISIBLE);
            howtoShareButton.setImageResource(R.drawable.share_howto);
        }

        TextView summary = findView(view, R.id.view_peer_list_item_summary);
        summary.setText(ctx.getString(R.string.summary_files_shared, peer.getNumSharedFiles()));
    }

    @Override
    protected void onItemClick(View v) {
        Context ctx = v.getContext();
        Peer peer = (Peer) v.getTag();

        if (peer == null) {
            return;
        }

        if (peer.isLocalHost()) {
            if (ctx instanceof MainActivity) { // this needs to be refactored with an intent
                ((MainActivity) ctx).showMyFiles();
            }
        } else {
            if (peer.getNumSharedFiles() > 0) {
                Intent i = null;
                i = new Intent(ctx, BrowsePeerActivity.class);
                i.putExtra(Constants.EXTRA_PEER_UUID, peer.getKey());
                ctx.startActivity(i);
            } else {
                UIUtils.showShortMessage(ctx, peer.getNickname() + " " + ctx.getString(R.string.not_sharing_files));
            }
        }
    }

    @Override
    protected boolean onItemLongClick(View v) {
        Peer peer = (Peer) v.getTag();

        if (peer == null) {
            return false;
        }

        if (!(v.getContext() instanceof Activity)) {
            return false;
        }

        Activity activity = (Activity) v.getContext();
        List<MenuItem> items = new ArrayList<MenuItem>();

        if (peer.isLocalHost()) {
            items.add(new ChangeNicknameMenuItem());
        }

        items.add(new BrowsePeerMenuItem());

        MenuDialog dlg = MenuDialog.newInstance(menuDialogId, items);
        dlg.show(activity.getFragmentManager());

        return true;
    }

    private void populateIcon(ImageView icon, Peer peer) {
        if (peer.isLocalHost()) {
            icon.setImageResource(R.drawable.my_files_device);
        } else {
            switch (peer.getDeviceMajorType()) {
            case Constants.DEVICE_MAJOR_TYPE_PHONE:
                icon.setImageResource(R.drawable.device_type_type_phone);
                break;
            case Constants.DEVICE_MAJOR_TYPE_TABLET:
                icon.setImageResource(R.drawable.device_type_icon_tablet);
                break;
            case Constants.DEVICE_MAJOR_TYPE_DESKTOP:
                icon.setImageResource(R.drawable.device_type_icon_desktop);
                break;
            default:
                icon.setImageResource(R.drawable.device_type_type_generic);
            }
        }
    }

    private static final class HowtoShareClickListener extends ClickAdapter<Activity> {

        public HowtoShareClickListener(Activity activity) {
            super(activity);
        }

        @Override
        public void onClick(Activity activity, View v) {
            ShareIndicationDialog dlg = new ShareIndicationDialog();
            dlg.show(activity.getFragmentManager());
        }
    }
}