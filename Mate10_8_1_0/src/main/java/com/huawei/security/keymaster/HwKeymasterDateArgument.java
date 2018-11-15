package com.huawei.security.keymaster;

import android.os.Parcel;
import java.util.Date;

class HwKeymasterDateArgument extends HwKeymasterArgument {
    public final Date date;

    public HwKeymasterDateArgument(int tag, Date date) {
        super(tag);
        switch (HwKeymasterDefs.getTagType(tag)) {
            case HwKeymasterDefs.KM_DATE /*1610612736*/:
                this.date = date;
                return;
            default:
                throw new IllegalArgumentException("Bad date tag " + tag);
        }
    }

    public HwKeymasterDateArgument(int tag, Parcel in) {
        super(tag);
        this.date = new Date(in.readLong());
    }

    public void writeValue(Parcel out) {
        out.writeLong(this.date.getTime());
    }
}
