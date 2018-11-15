package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CapJobInstance extends AManagedObject {
    public static final Creator<CapJobInstance> CREATOR = new Creator<CapJobInstance>() {
        public CapJobInstance createFromParcel(Parcel in) {
            return new CapJobInstance(in);
        }

        public CapJobInstance[] newArray(int size) {
            return new CapJobInstance[size];
        }
    };
    private Long analyzeTime;
    private Long createTime;
    private Integer id;
    private Long lastModifyTime;
    private String result;
    private String resultDesc;
    private String status;
    private String tasks;
    private String type;

    public CapJobInstance(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.type = cursor.getString(2);
        this.status = cursor.getString(3);
        this.tasks = cursor.getString(4);
        this.result = cursor.getString(5);
        this.resultDesc = cursor.getString(6);
        this.analyzeTime = cursor.isNull(7) ? null : Long.valueOf(cursor.getLong(7));
        this.createTime = cursor.isNull(8) ? null : Long.valueOf(cursor.getLong(8));
        if (!cursor.isNull(9)) {
            l = Long.valueOf(cursor.getLong(9));
        }
        this.lastModifyTime = l;
    }

    public CapJobInstance(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.type = in.readByte() == (byte) 0 ? null : in.readString();
        this.status = in.readByte() == (byte) 0 ? null : in.readString();
        this.tasks = in.readByte() == (byte) 0 ? null : in.readString();
        this.result = in.readByte() == (byte) 0 ? null : in.readString();
        this.resultDesc = in.readByte() == (byte) 0 ? null : in.readString();
        this.analyzeTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.createTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.lastModifyTime = l;
    }

    private CapJobInstance(Integer id, String type, String status, String tasks, String result, String resultDesc, Long analyzeTime, Long createTime, Long lastModifyTime) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.tasks = tasks;
        this.result = result;
        this.resultDesc = resultDesc;
        this.analyzeTime = analyzeTime;
        this.createTime = createTime;
        this.lastModifyTime = lastModifyTime;
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

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
        setValue();
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
        setValue();
    }

    public String getTasks() {
        return this.tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
        setValue();
    }

    public String getResult() {
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
        setValue();
    }

    public String getResultDesc() {
        return this.resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
        setValue();
    }

    public Long getAnalyzeTime() {
        return this.analyzeTime;
    }

    public void setAnalyzeTime(Long analyzeTime) {
        this.analyzeTime = analyzeTime;
        setValue();
    }

    public Long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
        setValue();
    }

    public Long getLastModifyTime() {
        return this.lastModifyTime;
    }

    public void setLastModifyTime(Long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
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
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.status != null) {
            out.writeByte((byte) 1);
            out.writeString(this.status);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tasks != null) {
            out.writeByte((byte) 1);
            out.writeString(this.tasks);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.result != null) {
            out.writeByte((byte) 1);
            out.writeString(this.result);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.resultDesc != null) {
            out.writeByte((byte) 1);
            out.writeString(this.resultDesc);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.analyzeTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.analyzeTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.createTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.createTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.lastModifyTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.lastModifyTime.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<CapJobInstance> getHelper() {
        return CapJobInstanceHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.pengine.CapJobInstance";
    }

    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CapJobInstance { id: ").append(this.id);
        sb.append(", type: ").append(this.type);
        sb.append(", status: ").append(this.status);
        sb.append(", tasks: ").append(this.tasks);
        sb.append(", result: ").append(this.result);
        sb.append(", resultDesc: ").append(this.resultDesc);
        sb.append(", analyzeTime: ").append(this.analyzeTime);
        sb.append(", createTime: ").append(this.createTime);
        sb.append(", lastModifyTime: ").append(this.lastModifyTime);
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
        return "0.0.7";
    }

    public int getDatabaseVersionCode() {
        return 7;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
