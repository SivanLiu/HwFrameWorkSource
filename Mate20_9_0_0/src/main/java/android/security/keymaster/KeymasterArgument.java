package android.security.keymaster;

import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

abstract class KeymasterArgument implements Parcelable {
    public static final Creator<KeymasterArgument> CREATOR = new Creator<KeymasterArgument>() {
        public KeymasterArgument createFromParcel(Parcel in) {
            int pos = in.dataPosition();
            int tag = in.readInt();
            int tagType = KeymasterDefs.getTagType(tag);
            if (tagType == Integer.MIN_VALUE || tagType == KeymasterDefs.KM_BYTES) {
                return new KeymasterBlobArgument(tag, in);
            }
            if (tagType != KeymasterDefs.KM_ULONG_REP) {
                if (tagType == 268435456 || tagType == 536870912 || tagType == 805306368 || tagType == 1073741824) {
                    return new KeymasterIntArgument(tag, in);
                }
                if (tagType != KeymasterDefs.KM_ULONG) {
                    if (tagType == KeymasterDefs.KM_DATE) {
                        return new KeymasterDateArgument(tag, in);
                    }
                    if (tagType == KeymasterDefs.KM_BOOL) {
                        return new KeymasterBooleanArgument(tag, in);
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad tag: ");
                    stringBuilder.append(tag);
                    stringBuilder.append(" at ");
                    stringBuilder.append(pos);
                    throw new ParcelFormatException(stringBuilder.toString());
                }
            }
            return new KeymasterLongArgument(tag, in);
        }

        public KeymasterArgument[] newArray(int size) {
            return new KeymasterArgument[size];
        }
    };
    public final int tag;

    public abstract void writeValue(Parcel parcel);

    protected KeymasterArgument(int tag) {
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
