package com.huawei.nb.client.ai;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class UpdatePackageInfo implements Parcelable {
    public static final Creator<UpdatePackageInfo> CREATOR = new Creator<UpdatePackageInfo>() {
        public UpdatePackageInfo createFromParcel(Parcel in) {
            UpdatePackageInfo info = new UpdatePackageInfo();
            info.resid = in.readString();
            info.errorCode = in.readInt();
            info.errorMessage = in.readString();
            info.isUpdateAvailable = in.readByte() != (byte) 0;
            info.newVersionCode = in.readLong();
            info.newPackageSize = in.readLong();
            return info;
        }

        public UpdatePackageInfo[] newArray(int size) {
            return new UpdatePackageInfo[size];
        }
    };
    int errorCode;
    String errorMessage;
    boolean isUpdateAvailable;
    long newPackageSize;
    long newVersionCode;
    String resid;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(this.resid);
        dest.writeInt(this.errorCode);
        dest.writeString(this.errorMessage);
        dest.writeByte((byte) (this.isUpdateAvailable ? 1 : 0));
        dest.writeLong(this.newVersionCode);
        dest.writeLong(this.newPackageSize);
    }

    public String getResid() {
        return this.resid;
    }

    public void setResid(String resid) {
        this.resid = resid;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isUpdateAvailable() {
        return this.isUpdateAvailable;
    }

    public void setUpdateAvailable(boolean updateAvailable) {
        this.isUpdateAvailable = updateAvailable;
    }

    public long getNewVersionCode() {
        return this.newVersionCode;
    }

    public void setNewVersionCode(long newVersionCode) {
        this.newVersionCode = newVersionCode;
    }

    public long getNewPackageSize() {
        return this.newPackageSize;
    }

    public void setNewPackageSize(long newPackageSize) {
        this.newPackageSize = newPackageSize;
    }
}
