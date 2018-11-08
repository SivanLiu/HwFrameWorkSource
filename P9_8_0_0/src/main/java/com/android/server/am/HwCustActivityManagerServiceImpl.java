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
        IOException e;
        Throwable th;
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File("sys/kernel/rcc/event");
            if (file.exists()) {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                try {
                    bw.write(event);
                    bw.flush();
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e2) {
                            Log.e(TAG, "can not close file descriptor!");
                        }
                    }
                    bufferedWriter = bw;
                } catch (IOException e3) {
                    e = e3;
                    bufferedWriter = bw;
                    try {
                        Log.e(TAG, e.toString());
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e4) {
                                Log.e(TAG, "can not close file descriptor!");
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e5) {
                                Log.e(TAG, "can not close file descriptor!");
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    bufferedWriter = bw;
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    throw th;
                }
            }
            Log.e(TAG, "sys/kernel/rcc/event doesn't exist!");
        } catch (IOException e6) {
            e = e6;
            Log.e(TAG, e.toString());
            if (bufferedWriter != null) {
                bufferedWriter.close();
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

    public boolean isInMultiWinBlackList(String pkg, ContentResolver resolver) {
        if (this.mBlackListPkg == null) {
            this.mBlackListPkg = Secure.getString(resolver, "multi_window_black_list") + ",";
        }
        if (!this.mBlackListPkg.contains(pkg + ",")) {
            return false;
        }
        Log.d(TAG, "isInMultiWinBlackList pkg:" + pkg);
        return true;
    }

    protected boolean isAddRestrictedForCust(String pkgName) {
        return IS_ADD_RESTRICTED ? this.pkgNames.contains(pkgName) : false;
    }
}
