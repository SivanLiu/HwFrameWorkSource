package com.android.server.pm;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import huawei.android.security.IHwSecurityDiagnosePlugin;
import huawei.android.security.IHwSecurityService;
import huawei.android.security.IHwSecurityService.Stub;

public class AntiMalPreInstallReport {
    private static final int DEVICE_SECURE_DIAGNOSE_ID = 2;
    private static final boolean HW_DEBUG;
    private static final String SECURITY_SERVICE = "securityserver";
    private static final String TAG = "AntiMalPreInstallReport";
    private AntiMalDataManager mDataManager;
    private ReportListener mListener;
    private Thread mReportTask = new Thread() {
        public void run() {
            AntiMalPreInstallReport.this.sendAntimaComponentInfo();
            AntiMalPreInstallReport.this.sendAntiMalData();
        }
    };

    interface ReportListener {
        void onReported();
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HW_DEBUG = z;
    }

    public AntiMalPreInstallReport(AntiMalDataManager dataManager) {
        this.mDataManager = dataManager;
    }

    public void report(ReportListener listener) {
        this.mListener = listener;
        this.mReportTask.start();
    }

    private void sendAntimaComponentInfo() {
        Bundle bundle = this.mDataManager.getAntimalComponentInfo();
        try {
            IHwSecurityService secServie = Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));
            if (secServie != null) {
                IHwSecurityDiagnosePlugin secDgnService = IHwSecurityDiagnosePlugin.Stub.asInterface(secServie.querySecurityInterface(2));
                if (secDgnService != null) {
                    secDgnService.sendComponentInfo(bundle);
                }
            }
        } catch (RemoteException re) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendAntiMalData re:");
            stringBuilder.append(re);
            Log.e(str, stringBuilder.toString());
        }
    }

    private void sendAntiMalData() {
        if (HW_DEBUG) {
            Log.d(TAG, "sendAntiMalData begin!");
        }
        if (this.mDataManager.needReport()) {
            if (HW_DEBUG) {
                Log.d(TAG, "sendAntiMalData Need send!");
            }
            Bundle bundle = this.mDataManager.collectData();
            try {
                IHwSecurityService secServie = Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));
                if (secServie != null) {
                    IHwSecurityDiagnosePlugin secDgnService = IHwSecurityDiagnosePlugin.Stub.asInterface(secServie.querySecurityInterface(2));
                    if (secDgnService != null) {
                        secDgnService.report(100, bundle);
                    }
                }
            } catch (RemoteException re) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendAntiMalData re:");
                stringBuilder.append(re);
                Log.e(str, stringBuilder.toString());
            }
        }
        if (this.mDataManager.needScanIllegalApks()) {
            this.mDataManager.writeAntiMalData();
        }
        onReported();
    }

    private void onReported() {
        if (this.mListener != null) {
            this.mListener.onReported();
        }
    }
}
