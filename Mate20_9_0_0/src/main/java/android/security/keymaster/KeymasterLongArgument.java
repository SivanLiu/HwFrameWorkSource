package android.security.keymaster;

import android.os.Parcel;

class KeymasterLongArgument extends KeymasterArgument {
    public final long value;

    public KeymasterLongArgument(int tag, long value) {
        super(tag);
        int tagType = KeymasterDefs.getTagType(tag);
        if (tagType == KeymasterDefs.KM_ULONG_REP || tagType == KeymasterDefs.KM_ULONG) {
            this.value = value;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad long tag ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public KeymasterLongArgument(int tag, Parcel in) {
        super(tag);
        this.value = in.readLong();
    }

    public void writeValue(Parcel out) {
        out.writeLong(this.value);
    }
}
