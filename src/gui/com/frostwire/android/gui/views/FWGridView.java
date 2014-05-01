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

package com.frostwire.android.gui.views;

import java.lang.reflect.Field;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class FWGridView extends GridView {

    int viewWidth = 0;
    int viewHeight = 0;
    
    public FWGridView(Context context, AttributeSet attrs, int defStyle) {
        super(new FWContextWrapper(context), attrs, defStyle);
    }

    public FWGridView(Context context, AttributeSet attrs) {
        super(new FWContextWrapper(context), attrs);
    }

    public FWGridView(Context context) {
        super(new FWContextWrapper(context));
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }
    

    /**
     * This is a hack.
     * The reason is that, the very first time we try to find out what are the dimensions
     * of the FWGridView component in the PromotionsAdapter.getView() method, it always
     * returns 0. The idea was to use the width of the component, and the orientation of the
     * device, and then we'd know if we're in a single column mode or 2 column mode when displaying
     * the promos. This however works every time, but I'm not sure if it'll break after Android API 16 (Jelly Bean)
     * since Android later introduced it's own getColumnWidth() method.
     * @return
     */
    public int getColumnWidth() {
        try {
            Field field = GridView.class.getDeclaredField("mColumnWidth");
            field.setAccessible(true);
            Integer value = (Integer) field.get(this);
            field.setAccessible(false);
            return value.intValue();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
}
