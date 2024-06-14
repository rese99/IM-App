package com.yun.IM.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

public class layout extends ConstraintLayout {
    private final String nameSpace = "http://com.yun.android";
    private float startY;
    private int barHeight = 0;
    private final int parentHeight;
    private final int showTime;
    private final int endTime;

    public layout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        showTime = Integer.parseInt(attrs.getAttributeValue(nameSpace, "showtime"));
        endTime = Integer.parseInt(attrs.getAttributeValue(nameSpace, "endtime"));
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            barHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        parentHeight = context.getResources().getDisplayMetrics().heightPixels;
        setY(parentHeight);
        setAlpha(0f);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startY = getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getRawY() >= startY) {
                    setY(event.getRawY() - barHeight);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (getY() - startY > getHeight() / 2) {
                    animate().setInterpolator(new DecelerateInterpolator()).setDuration(400).alpha(0f).y(parentHeight).start();
                } else {
                    animate().setInterpolator(new DecelerateInterpolator()).setDuration(400).y(parentHeight - getHeight()).start();
                }
                break;
        }
        return true;
    }

    public void show() {
        animate().setInterpolator(new DecelerateInterpolator()).setDuration(showTime).alpha(1f).y(parentHeight - getHeight()).start();
    }
    public void hidden(){
        animate().setInterpolator(new DecelerateInterpolator()).setDuration(endTime).alpha(0f).y(parentHeight).start();
    }
}
