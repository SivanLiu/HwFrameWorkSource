package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class SearchAuthorityItem extends AManagedObject {
    public static final Creator<SearchAuthorityItem> CREATOR = new Creator<SearchAuthorityItem>() {
        public SearchAuthorityItem createFromParcel(Parcel in) {
            return new SearchAuthorityItem(in);
        }

        public SearchAuthorityItem[] newArray(int size) {
            return new SearchAuthorityItem[size];
        }
    };
    private String callingPkgName;
    private Integer id;
    private String pkgName;

    public SearchAuthorityItem(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.pkgName = cursor.getString(2);
        this.callingPkgName = cursor.getString(3);
    }

    public SearchAuthorityItem(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.pkgName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.callingPkgName = str;
    }

    private SearchAuthorityItem(Integer id, String pkgName, String callingPkgName) {
        this.id = id;
        this.pkgName = pkgName;
        this.callingPkgName = callingPkgName;
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

    public String getCallingPkgName() {
        return this.callingPkgName;
    }

    public void setCallingPkgName(String callingPkgName) {
        this.callingPkgName = callingPkgName;
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
        if (this.callingPkgName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.callingPkgName);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<SearchAuthorityItem> getHelper() {
        return SearchAuthorityItemHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.SearchAuthorityItem";
    }

    public String getDatabaseName() {
        return "dsSearch";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SearchAuthorityItem { id: ").append(this.id);
        sb.append(", pkgName: ").append(this.pkgName);
        sb.append(", callingPkgName: ").append(this.callingPkgName);
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
