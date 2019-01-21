package android.emcom;

import android.content.Context;
import android.emcom.IEmcomManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import org.json.JSONObject;

public final class EmcomManager {
    private static final int DEF_MAINCARD_PS_STATUS = 0;
    public static final int EMCOMMANAGER_ERR = -1;
    public static final int EMCOMMANAGER_OK = 0;
    private static final int SLICE_OPERATION_FAILED = 1001;
    private static final String TAG = "EmcomManager";
    private static EmcomManager mEmcomManager;
    private IHandoffSdkInterface mHandoffService;
    private IEmcomManager mService;

    public static synchronized EmcomManager getInstance() {
        EmcomManager emcomManager;
        synchronized (EmcomManager.class) {
            if (mEmcomManager == null) {
                mEmcomManager = new EmcomManager();
            }
            emcomManager = mEmcomManager;
        }
        return emcomManager;
    }

    private IEmcomManager getService() {
        this.mService = Stub.asInterface(ServiceManager.getService(TAG));
        if (this.mService == null) {
            Log.i(TAG, "IEmcomManager getService() is null ");
        }
        return this.mService;
    }

    private IHandoffSdkInterface getHandoffSdkService() {
        this.mHandoffService = IHandoffSdkInterface.Stub.asInterface(ServiceManager.getService("com.huawei.pcassistant.handoffsdk.HandoffSdkService"));
        if (this.mHandoffService == null) {
            Log.i(TAG, "IHandoffSdkInterface getService() is null ");
        }
        return this.mHandoffService;
    }

