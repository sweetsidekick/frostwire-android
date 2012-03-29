package com.frostwire.android.gui.views;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public abstract class AbstractSwipeDetector extends SimpleOnGestureListener implements OnTouchListener {
    
    private static final String TAG = "FW.AbstractSwipeDetector";
    
    private final int MIN_DISTANCE = 100;
    private float downX;
    private float downY;
    private float upX;
    private float upY;
    private int  lastDownPointerId;
    
    public void onRightToLeftSwipe(){
    }

    public void onLeftToRightSwipe(){
    }

    public void onTopToBottomSwipe(){
    }

    public void onBottomToTopSwipe(){
    }
    
    public boolean onMultiTouchEvent(View v, MotionEvent event) {
        return true;
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        
        if (event.getPointerCount() == 2 &&
            event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            return onMultiTouchEvent(v, event);
        }
        
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                lastDownPointerId = event.getPointerId(0);
                return true;
            }
            case MotionEvent.ACTION_UP: {
                upX = event.getX();
                upY = event.getY();
                
                if (event.getPointerId(0) != lastDownPointerId) {
                    return false;
                }

                float deltaX = downX - upX;
                float deltaY = downY - upY;

                // horizontal
                if(Math.abs(deltaX) > MIN_DISTANCE){
                    if(deltaX < 0) { 
                        onLeftToRightSwipe(); 
                        return true; 
                    }
                    
                    if(deltaX > 0) { 
                        onRightToLeftSwipe(); 
                        return true; 
                    }
                }
                else {
                        return false;
                }

                // vertical
                if(Math.abs(deltaY) > MIN_DISTANCE) {
                    if(deltaY < 0) { 
                        onTopToBottomSwipe(); 
                        return true; 
                    }
                    if(deltaY > 0) { 
                        onBottomToTopSwipe(); 
                        return true; 
                    }
                }
                else {
                        return false;
                }

                return true;
            }
        }
        return false;
    }
}