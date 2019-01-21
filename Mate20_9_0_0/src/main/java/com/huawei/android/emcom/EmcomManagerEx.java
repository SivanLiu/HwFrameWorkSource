package com.huawei.android.emcom;

import android.emcom.EmailInfo;
import android.emcom.EmcomManager;
import android.emcom.IHandoffSdkCallback;
import android.emcom.IHandoffServiceCallback;
import android.emcom.ISliceSdkCallback;
import android.emcom.VideoInfo;
import android.os.RemoteException;
import android.util.Log;
import org.json.JSONObject;

public class EmcomManagerEx {
    private static final String TAG = "EmcomManagerEx";
    private static volatile EmcomManagerEx mEmcomManagerEx;

    public static synchronized EmcomManagerEx getInstance() {
        EmcomManagerEx emcomManagerEx;
        synchronized (EmcomManagerEx.class) {
            if (mEmcomManagerEx == null) {
                mEmcomManagerEx = new EmcomManagerEx();
            }
            emcomManagerEx = mEmcomManagerEx;
        }
        return emcomManagerEx;
    }

    public static void notifyEmailData(Object obj) throws RemoteException {
        if (obj instanceof EmailInfo) {
            EmailInfo eci = (EmailInfo) obj;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyEmailData eci=");
            stringBuilder.append(eci);
            Log.d(str, stringBuilder.toString());
            EmcomManager.getInstance().notifyEmailData(eci);
            return;
        }
        Log.d(TAG, "illegal EmailData");
    }

    public static void notifyVideoData(Object obj) throws RemoteException {
        if (obj instanceof VideoInfo) {
            VideoInfo vci = (VideoInfo) obj;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyVideoData vci = ");
            stringBuilder.append(vci);
            Log.e(str, stringBuilder.toString());
            EmcomManager.getInstance().notifyVideoData(vci);
            return;
        }
        Log.d(TAG, "illegal VideolData");
    }

    public static void notifyHwAppData(String module, String pkgName, String obj) throws RemoteException {
        if (obj != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyHwAppData info = ");
            stringBuilder.append(obj);
            Log.d(str, stringBuilder.toString());
            EmcomManager.getInstance().notifyHwAppData(module, pkgName, obj);
            return;
        }
        Log.d(TAG, "illegal notifyHwAppData");
    }

    public void responseForParaUpgrade(int paratype, int pathtype, int result) {
        EmcomManager.getInstance().responseForParaUpgrade(paratype, pathtype, result);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("responseForParaUpgrade: paratype = ");
        stringBuilder.append(paratype);
        stringBuilder.append(", pathtype = ");
        stringBuilder.append(pathtype);
        stringBuilder.append(", result = ");
        stringBuilder.append(result);
        Log.i(str, stringBuilder.toString());
    }

    public int registerAppCallback(String packageName, ISliceSdkCallback callback) throws NoSuchMethodError {
        return EmcomManager.getInstance().registerAppCallback(packageName, callback);
    }

    public void activeSlice(String packageName, String version, int sessionNumber, String serverList) throws NoSuchMethodError {
        EmcomManager.getInstance().activeSlice(packageName, version, sessionNumber, serverList);
    }

    public void deactiveSlice(String packageName, String version, int sessionNumber, String saId) throws NoSuchMethodError {
        EmcomManager.getInstance().deactiveSlice(packageName, version, sessionNumber, saId);
    }

    public void updateAppInfo(String packageName, String version, int sessionNumber, String saId, String appInfoJson) throws NoSuchMethodError {
        EmcomManager.getInstance().updateAppInfo(packageName, version, sessionNumber, saId, appInfoJson);
    }

    public String getRuntimeInfo(String packageName) throws NoSuchMethodError {
        return EmcomManager.getInstance().getRuntimeInfo(packageName);
    }

    public int registerHandoff(String packageName, int dataType, IHandoffSdkCallback callback) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerHandoff packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(" DataType: ");
        stringBuilder.append(dataType);
        Log.d(str, stringBuilder.toString());
        return EmcomManager.getInstance().registerHandoff(packageName, dataType, callback);
    }

    public int notifyHandoffServiceStart(IHandoffServiceCallback callback) {
        Log.d(TAG, "notifyHandoffServiceStart ");
        return EmcomManager.getInstance().notifyHandoffServiceStart(callback);
    }

    public int notifyHandoffServiceStop() {
        Log.d(TAG, "notifyHandoffServiceStop ");
        return EmcomManager.getInstance().notifyHandoffServiceStop();
    }

    public void notifyHandoffStateChg(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyHandoffStateChg state: ");
        stringBuilder.append(state);
        Log.d(str, stringBuilder.toString());
        EmcomManager.getInstance().notifyHandoffStateChg(state);
    }

    public int notifyHandoffDataEvent(String packageName, String para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyHandoffDataEvent packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(" para: ");
        stringBuilder.append(para);
        Log.d(str, stringBuilder.toString());
        return EmcomManager.getInstance().notifyHandoffDataEvent(packageName, para);
    }

    public int startHandoffService(String packageName, JSONObject para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startHandoffService packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(" para: ");
        stringBuilder.append(para.toString());
        Log.d(str, stringBuilder.toString());
        return EmcomManager.getInstance().startHandoffService(packageName, para);
    }

    public int stopHandoffService(String packageName, JSONObject para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopHandoffService packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(" para: ");
        stringBuilder.append(para.toString());
        Log.d(str, stringBuilder.toString());
        return EmcomManager.getInstance().stopHandoffService(packageName, para);
    }

    public int syncHandoffData(String packageName, JSONObject para) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("syncHandoffData packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(" para: ");
        stringBuilder.append(para.toString());
        Log.d(str, stringBuilder.toString());
        return EmcomManager.getInstance().syncHandoffData(packageName, para);
    }

    public boolean isEnableHandoff() {
        boolean isEnableHandoff = EmcomManager.getInstance().isEnableHandoff();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isEnableHandoff: ");
        stringBuilder.append(isEnableHandoff);
        Log.d(str, stringBuilder.toString());
        return isEnableHandoff;
    }

    public static void notifySmartMp(int status) {
        EmcomManager.getInstance().notifySmartMp(status);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifySmartMp: staus = ");
        stringBuilder.append(status);
        Log.i(str, stringBuilder.toString());
    }
}
