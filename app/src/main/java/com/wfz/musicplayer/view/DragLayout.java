package com.wfz.musicplayer.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.wfz.musicplayer.R;

/**
 * Created by Wufuzhao on 2016/5/10.
 */
public class DragLayout extends LinearLayout{
    private LinearLayout mDragView;
    private View mHeaderView;
    private View mDescView;
    private final ViewDragHelper mDragHelper;
    private int mDragRange;
    private int mTop;
    private boolean isOpened;

    private float mInitialMotionX;
    private float mInitialMotionY;

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, 1f, new DragHelperCallback());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDragView = (LinearLayout) findViewById(R.id.drag1);
        mHeaderView = findViewById(R.id.header);
        mHeaderView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isOpened)
                    close();
                else
                    open();
            }
        });
        mDescView = findViewById(R.id.desc);
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mDragView;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float p = (float)top/mDragRange;
            setBackgroundColor(Color.argb((int)(191*(1-p)),0,0,0));
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if(yvel>0){
                mDragHelper.settleCapturedViewAt(0, mDragRange);
            }else{
                mDragHelper.settleCapturedViewAt(0, mTop);
            }
            invalidate();
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            super.onEdgeTouched(edgeFlags, pointerId);
        }



        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int topBound = getPaddingTop();
            int bottomBound = getHeight() - mHeaderView.getHeight() - mHeaderView.getPaddingBottom();
            int newTop = Math.min(Math.max(top, topBound), bottomBound);
            return newTop;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return super.clampViewPositionHorizontal(child, left, dx);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if(state==0){
                if(mDragView.getTop()==mTop)
                    isOpened = true;
                else
                    isOpened = false;
            }
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mDragRange;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return 0;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDragHelper.processTouchEvent(ev);
        float x = ev.getX();
        float y = ev.getY();
        boolean isDragViewUnder = mDragHelper.isViewUnder(mDragView, (int) x, (int) y);
        return isDragViewUnder;
    }

    public boolean open(){
        if (mDragHelper.smoothSlideViewTo(mDragView, mDragView.getLeft(), mTop)) {
            postInvalidate();
            return true;
        }
        return false;
    }

    public boolean close(){
        if (mDragHelper.smoothSlideViewTo(mDragView, mDragView.getLeft(), mDragRange)) {
            postInvalidate();
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll()
    {
        if(mDragHelper.continueSettling(true))
        {
            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed,l,t,r,b);
        mDragRange = getHeight() - mHeaderView.getHeight();
        int mHeaderViewH = mHeaderView.getMeasuredHeight();
        mDragView.layout(
                0,
                b - mHeaderViewH,
                r,
                b+mDragRange);
    }
}
