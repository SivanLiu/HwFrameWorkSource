package com.android.internal.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.huawei.pgmng.PGAction;

public class ScreenshotHelper {
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";
    private static final String SYSUI_SCREENSHOT_SERVICE = "com.android.systemui.screenshot.TakeScreenshotService";
    private static final String TAG = "ScreenshotHelper";
    private final int SCREENSHOT_TIMEOUT_MS = PGAction.PG_ID_DEFAULT_FRONT;
    private final Context mContext;
    private ServiceConnection mScreenshotConnection = null;
    private final Object mScreenshotLock = new Object();

    public ScreenshotHelper(Context context) {
        this.mContext = context;
    }

    /* JADX WARNING: Missing block: B:15:0x004b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void takeScreenshot(int screenshotType, boolean hasStatus, boolean hasNav, Handler handler) {
        Throwable th;
        synchronized (this.mScreenshotLock) {
            final Handler handler2;
            try {
                if (this.mScreenshotConnection != null) {
                    return;
                }
                ComponentName serviceComponent = new ComponentName(SYSUI_PACKAGE, SYSUI_SCREENSHOT_SERVICE);
                Intent serviceIntent = new Intent();
                Runnable mScreenshotTimeout = new Runnable() {
                    public void run() {
                        synchronized (ScreenshotHelper.this.mScreenshotLock) {
                            if (ScreenshotHelper.this.mScreenshotConnection != null) {
                                ScreenshotHelper.this.mContext.unbindService(ScreenshotHelper.this.mScreenshotConnection);
                                ScreenshotHelper.this.mScreenshotConnection = null;
                                ScreenshotHelper.this.notifyScreenshotError();
                            }
                        }
                    }
                };
                serviceIntent.setComponent(serviceComponent);
                final int i = screenshotType;
                handler2 = handler;
                final Runnable runnable = mScreenshotTimeout;
                final boolean z = hasStatus;
                final boolean z2 = hasNav;
                AnonymousClass2 conn = new ServiceConnection() {
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        synchronized (ScreenshotHelper.this.mScreenshotLock) {
                            if (ScreenshotHelper.this.mScreenshotConnection != this) {
                                return;
                            }
                            Messenger messenger = new Messenger(service);
                            Message msg = Message.obtain(null, i);
                            msg.replyTo = new Messenger(new Handler(handler2.getLooper()) {
                                public void handleMessage(Message msg) {
                                    synchronized (ScreenshotHelper.this.mScreenshotLock) {
                                        if (ScreenshotHelper.this.mScreenshotConnection == this) {
                                            ScreenshotHelper.this.mContext.unbindService(ScreenshotHelper.this.mScreenshotConnection);
                                            ScreenshotHelper.this.mScreenshotConnection = null;
                                            handler2.removeCallbacks(runnable);
                                        }
                                    }
                                }
                            });
                            msg.arg1 = z;
                            msg.arg2 = z2;
                            try {
                                messenger.send(msg);
                            } catch (RemoteException e) {
                                String str = ScreenshotHelper.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Couldn't take screenshot: ");
                                stringBuilder.append(e);
                                Log.e(str, stringBuilder.toString());
                            }
                        }
                    }

                    public void onServiceDisconnected(ComponentName name) {
                        synchronized (ScreenshotHelper.this.mScreenshotLock) {
                            if (ScreenshotHelper.this.mScreenshotConnection != null) {
                                ScreenshotHelper.this.mContext.unbindService(ScreenshotHelper.this.mScreenshotConnection);
                                ScreenshotHelper.this.mScreenshotConnection = null;
                                handler2.removeCallbacks(runnable);
                                ScreenshotHelper.this.notifyScreenshotError();
                            }
                        }
                    }
                };
                if (this.mContext.bindServiceAsUser(serviceIntent, conn, 33554433, UserHandle.CURRENT)) {
                    this.mScreenshotConnection = conn;
                    handler.postDelayed(mScreenshotTimeout, 10000);
                } else {
                    handler2 = handler;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void notifyScreenshotError() {
        ComponentName errorComponent = new ComponentName(SYSUI_PACKAGE, SYSUI_SCREENSHOT_ERROR_RECEIVER);
        Intent errorIntent = new Intent("android.intent.action.USER_PRESENT");
        errorIntent.setComponent(errorComponent);
        errorIntent.addFlags(335544320);
        this.mContext.sendBroadcastAsUser(errorIntent, UserHandle.CURRENT);
    }
}
