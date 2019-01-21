package com.android.internal.telephony.dataconnection;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import android.util.Pair;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DataEnabledSettings {
    private static final String LOG_TAG = "DataEnabledSettings";
    public static final int REASON_DATA_ENABLED_BY_CARRIER = 4;
    public static final int REASON_INTERNAL_DATA_ENABLED = 1;
    public static final int REASON_POLICY_DATA_ENABLED = 3;
    public static final int REASON_REGISTERED = 0;
    public static final int REASON_USER_DATA_ENABLED = 2;
    private boolean mCarrierDataEnabled = true;
    private final RegistrantList mDataEnabledChangedRegistrants = new RegistrantList();
    private boolean mInternalDataEnabled = true;
    private Phone mPhone = null;
    private boolean mPolicyDataEnabled = true;
    private ContentResolver mResolver = null;
    private final LocalLog mSettingChangeLocalLog = new LocalLog(50);

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[mInternalDataEnabled=");
        stringBuilder.append(this.mInternalDataEnabled);
        stringBuilder.append(", isUserDataEnabled=");
        stringBuilder.append(isUserDataEnabled());
        stringBuilder.append(", isProvisioningDataEnabled=");
        stringBuilder.append(isProvisioningDataEnabled());
        stringBuilder.append(", mPolicyDataEnabled=");
        stringBuilder.append(this.mPolicyDataEnabled);
        stringBuilder.append(", mCarrierDataEnabled=");
        stringBuilder.append(this.mCarrierDataEnabled);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public DataEnabledSettings(Phone phone) {
        this.mPhone = phone;
        this.mResolver = this.mPhone.getContext().getContentResolver();
    }

    public synchronized void setInternalDataEnabled(boolean enabled) {
        localLog("InternalDataEnabled", enabled);
        boolean prevDataEnabled = isDataEnabled();
        this.mInternalDataEnabled = enabled;
        if (prevDataEnabled != isDataEnabled()) {
            notifyDataEnabledChanged(!prevDataEnabled, 1);
        }
    }

    public synchronized boolean isInternalDataEnabled() {
        return this.mInternalDataEnabled;
    }

    public synchronized void setUserDataEnabled(boolean enabled) {
        localLog("UserDataEnabled", enabled);
        boolean prevDataEnabled = isDataEnabled();
        boolean z = false;
        if (TelephonyManager.getDefault().getSimCount() == 1) {
            Global.putInt(this.mResolver, "mobile_data", enabled);
        } else if (VSimUtilsInner.isVSimSub(this.mPhone.getSubId())) {
            log("vsim does not save mobile data");
        } else {
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            for (int i = 0; i < phoneCount; i++) {
                ContentResolver contentResolver = this.mResolver;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mobile_data");
                stringBuilder.append(i);
                Global.putInt(contentResolver, stringBuilder.toString(), enabled);
            }
            Global.putInt(this.mResolver, "mobile_data", enabled);
        }
        if (prevDataEnabled != isDataEnabled()) {
            if (!prevDataEnabled) {
                z = true;
            }
            notifyDataEnabledChanged(z, 2);
        }
    }

    public synchronized boolean isUserDataEnabled() {
        boolean z;
        z = false;
        if (Global.getInt(this.mResolver, getMobileDataSettingName(), "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true")) ? 1 : 0) != 0) {
            z = true;
        }
        return z;
    }

    private String getMobileDataSettingName() {
        int subId = this.mPhone.getSubId();
        if (TelephonyManager.getDefault().getSimCount() == 1 || !SubscriptionManager.isValidSubscriptionId(subId)) {
            return "mobile_data";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mobile_data");
        stringBuilder.append(this.mPhone.getSubId());
        return stringBuilder.toString();
    }

    public synchronized void setPolicyDataEnabled(boolean enabled) {
        localLog("PolicyDataEnabled", enabled);
        boolean prevDataEnabled = isDataEnabled();
        this.mPolicyDataEnabled = enabled;
        if (prevDataEnabled != isDataEnabled()) {
            notifyDataEnabledChanged(!prevDataEnabled, 3);
        }
    }

    public synchronized boolean isPolicyDataEnabled() {
        return this.mPolicyDataEnabled;
    }

    public synchronized void setCarrierDataEnabled(boolean enabled) {
        localLog("CarrierDataEnabled", enabled);
        boolean prevDataEnabled = isDataEnabled();
        this.mCarrierDataEnabled = enabled;
        if (prevDataEnabled != isDataEnabled()) {
            notifyDataEnabledChanged(!prevDataEnabled, 4);
        }
    }

    public synchronized boolean isCarrierDataEnabled() {
        return this.mCarrierDataEnabled;
    }

    /* JADX WARNING: Missing block: B:21:0x0029, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isDataEnabled() {
        if (isAnySimDetected() || !isProvisioning()) {
            boolean z = this.mInternalDataEnabled && isUserDataEnabled() && this.mPolicyDataEnabled && this.mCarrierDataEnabled;
        } else {
            return isProvisioningDataEnabled();
        }
    }

    public boolean isAnySimDetected() {
        return "true".equals(System.getString(this.mResolver, "any_sim_detect"));
    }

    public boolean isProvisioning() {
        return Global.getInt(this.mResolver, "device_provisioned", 0) == 0;
    }

    public boolean isProvisioningDataEnabled() {
        String prov_property = SystemProperties.get("ro.com.android.prov_mobiledata", "false");
        int prov_mobile_data = Global.getInt(this.mResolver, "device_provisioning_mobile_data", "true".equalsIgnoreCase(prov_property));
        boolean retVal = prov_mobile_data != 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDataEnabled during provisioning retVal=");
        stringBuilder.append(retVal);
        stringBuilder.append(" - (");
        stringBuilder.append(prov_property);
        stringBuilder.append(", ");
        stringBuilder.append(prov_mobile_data);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        return retVal;
    }

    private void notifyDataEnabledChanged(boolean enabled, int reason) {
        this.mDataEnabledChangedRegistrants.notifyResult(new Pair(Boolean.valueOf(enabled), Integer.valueOf(reason)));
    }

    public void registerForDataEnabledChanged(Handler h, int what, Object obj) {
        this.mDataEnabledChangedRegistrants.addUnique(h, what, obj);
        notifyDataEnabledChanged(isDataEnabled(), 0);
    }

    public void unregisterForDataEnabledChanged(Handler h) {
        this.mDataEnabledChangedRegistrants.remove(h);
    }

    private void log(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    private void localLog(String name, boolean value) {
        LocalLog localLog = this.mSettingChangeLocalLog;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" change to ");
        stringBuilder.append(value);
        localLog.log(stringBuilder.toString());
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(" DataEnabledSettings=");
        this.mSettingChangeLocalLog.dump(fd, pw, args);
    }
}
