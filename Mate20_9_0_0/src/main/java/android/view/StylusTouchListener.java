package android.view;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import huawei.com.android.internal.policy.HiTouchSensor;

public class StylusTouchListener implements OnGestureListener, OnDoubleTapListener {
    private static final String TAG = "StylusTouchListener";
    private Context mContext = null;
    private final GestureDetector mGestureDetector;
    private boolean mIsDoubleTapOccur = false;
    private int mWindowType = 0;

    public StylusTouchListener(Context context) {
        this.mGestureDetector = new GestureDetector(context, this);
        this.mGestureDetector.setIsLongpressEnabled(true);
        this.mGestureDetector.setOnDoubleTapListener(this);
    }

    public boolean onDoubleTap(MotionEvent e) {
        Log.d(TAG, "onDoubleTap");
        this.mIsDoubleTapOccur = true;
        return false;
    }

    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.d(TAG, "onDoubleTapEvent");
        return false;
    }

    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(TAG, "onSingleTapConfirmed");
        return false;
    }

    public boolean onDown(MotionEvent e) {
        Log.e(TAG, "onDown");
        return false;
    }

    public void onShowPress(MotionEvent e) {
        Log.d(TAG, "onShowPress");
    }

    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG, "onSingleTapUp");
        return false;
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(TAG, "onScroll");
        return false;
    }

    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "onLongPress");
        if (!this.mIsDoubleTapOccur) {
            this.mIsDoubleTapOccur = false;
            if (this.mContext != null) {
                startHitouchSensor(e);
            }
        }
    }

    private void startHitouchSensor(MotionEvent e) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StylusTouchListener and mWindowType is: ");
        stringBuilder.append(this.mWindowType);
        Log.d(str, stringBuilder.toString());
        try {
            new HiTouchSensor(this.mContext).processStylusGessture(this.mContext, this.mWindowType, e.getX(), e.getY());
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startHitouchSensor error:");
            stringBuilder2.append(ex);
            Log.d(str2, stringBuilder2.toString());
        }
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "onFling");
        return false;
    }

    public void onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 1 || ev.getAction() == 3) {
            Log.d(TAG, "StylusTouchListener <- onTouchEvent.ACTION_UP");
            this.mIsDoubleTapOccur = false;
        }
        this.mGestureDetector.onTouchEvent(ev);
    }

    public void updateViewContext(Context context, int windowType) {
        this.mContext = context;
        this.mWindowType = windowType;
    }
}
