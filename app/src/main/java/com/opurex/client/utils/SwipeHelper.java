
/*
    Opurex Android client
    Copyright (C) Opurex contributors, see the COPYRIGHT file

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.opurex.client.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class SwipeHelper implements View.OnTouchListener {
    
    public interface SwipeListener {
        void onSwipeDelete(View view);
        void onSwipeEdit(View view);
    }
    
    private GestureDetector gestureDetector;
    private SwipeListener swipeListener;
    private View currentView;
    
    public SwipeHelper(SwipeListener listener) {
        this.swipeListener = listener;
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        currentView = v;
        
        if (gestureDetector == null) {
            gestureDetector = new GestureDetector(v.getContext(), new SwipeGestureDetector(new SwipeGestureDetector.OnSwipeListener() {
                @Override
                public void onSwipeLeft() {
                    // Swipe left to delete
                    animateSwipeDelete();
                }
                
                @Override
                public void onSwipeRight() {
                    // Swipe right to edit
                    animateSwipeEdit();
                }
                
                @Override
                public void onSwipeUp() {
                    // Not used
                }
                
                @Override
                public void onSwipeDown() {
                    // Not used
                }
            }));
        }
        
        return gestureDetector.onTouchEvent(event);
    }
    
    private void animateSwipeDelete() {
        if (currentView == null) return;
        
        ObjectAnimator animator = ObjectAnimator.ofFloat(currentView, "translationX", 0f, -currentView.getWidth());
        animator.setDuration(200);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (swipeListener != null) {
                    swipeListener.onSwipeDelete(currentView);
                }
                // Reset position
                currentView.setTranslationX(0f);
            }
        });
        animator.start();
    }
    
    private void animateSwipeEdit() {
        if (currentView == null) return;
        
        ObjectAnimator animator = ObjectAnimator.ofFloat(currentView, "translationX", 0f, currentView.getWidth() * 0.3f);
        animator.setDuration(200);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Bounce back
                ObjectAnimator bounceBack = ObjectAnimator.ofFloat(currentView, "translationX", currentView.getTranslationX(), 0f);
                bounceBack.setDuration(150);
                bounceBack.start();
                
                if (swipeListener != null) {
                    swipeListener.onSwipeEdit(currentView);
                }
            }
        });
        animator.start();
    }
}
