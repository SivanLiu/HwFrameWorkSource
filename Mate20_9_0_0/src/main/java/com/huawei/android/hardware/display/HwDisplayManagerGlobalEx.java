package com.huawei.android.hardware.display;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Singleton;
import com.huawei.android.hardware.display.IHwDisplayManager.Stub;
import java.util.regex.Pattern;

public class HwDisplayManagerGlobalEx implements IHwDisplayManagerGlobalEx {
    private final Singleton<IHwDisplayManager> IDisplayManagerSingleton = new Singleton<IHwDisplayManager>() {
        protected IHwDisplayManager create() {
            try {
                return Stub.asInterface(HwDisplayManagerGlobalEx.this.mDmg.getService().getHwInnerService());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    };
    IHwDisplayManagerGlobalInner mDmg;

    private IHwDisplayManager getService() {
        return (IHwDisplayManager) this.IDisplayManagerSingleton.get();
    }

    public HwDisplayManagerGlobalEx(IHwDisplayManagerGlobalInner dmg) {
        this.mDmg = dmg;
    }

    public void startWifiDisplayScan(int channelId) {
        if (getService() != null) {
            synchronized (this.mDmg.getLock()) {
                if (this.mDmg.getWifiDisplayScanNestCount() == 0) {
                    this.mDmg.addWifiDisplayScanNestCount();
                    this.mDmg.registerCallbackIfNeededLockedInner();
                    try {
                        getService().startWifiDisplayScan(channelId);
                    } catch (RemoteException ex) {
                        throw ex.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    public void connectWifiDisplay(String deviceAddress, String verificaitonCode) {
        String vCode = "";
        if (TextUtils.isEmpty(verificaitonCode)) {
            throw new IllegalArgumentException("verificaitonCode must not be null in this connectType");
        } else if (Pattern.compile("^[0-9A-Z]{4}$").matcher(verificaitonCode).matches()) {
            try {
                getService().connectWifiDisplay(deviceAddress, verificaitonCode);
            } catch (RemoteException p) {
                throw p.rethrowFromSystemServer();
            }
        } else {
            throw new IllegalArgumentException("the pattern of verificaitonCode error!");
        }
    }

    public void checkVerificationResult(boolean isRight) {
        try {
            getService().checkVerificationResult(isRight);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }
}
