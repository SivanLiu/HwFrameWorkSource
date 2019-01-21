package com.huawei.android.gameassist;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.Singleton;
import android.util.Slog;
import android.view.InputEvent;
import com.huawei.android.gameassist.IGamePadAIDL.Stub;

public class HwGameAssistGamePad {
    private static final Singleton<IGamePadAIDL> IHwGameAssistGamePadSingleton = new Singleton<IGamePadAIDL>() {
        protected IGamePadAIDL create() {
            if (HwGameAssistGamePad.mService == null) {
                HwGameAssistGamePad.bindService();
            }
            return HwGameAssistGamePad.mService;
        }
    };
    public static final int NO_SEND = 2;
    public static final int SEND_FAIL = 1;
    public static final int SEND_SUCCESS = 0;
    private static final String TAG = "HwGameAssistGamePad";
    static long mBindTime;
    private static ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            HwGameAssistGamePad.mService = Stub.asInterface(service);
            String str = HwGameAssistGamePad.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceConnected ");
            stringBuilder.append(HwGameAssistGamePad.mService);
            Slog.d(str, stringBuilder.toString());
            HwGameAssistGamePad.mSumDelayEvent = 0;
        }

        public void onServiceDisconnected(ComponentName className) {
            String str = HwGameAssistGamePad.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceDisconnected ");
            stringBuilder.append(HwGameAssistGamePad.mService);
            Slog.d(str, stringBuilder.toString());
            HwGameAssistGamePad.mService = null;
        }
    };
    public static Context mContext = null;
    static int mKeyMaxDelayTime = 5;
    static IGamePadAIDL mService = null;
    static long mSumDelayEvent = 0;

    public static IGamePadAIDL getService() {
        IHwGameAssistGamePadSingleton.get();
        return mService;
    }

    public static void bindService() {
        if (mService == null) {
            mBindTime = SystemClock.uptimeMillis();
            Intent intent = new Intent();
            Slog.d(TAG, "bindService");
            intent.setAction("com.huawei.gameassistant.equipservice");
            intent.setPackage("com.huawei.gameassistant");
            if (mContext != null) {
                try {
                    mContext.bindServiceAsUser(intent, mConnection, 1, UserHandle.SYSTEM);
                } catch (Exception e) {
                    Slog.d(TAG, "bindServiceAsUser failed: equipservice!");
                }
            } else {
                Slog.w(TAG, "mContext == null");
            }
        }
    }

    public static void unbindService() {
        if (mService != null) {
            Slog.d(TAG, "unbindService");
            mContext.unbindService(mConnection);
            mService = null;
        }
    }

    public static int notifyInputEvent(InputEvent event) {
        IGamePadAIDL gameService = getService();
        if (gameService != null) {
            try {
                long now = SystemClock.uptimeMillis();
                Trace.traceBegin(4, "gamepad");
                gameService.notifyInputEvent(event);
                Trace.traceEnd(4);
                if (SystemClock.uptimeMillis() - now > ((long) mKeyMaxDelayTime)) {
                    mSumDelayEvent++;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("GamePad notifyKeyEvent delay ");
                    stringBuilder.append(SystemClock.uptimeMillis() - now);
                    stringBuilder.append(" ms  ");
                    stringBuilder.append(mSumDelayEvent);
                    Slog.w(str, stringBuilder.toString());
                }
                return 0;
            } catch (RemoteException e) {
                Slog.d(TAG, "notifyKeyEvent failed: catch RemoteException!");
                unbindService();
                mService = null;
                return 1;
            }
        } else if (SystemClock.uptimeMillis() - mBindTime > 3000) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Service is null");
            stringBuilder2.append(gameService);
            Slog.d(str2, stringBuilder2.toString());
            return 1;
        } else {
            Slog.d(TAG, "Service is connecting...");
            return 2;
        }
    }
}
