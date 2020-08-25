package com.android.server.gesture;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class GestureNavView extends SurfaceView {
    private IGestureEventProxy mGestureEventProxy;
    private final Runnable mHideRunnable;
    private int mNavId;
    private WindowConfig mWindowConfig;

    public interface IGestureEventProxy {
        boolean onTouchEvent(GestureNavView gestureNavView, MotionEvent motionEvent);
    }

    public interface IGestureNavBackAnim {
        void onGestureAction(boolean z);

        void playDisappearAnim();

        void playFastSlidingAnim();

        void playScatterProcessAnim(float f, float f2);

        void setAnimPosition(float f);

        boolean setAnimProcess(float f);

        void setDockIcon(boolean z);

        void setNightMode(boolean z);

        void setSide(boolean z);

        void switchDockIcon(boolean z);
    }

    public GestureNavView(Context context) {
        this(context, 0);
    }

    public GestureNavView(Context context, int navId) {
        super(context);
        this.mNavId = -1;
        this.mWindowConfig = new WindowConfig();
        this.mHideRunnable = new Runnable() {
            /* class com.android.server.gesture.GestureNavView.AnonymousClass1 */

            public void run() {
                GestureNavView.this.setVisibility(8);
            }
        };
        this.mNavId = navId;
        init();
    }

    public GestureNavView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureNavView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mNavId = -1;
        this.mWindowConfig = new WindowConfig();
        this.mHideRunnable = new Runnable() {
            /* class com.android.server.gesture.GestureNavView.AnonymousClass1 */

            public void run() {
                GestureNavView.this.setVisibility(8);
            }
        };
        init();
    }

    private void init() {
        setZOrderOnTop(true);
        getHolder().setFormat(-2);
    }

    public static final class WindowConfig {
        public int displayHeight;
        public int displayWidth;
        public int height;
        public int locationOnScreenX;
        public int locationOnScreenY;
        public int startX;
        public int startY;
        public boolean usingNotch;
        public int width;

        public WindowConfig() {
            this(-1, -1, 0, 0, -1, -1, 0, 0);
        }

        public WindowConfig(int displayWidth2, int displayHeight2, int startX2, int startY2, int width2, int height2, int locationOnScreenX2, int locationOnScreenY2) {
            this.usingNotch = true;
            set(displayWidth2, displayHeight2, startX2, startY2, width2, height2, locationOnScreenX2, locationOnScreenY2);
        }

        private void set(int displayWidth2, int displayHeight2, int startX2, int startY2, int width2, int height2, int locationOnScreenX2, int locationOnScreenY2) {
            this.displayWidth = displayWidth2;
            this.displayHeight = displayHeight2;
            this.startX = startX2;
            this.startY = startY2;
            this.width = width2;
            this.height = height2;
            this.locationOnScreenX = locationOnScreenX2;
            this.locationOnScreenY = locationOnScreenY2;
        }

        public void update(int displayWidth2, int displayHeight2, int startX2, int startY2, int width2, int height2, int locationOnScreenX2, int locationOnScreenY2) {
            set(displayWidth2, displayHeight2, startX2, startY2, width2, height2, locationOnScreenX2, locationOnScreenY2);
        }

        public void udpateNotch(boolean isUsingNotch) {
            this.usingNotch = isUsingNotch;
        }

        public String toString() {
            return "d.w:" + this.displayWidth + ", d.h:" + this.displayHeight + ", s.x:" + this.startX + ", s.y:" + this.startY + ", w:" + this.width + ", h:" + this.height + ", uN:" + this.usingNotch + ", l.x:" + this.locationOnScreenX + ", l.y:" + this.locationOnScreenY;
        }
    }

    public void updateViewConfig(int displayWidth, int displayHeight, int startX, int startY, int width, int height, int locationOnScreenX, int locationOnScreenY) {
        this.mWindowConfig.update(displayWidth, displayHeight, startX, startY, width, height, locationOnScreenX, locationOnScreenY);
    }

    public void updateViewNotchState(boolean usingNotch) {
        this.mWindowConfig.udpateNotch(usingNotch);
    }

    public WindowConfig getViewConfig() {
        return this.mWindowConfig;
    }

    public int getNavId() {
        return this.mNavId;
    }

    public void setGestureEventProxy(IGestureEventProxy proxy) {
        this.mGestureEventProxy = proxy;
    }

    public void show(boolean isEnable, boolean delay) {
        if (isEnable || !delay) {
            removeCallbacks(this.mHideRunnable);
            setVisibility(isEnable ? 0 : 8);
            return;
        }
        postDelayed(this.mHideRunnable, 500);
    }

    public boolean onTouchEvent(MotionEvent event) {
        IGestureEventProxy iGestureEventProxy = this.mGestureEventProxy;
        if (iGestureEventProxy != null) {
            return iGestureEventProxy.onTouchEvent(this, event);
        }
        return super.onTouchEvent(event);
    }
}
