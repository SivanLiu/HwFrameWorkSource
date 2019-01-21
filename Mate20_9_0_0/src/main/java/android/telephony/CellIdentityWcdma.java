package android.telephony;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import java.util.Objects;

public final class CellIdentityWcdma extends CellIdentity {
    public static final Creator<CellIdentityWcdma> CREATOR = new Creator<CellIdentityWcdma>() {
        public CellIdentityWcdma createFromParcel(Parcel in) {
            in.readInt();
            return CellIdentityWcdma.createFromParcelBody(in);
        }

        public CellIdentityWcdma[] newArray(int size) {
            return new CellIdentityWcdma[size];
        }
    };
    private static final boolean DBG = false;
    private static final String TAG = CellIdentityWcdma.class.getSimpleName();
    private final int mCid;
    private final int mLac;
    private final int mPsc;
    private final int mUarfcn;

    public CellIdentityWcdma() {
        super(TAG, 5, null, null, null, null);
        this.mLac = Integer.MAX_VALUE;
        this.mCid = Integer.MAX_VALUE;
        this.mPsc = Integer.MAX_VALUE;
        this.mUarfcn = Integer.MAX_VALUE;
    }

    public CellIdentityWcdma(int mcc, int mnc, int lac, int cid, int psc) {
        this(lac, cid, psc, Integer.MAX_VALUE, String.valueOf(mcc), String.valueOf(mnc), null, null);
    }

    public CellIdentityWcdma(int mcc, int mnc, int lac, int cid, int psc, int uarfcn) {
        this(lac, cid, psc, uarfcn, String.valueOf(mcc), String.valueOf(mnc), null, null);
    }

    public CellIdentityWcdma(int lac, int cid, int psc, int uarfcn, String mccStr, String mncStr, String alphal, String alphas) {
        super(TAG, 4, mccStr, mncStr, alphal, alphas);
        this.mLac = lac;
        this.mCid = cid;
        this.mPsc = psc;
        this.mUarfcn = uarfcn;
    }

    private CellIdentityWcdma(CellIdentityWcdma cid) {
        this(cid.mLac, cid.mCid, cid.mPsc, cid.mUarfcn, cid.mMccStr, cid.mMncStr, cid.mAlphaLong, cid.mAlphaShort);
    }

    CellIdentityWcdma copy() {
        return new CellIdentityWcdma(this);
    }

    @Deprecated
    public int getMcc() {
        return this.mMccStr != null ? Integer.valueOf(this.mMccStr).intValue() : Integer.MAX_VALUE;
    }

    @Deprecated
    public int getMnc() {
        return this.mMncStr != null ? Integer.valueOf(this.mMncStr).intValue() : Integer.MAX_VALUE;
    }

    public int getLac() {
        return this.mLac;
    }

    public int getCid() {
        return this.mCid;
    }

    public int getPsc() {
        return this.mPsc;
    }

    public String getMccString() {
        return this.mMccStr;
    }

    public String getMncString() {
        return this.mMncStr;
    }

    public String getMobileNetworkOperator() {
        if (this.mMccStr == null || this.mMncStr == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mMccStr);
        stringBuilder.append(this.mMncStr);
        return stringBuilder.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.mLac), Integer.valueOf(this.mCid), Integer.valueOf(this.mPsc), Integer.valueOf(super.hashCode())});
    }

    public int getUarfcn() {
        return this.mUarfcn;
    }

    public int getChannelNumber() {
        return this.mUarfcn;
    }

    public boolean equals(Object other) {
        boolean z = true;
        if (this == other) {
            return true;
        }
        if (!(other instanceof CellIdentityWcdma)) {
            return false;
        }
        CellIdentityWcdma o = (CellIdentityWcdma) other;
        if (!(this.mLac == o.mLac && this.mCid == o.mCid && this.mPsc == o.mPsc && this.mUarfcn == o.mUarfcn && TextUtils.equals(this.mMccStr, o.mMccStr) && TextUtils.equals(this.mMncStr, o.mMncStr) && super.equals(other))) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(TAG);
        stringBuilder.append(":{ mPsc=");
        stringBuilder.append(this.mPsc);
        stringBuilder.append(" mUarfcn=");
        stringBuilder.append(this.mUarfcn);
        stringBuilder.append(" mMcc=");
        stringBuilder.append(this.mMccStr);
        stringBuilder.append(" mMnc=");
        stringBuilder.append(this.mMncStr);
        stringBuilder.append(" mAlphaLong=");
        stringBuilder.append(this.mAlphaLong);
        stringBuilder.append(" mAlphaShort=");
        stringBuilder.append(this.mAlphaShort);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, 4);
        dest.writeInt(this.mLac);
        dest.writeInt(this.mCid);
        dest.writeInt(this.mPsc);
        dest.writeInt(this.mUarfcn);
    }

    private CellIdentityWcdma(Parcel in) {
        super(TAG, 4, in);
        this.mLac = in.readInt();
        this.mCid = in.readInt();
        this.mPsc = in.readInt();
        this.mUarfcn = in.readInt();
    }

    protected static CellIdentityWcdma createFromParcelBody(Parcel in) {
        return new CellIdentityWcdma(in);
    }
}
