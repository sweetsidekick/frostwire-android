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

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.frostwire.android.gui.views.FWGridView;
import com.frostwire.android.gui.views.ImageLoader;
import com.frostwire.frostclick.Slide;
import com.squareup.picasso.Callback;

/**
 * Adapter in control of the List View shown when we're browsing the files of
 * one peer.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class PromotionsAdapter extends BaseAdapter {

    private static final double PROMO_HEIGHT_TO_WIDTH_RATIO = 0.52998;
    private final List<Slide> slides;
    private final ImageLoader imageLoader;

    public PromotionsAdapter(List<Slide> slides) {
        this.slides = slides;
        this.imageLoader = ImageLoader.getDefault();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView != null && convertView instanceof ImageView) {
            return convertView;
        }
        
        FWGridView gridView = (FWGridView) parent;
        int promoWidth = gridView.getColumnWidth(); //hack
        int promoHeight = (int) (promoWidth * PROMO_HEIGHT_TO_WIDTH_RATIO);

        ImageView imageView = new ImageView(parent.getContext());
        imageView.setScaleType(ScaleType.MATRIX);
        imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        imageView.setPadding(0, 0, 0, 0);
        imageView.setAdjustViewBounds(true);
        imageLoader.getPicasso().load(getItem(position).imageSrc).resize(promoWidth, promoHeight).into(imageView);
        
        return imageView;
    }

    @Override
    public int getCount() {
        return slides.size();
    }

    @Override
    public Slide getItem(int position) {
        return slides.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}