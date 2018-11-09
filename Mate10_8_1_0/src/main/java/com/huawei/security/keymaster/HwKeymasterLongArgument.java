package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterLongArgument extends HwKeymasterArgument {
    public final long value;

    public HwKeymasterLongArgument(int tag, long value) {
        super(tag);
        switch (HwKeymasterDefs.getTagType(tag)) {
            case HwKeymasterDefs.KM_ULONG_REP /*-1610612736*/:
            case HwKeymasterDefs.KM_ULONG /*1342177280*/:
                this.value = value;
                return;
            default:
                throw new IllegalArgumentException("Bad long tag " + tag);
        }
    }

    public HwKeymasterLongArgument(int tag, Parcel in) {
        super(tag);
        this.value = in.readLong();
    }

    public void writeValue(Parcel out) {
        out.writeLong(this.value);
    }
}
