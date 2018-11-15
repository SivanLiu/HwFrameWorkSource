package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawBankInfo extends AManagedObject {
    public static final Creator<RawBankInfo> CREATOR = new Creator<RawBankInfo>() {
        public RawBankInfo createFromParcel(Parcel in) {
            return new RawBankInfo(in);
        }

        public RawBankInfo[] newArray(int size) {
            return new RawBankInfo[size];
        }
    };
    private String mBankInfo;
    private Integer mId;
    private String mLastNo;
    private Double mRepayAmountUSD;
    private Double mRepayLowestCNY;
    private Double mRepayLowestUSD;
    private Double mRepaymentAmountCNY;
    private Date mRepaymentDate;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public RawBankInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mBankInfo = cursor.getString(3);
        this.mLastNo = cursor.getString(4);
        this.mRepaymentDate = cursor.isNull(5) ? null : new Date(cursor.getLong(5));
        this.mRepaymentAmountCNY = cursor.isNull(6) ? null : Double.valueOf(cursor.getDouble(6));
        this.mRepayLowestCNY = cursor.isNull(7) ? null : Double.valueOf(cursor.getDouble(7));
        this.mRepayAmountUSD = cursor.isNull(8) ? null : Double.valueOf(cursor.getDouble(8));
        this.mRepayLowestUSD = cursor.isNull(9) ? null : Double.valueOf(cursor.getDouble(9));
        if (!cursor.isNull(10)) {
            num = Integer.valueOf(cursor.getInt(10));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(11);
    }

    public RawBankInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mBankInfo = in.readByte() == (byte) 0 ? null : in.readString();
        this.mLastNo = in.readByte() == (byte) 0 ? null : in.readString();
        this.mRepaymentDate = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mRepaymentAmountCNY = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mRepayLowestCNY = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mRepayAmountUSD = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mRepayLowestUSD = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawBankInfo(Integer mId, Date mTimeStamp, String mBankInfo, String mLastNo, Date mRepaymentDate, Double mRepaymentAmountCNY, Double mRepayLowestCNY, Double mRepayAmountUSD, Double mRepayLowestUSD, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mBankInfo = mBankInfo;
        this.mLastNo = mLastNo;
        this.mRepaymentDate = mRepaymentDate;
        this.mRepaymentAmountCNY = mRepaymentAmountCNY;
        this.mRepayLowestCNY = mRepayLowestCNY;
        this.mRepayAmountUSD = mRepayAmountUSD;
        this.mRepayLowestUSD = mRepayLowestUSD;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMId() {
        return this.mId;
    }

    public void setMId(Integer mId) {
        this.mId = mId;
        setValue();
    }

    public Date getMTimeStamp() {
        return this.mTimeStamp;
    }

    public void setMTimeStamp(Date mTimeStamp) {
        this.mTimeStamp = mTimeStamp;
        setValue();
    }

    public String getMBankInfo() {
        return this.mBankInfo;
    }

    public void setMBankInfo(String mBankInfo) {
        this.mBankInfo = mBankInfo;
        setValue();
    }

    public String getMLastNo() {
        return this.mLastNo;
    }

    public void setMLastNo(String mLastNo) {
        this.mLastNo = mLastNo;
        setValue();
    }

    public Date getMRepaymentDate() {
        return this.mRepaymentDate;
    }

    public void setMRepaymentDate(Date mRepaymentDate) {
        this.mRepaymentDate = mRepaymentDate;
        setValue();
    }

    public Double getMRepaymentAmountCNY() {
        return this.mRepaymentAmountCNY;
    }

    public void setMRepaymentAmountCNY(Double mRepaymentAmountCNY) {
        this.mRepaymentAmountCNY = mRepaymentAmountCNY;
        setValue();
    }

    public Double getMRepayLowestCNY() {
        return this.mRepayLowestCNY;
    }

    public void setMRepayLowestCNY(Double mRepayLowestCNY) {
        this.mRepayLowestCNY = mRepayLowestCNY;
        setValue();
    }

    public Double getMRepayAmountUSD() {
        return this.mRepayAmountUSD;
    }

    public void setMRepayAmountUSD(Double mRepayAmountUSD) {
        this.mRepayAmountUSD = mRepayAmountUSD;
        setValue();
    }

    public Double getMRepayLowestUSD() {
        return this.mRepayLowestUSD;
    }

    public void setMRepayLowestUSD(Double mRepayLowestUSD) {
        this.mRepayLowestUSD = mRepayLowestUSD;
        setValue();
    }

    public Integer getMReservedInt() {
        return this.mReservedInt;
    }

    public void setMReservedInt(Integer mReservedInt) {
        this.mReservedInt = mReservedInt;
        setValue();
    }

    public String getMReservedText() {
        return this.mReservedText;
    }

    public void setMReservedText(String mReservedText) {
        this.mReservedText = mReservedText;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mTimeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mTimeStamp.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBankInfo != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mBankInfo);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLastNo != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mLastNo);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mRepaymentDate != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mRepaymentDate.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mRepaymentAmountCNY != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mRepaymentAmountCNY.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mRepayLowestCNY != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mRepayLowestCNY.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mRepayAmountUSD != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mRepayAmountUSD.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mRepayLowestUSD != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mRepayLowestUSD.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedInt != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReservedInt.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReservedText);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<RawBankInfo> getHelper() {
        return RawBankInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawBankInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawBankInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mBankInfo: ").append(this.mBankInfo);
        sb.append(", mLastNo: ").append(this.mLastNo);
        sb.append(", mRepaymentDate: ").append(this.mRepaymentDate);
        sb.append(", mRepaymentAmountCNY: ").append(this.mRepaymentAmountCNY);
        sb.append(", mRepayLowestCNY: ").append(this.mRepayLowestCNY);
        sb.append(", mRepayAmountUSD: ").append(this.mRepayAmountUSD);
        sb.append(", mRepayLowestUSD: ").append(this.mRepayLowestUSD);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
