package tmsdkobf;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.SparseArray;

public class il extends SparseArray<String> implements Parcelable {
    public static final Creator<il> CREATOR = new Creator<il>() {
        public il a(Parcel parcel) {
            int -l_2_I = parcel.readInt();
            if (-l_2_I >= 0) {
                return new il(parcel, -l_2_I);
            }
            throw new IllegalArgumentException("negative size " + -l_2_I);
        }

        public il[] ad(int i) {
            return new il[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return a(parcel);
        }

        public /* synthetic */ Object[] newArray(int i) {
            return ad(i);
        }
    };

    public il(int i) {
        super(i);
    }

    protected il(Parcel parcel, int i) {
        this((i + 32) & -32);
        for (int -l_3_I = 0; -l_3_I < i; -l_3_I++) {
            put(parcel.readInt(), parcel.readString());
        }
    }

    public int describeContents() {
        return 0;
    }

    public synchronized void writeToParcel(Parcel parcel, int i) {
        int -l_3_I = size();
        parcel.writeInt(-l_3_I);
        for (int -l_4_I = 0; -l_4_I < -l_3_I; -l_4_I++) {
            parcel.writeInt(keyAt(-l_4_I));
            parcel.writeString((String) valueAt(-l_4_I));
        }
    }
}
