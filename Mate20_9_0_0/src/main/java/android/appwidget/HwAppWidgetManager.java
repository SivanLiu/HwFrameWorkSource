package android.appwidget;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;
import com.android.internal.appwidget.IAppWidgetService;
import com.android.internal.appwidget.IAppWidgetService.Stub;

public class HwAppWidgetManager {
    private static final Singleton<IHwAppWidgetManager> IAppWidgetManagerSingleton = new Singleton<IHwAppWidgetManager>() {
        protected IHwAppWidgetManager create() {
            try {
                IAppWidgetService aws = Stub.asInterface(ServiceManager.getService(Context.APPWIDGET_SERVICE));
                if (aws != null) {
                    return IHwAppWidgetManager.Stub.asInterface(aws.getHwInnerService());
                }
                Log.e(HwAppWidgetManager.TAG, "get IAppWidgetService failed");
                return null;
            } catch (RemoteException e) {
                String str = HwAppWidgetManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IHwAppWidgetManager create() fail: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
    };
    private static final String TAG = "HwAppWidgetManager";

    public static IHwAppWidgetManager getService() {
        return (IHwAppWidgetManager) IAppWidgetManagerSingleton.get();
    }

    public static boolean registerAWSIMonitorCallback(IHwAWSIDAMonitorCallback callback) {
        if (getService() == null) {
            return false;
        }
        try {
            getService().registerAWSIMonitorCallback(callback);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "registerWMMonitorCallback catch RemoteException!");
            return false;
        }
    }
}
