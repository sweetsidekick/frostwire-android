/*
 * Copyright 2011 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.gui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frostwire.android.R;

/**
 * FrostWire Team notes:
 * 
 * Online resources for this class
 * - SwipeyTabs.java: http://code.google.com/p/android-playground/source/browse/trunk/SwipeyTabsSample/src/net/peterkuterna/android/apps/swipeytabs/SwipeyTabs.java
 * - android-playground project: http://code.google.com/p/android-playground/
 * - Android Swipey Tabs theory (by Kirill Grouchnikov): http://www.pushing-pixels.org/2011/08/11/android-tips-and-tricks-swipey-tabs.html
 *
 * This code was modified to support our custom tab indicators. In the future we will refactor it
 * for better reuse as a general component.
 */
public class SwipeyTabs extends ViewGroup implements OnPageChangeListener {

    protected final String TAG = "FW.SwipeyTabs";

    private SwipeyTabsAdapter mAdapter;

    private int mCurrentPos = -1;

    // height of the bar at the bottom of the tabs
    private int mBottomBarHeight = 0;
    // color for the bottom bar, fronted tab
    private int mBottomBarColor = 0xff96aa39;

    // holds the positions of the fronted tabs
    private int[] mFrontedTabPos;
    // holds the positions of the target position when swiping left
    private int[] mLeftTabPos;
    // holds the positions of the target position when swiping right
    private int[] mRightTabPos;
    // holds the positions of the current position on screen
    private int[] mCurrentTabPos;

    private int mWidth = -1;

    public SwipeyTabs(Context context) {
        this(context, null);
    }

    public SwipeyTabs(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeyTabs(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeyTabs, defStyle, 0);

        mBottomBarColor = a.getColor(R.styleable.SwipeyTabs_bottomBarColor, mBottomBarColor);
        mBottomBarHeight = a.getDimensionPixelSize(R.styleable.SwipeyTabs_bottomBarHeight, 2);

        a.recycle();

        init();
    }

    /**
     * Initialize the SwipeyTabs {@link ViewGroup}
     */
    private void init() {
        // enable the horizontal fading edges which will be drawn by the parent
        // View
        setHorizontalFadingEdgeEnabled(true);
        setFadingEdgeLength((int) (getResources().getDisplayMetrics().density * 35.0f + 0.5f));
        setWillNotDraw(false);
    }

    /**
     * Set the adapter.
     * 
     * @param adapter
     */
    public void setAdapter(SwipeyTabsAdapter adapter) {
        if (mAdapter != null) {
            // TODO: data set observer
        }

        mAdapter = adapter;
        mCurrentPos = -1;
        mFrontedTabPos = null;
        mLeftTabPos = null;
        mRightTabPos = null;
        mCurrentTabPos = null;

        // clean up our childs
        removeAllViews();

        if (mAdapter != null) {
            final int count = mAdapter.getCount();

            // add the child text views
            for (int i = 0; i < count; i++) {
                addView(mAdapter.getTab(i, this));
            }

            mCurrentPos = 0;
            mFrontedTabPos = new int[count];
            mLeftTabPos = new int[count];
            mRightTabPos = new int[count];
            mCurrentTabPos = new int[count];

            mWidth = -1;

            requestLayout();
        }
    }

    /**
     * Calculate the fronted, left and right positions
     * 
     * @param forceLayout
     *            force the current positions to the values of the calculated
     *            fronted positions
     */
    private void updateTabPositions(boolean forceLayout) {
        if (mAdapter == null) {
            return;
        }

        calculateTabPosition(mCurrentPos, mFrontedTabPos);
        calculateTabPosition(mCurrentPos + 1, mLeftTabPos);
        calculateTabPosition(mCurrentPos - 1, mRightTabPos);

        updateEllipsize();

        if (forceLayout) {
            final int count = mAdapter.getCount();
            for (int i = 0; i < count; i++) {
                mCurrentTabPos[i] = mFrontedTabPos[i];
            }
        }
    }

    /**
     * Calculate the position of the tabs.
     * 
     * @param position
     *            the position of the fronted tab
     * @param tabPositions
     *            the array in which to store the result
     */
    private void calculateTabPosition(int position, int[] tabPositions) {
        if (mAdapter == null) {
            return;
        }

        final int count = mAdapter.getCount();

        if (position >= 0 && position < count) {
            final int width = getMeasuredWidth();

            final View centerTab = getChildAt(position);
            tabPositions[position] = width / 2 - centerTab.getMeasuredWidth() / 2;
            for (int i = position - 1; i >= 0; i--) {
                final View tab = (View) getChildAt(i);
                if (i == position - 1) {
                    tabPositions[i] = 0 - tab.getPaddingLeft();
                } else {
                    tabPositions[i] = 0 - tab.getMeasuredWidth() - width;
                }
                tabPositions[i] = Math.min(tabPositions[i], tabPositions[i + 1] - tab.getMeasuredWidth());
            }
            for (int i = position + 1; i < count; i++) {
                final View tab = (View) getChildAt(i);
                if (i == position + 1) {
                    tabPositions[i] = width - tab.getMeasuredWidth() + tab.getPaddingRight();
                } else {
                    tabPositions[i] = width * 2;
                }
                final View prevTab = (View) getChildAt(i - 1);
                tabPositions[i] = Math.max(tabPositions[i], tabPositions[i - 1] + prevTab.getMeasuredWidth());
            }
        } else {
            for (int i = 0; i < tabPositions.length; i++) {
                tabPositions[i] = -1;
            }
        }
    }

