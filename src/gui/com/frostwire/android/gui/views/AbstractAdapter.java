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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;

import com.frostwire.android.R;

/**
 * We extend from ListAdapter to populate our ListViews.
 * This one allows us to click and long click on the elements of our ListViews.
 * 
 * @author gubatron
 * @author aldenml
 *
 * @param <T>
 */
public abstract class AbstractAdapter<T> extends ContextAdapter implements Filterable {

    private static String TAG = "FW.AbstractListAdapter";

    private final int viewItemId;

    private final ViewOnClickListener viewOnClickListener;
    private final CheckboxOnCheckedChangeListener<T> checkboxOnCheckedChangeListener;

    private ListAdapterFilter<T> filter;
    private boolean checkboxesVisibility;

    protected List<T> list;
    protected Set<T> checked;
    protected List<T> visualList;

    public AbstractAdapter(Context ctx, int viewItemId, List<T> list, Set<T> checked) {
        super(ctx);
        this.viewItemId = viewItemId;

        this.viewOnClickListener = new ViewOnClickListener(ctx);
        this.checkboxOnCheckedChangeListener = new CheckboxOnCheckedChangeListener<T>(this);

        this.list = list.equals(Collections.emptyList()) ? new ArrayList<T>() : list;
        this.checked = checked;
        this.visualList = list;
    }

    public AbstractAdapter(Context context, int viewItemId, List<T> list) {
        this(context, viewItemId, list, new HashSet<T>());
    }

    public AbstractAdapter(Context context, int viewItemId) {
        this(context, viewItemId, new ArrayList<T>(), new HashSet<T>());
    }

