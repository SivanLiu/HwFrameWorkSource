package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class Recommendation extends AManagedObject {
    public static final Creator<Recommendation> CREATOR = new Creator<Recommendation>() {
        public Recommendation createFromParcel(Parcel in) {
            return new Recommendation(in);
        }

        public Recommendation[] newArray(int size) {
            return new Recommendation[size];
        }
    };
    private Long businessId;
    private Long id;
    private Long itemId;
    private String message;
    private Long ruleId;
    private Date timeStamp;

    public Recommendation(Cursor cursor) {
        Date date = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.businessId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.itemId = cursor.isNull(3) ? null : Long.valueOf(cursor.getLong(3));
        this.ruleId = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.message = cursor.getString(5);
        if (!cursor.isNull(6)) {
            date = new Date(cursor.getLong(6));
        }
        this.timeStamp = date;
    }

    public Recommendation(Parcel in) {
        Date date = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.businessId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.itemId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.ruleId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.message = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            date = new Date(in.readLong());
        }
        this.timeStamp = date;
    }

    private Recommendation(Long id, Long businessId, Long itemId, Long ruleId, String message, Date timeStamp) {
        this.id = id;
        this.businessId = businessId;
        this.itemId = itemId;
        this.ruleId = ruleId;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
        setValue();
    }

    public Long getBusinessId() {
        return this.businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
        setValue();
    }

    public Long getItemId() {
        return this.itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
        setValue();
    }

    public Long getRuleId() {
        return this.ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
        setValue();
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
        setValue();
    }

    public Date getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.businessId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.businessId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.itemId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.itemId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.ruleId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.ruleId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.message != null) {
            out.writeByte((byte) 1);
            out.writeString(this.message);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timeStamp.getTime());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<Recommendation> getHelper() {
        return RecommendationHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.Recommendation";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Recommendation { id: ").append(this.id);
        sb.append(", businessId: ").append(this.businessId);
        sb.append(", itemId: ").append(this.itemId);
        sb.append(", ruleId: ").append(this.ruleId);
        sb.append(", message: ").append(this.message);
        sb.append(", timeStamp: ").append(this.timeStamp);
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
        return "0.0.3";
    }

    public int getDatabaseVersionCode() {
        return 3;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
