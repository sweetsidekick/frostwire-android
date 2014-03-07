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

package com.frostwire.android.gui.views;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.frostwire.android.R;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class ContextMenuAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<ContextMenuItem> items;

    public ContextMenuAdapter(LayoutInflater inflater, List<ContextMenuItem> items) {
        this.inflater = inflater;
        this.items = items;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ContextMenuItem item = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.view_menu_list_item, parent, false);
        }

        TextView textView = (TextView) convertView;

        textView.setTag(item);
        textView.setText(item.getTextResId());

        textView.setCompoundDrawablesWithIntrinsicBounds(item.getDrawableResId(), 0, 0, 0);

        return convertView;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ContextMenuItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
