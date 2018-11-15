package com.android.server.display;

import android.os.RemoteException;
import android.pc.IHwPCManager;
import android.util.HwPCUtils;
import android.util.Slog;

public class HwWifiDisplayAdapterEx implements IHwWifiDisplayAdapterEx {
    public static final String TAG = "HwWifiDisplayAdapterEx";
    IWifiDisplayAdapterInner mWfda;

    public HwWifiDisplayAdapterEx(IWifiDisplayAdapterInner wfda) {
        this.mWfda = wfda;
    }

    public void requestStartScanLocked(final int channelId) {
        this.mWfda.getHandlerInner().post(new Runnable() {
            public void run() {
                if (HwWifiDisplayAdapterEx.this.mWfda.getmDisplayControllerInner() != null) {
                    HwWifiDisplayAdapterEx.this.mWfda.getmDisplayControllerInner().requestStartScan(channelId);
                }
            }
        });
    }

    public void setConnectParameters(String address) {
        boolean isSupportHdcp = this.mWfda.getmPersistentDataStoreInner().isHdcpSupported(address);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestConnectLocked: isSupportHdcp ");
        stringBuilder.append(isSupportHdcp);
        Slog.d(str, stringBuilder.toString());
        this.mWfda.getmDisplayControllerInner().setConnectParameters(isSupportHdcp, null);
    }

    public void requestConnectLocked(final String address, final String verificaitonCode) {
        this.mWfda.getHandlerInner().post(new Runnable() {
            public void run() {
                if (HwWifiDisplayAdapterEx.this.mWfda.getmDisplayControllerInner() != null) {
                    HwWifiDisplayAdapterEx.this.mWfda.getmDisplayControllerInner().requestConnect(address);
                    boolean isSupportHdcp = HwWifiDisplayAdapterEx.this.mWfda.getmPersistentDataStoreInner().isHdcpSupported(address);
                    String str = HwWifiDisplayAdapterEx.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestConnectLocked: isSupportHdcp ");
                    stringBuilder.append(isSupportHdcp);
                    Slog.d(str, stringBuilder.toString());
                    HwWifiDisplayAdapterEx.this.mWfda.getmDisplayControllerInner().setConnectParameters(isSupportHdcp, verificaitonCode);
                }
            }
        });
    }

    public void checkVerificationResultLocked(final boolean isRight) {
        this.mWfda.getHandlerInner().post(new Runnable() {
            public void run() {
                if (HwWifiDisplayAdapterEx.this.mWfda.getmDisplayControllerInner() != null) {
                    HwWifiDisplayAdapterEx.this.mWfda.getmDisplayControllerInner().checkVerificationResult(isRight);
                }
            }
        });
    }

    public void LaunchMKForWifiMode() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.LaunchMKForWifiMode();
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException LaunchMKForWifiMode");
            }
        }
    }
}
