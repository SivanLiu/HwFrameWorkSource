package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class SearchHistory extends AManagedObject {
    public static final Creator<SearchHistory> CREATOR = new Creator<SearchHistory>() {
        public SearchHistory createFromParcel(Parcel in) {
            return new SearchHistory(in);
        }

        public SearchHistory[] newArray(int size) {
            return new SearchHistory[size];
        }
    };
    private Integer id;
    private String keyword;
    private String searchTime;

    public SearchHistory(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.keyword = cursor.getString(2);
        this.searchTime = cursor.getString(3);
    }

    public SearchHistory(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.keyword = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.searchTime = str;
    }

    private SearchHistory(Integer id, String keyword, String searchTime) {
        this.id = id;
        this.keyword = keyword;
        this.searchTime = searchTime;
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

    public String getKeyword() {
        return this.keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
        setValue();
    }

    public String getSearchTime() {
        return this.searchTime;
    }

    public void setSearchTime(String searchTime) {
        this.searchTime = searchTime;
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
        if (this.keyword != null) {
            out.writeByte((byte) 1);
            out.writeString(this.keyword);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.searchTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.searchTime);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<SearchHistory> getHelper() {
        return SearchHistoryHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.SearchHistory";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SearchHistory { id: ").append(this.id);
        sb.append(", keyword: ").append(this.keyword);
        sb.append(", searchTime: ").append(this.searchTime);
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
