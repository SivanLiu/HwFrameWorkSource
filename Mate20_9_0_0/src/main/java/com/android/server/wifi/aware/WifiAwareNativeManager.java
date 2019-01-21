package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.HalDeviceManager.ManagerStatusListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiAwareNativeManager {
    private static final String TAG = "WifiAwareNativeManager";
    private static final boolean VDBG = false;
    boolean mDbg = false;
    private HalDeviceManager mHalDeviceManager;
    private Handler mHandler;
    private InterfaceAvailableForRequestListener mInterfaceAvailableForRequestListener = new InterfaceAvailableForRequestListener(this, null);
    private InterfaceDestroyedListener mInterfaceDestroyedListener;
    private final Object mLock = new Object();
    private int mReferenceCount = 0;
    private WifiAwareNativeCallback mWifiAwareNativeCallback;
    private WifiAwareStateManager mWifiAwareStateManager;
    private IWifiNanIface mWifiNanIface = null;

    private class InterfaceAvailableForRequestListener implements com.android.server.wifi.HalDeviceManager.InterfaceAvailableForRequestListener {
        private InterfaceAvailableForRequestListener() {
        }

        /* synthetic */ InterfaceAvailableForRequestListener(WifiAwareNativeManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onAvailabilityChanged(boolean isAvailable) {
            if (WifiAwareNativeManager.this.mDbg) {
                String str = WifiAwareNativeManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Interface availability = ");
                stringBuilder.append(isAvailable);
                stringBuilder.append(", mWifiNanIface=");
                stringBuilder.append(WifiAwareNativeManager.this.mWifiNanIface);
                Log.d(str, stringBuilder.toString());
            }
            synchronized (WifiAwareNativeManager.this.mLock) {
                if (isAvailable) {
                    try {
                        WifiAwareNativeManager.this.mWifiAwareStateManager.enableUsage();
                    } catch (Throwable th) {
                    }
                } else if (WifiAwareNativeManager.this.mWifiNanIface == null) {
                    WifiAwareNativeManager.this.mWifiAwareStateManager.disableUsage();
                }
            }
        }
    }

    private class InterfaceDestroyedListener implements com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener {
        public boolean active;

        private InterfaceDestroyedListener() {
            this.active = true;
        }

        /* synthetic */ InterfaceDestroyedListener(WifiAwareNativeManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onDestroyed(String ifaceName) {
            if (WifiAwareNativeManager.this.mDbg) {
                String str = WifiAwareNativeManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Interface was destroyed: mWifiNanIface=");
                stringBuilder.append(WifiAwareNativeManager.this.mWifiNanIface);
                stringBuilder.append(", active=");
                stringBuilder.append(this.active);
                Log.d(str, stringBuilder.toString());
            }
            if (this.active && WifiAwareNativeManager.this.mWifiNanIface != null) {
                WifiAwareNativeManager.this.awareIsDown();
            }
        }
    }

    WifiAwareNativeManager(WifiAwareStateManager awareStateManager, HalDeviceManager halDeviceManager, WifiAwareNativeCallback wifiAwareNativeCallback) {
        this.mWifiAwareStateManager = awareStateManager;
        this.mHalDeviceManager = halDeviceManager;
        this.mWifiAwareNativeCallback = wifiAwareNativeCallback;
    }

    public android.hardware.wifi.V1_2.IWifiNanIface mockableCastTo_1_2(IWifiNanIface iface) {
        return android.hardware.wifi.V1_2.IWifiNanIface.castFrom(iface);
    }

    public void start(Handler handler) {
        this.mHandler = handler;
        this.mHalDeviceManager.initialize();
        this.mHalDeviceManager.registerStatusListener(new ManagerStatusListener() {
            public void onStatusChanged() {
                if (WifiAwareNativeManager.this.mHalDeviceManager.isStarted()) {
                    WifiAwareNativeManager.this.mHalDeviceManager.registerInterfaceAvailableForRequestListener(3, WifiAwareNativeManager.this.mInterfaceAvailableForRequestListener, WifiAwareNativeManager.this.mHandler);
                } else {
                    WifiAwareNativeManager.this.awareIsDown();
                }
            }
        }, this.mHandler);
        if (this.mHalDeviceManager.isStarted()) {
            this.mHalDeviceManager.registerInterfaceAvailableForRequestListener(3, this.mInterfaceAvailableForRequestListener, this.mHandler);
        }
    }

    @VisibleForTesting
    public IWifiNanIface getWifiNanIface() {
        IWifiNanIface iWifiNanIface;
        synchronized (this.mLock) {
            iWifiNanIface = this.mWifiNanIface;
        }
        return iWifiNanIface;
    }

    /* JADX WARNING: Missing block: B:35:0x00b9, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void tryToGetAware() {
        synchronized (this.mLock) {
            if (this.mDbg) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("tryToGetAware: mWifiNanIface=");
                stringBuilder.append(this.mWifiNanIface);
                stringBuilder.append(", mReferenceCount=");
                stringBuilder.append(this.mReferenceCount);
                Log.d(str, stringBuilder.toString());
            }
            if (this.mWifiNanIface != null) {
                this.mReferenceCount++;
            } else if (this.mHalDeviceManager == null) {
                Log.e(TAG, "tryToGetAware: mHalDeviceManager is null!?");
                awareIsDown();
            } else {
                this.mInterfaceDestroyedListener = new InterfaceDestroyedListener(this, null);
                IWifiNanIface iface = this.mHalDeviceManager.createNanIface(this.mInterfaceDestroyedListener, this.mHandler);
                if (iface == null) {
                    Log.e(TAG, "Was not able to obtain an IWifiNanIface (even though enabled!?)");
                    awareIsDown();
                } else {
                    if (this.mDbg) {
                        Log.v(TAG, "Obtained an IWifiNanIface");
                    }
                    try {
                        WifiStatus status;
                        android.hardware.wifi.V1_2.IWifiNanIface iface12 = mockableCastTo_1_2(iface);
                        if (iface12 == null) {
                            this.mWifiAwareNativeCallback.mIsHal12OrLater = false;
                            status = iface.registerEventCallback(this.mWifiAwareNativeCallback);
                        } else {
                            this.mWifiAwareNativeCallback.mIsHal12OrLater = true;
                            status = iface12.registerEventCallback_1_2(this.mWifiAwareNativeCallback);
                        }
                        if (status.code != 0) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("IWifiNanIface.registerEventCallback error: ");
                            stringBuilder2.append(statusString(status));
                            Log.e(str2, stringBuilder2.toString());
                            this.mHalDeviceManager.removeIface(iface);
                            awareIsDown();
                            return;
                        }
                        this.mWifiNanIface = iface;
                        this.mReferenceCount = 1;
                    } catch (RemoteException e) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("IWifiNanIface.registerEventCallback exception: ");
                        stringBuilder3.append(e);
                        Log.e(str3, stringBuilder3.toString());
                        awareIsDown();
                    }
                }
            }
        }
    }

    public void releaseAware() {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("releaseAware: mWifiNanIface=");
            stringBuilder.append(this.mWifiNanIface);
            stringBuilder.append(", mReferenceCount=");
            stringBuilder.append(this.mReferenceCount);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mWifiNanIface != null) {
            if (this.mHalDeviceManager == null) {
                Log.e(TAG, "releaseAware: mHalDeviceManager is null!?");
                return;
            }
            synchronized (this.mLock) {
                this.mReferenceCount--;
                if (this.mReferenceCount != 0) {
                    return;
                }
                this.mInterfaceDestroyedListener.active = false;
                this.mInterfaceDestroyedListener = null;
                this.mHalDeviceManager.removeIface(this.mWifiNanIface);
                this.mWifiNanIface = null;
            }
        }
    }

    private void awareIsDown() {
        synchronized (this.mLock) {
            if (this.mDbg) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("awareIsDown: mWifiNanIface=");
                stringBuilder.append(this.mWifiNanIface);
                stringBuilder.append(", mReferenceCount =");
                stringBuilder.append(this.mReferenceCount);
                Log.d(str, stringBuilder.toString());
            }
            this.mWifiNanIface = null;
            this.mReferenceCount = 0;
            this.mWifiAwareStateManager.disableUsage();
        }
    }

    private static String statusString(WifiStatus status) {
        if (status == null) {
            return "status=null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(status.code);
        sb.append(" (");
        sb.append(status.description);
        sb.append(")");
        return sb.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareNativeManager:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mWifiNanIface: ");
        stringBuilder.append(this.mWifiNanIface);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mReferenceCount: ");
        stringBuilder.append(this.mReferenceCount);
        pw.println(stringBuilder.toString());
        this.mWifiAwareNativeCallback.dump(fd, pw, args);
        this.mHalDeviceManager.dump(fd, pw, args);
    }
}
