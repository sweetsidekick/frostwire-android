package com.frostwire.android.gui.views;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public abstract class AbstractSwipeDetector extends SimpleOnGestureListener implements OnTouchListener {
    
    private final int MIN_DISTANCE = 100;
    private float downX;
    private float downY;
    private float upX;
    private float upY;
    
    public void onRightToLeftSwipe(){
    }

    public void onLeftToRightSwipe(){
    }

    public void onTopToBottomSwipe(){
    }

    public void onBottomToTopSwipe(){
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                upX = event.getX();
                upY = event.getY();

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