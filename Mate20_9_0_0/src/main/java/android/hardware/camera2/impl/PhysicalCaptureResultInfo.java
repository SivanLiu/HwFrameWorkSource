package android.hardware.camera2.impl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class PhysicalCaptureResultInfo implements Parcelable {
    public static final Creator<PhysicalCaptureResultInfo> CREATOR = new Creator<PhysicalCaptureResultInfo>() {
        public PhysicalCaptureResultInfo createFromParcel(Parcel in) {
            return new PhysicalCaptureResultInfo(in, null);
        }

        public PhysicalCaptureResultInfo[] newArray(int size) {
            return new PhysicalCaptureResultInfo[size];
        }
    };
    private String cameraId;
    private CameraMetadataNative cameraMetadata;

    private PhysicalCaptureResultInfo(Parcel in) {
        readFromParcel(in);
    }

    public PhysicalCaptureResultInfo(String cameraId, CameraMetadataNative cameraMetadata) {
        this.cameraId = cameraId;
        this.cameraMetadata = cameraMetadata;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.cameraId);
        this.cameraMetadata.writeToParcel(dest, flags);
    }

    public void readFromParcel(Parcel in) {
        this.cameraId = in.readString();
        this.cameraMetadata = new CameraMetadataNative();
        this.cameraMetadata.readFromParcel(in);
    }

    public String getCameraId() {
        return this.cameraId;
    }

    public CameraMetadataNative getCameraMetadata() {
        return this.cameraMetadata;
    }
}