    public XEngineAppInfo getAppInfo(Context context) {
        if (context == null) {
            Log.i(TAG, "context is null!");
            return null;
        }
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return null;
        }
        try {
            return service.getAppInfo(context.getPackageName());
        } catch (RemoteException e) {
            Log.i(TAG, "getAppInfo RemoteException ");
            return null;
        }
    }

    public void accelerate(Context context, int grade) {
        accelerateWithMainCardPsStatus(context, grade, 0);
    }

    public void accelerateWithMainCardPsStatus(Context context, int grade, int mainCardPsStatus) {
        if (context == null) {
            Log.i(TAG, "context is null!");
            return;
        }
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.accelerateWithMainCardServiceStatus(context.getPackageName(), grade, mainCardPsStatus);
        } catch (RemoteException e) {
            Log.i(TAG, "accelerate RemoteException ");
        }
    }

    public void notifyEmailData(EmailInfo eci) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.notifyEmailData(eci);
        } catch (RemoteException e) {
            Log.i(TAG, "notifyEmailData RemoteException ");
        }
    }

    public void notifyVideoData(VideoInfo eci) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.notifyVideoData(eci);
        } catch (RemoteException e) {
            Log.i(TAG, "notifyVideoData RemoteException ");
        }
    }

    public void notifyHwAppData(String module, String pkgName, String info) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.notifyHwAppData(module, pkgName, info);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyHwAppData RemoteException: ");
            stringBuilder.append(e.toString());
            Log.i(str, stringBuilder.toString());
        }
    }

    public void notifyAppData(String info) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.notifyAppData(info);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyAppData RemoteException: ");
            stringBuilder.append(e.toString());
            Log.i(str, stringBuilder.toString());
        }
    }

    public void responseForParaUpgrade(int paratype, int pathtype, int result) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.responseForParaUpgrade(paratype, pathtype, result);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("responseForParaUpgrade: paratype = ");
            stringBuilder.append(paratype);
            stringBuilder.append(", pathtype = ");
            stringBuilder.append(pathtype);
            stringBuilder.append(", result = ");
            stringBuilder.append(result);
            Log.i(str, stringBuilder.toString());
        } catch (RemoteException e) {
            Log.e(TAG, "responseForParaUpgrade RemoteException ");
        }
    }

    public void updateAppExperienceStatus(int uid, int experience, int rrt) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.updateAppExperienceStatus(uid, experience, rrt);
        } catch (RemoteException e) {
            Log.i(TAG, "updateAppExperienceStatus RemoteException ");
        }
    }

    public void notifyRunningStatus(int type, String packageName) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.notifyRunningStatus(type, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "notifyRunningStatus: RemoteException ");
        }
    }

    public String getSmartcareData(String module, String pkgName, String jsonStr) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.i(TAG, "getEmcomservice is null ");
            return null;
        }
        try {
            return service.getSmartcareData(module, pkgName, jsonStr);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSmartcareData RemoteException: ");
            stringBuilder.append(e.toString());
            Log.i(str, stringBuilder.toString());
            return null;
        }
    }

    public int registerAppCallback(String packageName, ISliceSdkCallback callback) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e("SliceSdkLogger", "EmcomManager: getEmcomservice is null ");
            return 1001;
        }
        try {
            return service.registerAppCallback(packageName, callback);
        } catch (RemoteException e) {
            Log.e("SliceSdkLogger", "EmcomManager: registerAppCallback: RemoteException ");
            return 1001;
        }
    }

    public void activeSlice(String packageName, String version, int sessionNumber, String serverList) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e("SliceSdkLogger", "EmcomManager: getEmcomservice is null ");
            return;
        }
        try {
            service.activeSlice(packageName, version, sessionNumber, serverList);
        } catch (RemoteException e) {
            Log.e("SliceSdkLogger", "EmcomManager: activeSlice: RemoteException ");
        }
    }

    public void deactiveSlice(String packageName, String version, int sessionNumber, String saId) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e("SliceSdkLogger", "EmcomManager: getEmcomservice is null ");
            return;
        }
        try {
            service.deactiveSlice(packageName, version, sessionNumber, saId);
        } catch (RemoteException e) {
            Log.e("SliceSdkLogger", "EmcomManager: deactiveSlice: RemoteException ");
        }
    }

    public void updateAppInfo(String packageName, String version, int sessionNumber, String saId, String appInfoJson) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e("SliceSdkLogger", "EmcomManager: getEmcomservice is null ");
            return;
        }
        try {
            service.updateAppInfo(packageName, version, sessionNumber, saId, appInfoJson);
        } catch (RemoteException e) {
            Log.e("SliceSdkLogger", "EmcomManager: updateAppInfo: RemoteException ");
        }
    }

    public String getRuntimeInfo(String packageName) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e("SliceSdkLogger", "EmcomManager: getEmcomservice is null ");
            return null;
        }
        try {
            return service.getRuntimeInfo(packageName);
        } catch (RemoteException e) {
            Log.e("SliceSdkLogger", "EmcomManager: getPhoneInfo: RemoteException ");
            return null;
        }
    }

    public int registerHandoff(String packageName, int dataType, IHandoffSdkCallback callback) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerHandoff packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(" DataType: ");
        stringBuilder.append(dataType);
        Log.d(str, stringBuilder.toString());
        IEmcomManager service = getService();
        if (service == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("registerHandoff getEmcomservice is null package: ");
            stringBuilder2.append(packageName);
            Log.e(str2, stringBuilder2.toString());
            return -1;
        }
        try {
            int retCode = service.registerHandoff(packageName, dataType, callback);
            if (retCode != 0) {
                return retCode;
            }
            return 0;
        } catch (RemoteException e) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("registerHandoff RemoteException package: ");
            stringBuilder3.append(packageName);
            Log.e(str3, stringBuilder3.toString());
            e.printStackTrace();
            return -1;
        }
    }

    public int notifyHandoffServiceStart(IHandoffServiceCallback callback) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyHandoffServiceStart callback: ");
        stringBuilder.append(callback);
        Log.d(str, stringBuilder.toString());
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "notifyHandoffServiceStart getEmcomservice is null");
            return -1;
        }
        try {
            int retCode = service.notifyHandoffServiceStart(callback);
            if (retCode != 0) {
                return retCode;
            }
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "notifyHandoffServiceStart RemoteException ");
            e.printStackTrace();
            return -1;
        }
    }

    public int notifyHandoffServiceStop() {
        Log.d(TAG, "notifyHandoffServiceStop ");
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "notifyHandoffServiceStop getEmcomservice is null");
            return -1;
        }
        try {
            return service.notifyHandoffServiceStop();
        } catch (RemoteException e) {
            Log.e(TAG, "notifyHandoffServiceStop RemoteException ");
            return -1;
        }
    }

    public void notifyHandoffStateChg(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyHandoffStateChg state: ");
        stringBuilder.append(state);
        Log.d(str, stringBuilder.toString());
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "notifyHandoffStateChg getEmcomservice is null");
            return;
        }
        try {
            service.notifyHandoffStateChg(state);
        } catch (RemoteException e) {
            Log.e(TAG, "notifyHandoffStateChg RemoteException ");
            e.printStackTrace();
        }
    }

    public int notifyHandoffDataEvent(String packageName, String para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyHandoffDataEvent packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(" para: ");
        stringBuilder.append(para);
        Log.d(str, stringBuilder.toString());
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "notifyHandoffDataEvent getEmcomservice is null");
            return -1;
        }
        try {
            int retCode = service.notifyHandoffDataEvent(packageName, para);
            if (retCode != 0) {
                return retCode;
            }
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "notifyHandoffDataEvent RemoteException ");
            e.printStackTrace();
            return -1;
        }
    }

    public int startHandoffService(String packageName, JSONObject para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startHandoffService packageName: ");
        stringBuilder.append(packageName);
        Log.d(str, stringBuilder.toString());
        IHandoffSdkInterface service = getHandoffSdkService();
        if (service == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startHandoffService getHandoffSdkService is null package: ");
            stringBuilder2.append(packageName);
            Log.e(str2, stringBuilder2.toString());
            return -1;
        } else if (isEnableHandoff()) {
            try {
                int retCode = service.startHandoffService(packageName, para.toString());
                if (retCode != 0) {
                    return retCode;
                }
                return 0;
            } catch (RemoteException e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("startHandoffService RemoteException package: ");
                stringBuilder3.append(packageName);
                Log.e(str3, stringBuilder3.toString());
                e.printStackTrace();
                return -1;
            }
        } else {
            String str4 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("startHandoffService but handoff disconnect packageName: ");
            stringBuilder4.append(packageName);
            Log.d(str4, stringBuilder4.toString());
            return -1;
        }
    }

    public int stopHandoffService(String packageName, JSONObject para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopHandoffService packageName: ");
        stringBuilder.append(packageName);
        Log.d(str, stringBuilder.toString());
        IHandoffSdkInterface service = getHandoffSdkService();
        if (service == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stopHandoffService getHandoffSdkService is null package: ");
            stringBuilder2.append(packageName);
            Log.e(str2, stringBuilder2.toString());
            return -1;
        } else if (isEnableHandoff()) {
            try {
                int retCode = service.stopHandoffService(packageName, para.toString());
                if (retCode != 0) {
                    return retCode;
                }
                return 0;
            } catch (RemoteException e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("stopHandoffService RemoteException package: ");
                stringBuilder3.append(packageName);
                Log.e(str3, stringBuilder3.toString());
                e.printStackTrace();
                return -1;
            }
        } else {
            String str4 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("stopHandoffService but handoff disconnect packageName: ");
            stringBuilder4.append(packageName);
            Log.d(str4, stringBuilder4.toString());
            return -1;
        }
    }

    public int syncHandoffData(String packageName, JSONObject para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("syncHandoffData packageName: ");
        stringBuilder.append(packageName);
        Log.d(str, stringBuilder.toString());
        IHandoffSdkInterface service = getHandoffSdkService();
        if (service == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("syncHandoffData getHandoffSdkService is null package: ");
            stringBuilder2.append(packageName);
            Log.e(str2, stringBuilder2.toString());
            return -1;
        } else if (isEnableHandoff()) {
            try {
                int retCode = service.syncHandoffData(packageName, para.toString());
                if (retCode != 0) {
                    return retCode;
                }
                return 0;
            } catch (RemoteException e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("syncHandoffData RemoteException package: ");
                stringBuilder3.append(packageName);
                Log.e(str3, stringBuilder3.toString());
                e.printStackTrace();
                return -1;
            }
        } else {
            String str4 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("syncHandoffData but handoff disconnect packageName: ");
            stringBuilder4.append(packageName);
            Log.d(str4, stringBuilder4.toString());
            return -1;
        }
    }

    public boolean isEnableHandoff() {
        boolean bEnable = false;
        Log.d(TAG, "isEnableHandoff");
        IHandoffSdkInterface service = getHandoffSdkService();
        if (service == null) {
            Log.e(TAG, "isEnableHandoff getHandoffSdkService is null ");
            return false;
        }
        try {
            bEnable = service.isEnableHandoff();
        } catch (RemoteException e) {
            Log.e(TAG, "isEnableHandoff RemoteException  ");
            e.printStackTrace();
        }
        return bEnable;
    }

    public void notifySmartMp(int status) {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "getEmcomservice is null ");
            return;
        }
        try {
            service.notifySmartMp(status);
        } catch (RemoteException e) {
            Log.e(TAG, "notifySmartMp: RemoteException ");
        }
    }

    public boolean isSmartMpEnable() {
        IEmcomManager service = getService();
        if (service == null) {
            Log.e(TAG, "getEmcomservice is null ");
            return false;
        }
        try {
            return service.isSmartMpEnable();
        } catch (RemoteException e) {
            Log.e(TAG, "isSmartMpEnable(): RemoteException ");
            return false;
        }
    }
}
