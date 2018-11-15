package com.huawei.security.keymaster;

import android.os.Parcel;
import java.util.Date;

class HwKeymasterDateArgument extends HwKeymasterArgument {
    public final Date date;

    public HwKeymasterDateArgument(int tag, Date date) {
        super(tag);
        if (HwKeymasterDefs.getTagType(tag) == HwKeymasterDefs.KM_DATE) {
            this.date = date;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad date tag ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public HwKeymasterDateArgument(int tag, Parcel in) {
        super(tag);
        this.date = new Date(in.readLong());
    }

    public void writeValue(Parcel out) {
        out.writeLong(this.date.getTime());
    }
}
