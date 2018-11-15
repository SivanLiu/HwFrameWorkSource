package com.android.server.display;

import android.app.ActivityManager;
import android.opengl.Matrix;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import java.lang.reflect.Array;
import java.util.Arrays;

public class DisplayTransformManager {
    static final float COLOR_SATURATION_BOOSTED = 1.1f;
    static final float COLOR_SATURATION_NATURAL = 1.0f;
    public static final int LEVEL_COLOR_MATRIX_GRAYSCALE = 200;
    public static final int LEVEL_COLOR_MATRIX_INVERT_COLOR = 300;
    public static final int LEVEL_COLOR_MATRIX_NIGHT_DISPLAY = 100;
    static final String PERSISTENT_PROPERTY_NATIVE_MODE = "persist.sys.sf.native_mode";
    static final String PERSISTENT_PROPERTY_SATURATION = "persist.sys.sf.color_saturation";
    private static final int SURFACE_FLINGER_TRANSACTION_COLOR_MATRIX = 1015;
    private static final int SURFACE_FLINGER_TRANSACTION_DALTONIZER = 1014;
    private static final int SURFACE_FLINGER_TRANSACTION_NATIVE_MODE = 1023;
    private static final int SURFACE_FLINGER_TRANSACTION_SATURATION = 1022;
    private static final String TAG = "DisplayTransformManager";
    @GuardedBy("mColorMatrix")
    private final SparseArray<float[]> mColorMatrix = new SparseArray(3);
    @GuardedBy("mDaltonizerModeLock")
    private int mDaltonizerMode = -1;
    private final Object mDaltonizerModeLock = new Object();
    private IBinder mSurfaceFlinger;
    @GuardedBy("mColorMatrix")
    private final float[][] mTempColorMatrix = ((float[][]) Array.newInstance(Float.TYPE, new int[]{2, 16}));

    DisplayTransformManager() {
    }

    public IBinder getSurfaceFlinger() {
        if (this.mSurfaceFlinger == null) {
            this.mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        }
        return this.mSurfaceFlinger;
    }

    public float[] getColorMatrix(int key) {
        float[] fArr = null;
        synchronized (this.mColorMatrix) {
            float[] value = (float[]) this.mColorMatrix.get(key);
            if (value != null) {
                fArr = Arrays.copyOf(value, value.length);
            }
        }
        return fArr;
    }

    public void setColorMatrix(int level, float[] value) {
        if (value == null || value.length == 16) {
            synchronized (this.mColorMatrix) {
                float[] oldValue = (float[]) this.mColorMatrix.get(level);
                if (!Arrays.equals(oldValue, value)) {
                    if (value == null) {
                        this.mColorMatrix.remove(level);
                    } else if (oldValue == null) {
                        this.mColorMatrix.put(level, Arrays.copyOf(value, value.length));
                    } else {
                        System.arraycopy(value, 0, oldValue, 0, value.length);
                    }
                    applyColorMatrix(computeColorMatrixLocked());
                }
            }
            return;
        }
        throw new IllegalArgumentException("Expected length: 16 (4x4 matrix), actual length: " + value.length);
    }

    @GuardedBy("mColorMatrix")
    private float[] computeColorMatrixLocked() {
        int count = this.mColorMatrix.size();
        if (count == 0) {
            return null;
        }
        float[][] result = this.mTempColorMatrix;
        Matrix.setIdentityM(result[0], 0);
        for (int i = 0; i < count; i++) {
            Matrix.multiplyMM(result[(i + 1) % 2], 0, result[i % 2], 0, (float[]) this.mColorMatrix.valueAt(i), 0);
        }
        return result[count % 2];
    }

    public int getDaltonizerMode() {
        int i;
        synchronized (this.mDaltonizerModeLock) {
            i = this.mDaltonizerMode;
        }
        return i;
    }

    public void setDaltonizerMode(int mode) {
        synchronized (this.mDaltonizerModeLock) {
            if (this.mDaltonizerMode != mode) {
                this.mDaltonizerMode = mode;
                applyDaltonizerMode(mode);
            }
        }
    }

    private static void applyColorMatrix(float[] m) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            if (m != null) {
                data.writeInt(1);
                for (int i = 0; i < 16; i++) {
                    data.writeFloat(m[i]);
                }
            } else {
                data.writeInt(0);
            }
            try {
                flinger.transact(SURFACE_FLINGER_TRANSACTION_COLOR_MATRIX, data, null, 0);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to set color transform", ex);
            } finally {
                data.recycle();
            }
        }
    }

    private static void applyDaltonizerMode(int mode) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(mode);
            try {
                flinger.transact(SURFACE_FLINGER_TRANSACTION_DALTONIZER, data, null, 0);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to set Daltonizer mode", ex);
            } finally {
                data.recycle();
            }
        }
    }

    public static boolean isNativeModeEnabled() {
        return SystemProperties.getBoolean(PERSISTENT_PROPERTY_NATIVE_MODE, false);
    }

    public boolean setColorMode(int colorMode) {
        if (colorMode == 0) {
            applySaturation(1.0f);
            setNativeMode(false);
        } else if (colorMode == 1) {
            applySaturation(COLOR_SATURATION_BOOSTED);
            setNativeMode(false);
        } else if (colorMode == 2) {
            applySaturation(1.0f);
            setNativeMode(true);
        }
        updateConfiguration();
        return true;
    }

    private void applySaturation(float saturation) {
        SystemProperties.set(PERSISTENT_PROPERTY_SATURATION, Float.toString(saturation));
        if (getSurfaceFlinger() != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeFloat(saturation);
            try {
                getSurfaceFlinger().transact(SURFACE_FLINGER_TRANSACTION_SATURATION, data, null, 0);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to set saturation", ex);
            } finally {
                data.recycle();
            }
        }
    }

    private void setNativeMode(boolean enabled) {
        SystemProperties.set(PERSISTENT_PROPERTY_NATIVE_MODE, enabled ? "1" : "0");
        if (getSurfaceFlinger() != null) {
            int i;
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            if (enabled) {
                i = 1;
            } else {
                i = 0;
            }
            data.writeInt(i);
            try {
                getSurfaceFlinger().transact(SURFACE_FLINGER_TRANSACTION_NATIVE_MODE, data, null, 0);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to set native mode", ex);
            } finally {
                data.recycle();
            }
        }
    }

    void updateConfiguration() {
        try {
            ActivityManager.getService().updateConfiguration(null);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not update configuration", e);
        }
    }
}
