package com.huawei.nb.model.applets;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class AppletsCard extends AManagedObject {
    public static final Creator<AppletsCard> CREATOR = new Creator<AppletsCard>() {
        public AppletsCard createFromParcel(Parcel in) {
            return new AppletsCard(in);
        }

        public AppletsCard[] newArray(int size) {
            return new AppletsCard[size];
        }
    };
    private String base_info;
    private String card_id;
    private Integer card_status;
    private String card_type;
    private Date life_cycle_date;
    private String routine_id;

    public AppletsCard(Cursor cursor) {
        Date date = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.routine_id = cursor.getString(1);
        this.card_id = cursor.getString(2);
        this.card_status = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.card_type = cursor.getString(4);
        this.base_info = cursor.getString(5);
        if (!cursor.isNull(6)) {
            date = new Date(cursor.getLong(6));
        }
        this.life_cycle_date = date;
    }

    public AppletsCard(Parcel in) {
        Date date = null;
        super(in);
        this.routine_id = in.readByte() == (byte) 0 ? null : in.readString();
        this.card_id = in.readByte() == (byte) 0 ? null : in.readString();
        this.card_status = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.card_type = in.readByte() == (byte) 0 ? null : in.readString();
        this.base_info = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            date = new Date(in.readLong());
        }
        this.life_cycle_date = date;
    }

    private AppletsCard(String routine_id, String card_id, Integer card_status, String card_type, String base_info, Date life_cycle_date) {
        this.routine_id = routine_id;
        this.card_id = card_id;
        this.card_status = card_status;
        this.card_type = card_type;
        this.base_info = base_info;
        this.life_cycle_date = life_cycle_date;
    }

    public int describeContents() {
        return 0;
    }

    public String getRoutine_id() {
        return this.routine_id;
    }

    public void setRoutine_id(String routine_id) {
        this.routine_id = routine_id;
        setValue();
    }

    public String getCard_id() {
        return this.card_id;
    }

    public void setCard_id(String card_id) {
        this.card_id = card_id;
        setValue();
    }

    public Integer getCard_status() {
        return this.card_status;
    }

    public void setCard_status(Integer card_status) {
        this.card_status = card_status;
        setValue();
    }

    public String getCard_type() {
        return this.card_type;
    }

    public void setCard_type(String card_type) {
        this.card_type = card_type;
        setValue();
    }

    public String getBase_info() {
        return this.base_info;
    }

    public void setBase_info(String base_info) {
        this.base_info = base_info;
        setValue();
    }

    public Date getLife_cycle_date() {
        return this.life_cycle_date;
    }

    public void setLife_cycle_date(Date life_cycle_date) {
        this.life_cycle_date = life_cycle_date;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.routine_id != null) {
            out.writeByte((byte) 1);
            out.writeString(this.routine_id);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.card_id != null) {
            out.writeByte((byte) 1);
            out.writeString(this.card_id);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.card_status != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.card_status.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.card_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.card_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.base_info != null) {
            out.writeByte((byte) 1);
            out.writeString(this.base_info);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.life_cycle_date != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.life_cycle_date.getTime());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AppletsCard> getHelper() {
        return AppletsCardHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.applets.AppletsCard";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AppletsCard { routine_id: ").append(this.routine_id);
        sb.append(", card_id: ").append(this.card_id);
        sb.append(", card_status: ").append(this.card_status);
        sb.append(", card_type: ").append(this.card_type);
        sb.append(", base_info: ").append(this.base_info);
        sb.append(", life_cycle_date: ").append(this.life_cycle_date);
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
        return "0.0.11";
    }

    public int getDatabaseVersionCode() {
        return 11;
    }

    public String getEntityVersion() {
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
