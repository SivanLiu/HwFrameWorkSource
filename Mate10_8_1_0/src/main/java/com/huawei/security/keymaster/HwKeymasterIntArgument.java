package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterIntArgument extends HwKeymasterArgument {
    public final int value;

    public HwKeymasterIntArgument(int tag, int value) {
        super(tag);
        switch (HwKeymasterDefs.getTagType(tag)) {
            case HwKeymasterDefs.KM_ENUM /*268435456*/:
            case HwKeymasterDefs.KM_ENUM_REP /*536870912*/:
            case HwKeymasterDefs.KM_UINT /*805306368*/:
            case HwKeymasterDefs.KM_UINT_REP /*1073741824*/:
                this.value = value;
                return;
            default:
                throw new IllegalArgumentException("Bad int tag " + tag);
        }
    }

    public HwKeymasterIntArgument(int tag, Parcel in) {
        super(tag);
        this.value = in.readInt();
    }

    public void writeValue(Parcel out) {
        out.writeInt(this.value);
    }
}
