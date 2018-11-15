package com.huawei.nb.query;

import android.os.Parcel;
import android.os.Parcelable.Creator;

public class RawQuery implements IQuery {
    public static final Creator<RawQuery> CREATOR = new Creator<RawQuery>() {
        public RawQuery createFromParcel(Parcel in) {
            return new RawQuery(in);
        }

        public RawQuery[] newArray(int size) {
            return new RawQuery[size];
        }
    };
    private String dbName;
    private String rawSQL;

    private RawQuery() {
    }

    protected RawQuery(Parcel in) {
        this.dbName = in.readString();
        this.rawSQL = in.readString();
    }

    public static RawQuery select(String rawSQL) {
        RawQuery query = new RawQuery();
        query.rawSQL = rawSQL;
        return query;
    }

    public RawQuery from(String dbName) {
        this.dbName = dbName;
        return this;
    }

    public String getDbName() {
        return this.dbName;
    }

    public String getRawSQL() {
        return this.rawSQL;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.dbName);
        dest.writeString(this.rawSQL);
    }
}
