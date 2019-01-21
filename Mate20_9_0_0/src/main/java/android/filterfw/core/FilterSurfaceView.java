package android.filterfw.core;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class FilterSurfaceView extends SurfaceView implements Callback {
    private static int STATE_ALLOCATED = 0;
    private static int STATE_CREATED = 1;
    private static int STATE_INITIALIZED = 2;
    private int mFormat;
    private GLEnvironment mGLEnv;
    private int mHeight;
    private Callback mListener;
    private int mState = STATE_ALLOCATED;
    private int mSurfaceId = -1;
    private int mWidth;

    public FilterSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public FilterSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public synchronized void bindToListener(Callback listener, GLEnvironment glEnv) {
        if (listener != null) {
            if (this.mListener != null) {
                if (this.mListener != listener) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempting to bind filter ");
                    stringBuilder.append(listener);
                    stringBuilder.append(" to SurfaceView with another open filter ");
                    stringBuilder.append(this.mListener);
                    stringBuilder.append(" attached already!");
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
            this.mListener = listener;
            if (!(this.mGLEnv == null || this.mGLEnv == glEnv)) {
                this.mGLEnv.unregisterSurfaceId(this.mSurfaceId);
            }
            this.mGLEnv = glEnv;
            if (this.mState >= STATE_CREATED) {
                registerSurface();
                this.mListener.surfaceCreated(getHolder());
                if (this.mState == STATE_INITIALIZED) {
                    this.mListener.surfaceChanged(getHolder(), this.mFormat, this.mWidth, this.mHeight);
                }
            }
        } else {
            throw new NullPointerException("Attempting to bind null filter to SurfaceView!");
        }
    }

    public synchronized void unbind() {
        this.mListener = null;
    }

    public synchronized int getSurfaceId() {
        return this.mSurfaceId;
    }

    public synchronized GLEnvironment getGLEnv() {
        return this.mGLEnv;
    }

    public synchronized void surfaceCreated(SurfaceHolder holder) {
        this.mState = STATE_CREATED;
        if (this.mGLEnv != null) {
            registerSurface();
        }
        if (this.mListener != null) {
            this.mListener.surfaceCreated(holder);
        }
    }

    public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.mFormat = format;
        this.mWidth = width;
        this.mHeight = height;
        this.mState = STATE_INITIALIZED;
        if (this.mListener != null) {
            this.mListener.surfaceChanged(holder, format, width, height);
        }
    }

    public synchronized void surfaceDestroyed(SurfaceHolder holder) {
        this.mState = STATE_ALLOCATED;
        if (this.mListener != null) {
            this.mListener.surfaceDestroyed(holder);
        }
        unregisterSurface();
    }

    private void registerSurface() {
        this.mSurfaceId = this.mGLEnv.registerSurface(getHolder().getSurface());
        if (this.mSurfaceId < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not register Surface: ");
            stringBuilder.append(getHolder().getSurface());
            stringBuilder.append(" in FilterSurfaceView!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    private void unregisterSurface() {
        if (this.mGLEnv != null && this.mSurfaceId > 0) {
            this.mGLEnv.unregisterSurfaceId(this.mSurfaceId);
        }
    }
}
