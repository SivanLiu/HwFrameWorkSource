package com.huawei.server.pm.antimal;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;

public class AntiMalComponentInfo implements Parcelable {
    public static final String ANTIMAL_TYPE_MASK = "antimal_type_mask";
    static final int BIT_ANTIMAL_TYPE_ADD = 1;
    static final int BIT_ANTIMAL_TYPE_DELETE = 4;
    static final int BIT_ANTIMAL_TYPE_MODIFY = 2;
    static final int BIT_ANTIMAL_TYPE_NORMAL = 0;
    public static final Creator<AntiMalComponentInfo> CREATOR = new Creator<AntiMalComponentInfo>() {
        /* class com.huawei.server.pm.antimal.AntiMalComponentInfo.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AntiMalComponentInfo createFromParcel(Parcel source) {
            return new AntiMalComponentInfo(source);
        }

        @Override // android.os.Parcelable.Creator
        public AntiMalComponentInfo[] newArray(int size) {
            return new AntiMalComponentInfo[size];
        }
    };
    private static final String ECOTA = "cust/ecota/";
    private static final boolean HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    public static final String NAME_TAG = "name";
    private static final String TAG = "HW-AntiMalComponentInfo";
    public static final String VERIFY_STATUS = "verify_status";
    public final String componentName;
    int mAntimalTypeMask;
    int mVerifyStatus;

    public interface VerifyStatus {
        public static final int PARSE_WHITE_LIST_FAILED = 4;
        public static final int SIGN_FILE_NOT_EXIST = 2;
        public static final int VERIFY_FAILED = 3;
        public static final int VERIFY_SECCUSS = 0;
        public static final int WHITE_LIST_NOT_EXIST = 1;
    }

    public AntiMalComponentInfo(String whiteListPath) {
        this.componentName = getComponentName(whiteListPath);
    }

    public AntiMalComponentInfo(String name, int verifyStatus, int antimalType) {
        this.componentName = name;
        this.mVerifyStatus = verifyStatus;
        this.mAntimalTypeMask = antimalType;
    }

    public AntiMalComponentInfo(Parcel source) {
        if (source != null) {
            this.componentName = source.readString();
            this.mVerifyStatus = source.readInt();
            this.mAntimalTypeMask = source.readInt();
            return;
        }
        this.componentName = null;
        this.mVerifyStatus = 0;
        this.mAntimalTypeMask = 0;
    }

    public boolean isVerifyStatusValid() {
        if (HW_DEBUG) {
            Log.d(TAG, "isVerifyStatusValid name = " + this.componentName + " mVerifyStatus = " + this.mVerifyStatus);
        }
        return this.mVerifyStatus == 0;
    }

    public boolean isNormal() {
        int i = this.mVerifyStatus;
        return (i == 0 || i == 1) && this.mAntimalTypeMask == 0;
    }

    public void setAntiMalStatus(int bitMask) {
        this.mAntimalTypeMask |= bitMask;
    }

    public int getVerifyStatus() {
        return this.mVerifyStatus;
    }

    /* access modifiers changed from: package-private */
    public int getAntimalTypeMask() {
        return this.mAntimalTypeMask;
    }

    public void setVerifyStatus(int status) {
        this.mVerifyStatus = status;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) {
            dest.writeString(this.componentName);
            dest.writeInt(this.mVerifyStatus);
            dest.writeInt(this.mAntimalTypeMask);
        }
    }

    public String toString() {
        return "ComponetName : " + this.componentName + " Verify Status : " + this.mVerifyStatus + " Antimal Type mask : " + this.mAntimalTypeMask;
    }

    private String getComponentName(String whiteListPath) {
        if (TextUtils.isEmpty(whiteListPath)) {
            return null;
        }
        if (whiteListPath.contains(ECOTA)) {
            return ECOTA;
        }
        int subBegin = 0;
        String sub = File.separator;
        if (whiteListPath.startsWith(sub)) {
            subBegin = 1;
        }
        String subPath = whiteListPath.substring(subBegin, whiteListPath.length());
        int index = subPath.indexOf(sub);
        String name = index > 0 ? subPath.substring(0, index + 1) : subPath;
        if (HW_DEBUG) {
            Log.d(TAG, "getComponentName path = " + name + "index = " + index);
        }
        return name;
    }
}
