package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterBlobArgument extends HwKeymasterArgument {
    public final byte[] blob;

    public HwKeymasterBlobArgument(int tag, byte[] blob) {
        super(tag);
        switch (HwKeymasterDefs.getTagType(tag)) {
            case HwKeymasterDefs.KM_BIGNUM /*-2147483648*/:
            case HwKeymasterDefs.KM_BYTES /*-1879048192*/:
                this.blob = blob;
                return;
            default:
                throw new IllegalArgumentException("Bad blob tag " + tag);
        }
    }

    public HwKeymasterBlobArgument(int tag, Parcel in) {
        super(tag);
        this.blob = in.createByteArray();
    }

    public void writeValue(Parcel out) {
        out.writeByteArray(this.blob);
    }
}
