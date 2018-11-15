package com.huawei.security.keymaster;

import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

abstract class HwKeymasterArgument implements Parcelable {
    public static final Creator<HwKeymasterArgument> CREATOR = new Creator<HwKeymasterArgument>() {
        public HwKeymasterArgument createFromParcel(Parcel in) {
            int pos = in.dataPosition();
            int tag = in.readInt();
            switch (HwKeymasterDefs.getTagType(tag)) {
                case HwKeymasterDefs.KM_BIGNUM /*-2147483648*/:
                case HwKeymasterDefs.KM_BYTES /*-1879048192*/:
                    return new HwKeymasterBlobArgument(tag, in);
                case HwKeymasterDefs.KM_ULONG_REP /*-1610612736*/:
                case HwKeymasterDefs.KM_ULONG /*1342177280*/:
                    return new HwKeymasterLongArgument(tag, in);
                case HwKeymasterDefs.KM_ENUM /*268435456*/:
                case HwKeymasterDefs.KM_ENUM_REP /*536870912*/:
                case HwKeymasterDefs.KM_UINT /*805306368*/:
                case HwKeymasterDefs.KM_UINT_REP /*1073741824*/:
                    return new HwKeymasterIntArgument(tag, in);
                case HwKeymasterDefs.KM_DATE /*1610612736*/:
                    return new HwKeymasterDateArgument(tag, in);
                case HwKeymasterDefs.KM_BOOL /*1879048192*/:
                    return new HwKeymasterBooleanArgument(tag, in);
                default:
                    throw new ParcelFormatException("Bad tag: " + tag + " at " + pos);
            }
        }

        public HwKeymasterArgument[] newArray(int size) {
            return new HwKeymasterArgument[size];
        }
    };
    public final int tag;

    public abstract void writeValue(Parcel parcel);

    protected HwKeymasterArgument(int tag) {
        this.tag = tag;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.tag);
        writeValue(out);
    }
}
