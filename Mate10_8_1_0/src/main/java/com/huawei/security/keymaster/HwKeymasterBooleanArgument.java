package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterBooleanArgument extends HwKeymasterArgument {
    public HwKeymasterBooleanArgument(int tag) {
        super(tag);
        switch (HwKeymasterDefs.getTagType(tag)) {
            case HwKeymasterDefs.KM_BOOL /*1879048192*/:
                return;
            default:
                throw new IllegalArgumentException("Bad bool tag " + tag);
        }
    }

    public HwKeymasterBooleanArgument(int tag, Parcel in) {
        super(tag);
    }

    public void writeValue(Parcel out) {
    }
}
