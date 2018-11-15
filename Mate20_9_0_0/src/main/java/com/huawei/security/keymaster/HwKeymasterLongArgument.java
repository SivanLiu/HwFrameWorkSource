package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterLongArgument extends HwKeymasterArgument {
    public final long value;

    public HwKeymasterLongArgument(int tag, long value) {
        super(tag);
        int tagType = HwKeymasterDefs.getTagType(tag);
        if (tagType == HwKeymasterDefs.KM_ULONG_REP || tagType == HwKeymasterDefs.KM_ULONG) {
            this.value = value;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad long tag ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public HwKeymasterLongArgument(int tag, Parcel in) {
        super(tag);
        this.value = in.readLong();
    }

    public void writeValue(Parcel out) {
        out.writeLong(this.value);
    }
}
