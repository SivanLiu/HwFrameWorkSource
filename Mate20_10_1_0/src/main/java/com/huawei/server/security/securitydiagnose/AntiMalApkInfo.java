package com.huawei.server.security.securitydiagnose;

import android.content.pm.PackageParser;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AntiMalApkInfo implements Parcelable, Comparable<AntiMalApkInfo> {
    private static final int APK_INFO_IS_NULL = 1;
    private static final int CONTENT_DESCRIPTOR = 0;
    public static final Creator<AntiMalApkInfo> CREATOR = new Creator<AntiMalApkInfo>() {
        /* class com.huawei.server.security.securitydiagnose.AntiMalApkInfo.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AntiMalApkInfo createFromParcel(Parcel source) {
            return new AntiMalApkInfo(source);
        }

        @Override // android.os.Parcelable.Creator
        public AntiMalApkInfo[] newArray(int size) {
            return new AntiMalApkInfo[size];
        }
    };
    private static final int INVALID_TYPE = -1;
    private static final int INVALID_VERSION = -1;
    private static final int PACKAGE_NAME_IS_NULL = -1;
    private static final String PATH_SLANT = File.separator;
    private static final int RET_DEFAULT_ERROR_VALUE = -1;
    private static final String TAG = "AntiMalApkInfo";
    private final String mApkName;
    private String mFrom;
    private final String mLastModifyTime;
    private final String mPackageName;
    private final String mPath;
    private final int mType;
    private int mVersion;

    public AntiMalApkInfo(PackageParser.Package pkg, int type) {
        if (pkg != null) {
            this.mPackageName = pkg.packageName;
            this.mPath = formatPath(pkg.baseCodePath);
            this.mApkName = null;
            this.mType = type;
            this.mLastModifyTime = getFileLastModifiedTime(pkg.codePath);
            this.mVersion = pkg.mVersionCode;
            return;
        }
        this.mPackageName = null;
        this.mPath = null;
        this.mApkName = null;
        this.mType = -1;
        this.mLastModifyTime = null;
        this.mFrom = null;
        this.mVersion = -1;
    }

    private AntiMalApkInfo(Builder builder) {
        this.mPackageName = builder.mPackageName;
        this.mPath = builder.mPath;
        this.mApkName = builder.mApkName;
        this.mType = builder.mType;
        this.mLastModifyTime = builder.mLastModifyTime;
        this.mFrom = builder.mFrom;
        this.mVersion = builder.mVersion;
    }

    private AntiMalApkInfo(Parcel source) {
        if (source != null) {
            this.mPackageName = source.readString();
            this.mPath = source.readString();
            this.mApkName = source.readString();
            this.mType = source.readInt();
            this.mLastModifyTime = source.readString();
            this.mFrom = source.readString();
            this.mVersion = source.readInt();
            return;
        }
        this.mPackageName = null;
        this.mPath = null;
        this.mApkName = null;
        this.mType = -1;
        this.mLastModifyTime = null;
        this.mFrom = null;
        this.mVersion = -1;
    }

    public static final class Builder {
        /* access modifiers changed from: private */
        public String mApkName;
        /* access modifiers changed from: private */
        public String mFrom;
        /* access modifiers changed from: private */
        public String mLastModifyTime;
        /* access modifiers changed from: private */
        public String mPackageName;
        /* access modifiers changed from: private */
        public String mPath;
        /* access modifiers changed from: private */
        public int mType;
        /* access modifiers changed from: private */
        public int mVersion;

        public Builder setPackageName(String packageName) {
            this.mPackageName = packageName;
            return this;
        }

        public Builder setPath(String path) {
            this.mPath = path;
            return this;
        }

        public Builder setApkName(String apkName) {
            this.mApkName = apkName;
            return this;
        }

        public Builder setType(int type) {
            this.mType = type;
            return this;
        }

        public Builder setLastModifyTime(String lastModifyTime) {
            this.mLastModifyTime = lastModifyTime;
            return this;
        }

        public Builder setFrom(String from) {
            this.mFrom = from;
            return this;
        }

        public Builder setVersion(int version) {
            this.mVersion = version;
            return this;
        }

        public AntiMalApkInfo build() {
            return new AntiMalApkInfo(this);
        }
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getPath() {
        return this.mPath;
    }

    public String getApkName() {
        return this.mApkName;
    }

    public String getLastModifyTime() {
        return this.mLastModifyTime;
    }

    public String getFrom() {
        return this.mFrom;
    }

    public int getType() {
        return this.mType;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) {
            dest.writeString(this.mPackageName);
            dest.writeString(this.mPath);
            dest.writeString(this.mApkName);
            dest.writeInt(this.mType);
            dest.writeString(this.mLastModifyTime);
            dest.writeString(this.mFrom);
            dest.writeInt(this.mVersion);
        }
    }

    public int compareTo(AntiMalApkInfo apkInfo) {
        if (apkInfo == null) {
            return 1;
        }
        String str = this.mPackageName;
        if (str != null) {
            return str.compareTo(apkInfo.mPackageName);
        }
        return -1;
    }

    public int hashCode() {
        if (this.mPackageName == null || this.mApkName == null) {
            return 0;
        }
        return super.hashCode() + this.mPackageName.hashCode() + this.mApkName.hashCode();
    }

    public boolean equals(Object in) {
        if (in == null || !(in instanceof AntiMalApkInfo)) {
            return false;
        }
        AntiMalApkInfo apkInfo = (AntiMalApkInfo) in;
        if (!stringEquals(this.mPackageName, apkInfo.mPackageName) || !stringEquals(this.mPath, apkInfo.mPath) || !stringEquals(this.mApkName, apkInfo.mApkName) || !stringEquals(this.mLastModifyTime, apkInfo.mLastModifyTime) || !stringEquals(this.mFrom, apkInfo.mFrom) || this.mType != apkInfo.mType || this.mVersion != apkInfo.mVersion) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "PackageName : " + this.mPackageName + " Path : " + this.mPath + " ApkName : " + this.mApkName + " Type : " + this.mType + " LastModifyTime : " + this.mLastModifyTime + " From : " + this.mFrom + " Version : " + this.mVersion;
    }

    private String formatTime(long minSec) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(minSec));
    }

    private String formatPath(String path) {
        if (path == null || !path.startsWith(PATH_SLANT)) {
            return path;
        }
        return path.substring(1, path.length());
    }

    private String getFileLastModifiedTime(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "getFileLastModifiedTime fileName param error");
            return null;
        }
        File apkFile = new File(fileName);
        try {
            if (apkFile.exists()) {
                return formatTime(apkFile.lastModified());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "getFileLastModifiedTime no read access to the file or directory");
        } catch (Exception e2) {
            Log.e(TAG, "getFileLastModifiedTime failed");
        }
        return null;
    }

    private boolean stringEquals(String srcStr, String dstStr) {
        if (TextUtils.isEmpty(srcStr)) {
            return TextUtils.isEmpty(dstStr);
        }
        if (!srcStr.equalsIgnoreCase(dstStr)) {
            return srcStr.length() < 1 && dstStr == null;
        }
        return true;
    }

    public static class AntiMalType {
        public static final int APK_DELETED = 3;
        public static final int NORMAL = 0;
        public static final int NOT_IN_WHITE_LIST = 1;
        public static final int SIGNATURE_CHANGED = 2;

        private AntiMalType() {
        }
    }
}
