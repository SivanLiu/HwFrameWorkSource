package com.huawei.nb.model.coordinator;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CoordinatorAudit extends AManagedObject {
    public static final Creator<CoordinatorAudit> CREATOR = new Creator<CoordinatorAudit>() {
        public CoordinatorAudit createFromParcel(Parcel in) {
            return new CoordinatorAudit(in);
        }

        public CoordinatorAudit[] newArray(int size) {
            return new CoordinatorAudit[size];
        }
    };
    private String appPackageName;
    private Long dataSize;
    private Integer id;
    private Long isNeedRetry;
    private boolean isRequestSuccess;
    private String netWorkState;
    private String requestDate;
    private Long successTransferTime;
    private Long successVerifyTime;
    private Long timeStamp;
    private String url;

    public CoordinatorAudit(Cursor cursor) {
        boolean z;
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.appPackageName = cursor.getString(2);
        this.url = cursor.getString(3);
        this.netWorkState = cursor.getString(4);
        this.timeStamp = cursor.isNull(5) ? null : Long.valueOf(cursor.getLong(5));
        this.isNeedRetry = cursor.isNull(6) ? null : Long.valueOf(cursor.getLong(6));
        this.successVerifyTime = cursor.isNull(7) ? null : Long.valueOf(cursor.getLong(7));
        this.successTransferTime = cursor.isNull(8) ? null : Long.valueOf(cursor.getLong(8));
        if (!cursor.isNull(9)) {
            l = Long.valueOf(cursor.getLong(9));
        }
        this.dataSize = l;
        this.requestDate = cursor.getString(10);
        if (cursor.getInt(11) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isRequestSuccess = z;
    }

    public CoordinatorAudit(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.appPackageName = in.readByte() == (byte) 0 ? null : in.readString();
        this.url = in.readByte() == (byte) 0 ? null : in.readString();
        this.netWorkState = in.readByte() == (byte) 0 ? null : in.readString();
        this.timeStamp = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.isNeedRetry = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.successVerifyTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.successTransferTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.dataSize = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.requestDate = str;
        this.isRequestSuccess = in.readByte() != (byte) 0;
    }

    private CoordinatorAudit(Integer id, String appPackageName, String url, String netWorkState, Long timeStamp, Long isNeedRetry, Long successVerifyTime, Long successTransferTime, Long dataSize, String requestDate, boolean isRequestSuccess) {
        this.id = id;
        this.appPackageName = appPackageName;
        this.url = url;
        this.netWorkState = netWorkState;
        this.timeStamp = timeStamp;
        this.isNeedRetry = isNeedRetry;
        this.successVerifyTime = successVerifyTime;
        this.successTransferTime = successTransferTime;
        this.dataSize = dataSize;
        this.requestDate = requestDate;
        this.isRequestSuccess = isRequestSuccess;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public String getAppPackageName() {
        return this.appPackageName;
    }

    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
        setValue();
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
        setValue();
    }

    public String getNetWorkState() {
        return this.netWorkState;
    }

    public void setNetWorkState(String netWorkState) {
        this.netWorkState = netWorkState;
        setValue();
    }

    public Long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
        setValue();
    }

    public Long getIsNeedRetry() {
        return this.isNeedRetry;
    }

    public void setIsNeedRetry(Long isNeedRetry) {
        this.isNeedRetry = isNeedRetry;
        setValue();
    }

    public Long getSuccessVerifyTime() {
        return this.successVerifyTime;
    }

    public void setSuccessVerifyTime(Long successVerifyTime) {
        this.successVerifyTime = successVerifyTime;
        setValue();
    }

    public Long getSuccessTransferTime() {
        return this.successTransferTime;
    }

    public void setSuccessTransferTime(Long successTransferTime) {
        this.successTransferTime = successTransferTime;
        setValue();
    }

    public Long getDataSize() {
        return this.dataSize;
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
        setValue();
    }

    public String getRequestDate() {
        return this.requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
        setValue();
    }

    public boolean getIsRequestSuccess() {
        return this.isRequestSuccess;
    }

    public void setIsRequestSuccess(boolean isRequestSuccess) {
        this.isRequestSuccess = isRequestSuccess;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        byte b = (byte) 1;
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.appPackageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.appPackageName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.url != null) {
            out.writeByte((byte) 1);
            out.writeString(this.url);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.netWorkState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.netWorkState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timeStamp.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isNeedRetry != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.isNeedRetry.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.successVerifyTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.successVerifyTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.successTransferTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.successTransferTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataSize != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.dataSize.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.requestDate != null) {
            out.writeByte((byte) 1);
            out.writeString(this.requestDate);
        } else {
            out.writeByte((byte) 0);
        }
        if (!this.isRequestSuccess) {
            b = (byte) 0;
        }
        out.writeByte(b);
    }

    public AEntityHelper<CoordinatorAudit> getHelper() {
        return CoordinatorAuditHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.coordinator.CoordinatorAudit";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CoordinatorAudit { id: ").append(this.id);
        sb.append(", appPackageName: ").append(this.appPackageName);
        sb.append(", url: ").append(this.url);
        sb.append(", netWorkState: ").append(this.netWorkState);
        sb.append(", timeStamp: ").append(this.timeStamp);
        sb.append(", isNeedRetry: ").append(this.isNeedRetry);
        sb.append(", successVerifyTime: ").append(this.successVerifyTime);
        sb.append(", successTransferTime: ").append(this.successTransferTime);
        sb.append(", dataSize: ").append(this.dataSize);
        sb.append(", requestDate: ").append(this.requestDate);
        sb.append(", isRequestSuccess: ").append(this.isRequestSuccess);
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
        return "0.0.13";
    }

    public int getDatabaseVersionCode() {
        return 13;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
