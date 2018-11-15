package com.huawei.security.keymaster;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class HwOperationResult implements Parcelable {
    public static final Creator<HwOperationResult> CREATOR = new Creator<HwOperationResult>() {
        public HwOperationResult createFromParcel(Parcel in) {
            return new HwOperationResult(in);
        }

        public HwOperationResult[] newArray(int length) {
            return new HwOperationResult[length];
        }
    };
    public final int inputConsumed;
    public final long operationHandle;
    public final HwKeymasterArguments outParams;
    public final byte[] output;
    public final int resultCode;
    public final IBinder token;

    public HwOperationResult(int resultCode, IBinder token, long operationHandle, int inputConsumed, byte[] output, HwKeymasterArguments outParams) {
        this.resultCode = resultCode;
        this.token = token;
        this.operationHandle = operationHandle;
        this.inputConsumed = inputConsumed;
        this.output = output;
        this.outParams = outParams;
    }

    protected HwOperationResult(Parcel in) {
        this.resultCode = in.readInt();
        this.token = in.readStrongBinder();
        this.operationHandle = in.readLong();
        this.inputConsumed = in.readInt();
        this.output = in.createByteArray();
        this.outParams = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.resultCode);
        out.writeStrongBinder(this.token);
        out.writeLong(this.operationHandle);
        out.writeInt(this.inputConsumed);
        out.writeByteArray(this.output);
        this.outParams.writeToParcel(out, flags);
    }
}
