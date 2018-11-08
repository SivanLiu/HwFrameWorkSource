package tmsdkobf;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public interface ju {

    public static class a implements Parcelable {
        public static final Creator<a> CREATOR = new Creator<a>() {
            public a[] ak(int i) {
                return new a[i];
            }

            public a b(Parcel parcel) {
                return a.b(parcel);
            }

            public /* synthetic */ Object createFromParcel(Parcel parcel) {
                return b(parcel);
            }

            public /* synthetic */ Object[] newArray(int i) {
                return ak(i);
            }
        };
        public p tA;
        public c tB;
        public long ty;
        public long tz;

        public a(long j, long j2, p pVar) {
            this.ty = j;
            this.tz = j2;
            this.tA = pVar;
        }

        private static byte[] a(p pVar) {
            return pVar != null ? nn.d(pVar) : new byte[0];
        }

        private static a b(Parcel parcel) {
            long -l_1_J = parcel.readLong();
            long -l_3_J = parcel.readLong();
            int -l_5_I = parcel.readInt();
            byte[] -l_6_R = null;
            if (-l_5_I > 0) {
                -l_6_R = new byte[-l_5_I];
                parcel.readByteArray(-l_6_R);
            }
            Object -l_7_R = new a(-l_1_J, -l_3_J, i(-l_6_R));
            if (parcel.readByte() == (byte) 1) {
                -l_7_R.tB = new c(parcel.readInt(), parcel.readInt());
            }
            return -l_7_R;
        }

        private static p i(byte[] bArr) {
            return (bArr == null || bArr.length == 0) ? null : (p) nn.a(bArr, new p(), false);
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.ty);
            parcel.writeLong(this.tz);
            Object -l_3_R = a(this.tA);
            parcel.writeInt(-l_3_R.length);
            if (-l_3_R.length > 0) {
                parcel.writeByteArray(-l_3_R);
            }
            if (this.tB == null) {
                parcel.writeByte((byte) 0);
                return;
            }
            parcel.writeByte((byte) 1);
            parcel.writeInt(this.tB.tD);
            parcel.writeInt(this.tB.tE);
        }
    }

    public static abstract class b {
        public int tC = 0;

        public abstract void b(a aVar);
    }

    public static class c {
        public int tD;
        public int tE;

        public c(int i, int i2) {
            this.tD = i;
            this.tE = i2;
        }
    }

    void C(int i);

    void a(int i, b bVar);

    void a(a aVar, int i, int i2);

    void h();

    void i();
}
