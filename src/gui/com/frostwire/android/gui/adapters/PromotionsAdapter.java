/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
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

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.frostwire.android.R;
import com.frostwire.android.gui.search.PromotionsHandler;
import com.frostwire.android.gui.views.ImageLoader;

/**
 * Adapter in control of the List View shown when we're browsing the files of
 * one peer.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class PromotionsAdapter extends BaseAdapter {

    private final Context context;
    private final List<PromotionsHandler.Slide> slides;
    private final ImageLoader imageLoader;
    private final Drawable defaultDrawable;

    public PromotionsAdapter(Context context, List<PromotionsHandler.Slide> slides) {
        this.context = context;
        this.slides = slides;
        this.imageLoader = ImageLoader.getDefault();
        this.defaultDrawable = context.getResources().getDrawable(R.drawable.promotion_default);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            //imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setAdjustViewBounds(true);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        try {
            imageLoader.displayImage(getItem(position).imageSrc, imageView, defaultDrawable);
        } catch (Throwable e) {
            // ignore
        }

        return imageView;
    }

    @Override
    public int getCount() {
        return slides.size();
    }

    @Override
    public PromotionsHandler.Slide getItem(int position) {
        return slides.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}