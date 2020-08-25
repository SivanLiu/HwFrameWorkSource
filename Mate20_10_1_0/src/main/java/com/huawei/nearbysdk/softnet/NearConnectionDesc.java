package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;

public class NearConnectionDesc implements Parcelable {
    public static final Creator<NearConnectionDesc> CREATOR = new Creator<NearConnectionDesc>() {
        /* class com.huawei.nearbysdk.softnet.NearConnectionDesc.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearConnectionDesc createFromParcel(Parcel in) {
            return new NearConnectionDesc(in);
        }

        @Override // android.os.Parcelable.Creator
        public NearConnectionDesc[] newArray(int size) {
            return new NearConnectionDesc[size];
        }
    };
    /* access modifiers changed from: private */
    public int mFd;
    /* access modifiers changed from: private */
    public boolean mIsIncomming;

    protected NearConnectionDesc(Parcel in) {
        this.mIsIncomming = in.createBooleanArray()[0];
        this.mFd = in.readInt();
    }

    private NearConnectionDesc() {
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBooleanArray(new boolean[]{this.mIsIncomming});
        dest.writeInt(this.mFd);
    }

    public int describeContents() {
        return 0;
    }

    public boolean getIsIncomming() {
        return this.mIsIncomming;
    }

    public int getFd() {
        return this.mFd;
    }

    public static class Builder {
        NearConnectionDesc info = new NearConnectionDesc();

        public Builder isIncomming(boolean isIncomming) {
            boolean unused = this.info.mIsIncomming = isIncomming;
            return this;
        }

        public Builder fd(int fd) {
            int unused = this.info.mFd = fd;
            return this;
        }

        public NearConnectionDesc build() {
            return this.info;
        }
    }
}