    /**
     * Update the ellipsize of the text views
     */
    private void updateEllipsize() {
        if (mAdapter == null) {
            return;
        }

        final int count = mAdapter.getCount();

        for (int i = 0; i < count; i++) {
            LinearLayout layout = (LinearLayout) getChildAt(i);
            TextView tabText = (TextView) layout.getChildAt(0);

            if (i < mCurrentPos) {
                tabText.setEllipsize(null);
                tabText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            } else if (i == mCurrentPos) {
                tabText.setEllipsize(TruncateAt.END);
                tabText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else if (i > mCurrentPos) {
                tabText.setEllipsize(null);
                tabText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        measureTabs(widthMeasureSpec, heightMeasureSpec);

        int height = 0;
        final View v = getChildAt(0);
        if (v != null) {
            height = v.getMeasuredHeight();
        }

        setMeasuredDimension(resolveSize(widthSize, widthMeasureSpec), resolveSize(height + mBottomBarHeight, heightMeasureSpec));

        if (mWidth != widthSize) {
            mWidth = widthSize;
            updateTabPositions(true);
        }
    }

    /**
     * Measure our tab text views
     * 
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    private void measureTabs(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAdapter == null) {
            return;
        }

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int maxWidth = (int) (widthSize * 0.333);

        final int count = mAdapter.getCount();

        for (int i = 0; i < count; i++) {
            LayoutParams layoutParams = (LayoutParams) getChildAt(i).getLayoutParams();
            final int widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
            final int heightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY);
            getChildAt(i).measure(widthSpec, heightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mAdapter == null) {
            return;
        }

        final int count = mAdapter.getCount();

        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);

            v.layout(mCurrentTabPos[i], this.getPaddingTop(), mCurrentTabPos[i] + v.getMeasuredWidth(), this.getPaddingTop() + v.getMeasuredHeight());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mCurrentPos != -1) {
            int count = mAdapter.getCount();
            for (int i = 0; i < count; i++) {
                final LinearLayout layout = (LinearLayout) getChildAt(i);
                layout.setPressed(mCurrentPos == i);
            }
        }

        super.dispatchDraw(canvas);
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        // forced so that we will always have the left fading edge
        return 1.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        // forced so that we will always have the right fading edge
        return 1.0f;
    }

    public void onPageScrollStateChanged(int state) {
        if (mAdapter != null) {
            mAdapter.onPageScrollStateChanged(state);
        }
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mAdapter == null) {
            return;
        }

        final int count = mAdapter.getCount();

        float x = 0.0f;
        int dir = 0;

        // detect the swipe direction
        if (positionOffsetPixels != 0 && mCurrentPos == position) {
            dir = -1;
            x = positionOffset;
        } else if (positionOffsetPixels != 0 && mCurrentPos != position) {
            dir = 1;
            x = 1.0f - positionOffset;
        }

        // update the current positions
        for (int i = 0; i < count; i++) {
            final float curX = mFrontedTabPos[i];
            float toX = 0.0f;

            if (dir < 0) {
                toX = mLeftTabPos[i];
            } else if (dir > 0) {
                toX = mRightTabPos[i];
            } else {
                toX = mFrontedTabPos[i];
            }

            final int offsetX = (int) ((toX - curX) * x + 0.5f);
            final int newX = (int) (curX + offsetX);

            mCurrentTabPos[i] = newX;
        }

        requestLayout();
        invalidate();

        if (mAdapter != null) {
            mAdapter.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    public void onPageSelected(int position) {
        mCurrentPos = position;
        updateTabPositions(false);

        if (mAdapter != null) {
            mAdapter.onPageSelected(position);
        }
    }

    public interface SwipeyTabsAdapter extends OnPageChangeListener {

        /**
         * Return the number swipey tabs. Needs to be aligned with the number of
         * items in your {@link PagerAdapter}.
         * 
         * @return
         */
        int getCount();

        /**
         * Build {@link TextView} to diplay as a swipey tab.
         * 
         * @param position the position of the tab
         * @param root the root view
         * @return
         */
        View getTab(int position, SwipeyTabs root);
    }
}