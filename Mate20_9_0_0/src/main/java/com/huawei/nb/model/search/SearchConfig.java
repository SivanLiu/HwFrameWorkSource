package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class SearchConfig extends AManagedObject {
    public static final Creator<SearchConfig> CREATOR = new Creator<SearchConfig>() {
        public SearchConfig createFromParcel(Parcel in) {
            return new SearchConfig(in);
        }

        public SearchConfig[] newArray(int size) {
            return new SearchConfig[size];
        }
    };
    private Integer id;
    private String name;
    private String value;

    public SearchConfig(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.name = cursor.getString(2);
        this.value = cursor.getString(3);
    }

    public SearchConfig(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.value = str;
    }

    private SearchConfig(Integer id, String name, String value) {
        this.id = id;
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

    public AEntityHelper<SearchConfig> getHelper() {
        return SearchConfigHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.SearchConfig";
    }

    public String getDatabaseName() {
        return "dsSearch";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SearchConfig { id: ").append(this.id);
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
