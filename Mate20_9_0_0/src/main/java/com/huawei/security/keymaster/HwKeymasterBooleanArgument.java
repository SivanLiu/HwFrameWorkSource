package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterBooleanArgument extends HwKeymasterArgument {
    public HwKeymasterBooleanArgument(int tag) {
        super(tag);
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_BOOL) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad bool tag ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public HwKeymasterBooleanArgument(int tag, Parcel in) {
        super(tag);
    }

    public void writeValue(Parcel out) {
    }
}
