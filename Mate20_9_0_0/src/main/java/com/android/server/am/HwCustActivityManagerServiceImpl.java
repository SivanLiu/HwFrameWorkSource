package com.android.server.am;

import android.content.ContentResolver;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

public class HwCustActivityManagerServiceImpl extends HwCustActivityManagerService {
    private static final boolean IS_ADD_RESTRICTED = SystemProperties.getBoolean("ro.config.add_restricted_bg", false);
    private static final boolean IS_IQI_Enable = SystemProperties.getBoolean("ro.config.iqi_att_support", false);
    static final String TAG = "HwCustAMSImpl";
    private boolean mAllowMemoryCompress = SystemProperties.getBoolean("ro.config.enable_rcc", false);
    private String mBlackListPkg = null;
    private boolean mDelaySwitchUserDlg = SystemProperties.getBoolean("ro.config.DelaySwitchUserDlg", false);
    private Set<String> pkgNames = new HashSet<String>() {
        {
            add("com.nttdocomo.android.homeagent");
        }
    };

    protected boolean isIQIEnable() {
        return IS_IQI_Enable;
    }

    protected boolean shouldDelaySwitchUserDlg() {
        return this.mDelaySwitchUserDlg;
    }

    protected boolean isAllowRamCompress() {
        return this.mAllowMemoryCompress;
    }

    protected void setEvent(String event) {
        BufferedWriter bw = null;
        try {
            File file = new File("sys/kernel/rcc/event");
            if (file.exists()) {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                bw.write(event);
                bw.flush();
                try {
                    bw.close();
                } catch (IOException e) {
                    Log.e(TAG, "can not close file descriptor!");
                }
                return;
            }
            Log.e(TAG, "sys/kernel/rcc/event doesn't exist!");
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e2) {
                    Log.e(TAG, "can not close file descriptor!");
                }
            }
        } catch (IOException e3) {
            Log.e(TAG, e3.toString());
            if (bw != null) {
                bw.close();
            }
        } catch (Throwable th) {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e4) {
                    Log.e(TAG, "can not close file descriptor!");
                }
            }
        }
    }

    protected int addProcesstoPersitList(ProcessRecord proc) {
        int maxAdj = proc.maxAdj;
        if (proc.processName.equals("diagandroid.iqd")) {
            return -800;
        }
        return maxAdj;
    }

    protected boolean isAddRestrictedForCust(String pkgName) {
        return IS_ADD_RESTRICTED && this.pkgNames.contains(pkgName);
    }

    public boolean isInMultiWinBlackList(String pkg, ContentResolver resolver) {
        if (this.mBlackListPkg == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Secure.getString(resolver, "multi_window_black_list"));
            stringBuilder.append(",");
            this.mBlackListPkg = stringBuilder.toString();
        }
        String str = this.mBlackListPkg;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(pkg);
        stringBuilder2.append(",");
        if (!str.contains(stringBuilder2.toString())) {
            return false;
        }
        str = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isInMultiWinBlackList pkg:");
        stringBuilder2.append(pkg);
        Log.d(str, stringBuilder2.toString());
        return true;
    }
}
