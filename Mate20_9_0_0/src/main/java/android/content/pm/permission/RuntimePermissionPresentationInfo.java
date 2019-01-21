package android.content.pm.permission;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

@SystemApi
public final class RuntimePermissionPresentationInfo implements Parcelable {
    public static final Creator<RuntimePermissionPresentationInfo> CREATOR = new Creator<RuntimePermissionPresentationInfo>() {
        public RuntimePermissionPresentationInfo createFromParcel(Parcel source) {
            return new RuntimePermissionPresentationInfo(source, null);
        }

        public RuntimePermissionPresentationInfo[] newArray(int size) {
            return new RuntimePermissionPresentationInfo[size];
        }
    };
    private static final int FLAG_GRANTED = 1;
    private static final int FLAG_STANDARD = 2;
    private final int mFlags;
    private final CharSequence mLabel;

    /* synthetic */ RuntimePermissionPresentationInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public RuntimePermissionPresentationInfo(CharSequence label, boolean granted, boolean standard) {
        this.mLabel = label;
        int flags = 0;
        if (granted) {
            flags = 0 | 1;
        }
        if (standard) {
            flags |= 2;
        }
        this.mFlags = flags;
    }

    private RuntimePermissionPresentationInfo(Parcel parcel) {
        this.mLabel = parcel.readCharSequence();
        this.mFlags = parcel.readInt();
    }

    public boolean isGranted() {
        return (this.mFlags & 1) != 0;
    }

    public boolean isStandard() {
        return (this.mFlags & 2) != 0;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeCharSequence(this.mLabel);
        parcel.writeInt(this.mFlags);
    }
}
