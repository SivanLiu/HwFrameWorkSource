package com.huawei.security.keymaster;

import android.os.Parcel;

class HwKeymasterBlobArgument extends HwKeymasterArgument {
    public final byte[] blob;

    public HwKeymasterBlobArgument(int tag, byte[] blob) {
        super(tag);
        int tagType = HwKeymasterDefs.getTagType(tag);
        if (tagType == HwKeymasterDefs.KM_BIGNUM || tagType == HwKeymasterDefs.KM_BYTES) {
            this.blob = blob;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad blob tag ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public HwKeymasterBlobArgument(int tag, Parcel in) {
        super(tag);
        this.blob = in.createByteArray();
    }

    public void writeValue(Parcel out) {
        out.writeByteArray(this.blob);
    }
}
