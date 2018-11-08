package android.app;

import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.input.InputManager;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import dalvik.system.CloseGuard;

public class ActivityView extends ViewGroup {
    private static final String DISPLAY_NAME = "ActivityViewVirtualDisplay";
    private static final String TAG = "ActivityView";
    private StateCallback mActivityViewCallback;
    private final CloseGuard mGuard;
    private IInputForwarder mInputForwarder;
    private boolean mOpened;
    private Surface mSurface;
    private final SurfaceCallback mSurfaceCallback;
    private final SurfaceView mSurfaceView;
    private VirtualDisplay mVirtualDisplay;

    public static abstract class StateCallback {
        public abstract void onActivityViewDestroyed(ActivityView activityView);

        public abstract void onActivityViewReady(ActivityView activityView);
    }

    private class SurfaceCallback implements Callback {
        private SurfaceCallback() {
        }

        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            ActivityView.this.mSurface = ActivityView.this.mSurfaceView.getHolder().getSurface();
            if (ActivityView.this.mVirtualDisplay == null) {
                ActivityView.this.initVirtualDisplay();
                if (ActivityView.this.mVirtualDisplay != null && ActivityView.this.mActivityViewCallback != null) {
                    ActivityView.this.mActivityViewCallback.onActivityViewReady(ActivityView.this);
                    return;
                }
                return;
            }
            ActivityView.this.mVirtualDisplay.setSurface(surfaceHolder.getSurface());
        }

        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            if (ActivityView.this.mVirtualDisplay != null) {
                ActivityView.this.mVirtualDisplay.resize(width, height, ActivityView.this.getBaseDisplayDensity());
            }
        }

        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            ActivityView.this.mSurface.release();
            ActivityView.this.mSurface = null;
            if (ActivityView.this.mVirtualDisplay != null) {
                ActivityView.this.mVirtualDisplay.setSurface(null);
            }
        }
    }

    public ActivityView(Context context) {
        this(context, null);
    }

    public ActivityView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActivityView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mGuard = CloseGuard.get();
        this.mSurfaceView = new SurfaceView(context);
        this.mSurfaceCallback = new SurfaceCallback();
        this.mSurfaceView.getHolder().addCallback(this.mSurfaceCallback);
        addView(this.mSurfaceView);
        this.mOpened = true;
        this.mGuard.open("release");
    }

    public void setCallback(StateCallback callback) {
        this.mActivityViewCallback = callback;
        if (this.mVirtualDisplay != null && this.mActivityViewCallback != null) {
            this.mActivityViewCallback.onActivityViewReady(this);
        }
    }

    public void startActivity(Intent intent) {
        getContext().startActivity(intent, prepareActivityOptions().toBundle());
    }

    public void startActivity(PendingIntent pendingIntent) {
        try {
            pendingIntent.send(null, 0, null, null, null, null, prepareActivityOptions().toBundle());
        } catch (CanceledException e) {
            throw new RuntimeException(e);
        }
    }

    private ActivityOptions prepareActivityOptions() {
        if (this.mVirtualDisplay == null) {
            throw new IllegalStateException("Trying to start activity before ActivityView is ready.");
        }
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(this.mVirtualDisplay.getDisplay().getDisplayId());
        return options;
    }

    public void release() {
        if (this.mVirtualDisplay == null) {
            throw new IllegalStateException("Trying to release container that is not initialized.");
        }
        performRelease();
    }

    public void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mSurfaceView.layout(0, 0, r - l, b - t);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return !injectInputEvent(event) ? super.onTouchEvent(event) : true;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.isFromSource(2) && injectInputEvent(event)) {
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private boolean injectInputEvent(InputEvent event) {
        if (this.mInputForwarder != null) {
            try {
                return this.mInputForwarder.forwardEvent(event);
            } catch (RemoteException e) {
                e.rethrowAsRuntimeException();
            }
        }
        return false;
    }

    private void initVirtualDisplay() {
        if (this.mVirtualDisplay != null) {
            throw new IllegalStateException("Trying to initialize for the second time.");
        }
        this.mVirtualDisplay = ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).createVirtualDisplay("ActivityViewVirtualDisplay@" + System.identityHashCode(this), this.mSurfaceView.getWidth(), this.mSurfaceView.getHeight(), getBaseDisplayDensity(), this.mSurface, 0);
        if (this.mVirtualDisplay == null) {
            Log.e(TAG, "Failed to initialize ActivityView");
        } else {
            this.mInputForwarder = InputManager.getInstance().createInputForwarder(this.mVirtualDisplay.getDisplay().getDisplayId());
        }
    }

    private void performRelease() {
        if (this.mOpened) {
            boolean displayReleased;
            this.mSurfaceView.getHolder().removeCallback(this.mSurfaceCallback);
            if (this.mInputForwarder != null) {
                this.mInputForwarder = null;
            }
            if (this.mVirtualDisplay != null) {
                this.mVirtualDisplay.release();
                this.mVirtualDisplay = null;
                displayReleased = true;
            } else {
                displayReleased = false;
            }
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            if (displayReleased && this.mActivityViewCallback != null) {
                this.mActivityViewCallback.onActivityViewDestroyed(this);
            }
            this.mGuard.close();
            this.mOpened = false;
        }
    }

    private int getBaseDisplayDensity() {
        WindowManager wm = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.densityDpi;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGuard != null) {
                this.mGuard.warnIfOpen();
                performRelease();
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }
}
