package android.net.wifi.p2p;

import android.net.wifi.p2p.IWifiP2pManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class WifiP2pManagerHisiExt {
    public static final String SWITCH_TO_P2P_MODE = "android.net.wifi.p2p.hisi.SWITCH_TO_P2P_MODE";
    private static final String TAG = "WifiP2pManagerHisiExt";
    private IWifiP2pManager wifiP2pService;

    public WifiP2pManagerHisiExt() {
        this.wifiP2pService = null;
        this.wifiP2pService = Stub.asInterface(ServiceManager.getService("wifip2p"));
    }

    public boolean setWifiP2pEnabled(int p2pFlag) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setWifiP2pEnabled() is called! p2pFlag =");
        stringBuilder.append(p2pFlag);
        Log.d(str, stringBuilder.toString());
        try {
            if (this.wifiP2pService != null) {
                return this.wifiP2pService.setWifiP2pEnabled(p2pFlag);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isWifiP2pEnabled() {
        Log.d(TAG, "isWifiP2pEnabled() is called!");
        try {
            if (this.wifiP2pService != null) {
                return this.wifiP2pService.isWifiP2pEnabled();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setRecoveryWifiFlag(boolean flag) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setRecoveryWifiFlag() is called! flag = ");
        stringBuilder.append(flag);
        Log.d(str, stringBuilder.toString());
        try {
            if (this.wifiP2pService != null) {
                this.wifiP2pService.setRecoveryWifiFlag(flag);
            }
        } catch (RemoteException e) {
        }
    }
}