    public int getViewItemId() {
        return viewItemId;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean areAllItemsEnabled() {
        return true;
    }

    public boolean isEnabled(int position) {
        return true;
    }

    public int getItemViewType(int position) {
        return 0;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public boolean isEmpty() {
        return getCount() == 0;
    }

    public Set<T> getChecked() {
        return checked;
    }

    public void clearChecked() {
        if (checked != null && checked.size() > 0) {
            checked.clear();
            notifyDataSetChanged();
        }
    }

    public void checkAll() {
        checked.clear();
        if (visualList != null) {
            checked.addAll(visualList);
        }
        notifyDataSetChanged();
    }

    /** This will return the count for the current file type */
    public int getCount() {
        return visualList == null ? 0 : visualList.size();
    }

    /** Should return the total count for all file types. */
    public int getTotalCount() {
        return list == null ? 0 : list.size();
    }

    public T getItem(int position) {
        return visualList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void setList(List<T> list) {
        this.list = list.equals(Collections.emptyList()) ? new ArrayList<T>() : list;
        this.visualList = this.list;
        this.checked.clear();
        notifyDataSetInvalidated();
    }

    public void addList(List<T> g, boolean checked) {
        visualList.addAll(g);
        if (visualList != list) {
            list.addAll(g);
        }
        if (checked) {
            this.checked.addAll(g);
        }
        notifyDataSetChanged();
    }

    /**
     * Adds new results to the existing list.
     * @param g
     */
    public void addList(List<T> g) {
        addList(g, false);
    }

    public void addItem(T item) {
        addItem(item, true);
    }

    public void addItem(T item, boolean visible) {
        if (visible) {
            visualList.add(item);
            if (visualList != list) {
                list.add(item);
            }
        } else {
            if (visualList == list) {
                visualList = new ArrayList<T>(list);
            }
            list.add(item);
        }
        notifyDataSetChanged();
    }

    public void deleteItem(T item) {
        visualList.remove(item);
        if (visualList != list) {
            list.remove(item);
        }
        if (checked.contains(item)) {
            checked.remove(item);
        }
        notifyDataSetChanged();
    }

    public void updateList(List<T> g) {
        list = g;
        visualList = g;
        checked.clear();
        notifyDataSetChanged();
    }

    public void clear() {
        if (list != null) {
            list.clear();
        }
        if (visualList != null) {
            visualList.clear();
        }
        if (checked != null) {
            checked.clear();
        }
        notifyDataSetInvalidated();
    }

    public List<T> getList() {
        return list;
    }

    @Override
    public View getView(Context ctx, int position, View convertView, ViewGroup parent) {
        T item = getItem(position);

        if (convertView == null) {
            // every list view item is wrapped in a generic container which has a hidden checkbox on the left hand side.
            convertView = View.inflate(ctx, R.layout.view_selectable_list_item, null);
            LinearLayout container = findView(convertView, R.id.view_selectable_list_item_container);
            View.inflate(ctx, viewItemId, container);
        }

        try {

            initTouchFeedback(convertView, item);
            initCheckBox(convertView, item);

            populateView(convertView, item);

        } catch (Throwable e) {
            Log.e(TAG, "Fatal error getting view: " + e.getMessage(), e);
        }

        return convertView;
    }

    public Filter getFilter() {
        return new AbstractListAdapterFilter<T>(this, filter);
    }

    /**
     * So that results can be filtered. This discriminator should define which fields of T are the ones eligible for filtering.
     * @param discriminator
     */
    public void setAdapterFilter(ListAdapterFilter<T> filter) {
        this.filter = filter;
    }

    public boolean getCheckboxesVisibility() {
        return checkboxesVisibility;
    }

    public void setCheckboxesVisibility(boolean checkboxesVisibility) {
        this.checkboxesVisibility = checkboxesVisibility;
        notifyDataSetChanged();
    }

    /**
     * Implement this method to refresh the UI contents of the List Item with the data.
     * @param view
     * @param data
     */
    protected abstract void populateView(View view, T data);

    /**
     * Helper function.
     * 
     * @param <TView>
     * @param view
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static <TView extends View> TView findView(View view, int id) {
        return (TView) view.findViewById(id);
    }

    /**
     * If you want to create a menu per item, return here the menu adapter.
     * The menu will be created automatically and the vent long click will be eaten.
     */
    protected MenuAdapter getMenuAdapter(View view) {
        return null;
    }

    /**
     * Sets up the behavior of a possible checkbox to check this item.
     * 
     * Takes in consideration:
     * - Only so many views are created and reused by the ListView
     * - Setting the correct checked/unchecked value without triggering the onCheckedChanged event.
     * 
     * @see getChecked()
     * 
     * @param view
     * @param item
     */
    private void initCheckBox(View view, T item) {

        CheckBox checkbox = findView(view, R.id.view_selectable_list_item_checkbox);

        if (checkbox != null) {
            checkbox.setVisibility((checkboxesVisibility) ? View.VISIBLE : View.GONE);

            // so we won't re-trigger a onCheckedChangeListener, we do this because views are re-used.
            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(checkboxesVisibility && checked.contains(item));

            checkbox.setTag(item);
            checkbox.setOnCheckedChangeListener(checkboxOnCheckedChangeListener);
        }
    }

    private void initTouchFeedback(View v, T item) {
        if (v instanceof CheckBox) {
            return;
        }

        v.setOnClickListener(viewOnClickListener);
        v.setOnLongClickListener(viewOnClickListener);
        v.setTag(item);

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = vg.getChildAt(i);
                initTouchFeedback(child, item);
            }
        }
    }

    private static final class ViewOnClickListener extends ClickAdapter<Context> {

        public ViewOnClickListener(Context ctx) {
            super(ctx);
        }

        @Override
        public void onClick(Context ctx, View v) {
            if (ctx instanceof OnAdapterClickListener) {
                ((OnAdapterClickListener) ctx).onClick(v);
            }
        }

        @Override
        public boolean onLongClick(Context ctx, View v) {
            if (ctx instanceof OnAdapterClickListener) {
                return ((OnAdapterClickListener) ctx).onLongClick(v);
            } else {
                return false;
            }
        }
    }

    private static final class CheckboxOnCheckedChangeListener<T> extends ClickAdapter<AbstractAdapter<T>> {

        public CheckboxOnCheckedChangeListener(AbstractAdapter<T> adapter) {
            super(adapter);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCheckedChanged(AbstractAdapter<T> adapter, CompoundButton buttonView, boolean isChecked) {
            T item = (T) buttonView.getTag();

            if (isChecked && !adapter.checked.contains(item)) {
                adapter.checked.add(item);
            } else {
                adapter.checked.remove(item);
            }
        }
    }

    private static final class AbstractListAdapterFilter<T> extends Filter {

        private final AbstractAdapter<T> adapter;
        private final ListAdapterFilter<T> filter;

        public AbstractListAdapterFilter(AbstractAdapter<T> adapter, ListAdapterFilter<T> filter) {
            this.adapter = adapter;
            this.filter = filter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            List<T> list = adapter.getList();

            FilterResults result = new FilterResults();
            if (filter == null) {
                /** || StringUtils.isNullOrEmpty(constraint.toString(), true)) { */
                result.values = list;
                result.count = list.size();
            } else {
                List<T> filtered = new ArrayList<T>();
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    T obj = list.get(i);
                    if (filter.accept(obj, constraint)) {
                        filtered.add(obj);
                    }
                }
                result.values = filtered;
                result.count = filtered.size();
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            adapter.visualList = (List<T>) results.values;
            adapter.notifyDataSetInvalidated();
        }

    }

    public static interface OnAdapterClickListener {

        public void onClick(View v);

        public boolean onLongClick(View v);
    }
}