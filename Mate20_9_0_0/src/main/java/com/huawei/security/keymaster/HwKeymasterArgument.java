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
            int tagType = HwKeymasterDefs.getTagType(tag);
            if (tagType == HwKeymasterDefs.KM_BIGNUM || tagType == HwKeymasterDefs.KM_BYTES) {
                return new HwKeymasterBlobArgument(tag, in);
            }
            if (tagType != HwKeymasterDefs.KM_ULONG_REP) {
                if (tagType == HwKeymasterDefs.KM_ENUM || tagType == HwKeymasterDefs.KM_ENUM_REP || tagType == HwKeymasterDefs.KM_UINT || tagType == HwKeymasterDefs.KM_UINT_REP) {
                    return new HwKeymasterIntArgument(tag, in);
                }
                if (tagType != HwKeymasterDefs.KM_ULONG) {
                    if (tagType == HwKeymasterDefs.KM_DATE) {
                        return new HwKeymasterDateArgument(tag, in);
                    }
                    if (tagType == HwKeymasterDefs.KM_BOOL) {
                        return new HwKeymasterBooleanArgument(tag, in);
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad tag: ");
                    stringBuilder.append(tag);
                    stringBuilder.append(" at ");
                    stringBuilder.append(pos);
                    throw new ParcelFormatException(stringBuilder.toString());
                }
            }
            return new HwKeymasterLongArgument(tag, in);
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
