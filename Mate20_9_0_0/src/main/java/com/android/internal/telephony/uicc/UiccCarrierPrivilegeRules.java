package com.android.internal.telephony.uicc;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class UiccCarrierPrivilegeRules extends Handler {
    private static final int ARAD = 0;
    private static final String ARAD_AID = "A00000015144414300";
    private static final int ARAM = 1;
    private static final String ARAM_AID = "A00000015141434C00";
    private static final String CARRIER_PRIVILEGE_AID = "FFFFFFFFFFFF";
    private static final int CLA = 128;
    private static final int COMMAND = 202;
    private static final String DATA = "";
    private static final boolean DBG = false;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 3;
    private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 1;
    private static final int EVENT_PKCS15_READ_DONE = 4;
    private static final int EVENT_TRANSMIT_LOGICAL_CHANNEL_DONE = 2;
    private static final String LOG_TAG = "UiccCarrierPrivilegeRules";
    private static final int MAX_RETRY = 1;
    private static final int P1 = 255;
    private static final int P2 = 64;
    private static final int P2_EXTENDED_DATA = 96;
    private static final int P3 = 0;
    private static final int RETRY_INTERVAL_MS = 10000;
    private static final int STATE_ERROR = 2;
    private static final int STATE_LOADED = 1;
    private static final int STATE_LOADING = 0;
    private static final String TAG_AID_REF_DO = "4F";
    private static final String TAG_ALL_REF_AR_DO = "FF40";
    private static final String TAG_AR_DO = "E3";
    private static final String TAG_DEVICE_APP_ID_REF_DO = "C1";
    private static final String TAG_PERM_AR_DO = "DB";
    private static final String TAG_PKG_REF_DO = "CA";
    private static final String TAG_REF_AR_DO = "E2";
    private static final String TAG_REF_DO = "E1";
    private int mAIDInUse;
    private List<UiccAccessRule> mAccessRules;
    private int mChannelId;
    private boolean mCheckedRules = false;
    private Message mLoadedCallback;
    private int mRetryCount;
    private final Runnable mRetryRunnable = new Runnable() {
        public void run() {
            UiccCarrierPrivilegeRules.this.openChannel(UiccCarrierPrivilegeRules.this.mAIDInUse);
        }
    };
    private String mRules;
    private AtomicInteger mState;
    private String mStatusMessage;
    private UiccPkcs15 mUiccPkcs15;
    private UiccProfile mUiccProfile;

    public static class TLV {
        private static final int SINGLE_BYTE_MAX_LENGTH = 128;
        private Integer length;
        private String lengthBytes;
        private String tag;
        private String value;

        public TLV(String tag) {
            this.tag = tag;
        }

        public String getValue() {
            if (this.value == null) {
                return UiccCarrierPrivilegeRules.DATA;
            }
            return this.value;
        }

        public String parseLength(String data) {
            int offset = this.tag.length();
            int firstByte = Integer.parseInt(data.substring(offset, offset + 2), 16);
            if (firstByte < 128) {
                this.length = Integer.valueOf(firstByte * 2);
                this.lengthBytes = data.substring(offset, offset + 2);
            } else {
                int numBytes = firstByte - 128;
                this.length = Integer.valueOf(Integer.parseInt(data.substring(offset + 2, (offset + 2) + (numBytes * 2)), 16) * 2);
                this.lengthBytes = data.substring(offset, (offset + 2) + (numBytes * 2));
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TLV parseLength length=");
            stringBuilder.append(this.length);
            stringBuilder.append("lenghtBytes: ");
            stringBuilder.append(this.lengthBytes);
            UiccCarrierPrivilegeRules.log(stringBuilder.toString());
            return this.lengthBytes;
        }

        public String parse(String data, boolean shouldConsumeAll) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Parse TLV: ");
            stringBuilder.append(this.tag);
            UiccCarrierPrivilegeRules.log(stringBuilder.toString());
            if (data.startsWith(this.tag)) {
                int index = this.tag.length();
                if (index + 2 <= data.length()) {
                    parseLength(data);
                    index += this.lengthBytes.length();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("index=");
                    stringBuilder2.append(index);
                    stringBuilder2.append(" length=");
                    stringBuilder2.append(this.length);
                    stringBuilder2.append("data.length=");
                    stringBuilder2.append(data.length());
                    UiccCarrierPrivilegeRules.log(stringBuilder2.toString());
                    int remainingLength = data.length() - (this.length.intValue() + index);
                    if (remainingLength < 0) {
                        throw new IllegalArgumentException("Not enough data.");
                    } else if (!shouldConsumeAll || remainingLength == 0) {
                        this.value = data.substring(index, this.length.intValue() + index);
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Got TLV: ");
                        stringBuilder3.append(this.tag);
                        stringBuilder3.append(",");
                        stringBuilder3.append(this.length);
                        stringBuilder3.append(",");
                        stringBuilder3.append(this.value);
                        UiccCarrierPrivilegeRules.log(stringBuilder3.toString());
                        return data.substring(this.length.intValue() + index);
                    } else {
                        throw new IllegalArgumentException("Did not consume all.");
                    }
                }
                throw new IllegalArgumentException("No length.");
            }
            throw new IllegalArgumentException("Tags don't match.");
        }
    }

    private void openChannel(int aidId) {
        this.mUiccProfile.iccOpenLogicalChannel(aidId == 0 ? ARAD_AID : ARAM_AID, 0, obtainMessage(1, 0, aidId, null));
    }

    public UiccCarrierPrivilegeRules(UiccProfile uiccProfile, Message loadedCallback) {
        log("Creating UiccCarrierPrivilegeRules");
        this.mUiccProfile = uiccProfile;
        this.mState = new AtomicInteger(0);
        this.mStatusMessage = "Not loaded.";
        this.mLoadedCallback = loadedCallback;
        this.mRules = DATA;
        this.mAccessRules = new ArrayList();
        this.mAIDInUse = 0;
        openChannel(this.mAIDInUse);
    }

    public boolean areCarrierPriviligeRulesLoaded() {
        return this.mState.get() != 0;
    }

    public boolean hasCarrierPrivilegeRules() {
        return (this.mState.get() == 0 || this.mAccessRules == null || this.mAccessRules.size() <= 0) ? false : true;
    }

    public List<String> getPackageNames() {
        List<String> pkgNames = new ArrayList();
        if (this.mAccessRules != null) {
            for (UiccAccessRule ar : this.mAccessRules) {
                if (!TextUtils.isEmpty(ar.getPackageName())) {
                    pkgNames.add(ar.getPackageName());
                }
            }
        }
        return pkgNames;
    }

    public List<UiccAccessRule> getAccessRules() {
        if (this.mAccessRules == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.mAccessRules);
    }

    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        int state = this.mState.get();
        if (state == 0) {
            return -1;
        }
        if (state == 2) {
            return -2;
        }
        for (UiccAccessRule ar : this.mAccessRules) {
            int accessStatus = ar.getCarrierPrivilegeStatus(signature, packageName);
            if (accessStatus != 0) {
                return accessStatus;
            }
        }
        return 0;
    }

    public int getCarrierPrivilegeStatus(PackageManager packageManager, String packageName) {
        try {
            if (hasCarrierPrivilegeRules()) {
                return getCarrierPrivilegeStatus(packageManager.getPackageInfo(packageName, 32832));
            }
            int state = this.mState.get();
            if (state == 0) {
                return -1;
            }
            if (state == 2) {
                return -2;
            }
            return 0;
        } catch (NameNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" not found for carrier privilege status check");
            log(stringBuilder.toString());
            return 0;
        }
    }

    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        int state = this.mState.get();
        if (state == 0) {
            return -1;
        }
        if (state == 2) {
            return -2;
        }
        for (UiccAccessRule ar : this.mAccessRules) {
            int accessStatus = ar.getCarrierPrivilegeStatus(packageInfo);
            if (accessStatus != 0) {
                return accessStatus;
            }
        }
        return 0;
    }

    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        return getCarrierPrivilegeStatusForUid(packageManager, Binder.getCallingUid());
    }

    public int getCarrierPrivilegeStatusForUid(PackageManager packageManager, int uid) {
        for (String pkg : packageManager.getPackagesForUid(uid)) {
            int accessStatus = getCarrierPrivilegeStatus(packageManager, pkg);
            if (accessStatus != 0) {
                return accessStatus;
            }
        }
        return 0;
    }

    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        List<String> packages = new ArrayList();
        List<ResolveInfo> receivers = new ArrayList();
        receivers.addAll(packageManager.queryBroadcastReceivers(intent, 0));
        receivers.addAll(packageManager.queryIntentContentProviders(intent, 0));
        receivers.addAll(packageManager.queryIntentActivities(intent, 0));
        receivers.addAll(packageManager.queryIntentServices(intent, 0));
        for (ResolveInfo resolveInfo : receivers) {
            String packageName = getPackageName(resolveInfo);
            if (packageName != null) {
                int status = getCarrierPrivilegeStatus(packageManager, packageName);
                if (status == 1) {
                    packages.add(packageName);
                } else if (status != 0) {
                    return null;
                }
            }
        }
        return packages;
    }

    private String getPackageName(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        if (resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo.packageName;
        }
        if (resolveInfo.providerInfo != null) {
            return resolveInfo.providerInfo.packageName;
        }
        return null;
    }

    public void handleMessage(Message msg) {
        StringBuilder stringBuilder;
        Message message = msg;
        this.mAIDInUse = message.arg2;
        String errorMsg;
        switch (message.what) {
            case 1:
                log("EVENT_OPEN_LOGICAL_CHANNEL_DONE");
                AsyncResult ar = message.obj;
                if (ar.exception == null && ar.result != null && (ar.result instanceof int[])) {
                    this.mChannelId = ((int[]) ar.result)[0];
                    if (this.mChannelId <= 0) {
                        Rlog.e(LOG_TAG, "Error channelId!");
                        updateState(2, "Error channelId !");
                        return;
                    }
                    this.mUiccProfile.iccTransmitApduLogicalChannel(this.mChannelId, 128, COMMAND, 255, 64, 0, DATA, obtainMessage(2, this.mChannelId, this.mAIDInUse));
                    return;
                } else if ((ar.exception instanceof CommandException) && this.mRetryCount < 1 && ((CommandException) ar.exception).getCommandError() == Error.MISSING_RESOURCE) {
                    this.mRetryCount++;
                    removeCallbacks(this.mRetryRunnable);
                    postDelayed(this.mRetryRunnable, 10000);
                    return;
                } else {
                    if (this.mAIDInUse == 0) {
                        this.mRules = DATA;
                        openChannel(1);
                    }
                    if (this.mAIDInUse != 1) {
                        return;
                    }
                    if (this.mCheckedRules) {
                        updateState(1, "Success!");
                        return;
                    }
                    log("No ARA, try ARF next.");
                    this.mUiccPkcs15 = new UiccPkcs15(this.mUiccProfile, obtainMessage(4));
                    return;
                }
            case 2:
                log("EVENT_TRANSMIT_LOGICAL_CHANNEL_DONE");
                AsyncResult ar2 = message.obj;
                if (ar2.exception == null && ar2.result != null && (ar2.result instanceof IccIoResult)) {
                    IccIoResult response = ar2.result;
                    if (response.sw1 == 144 && response.sw2 == 0 && response.payload != null && response.payload.length > 0) {
                        try {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(this.mRules);
                            stringBuilder2.append(IccUtils.bytesToHexString(response.payload).toUpperCase(Locale.US));
                            this.mRules = stringBuilder2.toString();
                            if (isDataComplete()) {
                                this.mAccessRules.addAll(parseRules(this.mRules));
                                if (this.mAIDInUse == 0) {
                                    this.mCheckedRules = true;
                                } else {
                                    updateState(1, "Success!");
                                }
                            } else {
                                this.mUiccProfile.iccTransmitApduLogicalChannel(this.mChannelId, 128, COMMAND, 255, 96, 0, DATA, obtainMessage(2, this.mChannelId, this.mAIDInUse));
                                return;
                            }
                        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
                            if (this.mAIDInUse == 1) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Error parsing rules: ");
                                stringBuilder.append(ex);
                                updateState(2, stringBuilder.toString());
                            }
                        }
                    } else if (this.mAIDInUse == 1) {
                        errorMsg = new StringBuilder();
                        errorMsg.append("Invalid response: payload=");
                        errorMsg.append(response.payload);
                        errorMsg.append(" sw1=");
                        errorMsg.append(response.sw1);
                        errorMsg.append(" sw2=");
                        errorMsg.append(response.sw2);
                        updateState(2, errorMsg.toString());
                    }
                } else if (this.mAIDInUse == 1) {
                    updateState(2, "Error reading value from SIM.");
                }
                this.mUiccProfile.iccCloseLogicalChannel(this.mChannelId, obtainMessage(3, 0, this.mAIDInUse));
                this.mChannelId = -1;
                return;
            case 3:
                log("EVENT_CLOSE_LOGICAL_CHANNEL_DONE");
                if (this.mAIDInUse == 0) {
                    this.mRules = DATA;
                    openChannel(1);
                    return;
                }
                return;
            case 4:
                log("EVENT_PKCS15_READ_DONE");
                if (this.mUiccPkcs15 == null || this.mUiccPkcs15.getRules() == null) {
                    updateState(2, "No ARA or ARF.");
                    return;
                }
                for (String cert : this.mUiccPkcs15.getRules()) {
                    this.mAccessRules.add(new UiccAccessRule(IccUtils.hexStringToBytes(cert), DATA, 0));
                }
                updateState(1, "Success!");
                return;
            default:
                errorMsg = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown event ");
                stringBuilder.append(message.what);
                Rlog.e(errorMsg, stringBuilder.toString());
                return;
        }
    }

    private boolean isDataComplete() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isDataComplete mRules:");
        stringBuilder.append(this.mRules);
        log(stringBuilder.toString());
        if (this.mRules.startsWith(TAG_ALL_REF_AR_DO)) {
            TLV allRules = new TLV(TAG_ALL_REF_AR_DO);
            String lengthBytes = allRules.parseLength(this.mRules);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isDataComplete lengthBytes: ");
            stringBuilder2.append(lengthBytes);
            log(stringBuilder2.toString());
            if (this.mRules.length() == (TAG_ALL_REF_AR_DO.length() + lengthBytes.length()) + allRules.length.intValue()) {
                log("isDataComplete yes");
                return true;
            }
            log("isDataComplete no");
            return false;
        }
        throw new IllegalArgumentException("Tags don't match.");
    }

    private static List<UiccAccessRule> parseRules(String rules) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Got rules: ");
        stringBuilder.append(rules);
        log(stringBuilder.toString());
        TLV allRefArDo = new TLV(TAG_ALL_REF_AR_DO);
        allRefArDo.parse(rules, true);
        String arDos = allRefArDo.value;
        List<UiccAccessRule> accessRules = new ArrayList();
        while (!arDos.isEmpty()) {
            TLV refArDo = new TLV(TAG_REF_AR_DO);
            arDos = refArDo.parse(arDos, false);
            UiccAccessRule accessRule = parseRefArdo(refArDo.value);
            if (accessRule != null) {
                accessRules.add(accessRule);
            } else {
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Skip unrecognized rule.");
                stringBuilder2.append(refArDo.value);
                Rlog.e(str, stringBuilder2.toString());
            }
        }
        return accessRules;
    }

    private static UiccAccessRule parseRefArdo(String rule) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Got rule: ");
        stringBuilder.append(rule);
        log(stringBuilder.toString());
        String certificateHash = null;
        String packageName = null;
        while (!rule.isEmpty()) {
            TLV refDo;
            if (rule.startsWith(TAG_REF_DO)) {
                String tmp;
                refDo = new TLV(TAG_REF_DO);
                rule = refDo.parse(rule, false);
                TLV deviceDo = new TLV(TAG_DEVICE_APP_ID_REF_DO);
                if (refDo.value.startsWith(TAG_AID_REF_DO)) {
                    TLV cpDo = new TLV(TAG_AID_REF_DO);
                    String remain = cpDo.parse(refDo.value, false);
                    if (!cpDo.lengthBytes.equals("06") || !cpDo.value.equals(CARRIER_PRIVILEGE_AID) || remain.isEmpty() || !remain.startsWith(TAG_DEVICE_APP_ID_REF_DO)) {
                        return null;
                    }
                    tmp = deviceDo.parse(remain, false);
                    certificateHash = deviceDo.value;
                } else if (!refDo.value.startsWith(TAG_DEVICE_APP_ID_REF_DO)) {
                    return null;
                } else {
                    tmp = deviceDo.parse(refDo.value, false);
                    certificateHash = deviceDo.value;
                }
                if (tmp.isEmpty()) {
                    packageName = null;
                } else if (!tmp.startsWith(TAG_PKG_REF_DO)) {
                    return null;
                } else {
                    TLV pkgDo = new TLV(TAG_PKG_REF_DO);
                    pkgDo.parse(tmp, true);
                    packageName = new String(IccUtils.hexStringToBytes(pkgDo.value));
                }
            } else if (rule.startsWith(TAG_AR_DO)) {
                refDo = new TLV(TAG_AR_DO);
                rule = refDo.parse(rule, false);
                String remain2 = refDo.value;
                while (!remain2.isEmpty() && !remain2.startsWith(TAG_PERM_AR_DO)) {
                    remain2 = new TLV(remain2.substring(0, 2)).parse(remain2, false);
                }
                if (remain2.isEmpty()) {
                    return null;
                }
                new TLV(TAG_PERM_AR_DO).parse(remain2, true);
            } else {
                throw new RuntimeException("Invalid Rule type");
            }
        }
        return new UiccAccessRule(IccUtils.hexStringToBytes(certificateHash), packageName, 0);
    }

    private void updateState(int newState, String statusMessage) {
        this.mState.set(newState);
        if (this.mLoadedCallback != null) {
            this.mLoadedCallback.sendToTarget();
        }
        this.mStatusMessage = statusMessage;
    }

    private static void log(String msg) {
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UiccCarrierPrivilegeRules: ");
        stringBuilder.append(this);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mState=");
        stringBuilder.append(getStateString(this.mState.get()));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mStatusMessage='");
        stringBuilder.append(this.mStatusMessage);
        stringBuilder.append("'");
        pw.println(stringBuilder.toString());
        if (this.mAccessRules != null) {
            pw.println(" mAccessRules: ");
            for (UiccAccessRule ar : this.mAccessRules) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  rule='");
                stringBuilder2.append(ar);
                stringBuilder2.append("'");
                pw.println(stringBuilder2.toString());
            }
        } else {
            pw.println(" mAccessRules: null");
        }
        if (this.mUiccPkcs15 != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mUiccPkcs15: ");
            stringBuilder.append(this.mUiccPkcs15);
            pw.println(stringBuilder.toString());
            this.mUiccPkcs15.dump(fd, pw, args);
        } else {
            pw.println(" mUiccPkcs15: null");
        }
        pw.flush();
    }

    private String getStateString(int state) {
        switch (state) {
            case 0:
                return "STATE_LOADING";
            case 1:
                return "STATE_LOADED";
            case 2:
                return "STATE_ERROR";
            default:
                return "UNKNOWN";
        }
    }
}
