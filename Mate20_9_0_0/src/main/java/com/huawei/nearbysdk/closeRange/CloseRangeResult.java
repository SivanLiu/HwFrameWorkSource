package com.huawei.nearbysdk.closeRange;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.SparseArray;

public final class CloseRangeResult implements Parcelable {
    public static final Creator<CloseRangeResult> CREATOR = new Creator<CloseRangeResult>() {
        public CloseRangeResult createFromParcel(Parcel source) {
            Parcel parcel = source;
            return new CloseRangeResult(source.readLong(), (CloseRangeBusinessType) parcel.readParcelable(CloseRangeBusinessType.class.getClassLoader()), (CloseRangeDevice) parcel.readParcelable(CloseRangeDevice.class.getClassLoader()), source.readInt(), source.readInt(), (ScanResult) parcel.readParcelable(ScanResult.class.getClassLoader()));
        }

        public CloseRangeResult[] newArray(int size) {
            return new CloseRangeResult[size];
        }
    };
    private CloseRangeBusinessType businessType;
    private int callbackType;
    private CloseRangeDevice device;
    private int errorCode;
    private SparseArray<byte[]> hwSpecDataArray = null;
    private SparseArray<byte[]> manufacturerDataArray = null;
    private ScanResult result;
    private long timeStamp;

    public CloseRangeResult(long timeStamp, CloseRangeBusinessType businessType, CloseRangeDevice device, int callbackType, int errorCode, ScanResult result) {
        this.timeStamp = timeStamp;
        this.businessType = businessType;
        this.device = device;
        this.callbackType = callbackType;
        this.errorCode = errorCode;
        this.result = result;
    }

    public final CloseRangeBusinessType getBusinessType() {
        return this.businessType;
    }

    public final CloseRangeDevice getDevice() {
        return this.device;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public int getCallbackType() {
        return this.callbackType;
    }

    public void setCallbackType(int callbackType) {
        this.callbackType = callbackType;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public ScanResult getResult() {
        return this.result;
    }

    public synchronized SparseArray<byte[]> getHwSpecDataArray() {
        if (this.hwSpecDataArray == null) {
            if (this.result == null) {
                this.hwSpecDataArray = new SparseArray();
                return this.hwSpecDataArray;
            }
            ScanRecord record = this.result.getScanRecord();
            if (record == null) {
                this.hwSpecDataArray = new SparseArray();
                return this.hwSpecDataArray;
            }
            byte[] hwSpecRawData = record.getServiceData(CloseRangeProtocol.PARCEL_UUID_CLOSERANGE);
            if (hwSpecRawData == null || hwSpecRawData.length == 0) {
                return this.hwSpecDataArray;
            }
            this.hwSpecDataArray = CloseRangeProtocol.parseCloseRangeServiceData(hwSpecRawData);
        }
        return this.hwSpecDataArray;
    }

    public synchronized SparseArray<byte[]> getManufacturerDataArray() {
        if (this.manufacturerDataArray == null) {
            if (this.result == null) {
                this.manufacturerDataArray = new SparseArray();
                return this.manufacturerDataArray;
            }
            ScanRecord record = this.result.getScanRecord();
            if (record == null) {
                this.manufacturerDataArray = new SparseArray();
                return this.manufacturerDataArray;
            }
            this.manufacturerDataArray = record.getManufacturerSpecificData();
        }
        return this.manufacturerDataArray;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.timeStamp);
        dest.writeParcelable(this.device, flags);
        dest.writeParcelable(this.businessType, flags);
        dest.writeInt(this.callbackType);
        dest.writeInt(this.errorCode);
        dest.writeParcelable(this.result, flags);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CloseRangeResult{timeStamp=");
        stringBuilder.append(this.timeStamp);
        stringBuilder.append(", businessType=");
        stringBuilder.append(this.businessType);
        stringBuilder.append(", callbackType=");
        stringBuilder.append(this.callbackType);
        stringBuilder.append(", errorCode=");
        stringBuilder.append(this.errorCode);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
