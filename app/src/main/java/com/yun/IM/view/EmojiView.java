package com.yun.IM.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.yun.IM.activities.ChatActivity;

public class EmojiView extends androidx.appcompat.widget.AppCompatTextView {
    private float x;
    private float y;

    public EmojiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (event.getX() == x && event.getY() == y) {
                    ChatActivity.show(getText().toString().trim());
                }
                break;
        }
        return true;
    }
}
