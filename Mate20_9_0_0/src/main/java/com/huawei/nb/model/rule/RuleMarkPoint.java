package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RuleMarkPoint extends AManagedObject {
    public static final Creator<RuleMarkPoint> CREATOR = new Creator<RuleMarkPoint>() {
        public RuleMarkPoint createFromParcel(Parcel in) {
            return new RuleMarkPoint(in);
        }

        public RuleMarkPoint[] newArray(int size) {
            return new RuleMarkPoint[size];
        }
    };
    private String businessName;
    private Integer category;
    private Long id;
    private String itemName;
    private String operatorName;
    private Integer recommendedCount;
    private String ruleName;
    private Date timeStamp;

    public RuleMarkPoint(Cursor cursor) {
        Date date = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.ruleName = cursor.getString(2);
        this.businessName = cursor.getString(3);
        this.operatorName = cursor.getString(4);
        this.itemName = cursor.getString(5);
        this.recommendedCount = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        this.category = cursor.isNull(7) ? null : Integer.valueOf(cursor.getInt(7));
        if (!cursor.isNull(8)) {
            date = new Date(cursor.getLong(8));
        }
        this.timeStamp = date;
    }

    public RuleMarkPoint(Parcel in) {
        Date date = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.ruleName = in.readByte() == (byte) 0 ? null : in.readString();
        this.businessName = in.readByte() == (byte) 0 ? null : in.readString();
        this.operatorName = in.readByte() == (byte) 0 ? null : in.readString();
        this.itemName = in.readByte() == (byte) 0 ? null : in.readString();
        this.recommendedCount = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.category = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            date = new Date(in.readLong());
        }
        this.timeStamp = date;
    }

    private RuleMarkPoint(Long id, String ruleName, String businessName, String operatorName, String itemName, Integer recommendedCount, Integer category, Date timeStamp) {
        this.id = id;
        this.ruleName = ruleName;
        this.businessName = businessName;
        this.operatorName = operatorName;
        this.itemName = itemName;
        this.recommendedCount = recommendedCount;
        this.category = category;
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

    public String getRuleName() {
        return this.ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
        setValue();
    }

    public String getBusinessName() {
        return this.businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
        setValue();
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
        setValue();
    }

    public String getItemName() {
        return this.itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
        setValue();
    }

    public Integer getRecommendedCount() {
        return this.recommendedCount;
    }

    public void setRecommendedCount(Integer recommendedCount) {
        this.recommendedCount = recommendedCount;
        setValue();
    }

    public Integer getCategory() {
        return this.category;
    }

    public void setCategory(Integer category) {
        this.category = category;
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
        if (this.ruleName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.ruleName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.businessName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.businessName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.operatorName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.operatorName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.itemName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.itemName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.recommendedCount != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.recommendedCount.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.category != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.category.intValue());
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

    public AEntityHelper<RuleMarkPoint> getHelper() {
        return RuleMarkPointHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.RuleMarkPoint";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RuleMarkPoint { id: ").append(this.id);
        sb.append(", ruleName: ").append(this.ruleName);
        sb.append(", businessName: ").append(this.businessName);
        sb.append(", operatorName: ").append(this.operatorName);
        sb.append(", itemName: ").append(this.itemName);
        sb.append(", recommendedCount: ").append(this.recommendedCount);
        sb.append(", category: ").append(this.category);
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
