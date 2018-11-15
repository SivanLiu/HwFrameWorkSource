package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class Parameter extends AManagedObject {
    public static final Creator<Parameter> CREATOR = new Creator<Parameter>() {
        public Parameter createFromParcel(Parcel in) {
            return new Parameter(in);
        }

        public Parameter[] newArray(int size) {
            return new Parameter[size];
        }
    };
    private Integer id;
    private String module;
    private String name;
    private String value;

    public Parameter(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.module = cursor.getString(2);
        this.name = cursor.getString(3);
        this.value = cursor.getString(4);
    }

    public Parameter(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.module = in.readByte() == (byte) 0 ? null : in.readString();
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.value = str;
    }

    private Parameter(Integer id, String module, String name, String value) {
        this.id = id;
        this.module = module;
        this.name = name;
        this.value = value;
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

    public String getModule() {
        return this.module;
    }

    public void setModule(String module) {
        this.module = module;
        setValue();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        setValue();
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
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
        if (this.module != null) {
            out.writeByte((byte) 1);
            out.writeString(this.module);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.value != null) {
            out.writeByte((byte) 1);
            out.writeString(this.value);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<Parameter> getHelper() {
        return ParameterHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.pengine.Parameter";
    }

    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Parameter { id: ").append(this.id);
        sb.append(", module: ").append(this.module);
        sb.append(", name: ").append(this.name);
        sb.append(", value: ").append(this.value);
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
