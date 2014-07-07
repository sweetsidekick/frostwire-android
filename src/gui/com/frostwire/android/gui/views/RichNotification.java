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
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frostwire.android.R;


public class RichNotification extends LinearLayout {
	public static final List<Integer> wasDismissed = new ArrayList<Integer>();
	private final String title;
	private final String description;
	private final Drawable icon;
	private OnClickListener clickListener;
	
	public RichNotification(Context context, AttributeSet attrs) {
		super(context, attrs);		
		TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RichNotification);
		icon = attributes.getDrawable(R.styleable.RichNotification_rich_notification_icon);
		title = attributes.getString(R.styleable.RichNotification_rich_notification_title);
		description = attributes.getString(R.styleable.RichNotification_rich_notification_description);
		attributes.recycle();
		clickListener = null;
	}


	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		View.inflate(getContext(), R.layout.view_rich_notification, this);
		
		ImageView imageViewIcon = (ImageView) findViewById(R.id.view_rich_notification_icon);
		if (imageViewIcon != null && icon != null) {
			imageViewIcon.setBackgroundDrawable(icon);
		}
		
		TextView textViewTitle = (TextView) findViewById(R.id.view_rich_notification_title);
		if (textViewTitle != null && title != null) {
			textViewTitle.setText(title);
			textViewTitle.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onClickNotification();
				}
			});
		}
		
		TextView textViewDescription = (TextView) findViewById(R.id.view_rich_notification_text);
		if (textViewDescription != null && description != null) {
			textViewDescription.setText(description);
			textViewDescription.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onClickNotification();
				}
			});
		}
		
		ImageButton dismissButton = (ImageButton) findViewById(R.id.view_rich_notification_close_button);
		dismissButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onDismiss();				
			}
		});
	}
	
	public void setOnClickListener(OnClickListener listener) {
		clickListener = listener;
	}
	
	public boolean wasDismissed() {
		return wasDismissed.contains(this.getId());
	}
	
	protected void onDismiss() {
		wasDismissed.add(getId());
		setVisibility(View.GONE);
	}

	protected void onClickNotification() {
		if (clickListener != null) {
			clickListener.onClick(this);
		}
	}
}