package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class SearchIndexSwitch extends AManagedObject {
    public static final Creator<SearchIndexSwitch> CREATOR = new Creator<SearchIndexSwitch>() {
        public SearchIndexSwitch createFromParcel(Parcel in) {
            return new SearchIndexSwitch(in);
        }

        public SearchIndexSwitch[] newArray(int size) {
            return new SearchIndexSwitch[size];
        }
    };
    private Integer id;
    private boolean isSwitchOn;
    private Integer userId;

    public SearchIndexSwitch(Cursor cursor) {
        boolean z;
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        if (!cursor.isNull(2)) {
            num = Integer.valueOf(cursor.getInt(2));
        }
        this.userId = num;
        if (cursor.getInt(3) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isSwitchOn = z;
    }

    public SearchIndexSwitch(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.userId = num;
        this.isSwitchOn = in.readByte() != (byte) 0;
    }

    private SearchIndexSwitch(Integer id, Integer userId, boolean isSwitchOn) {
        this.id = id;
        this.userId = userId;
        this.isSwitchOn = isSwitchOn;
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

    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
        setValue();
    }

    public boolean getIsSwitchOn() {
        return this.isSwitchOn;
    }

    public void setIsSwitchOn(boolean isSwitchOn) {
        this.isSwitchOn = isSwitchOn;
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
        if (this.userId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.userId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (!this.isSwitchOn) {
            b = (byte) 0;
        }
        out.writeByte(b);
    }

    public AEntityHelper<SearchIndexSwitch> getHelper() {
        return SearchIndexSwitchHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.SearchIndexSwitch";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SearchIndexSwitch { id: ").append(this.id);
        sb.append(", userId: ").append(this.userId);
        sb.append(", isSwitchOn: ").append(this.isSwitchOn);
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
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
