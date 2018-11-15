package com.android.server.gesture;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import com.android.server.gesture.GestureNavView.IGestureNavBackAnim;
import com.android.server.gesture.GestureNavView.WindowConfig;
import com.android.server.gesture.anim.GLGestureBackView;
import com.android.server.gesture.anim.GLGestureBackView.GestureBackAnimListener;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;

public class GestureNavAnimProxy implements IGestureNavBackAnim {
    private static final int DEFAULT_ANIM_MAX_TIME = 2000;
    private static final int MSG_ANIM_TIMEOUT = 1;
    private static final String TAG = "GestureNavAnim";
    private GLGestureBackView mBackAnimView;
    private GestureAnimContainer mBackContainer;
    private Context mContext;
    private boolean mGestureNavReady;
    private Handler mHandler;
    private final Object mLock = new Object();
    private WindowConfig mWindowConfig = new WindowConfig();
    private WindowManager mWindowManager;
    private boolean mWindowViewSetuped;

    interface AnimContainerListener {
        void onAttachedToWindow();

        void onDetachedFromWindow();
    }

    private final class AnimHandler extends Handler {
        public AnimHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Log.i(GestureNavAnimProxy.TAG, "animation timeout, force hide views");
                GestureNavAnimProxy.this.showBackContainer(false);
            }
        }
    }

    static class GestureAnimContainer extends FrameLayout {
        private AnimContainerListener mListener;

        public GestureAnimContainer(Context context) {
            super(context);
        }

        public void setListener(AnimContainerListener listener) {
            this.mListener = listener;
        }

        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (this.mListener != null) {
                this.mListener.onAttachedToWindow();
            }
        }

        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (this.mListener != null) {
                this.mListener.onDetachedFromWindow();
            }
        }
    }

    private final class AnimContainerListenerImpl implements AnimContainerListener {
        private AnimContainerListenerImpl() {
        }

        public void onAttachedToWindow() {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavAnimProxy.TAG, "container attached to window");
            }
            if (GestureNavAnimProxy.this.mBackAnimView != null) {
                GestureNavAnimProxy.this.mBackAnimView.onResume();
            }
        }

        public void onDetachedFromWindow() {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavAnimProxy.TAG, "container detached from window");
            }
            if (GestureNavAnimProxy.this.mBackAnimView != null) {
                GestureNavAnimProxy.this.mBackAnimView.onPause();
            }
        }
    }

    private final class GestureBackAnimListenerImpl implements GestureBackAnimListener {
        private GestureBackAnimListenerImpl() {
        }

        public void onAnimationEnd(int animType) {
            if (GestureNavConst.DEBUG) {
                String str = GestureNavAnimProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("back anim end, animType=");
                stringBuilder.append(animType);
                Log.d(str, stringBuilder.toString());
            }
            GestureNavAnimProxy.this.mHandler.removeMessages(1);
            if (GestureNavAnimProxy.this.mBackAnimView != null) {
                GestureNavAnimProxy.this.mBackAnimView.setDraw(false);
            }
            GestureNavAnimProxy.this.showBackContainer(false);
        }
    }

    public GestureNavAnimProxy(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new AnimHandler(looper);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
    }

    private void showBackContainer(boolean show) {
        if (GestureNavConst.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showBackContainer show=");
            stringBuilder.append(show);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mBackContainer != null) {
            this.mBackContainer.setVisibility(show ? 0 : 8);
        }
    }

    private void updateGestureNavAnimWindow() {
        synchronized (this.mLock) {
            updateAnimWindowLocked();
        }
    }

    private void updateAnimWindowLocked() {
        if (this.mGestureNavReady) {
            if (this.mWindowViewSetuped) {
                updateAnimWindows();
            } else {
                createAnimWindows();
            }
        } else if (this.mWindowViewSetuped) {
            destroyNavWindows();
        }
    }

    private void createAnimWindows() {
        Log.i(TAG, "createAnimWindows");
        this.mBackContainer = new GestureAnimContainer(this.mContext);
        this.mBackContainer.setListener(new AnimContainerListenerImpl());
        this.mBackAnimView = new GLGestureBackView(this.mContext);
        this.mBackAnimView.addAnimationListener(new GestureBackAnimListenerImpl());
        this.mBackContainer.addView(this.mBackAnimView, -1, -1);
        GestureUtils.addWindowView(this.mWindowManager, this.mBackContainer, createLayoutParams(TAG, this.mWindowConfig));
        showBackContainer(false);
        this.mWindowViewSetuped = true;
    }

    private void updateAnimWindows() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "updateAnimWindows");
        }
        GestureUtils.updateViewLayout(this.mWindowManager, this.mBackContainer, createLayoutParams(TAG, this.mWindowConfig));
    }

    private void destroyNavWindows() {
        Log.i(TAG, "destroyNavWindows");
        this.mWindowViewSetuped = false;
        showBackContainer(false);
        GestureUtils.removeWindowView(this.mWindowManager, this.mBackContainer, true);
        this.mBackContainer = null;
    }

    private LayoutParams createLayoutParams(String title, WindowConfig config) {
        LayoutParams lp = new LayoutParams(HwArbitrationDEFS.MSG_VPN_STATE_OPEN, MemoryConstant.MSG_PREREAD_DATA_REMOVE);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.format = -2;
        lp.gravity = 51;
        lp.x = config.startX;
        lp.y = config.startY;
        lp.width = config.width;
        lp.height = config.height;
        lp.windowAnimations = 0;
        lp.softInputMode = 49;
        lp.setTitle(title);
        if (config.usingNotch) {
            lp.hwFlags |= 65536;
        } else {
            lp.hwFlags &= -65537;
        }
        return lp;
    }

    public void updateViewConfig(int _displayWidth, int _displayHeight, int _startX, int _startY, int _width, int _height, int _locationOnScreenX, int _locationOnScreenY) {
        synchronized (this.mLock) {
            this.mWindowConfig.update(_displayWidth, _displayHeight, _startX, _startY, _width, _height, _locationOnScreenX, _locationOnScreenY);
        }
    }

    public void updateViewNotchState(boolean usingNotch) {
        synchronized (this.mLock) {
            this.mWindowConfig.udpateNotch(usingNotch);
        }
    }

    public void onNavCreate() {
        this.mGestureNavReady = true;
        updateGestureNavAnimWindow();
    }

    public void onNavUpdate() {
        updateGestureNavAnimWindow();
    }

    public void onNavDestroy() {
        this.mGestureNavReady = false;
        updateGestureNavAnimWindow();
    }

    public void onGestureAction(boolean down) {
    }

    public void setSide(boolean isLeft) {
        if (GestureNavConst.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSide isLeft=");
            stringBuilder.append(isLeft);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mBackAnimView != null) {
            this.mBackAnimView.setSide(isLeft);
        }
    }

    public void setAnimPosition(float y) {
        if (GestureNavConst.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAnimPosition y=");
            stringBuilder.append(y);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mBackAnimView != null) {
            showBackContainer(true);
            this.mBackAnimView.setDraw(true);
            this.mHandler.removeMessages(1);
            this.mBackAnimView.setAnimPosition(y);
        }
    }

    public boolean setAnimProcess(float process) {
        if (this.mBackAnimView == null) {
            return false;
        }
        this.mBackAnimView.setAnimProcess(process);
        return true;
    }

    public void playDisappearAnim() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "playDisappearAnim");
        }
        if (this.mBackAnimView != null) {
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendEmptyMessageDelayed(1, 2000);
            }
            this.mBackAnimView.playDisappearAnim();
        }
    }

    public void playFastSlidingAnim() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "playFastSlidingAnim");
        }
        if (this.mBackAnimView != null) {
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendEmptyMessageDelayed(1, 2000);
            }
            this.mBackAnimView.playFastSlidingAnim();
        }
    }
}
