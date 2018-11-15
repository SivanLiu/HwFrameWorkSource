package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CapTaskInstance extends AManagedObject {
    public static final Creator<CapTaskInstance> CREATOR = new Creator<CapTaskInstance>() {
        public CapTaskInstance createFromParcel(Parcel in) {
            return new CapTaskInstance(in);
        }

        public CapTaskInstance[] newArray(int size) {
            return new CapTaskInstance[size];
        }
    };
    private String attrs;
    private Long createTime;
    private Long jobId;
    private Long lastModifyTime;
    private String result;
    private String resultDesc;
    private String status;
    private Integer taskId;
    private String taskName;

    public CapTaskInstance(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.taskId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.jobId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.taskName = cursor.getString(3);
        this.status = cursor.getString(4);
        this.result = cursor.getString(5);
        this.resultDesc = cursor.getString(6);
        this.createTime = cursor.isNull(7) ? null : Long.valueOf(cursor.getLong(7));
        if (!cursor.isNull(8)) {
            l = Long.valueOf(cursor.getLong(8));
        }
        this.lastModifyTime = l;
        this.attrs = cursor.getString(9);
    }

    public CapTaskInstance(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.taskId = null;
            in.readInt();
        } else {
            this.taskId = Integer.valueOf(in.readInt());
        }
        this.jobId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.taskName = in.readByte() == (byte) 0 ? null : in.readString();
        this.status = in.readByte() == (byte) 0 ? null : in.readString();
        this.result = in.readByte() == (byte) 0 ? null : in.readString();
        this.resultDesc = in.readByte() == (byte) 0 ? null : in.readString();
        this.createTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.lastModifyTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.attrs = str;
    }

    private CapTaskInstance(Integer taskId, Long jobId, String taskName, String status, String result, String resultDesc, Long createTime, Long lastModifyTime, String attrs) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.taskName = taskName;
        this.status = status;
        this.result = result;
        this.resultDesc = resultDesc;
        this.createTime = createTime;
        this.lastModifyTime = lastModifyTime;
        this.attrs = attrs;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getTaskId() {
        return this.taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
        setValue();
    }

    public Long getJobId() {
        return this.jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
        setValue();
    }

    public String getTaskName() {
        return this.taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
        setValue();
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getAttrs() {
        return this.attrs;
    }

    public void setAttrs(String attrs) {
        this.attrs = attrs;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.taskId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.taskId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.jobId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.jobId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.taskName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.taskName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.status != null) {
            out.writeByte((byte) 1);
            out.writeString(this.status);
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
        if (this.createTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.createTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.lastModifyTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.lastModifyTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.attrs != null) {
            out.writeByte((byte) 1);
            out.writeString(this.attrs);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<CapTaskInstance> getHelper() {
        return CapTaskInstanceHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.pengine.CapTaskInstance";
    }

    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CapTaskInstance { taskId: ").append(this.taskId);
        sb.append(", jobId: ").append(this.jobId);
        sb.append(", taskName: ").append(this.taskName);
        sb.append(", status: ").append(this.status);
        sb.append(", result: ").append(this.result);
        sb.append(", resultDesc: ").append(this.resultDesc);
        sb.append(", createTime: ").append(this.createTime);
        sb.append(", lastModifyTime: ").append(this.lastModifyTime);
        sb.append(", attrs: ").append(this.attrs);
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
