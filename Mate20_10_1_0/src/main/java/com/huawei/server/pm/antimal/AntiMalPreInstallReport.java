package com.huawei.server.pm.antimal;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.securitydiagnose.HwSecurityDiagnoseManager;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import com.huawei.server.security.securitydiagnose.AntiMalApkInfo;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.android.security.IHwSecurityDiagnosePlugin;
import huawei.android.security.IHwSecurityService;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Iterator;

public class AntiMalPreInstallReport {
    private static final int CREDIBLE_NOT_TRUST = 0;
    private static final int DEVICE_SECURE_DIAGNOSE_ID = 2;
    private static final int INIT_SIZE = 64;
    private static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String SECURITY_SERVICE = "securityserver";
    private static final int STATUS_RISK = 1;
    private static final int STATUS_SAFE = 0;
    private static final int STP_VERSION = 1;
    private static final String TAG = "HW-AntiMalPreInstallReport";
    private static final int THREATEN_ID = 517;
    private AntiMalDataManager mDataManager;
    private ReportListener mListener;
    private Thread mReportTask = new Thread("AntiMalReportTask") {
        /* class com.huawei.server.pm.antimal.AntiMalPreInstallReport.AnonymousClass1 */

        public void run() {
            AntiMalPreInstallReport.this.sendAntimaComponentInfo();
            AntiMalPreInstallReport.this.sendAntiMalData();
            AntiMalPreInstallReport.this.reportAntiMalData();
        }
    };

    interface ReportListener {
        void onReported();
    }

    public AntiMalPreInstallReport(AntiMalDataManager dataManager) {
        this.mDataManager = dataManager;
    }

    private static class AntiMalUncaughtException implements Thread.UncaughtExceptionHandler {
        private AntiMalUncaughtException() {
        }

        public void uncaughtException(Thread t, Throwable e) {
            Log.e(AntiMalPreInstallReport.TAG, "catch uncaughtException.");
        }
    }

    public void report(ReportListener listener) {
        this.mListener = listener;
        this.mReportTask.setUncaughtExceptionHandler(new AntiMalUncaughtException());
        this.mReportTask.start();
    }

    /* access modifiers changed from: private */
    public void sendAntimaComponentInfo() {
        IHwSecurityDiagnosePlugin secDgnService;
        Bundle bundle = this.mDataManager.getAntimalComponentInfo();
        try {
            IHwSecurityService secServie = IHwSecurityService.Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));
            if (secServie != null && (secDgnService = IHwSecurityDiagnosePlugin.Stub.asInterface(secServie.querySecurityInterface(2))) != null) {
                secDgnService.sendComponentInfo(bundle);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "security diagnostics service fails to send ComponentInfo.");
        }
    }

    /* access modifiers changed from: private */
    public void sendAntiMalData() {
        if (IS_HW_DEBUG) {
            Log.d(TAG, "sendAntiMalData begin!");
        }
        if (this.mDataManager.isNeedReport()) {
            doReport();
        }
        if (this.mDataManager.isNeedScanIllegalApks()) {
            this.mDataManager.writeAntiMalData();
        }
        onReported();
    }

    /* access modifiers changed from: private */
    public void reportAntiMalData() {
        String extendInfo;
        if (IS_HW_DEBUG) {
            Log.d(TAG, "ReportAntiMalData begin!");
        }
        HwSecurityDiagnoseManager sdm = HwSecurityDiagnoseManager.getInstance();
        if (sdm == null) {
            Log.w(TAG, "Get security diagnose manager's instance failed when report antimal data.");
            return;
        }
        String componentListInfo = getComponentListInfo();
        if (!TextUtils.isEmpty(componentListInfo)) {
            String appListInfo = getAppListInfo();
            int status = (this.mDataManager.isApksAbnormal() || !this.mDataManager.isCurAntiMalResultNormal()) ? 1 : 0;
            if (IS_HW_DEBUG) {
                Log.d(TAG, "Report " + (status == 1 ? "risk" : "safe") + " info");
                StringBuilder sb = new StringBuilder();
                sb.append("Apk abnromal : ");
                sb.append(this.mDataManager.isApksAbnormal());
                sb.append(", component abnormal : ");
                sb.append(!this.mDataManager.isCurAntiMalResultNormal());
                Log.d(TAG, sb.toString());
            }
            if (status == 1) {
                extendInfo = "ComponentInfo:[" + componentListInfo + "]Apk:" + appListInfo;
            } else {
                extendInfo = "has no abnormal info";
            }
            int sendRes = sdm.sendThreatenInfo((int) THREATEN_ID, (byte) status, (byte) 0, (byte) 1, "antimal-sys", extendInfo);
            if (IS_HW_DEBUG) {
                Log.d(TAG, "Antimal-sys :" + extendInfo);
            }
            if (sendRes != 0) {
                Log.w(TAG, "Failed to send antimal threaten info, res: " + sendRes);
            }
        }
    }

    private String getComponentListInfo() {
        Bundle component = this.mDataManager.getAntimalComponentInfo();
        StringBuffer buf = new StringBuffer(64);
        if (component == null) {
            Log.e(TAG, "getCompnentList component is NULL!");
            return "";
        }
        try {
            ArrayList<AntiMalComponentInfo> componentList = component.getParcelableArrayList(HwSecDiagnoseConstant.COMPONENT_LIST);
            if (componentList != null) {
                if (componentList.size() != 0) {
                    Iterator<AntiMalComponentInfo> it = componentList.iterator();
                    while (it.hasNext()) {
                        AntiMalComponentInfo ai = it.next();
                        buf.append(ai.componentName + AwarenessInnerConstants.COLON_KEY + ai.getVerifyStatus() + "," + ai.getAntimalTypeMask() + ";");
                    }
                    return buf.toString();
                }
            }
            Log.e(TAG, "getCompnentList componentList is null!");
            return "";
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Security diagnostics service fails to get componentList.");
            return "";
        }
    }

    private String getAppListInfo() {
        Bundle bundle = this.mDataManager.collectData();
        if (bundle == null) {
            Log.e(TAG, "getAppListInfo bundle is NULL!");
            return "";
        }
        try {
            ArrayList<AntiMalApkInfo> appList = bundle.getParcelableArrayList(HwSecDiagnoseConstant.ANTIMAL_APK_LIST);
            if (appList != null) {
                if (appList.size() != 0) {
                    return appList.toString();
                }
            }
            if (IS_HW_DEBUG) {
                Log.d(TAG, "reportAntiMalData appList is null!");
            }
            return "";
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Security diagnostics service fails to get appList.");
            return "";
        }
    }

    private void onReported() {
        ReportListener reportListener = this.mListener;
        if (reportListener != null) {
            reportListener.onReported();
        }
    }

    private void doReport() {
        IHwSecurityDiagnosePlugin secDgnService;
        if (IS_HW_DEBUG) {
            Log.d(TAG, "sendAntiMalData Need send!");
        }
        Bundle bundle = this.mDataManager.collectData();
        try {
            IHwSecurityService secServie = IHwSecurityService.Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));
            if (secServie != null && (secDgnService = IHwSecurityDiagnosePlugin.Stub.asInterface(secServie.querySecurityInterface(2))) != null) {
                secDgnService.report(100, bundle);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "security diagnostics service fails to send AntiMalData.");
        }
    }
}
