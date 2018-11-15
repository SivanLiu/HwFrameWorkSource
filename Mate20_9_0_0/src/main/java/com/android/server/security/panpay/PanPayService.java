package com.android.server.security.panpay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.core.IHwSecurityPlugin.Creator;
import huawei.android.security.panpay.IPanPay.Stub;
import huawei.android.security.panpay.IPanPayCallBack;

public class PanPayService extends Stub implements IHwSecurityPlugin {
    public static final Object BINDLOCK = new Object();
    public static final Creator CREATOR = new Creator() {
        public IHwSecurityPlugin createPlugin(Context context) {
            Log.d(PanPayService.TAG, "create PanPayService");
            return new PanPayService(context);
        }

        public String getPluginPermission() {
            return null;
        }
    };
    private static final String OP_DELETE_SSD = "OP_DELETE_SSD";
    private static final String OP_EXEC_APDU = "OP_EXEC_APDU";
    private static final String OP_QUERY_APP_LIST = "OP_QUERY_APP_LIST";
    private static final String OP_QUERY_SSD_INSTALLED = "OP_QUERY_SSD_INSTALLED";
    private static final String PANPAY_MANAGER_PERMISSION = "com.huawei.ukey.permission.UKEY_MANAGER";
    private static final String TAG = "PanPayService";
    private Context mContext = null;

    private class ConnectReceiver extends BroadcastReceiver {
        private ConnectReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                try {
                    ConnectivityManager cm = (ConnectivityManager) PanPayService.this.mContext.getSystemService("connectivity");
                    if (cm != null) {
                        NetworkInfo info = cm.getActiveNetworkInfo();
                        if (info != null && info.isAvailable()) {
                            Log.d(PanPayService.TAG, "PanPayService Network isAvailable");
                            PanPayService.this.onNetworkConnected();
                        }
                    }
                } catch (Exception e) {
                    String str = PanPayService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PanPayService Network isAvailable getconf Exception");
                    stringBuilder.append(e.getMessage());
                    Log.d(str, stringBuilder.toString());
                }
            }
        }
    }

    public PanPayService(Context context) {
        this.mContext = context;
        Log.d(TAG, "create PanPayService PanPayService");
    }

    public void onStart() {
        this.mContext.enforceCallingOrSelfPermission(PANPAY_MANAGER_PERMISSION, "does not have ukey manager permission!");
    }

    public void onStop() {
        this.mContext.enforceCallingOrSelfPermission(PANPAY_MANAGER_PERMISSION, "does not have ukey manager permission!");
    }

    public IBinder asBinder() {
        this.mContext.enforceCallingOrSelfPermission(PANPAY_MANAGER_PERMISSION, "does not have ukey manager permission!");
        return this;
    }

    private void onNetworkConnected() {
    }

    public void updateAppInfo(String type, String key, IPanPayCallBack callback) {
        this.mContext.enforceCallingOrSelfPermission(PANPAY_MANAGER_PERMISSION, "does not have ukey manager permission!");
    }

    public void operateAppInfo(String type, String key, IPanPayCallBack callback) {
        this.mContext.enforceCallingOrSelfPermission(PANPAY_MANAGER_PERMISSION, "does not have ukey manager permission!");
    }
}
