package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;

public class NearConnectionResult implements Parcelable {
    public static final Creator<NearConnectionResult> CREATOR = new Creator<NearConnectionResult>() {
        /* class com.huawei.nearbysdk.softnet.NearConnectionResult.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearConnectionResult createFromParcel(Parcel in) {
            return new NearConnectionResult(in);
        }

        @Override // android.os.Parcelable.Creator
        public NearConnectionResult[] newArray(int size) {
            return new NearConnectionResult[size];
        }
    };
    /* access modifiers changed from: private */
    public byte[] mResultData;
    /* access modifiers changed from: private */
    public int mStatus;

    protected NearConnectionResult(Parcel in) {
        this.mStatus = in.readInt();
        this.mResultData = in.createByteArray();
    }

    private NearConnectionResult() {
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mStatus);
        dest.writeByteArray(this.mResultData);
    }

    public int describeContents() {
        return 0;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public byte[] getResultData() {
        return this.mResultData;
    }

    public static class Builder {
        NearConnectionResult info = new NearConnectionResult();

        public Builder status(int status) {
            int unused = this.info.mStatus = status;
            return this;
        }

        public Builder resultData(byte[] resultData) {
            byte[] unused = this.info.mResultData = resultData;
            return this;
        }

        public NearConnectionResult build() {
            return this.info;
        }
    }
}
