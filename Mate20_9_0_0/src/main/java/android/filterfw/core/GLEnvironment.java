package android.filterfw.core;

import android.graphics.SurfaceTexture;
import android.media.MediaRecorder;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

public class GLEnvironment {
    private int glEnvId;
    private boolean mManageContext = true;

    private native boolean nativeActivate();

    private native boolean nativeActivateSurfaceId(int i);

    private native int nativeAddSurface(Surface surface);

    private native int nativeAddSurfaceFromMediaRecorder(MediaRecorder mediaRecorder);

    private native int nativeAddSurfaceWidthHeight(Surface surface, int i, int i2);

    private native boolean nativeAllocate();

    private native boolean nativeDeactivate();

    private native boolean nativeDeallocate();

    private native boolean nativeDisconnectSurfaceMediaSource(MediaRecorder mediaRecorder);

    private native boolean nativeInitWithCurrentContext();

    private native boolean nativeInitWithNewContext();

    private native boolean nativeIsActive();

    private static native boolean nativeIsAnyContextActive();

    private native boolean nativeIsContextActive();

    private native boolean nativeRemoveSurfaceId(int i);

    private native boolean nativeSetSurfaceTimestamp(long j);

    private native boolean nativeSwapBuffers();

    public GLEnvironment() {
        nativeAllocate();
    }

    private GLEnvironment(NativeAllocatorTag tag) {
    }

    public synchronized void tearDown() {
        if (this.glEnvId != -1) {
            nativeDeallocate();
            this.glEnvId = -1;
        }
    }

    protected void finalize() throws Throwable {
        tearDown();
    }

    public void initWithNewContext() {
        this.mManageContext = true;
        if (!nativeInitWithNewContext()) {
            throw new RuntimeException("Could not initialize GLEnvironment with new context!");
        }
    }

    public void initWithCurrentContext() {
        this.mManageContext = false;
        if (!nativeInitWithCurrentContext()) {
            throw new RuntimeException("Could not initialize GLEnvironment with current context!");
        }
    }

    public boolean isActive() {
        return nativeIsActive();
    }

    public boolean isContextActive() {
        return nativeIsContextActive();
    }

    public static boolean isAnyContextActive() {
        return nativeIsAnyContextActive();
    }

    public void activate() {
        if (Looper.myLooper() != null && Looper.myLooper().equals(Looper.getMainLooper())) {
            Log.e("FilterFramework", "Activating GL context in UI thread!");
        }
        if (this.mManageContext && !nativeActivate()) {
            throw new RuntimeException("Could not activate GLEnvironment!");
        }
    }

    public void deactivate() {
        if (this.mManageContext && !nativeDeactivate()) {
            throw new RuntimeException("Could not deactivate GLEnvironment!");
        }
    }

    public void swapBuffers() {
        if (!nativeSwapBuffers()) {
            throw new RuntimeException("Error swapping EGL buffers!");
        }
    }

    public int registerSurface(Surface surface) {
        int result = nativeAddSurface(surface);
        if (result >= 0) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error registering surface ");
        stringBuilder.append(surface);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public int registerSurfaceTexture(SurfaceTexture surfaceTexture, int width, int height) {
        Surface surface = new Surface(surfaceTexture);
        int result = nativeAddSurfaceWidthHeight(surface, width, height);
        surface.release();
        if (result >= 0) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error registering surfaceTexture ");
        stringBuilder.append(surfaceTexture);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public int registerSurfaceFromMediaRecorder(MediaRecorder mediaRecorder) {
        int result = nativeAddSurfaceFromMediaRecorder(mediaRecorder);
        if (result >= 0) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error registering surface from MediaRecorder");
        stringBuilder.append(mediaRecorder);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public void activateSurfaceWithId(int surfaceId) {
        if (!nativeActivateSurfaceId(surfaceId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not activate surface ");
            stringBuilder.append(surfaceId);
            stringBuilder.append("!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public void unregisterSurfaceId(int surfaceId) {
        if (!nativeRemoveSurfaceId(surfaceId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not unregister surface ");
            stringBuilder.append(surfaceId);
            stringBuilder.append("!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public void setSurfaceTimestamp(long timestamp) {
        if (!nativeSetSurfaceTimestamp(timestamp)) {
            throw new RuntimeException("Could not set timestamp for current surface!");
        }
    }

    static {
        System.loadLibrary("filterfw");
    }
}
