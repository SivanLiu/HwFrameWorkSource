package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterIntArgument extends HwKeymasterArgument {
    public final int value;

    public HwKeymasterIntArgument(int tag, int value) {
        super(tag);
        int tagType = HwKeymasterDefs.getTagType(tag);
        if (tagType == HwKeymasterDefs.KM_ENUM || tagType == HwKeymasterDefs.KM_ENUM_REP || tagType == HwKeymasterDefs.KM_UINT || tagType == HwKeymasterDefs.KM_UINT_REP) {
            this.value = value;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad int tag ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public HwKeymasterIntArgument(int tag, Parcel in) {
        super(tag);
        this.value = in.readInt();
    }

    public void writeValue(Parcel out) {
        out.writeInt(this.value);
    }
}
