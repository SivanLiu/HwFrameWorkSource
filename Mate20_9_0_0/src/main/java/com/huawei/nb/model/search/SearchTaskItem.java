package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class SearchTaskItem extends AManagedObject {
    public static final Creator<SearchTaskItem> CREATOR = new Creator<SearchTaskItem>() {
        public SearchTaskItem createFromParcel(Parcel in) {
            return new SearchTaskItem(in);
        }

        public SearchTaskItem[] newArray(int size) {
            return new SearchTaskItem[size];
        }
    };
    private Integer id;
    private String ids;
    private Integer op;
    private String pkgName;
    private Integer runTimes;
    private Integer type;
    private Integer userId;

    public SearchTaskItem(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.pkgName = cursor.getString(2);
        this.ids = cursor.getString(3);
        this.op = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.userId = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        this.type = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.runTimes = num;
    }

    public SearchTaskItem(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.pkgName = in.readByte() == (byte) 0 ? null : in.readString();
        this.ids = in.readByte() == (byte) 0 ? null : in.readString();
        this.op = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.userId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.type = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.runTimes = num;
    }

    private SearchTaskItem(Integer id, String pkgName, String ids, Integer op, Integer userId, Integer type, Integer runTimes) {
        this.id = id;
        this.pkgName = pkgName;
        this.ids = ids;
        this.op = op;
        this.userId = userId;
        this.type = type;
        this.runTimes = runTimes;
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

    public String getPkgName() {
        return this.pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
        setValue();
    }

    public String getIds() {
        return this.ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
        setValue();
    }

    public Integer getOp() {
        return this.op;
    }

    public void setOp(Integer op) {
        this.op = op;
        setValue();
    }

    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
        setValue();
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer type) {
        this.type = type;
        setValue();
    }

    public Integer getRunTimes() {
        return this.runTimes;
    }

    public void setRunTimes(Integer runTimes) {
        this.runTimes = runTimes;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.pkgName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.pkgName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.ids != null) {
            out.writeByte((byte) 1);
            out.writeString(this.ids);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.op != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.op.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.userId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.userId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.type.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.runTimes != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.runTimes.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<SearchTaskItem> getHelper() {
        return SearchTaskItemHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.SearchTaskItem";
    }

    public String getDatabaseName() {
        return "dsSearch";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SearchTaskItem { id: ").append(this.id);
        sb.append(", pkgName: ").append(this.pkgName);
        sb.append(", ids: ").append(this.ids);
        sb.append(", op: ").append(this.op);
        sb.append(", userId: ").append(this.userId);
        sb.append(", type: ").append(this.type);
        sb.append(", runTimes: ").append(this.runTimes);
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
        return "0.0.5";
    }

    public int getDatabaseVersionCode() {
        return 5;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
