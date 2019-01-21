package android.security.keymaster;

import android.os.Parcel;
import java.util.Date;

class KeymasterDateArgument extends KeymasterArgument {
    public final Date date;

    public KeymasterDateArgument(int tag, Date date) {
        super(tag);
        if (KeymasterDefs.getTagType(tag) == KeymasterDefs.KM_DATE) {
            this.date = date;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad date tag ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public KeymasterDateArgument(int tag, Parcel in) {
        super(tag);
        this.date = new Date(in.readLong());
    }

    public void writeValue(Parcel out) {
        out.writeLong(this.date.getTime());
    }
}
