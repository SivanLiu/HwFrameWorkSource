package com.android.server.wm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.Slog;
import com.android.internal.view.RotationPolicy;
import com.huawei.IntelliServer.intellilib.IIntelliListener;
import com.huawei.IntelliServer.intellilib.IIntelliService;
import com.huawei.IntelliServer.intellilib.IIntelliService.Stub;
import com.huawei.IntelliServer.intellilib.IntelliAlgoResult;

public class IntelliServiceManager {
    private static final boolean DEBUG = Log.HWLog;
    private static final int DELAY_TIME = 800;
    private static final int INTELLI_ALGO_FACE_ORIENTION = 2;
    public static final int INTELLI_FACE_ORIENTION_TIMEMOUT = -2;
    private static final String RO_SWITCH = "ro.config.face_detect";
    private static final String RO_SWITCH_ROTATION = "ro.config.face_smart_rotation";
    private static final String SERVICE_ACTION = "com.huawei.intelliServer.intelliServer";
    private static final String SERVICE_CLASS = "com.huawei.intelliServer.intelliServer.IntelliService";
    private static final String SERVICE_PACKAGE = "com.huawei.intelliServer.intelliServer";
    private static final String SETTING_SWITCH = "smart_rotation_key";
    public static final String TAG = "IntelliServiceManager";
    private static int mDisplayRotation;
    private static IntelliServiceManager mInstance = null;
    private static int mSensorRotation;
    private FaceRotationCallback mCallback;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            IntelliServiceManager.this.mRemote = Stub.asInterface(iBinder);
            IntelliServiceManager.this.registerListener("serviceconnect");
            IntelliServiceManager.this.mContext.getMainThreadHandler().removeCallbacks(IntelliServiceManager.this.mFinishRunnble);
            IntelliServiceManager.this.mContext.getMainThreadHandler().postDelayed(IntelliServiceManager.this.mFinishRunnble, 800);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(IntelliServiceManager.TAG, "onServiceDisconnected");
            IntelliServiceManager.this.mRemote = null;
            IntelliServiceManager.this.mIsKeepPortrait = false;
            IntelliServiceManager.this.mIsRunning = false;
            IntelliServiceManager.this.mCallback = null;
        }
    };
    private Context mContext;
    private int mFaceRotaion = -2;
    Runnable mFinishRunnble = new Runnable() {
        public void run() {
            Slog.w(IntelliServiceManager.TAG, "detect face timeout");
            IntelliServiceManager.this.stopIntelliService(-2);
        }
    };
    private IIntelliListener mIntelliEventListener = new IIntelliListener.Stub() {
        public void onEvent(IntelliAlgoResult intelliAlgoResult) throws RemoteException {
            if (IntelliServiceManager.this.mCallback == null) {
                return;
            }
            if (intelliAlgoResult == null) {
                Slog.e(IntelliServiceManager.TAG, "intelliAlgoResult is null");
                IntelliServiceManager.this.stopIntelliService(-2);
            } else if (intelliAlgoResult.getRotation() != -1) {
                if (IntelliServiceManager.DEBUG) {
                    String str = IntelliServiceManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("intelliAlgoResult rotation: ");
                    stringBuilder.append(intelliAlgoResult.getRotation());
                    Slog.i(str, stringBuilder.toString());
                }
                IntelliServiceManager.this.stopIntelliService(intelliAlgoResult.getRotation());
            }
        }

        public void onErr(int i) throws RemoteException {
            String str = IntelliServiceManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("face Rotation onErr ");
            stringBuilder.append(i);
            Slog.e(str, stringBuilder.toString());
        }
    };
    private boolean mIsKeepPortrait = false;
    private boolean mIsRunning = false;
    private IIntelliService mRemote;
    private Intent mServiceIntent;
    private long startTime;

    public interface FaceRotationCallback {
        void onEvent(int i);
    }

    private IntelliServiceManager(Context context) {
        this.mContext = context;
        this.mServiceIntent = new Intent("com.huawei.intelliServer.intelliServer");
        this.mServiceIntent.setClassName("com.huawei.intelliServer.intelliServer", SERVICE_CLASS);
    }

    public static IntelliServiceManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (IntelliServiceManager.class) {
                if (mInstance == null) {
                    mInstance = new IntelliServiceManager(context);
                }
            }
        }
        return mInstance;
    }

    public static boolean isRotationConsistentFR() {
        boolean z = true;
        if (mDisplayRotation == -1 || mSensorRotation == -1) {
            return true;
        }
        if (mDisplayRotation != mSensorRotation) {
            z = false;
        }
        return z;
    }

    public static void setSensorRotation(int sensorRotation) {
        mSensorRotation = sensorRotation;
    }

    public static void setDisplayRotation(int displayRotation) {
        mDisplayRotation = displayRotation;
    }

    private void stopIntelliService(final int faceRotation) {
        this.mFaceRotaion = faceRotation;
        this.mIsKeepPortrait = false;
        new Thread(new Runnable() {
            public void run() {
                if (IntelliServiceManager.this.mCallback != null) {
                    IntelliServiceManager.this.mCallback.onEvent(faceRotation);
                }
                IntelliServiceManager.this.mContext.getMainThreadHandler().removeCallbacks(IntelliServiceManager.this.mFinishRunnble);
                IntelliServiceManager.this.mIsRunning = false;
                IntelliServiceManager.this.mCallback = null;
                if (IntelliServiceManager.this.mRemote != null) {
                    try {
                        IntelliServiceManager.this.mRemote.unregistListener(2, IntelliServiceManager.this.mIntelliEventListener);
                    } catch (RemoteException e) {
                        Slog.e(IntelliServiceManager.TAG, "unregisiterListener fail");
                    }
                    String str = IntelliServiceManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("close camera, detect cost ");
                    stringBuilder.append(Long.toString(IntelliServiceManager.this.startTime - System.currentTimeMillis()));
                    Slog.i(str, stringBuilder.toString());
                }
            }
        }).start();
    }

    public int getFaceRotaion() {
        return this.mFaceRotaion;
    }

    public boolean isKeepPortrait() {
        return this.mIsKeepPortrait;
    }

    public void setKeepPortrait(boolean IsKeepPortrait) {
        this.mIsKeepPortrait = IsKeepPortrait;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set IsKeepPortrait = ");
            stringBuilder.append(this.mIsKeepPortrait);
            Slog.d(str, stringBuilder.toString());
        }
    }

    private void registerListener(String reason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerListener for:");
        stringBuilder.append(reason);
        stringBuilder.append("cost ");
        stringBuilder.append(Long.toString(this.startTime - System.currentTimeMillis()));
        Slog.i(str, stringBuilder.toString());
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (IntelliServiceManager.this.mRemote != null) {
                        IntelliServiceManager.this.mRemote.registListener(2, IntelliServiceManager.this.mIntelliEventListener);
                    }
                } catch (RemoteException e) {
                    Slog.e(IntelliServiceManager.TAG, "regisiterListener fail ");
                }
            }
        }).start();
    }

    private void bindIntelliService() {
        new Thread(new Runnable() {
            public void run() {
                Slog.d(IntelliServiceManager.TAG, "bindIntelliService");
                IntelliServiceManager.this.mContext.bindServiceAsUser(IntelliServiceManager.this.mServiceIntent, IntelliServiceManager.this.mConnection, 1, UserHandle.CURRENT);
            }
        }).start();
    }

    public void unbindIntelliService() {
        if (this.mRemote != null) {
            new Thread(new Runnable() {
                public void run() {
                    Slog.d(IntelliServiceManager.TAG, "unbindIntelliService");
                    IntelliServiceManager.this.mContext.unbindService(IntelliServiceManager.this.mConnection);
                    IntelliServiceManager.this.mRemote = null;
                }
            }).start();
        }
    }

    public void startIntelliService(FaceRotationCallback faceRotationCallback) {
        if (faceRotationCallback == null) {
            Slog.w(TAG, "face rotation call back is null");
            return;
        }
        if (!this.mIsRunning) {
            this.mIsRunning = true;
            this.startTime = System.currentTimeMillis();
            if (this.mRemote == null) {
                bindIntelliService();
            } else {
                registerListener("startservice");
            }
        } else if (DEBUG) {
            Slog.d(TAG, "IntelliService is already running");
        }
        this.mContext.getMainThreadHandler().removeCallbacks(this.mFinishRunnble);
        this.mContext.getMainThreadHandler().postDelayed(this.mFinishRunnble, 800);
        this.mCallback = faceRotationCallback;
    }

    public static boolean isIntelliServiceEnabled(Context context, int orientation, int userId) {
        if (RotationPolicy.isRotationLocked(context) || SystemProperties.getInt(RO_SWITCH, 0) == 0 || !SystemProperties.getBoolean(RO_SWITCH_ROTATION, true)) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (Secure.getIntForUser(context.getContentResolver(), SETTING_SWITCH, 0, userId) != 1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setting switch is off for user ");
            stringBuilder.append(userId);
            Slog.d(str, stringBuilder.toString());
            return false;
        } else if (orientation == 0 || orientation == 1 || orientation == 5 || orientation == 6 || orientation == 8) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("skip process orientation ");
            stringBuilder.append(orientation);
            Slog.d(str, stringBuilder.toString());
            return false;
        } else if (!isRotationConsistentFR()) {
            return true;
        } else {
            Slog.d(TAG, "sensor rotation consistent with display");
            return false;
        }
    }
}
