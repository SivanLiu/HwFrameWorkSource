package com.huawei.nb.query;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class QueryContainer implements Parcelable {
    public static final Creator<QueryContainer> CREATOR = new Creator<QueryContainer>() {
        public QueryContainer createFromParcel(Parcel in) {
            return new QueryContainer(in);
        }

        public QueryContainer[] newArray(int size) {
            return new QueryContainer[size];
        }
    };
    private String pkgName;
    private IQuery query;

    public QueryContainer(IQuery query, String pkgName) {
        this.query = query;
        this.pkgName = pkgName;
    }

    public IQuery getQuery() {
        return this.query;
    }

    public String getPkgName() {
        return this.pkgName;
    }

    protected QueryContainer(Parcel in) {
        String queryName = in.readString();
        if (Query.class.getName().equals(queryName)) {
            this.query = (IQuery) in.readParcelable(Query.class.getClassLoader());
        } else if (RawQuery.class.getName().equals(queryName)) {
            this.query = (IQuery) in.readParcelable(RawQuery.class.getClassLoader());
        } else if (RelationshipQuery.class.getName().equals(queryName)) {
            this.query = (IQuery) in.readParcelable(RelationshipQuery.class.getClassLoader());
        } else {
            this.query = null;
        }
        if (in.readInt() == 1) {
            this.pkgName = in.readString();
        } else {
            this.pkgName = null;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.query.getClass().getName());
        dest.writeParcelable(this.query, flags);
        if (this.pkgName != null) {
            dest.writeInt(1);
            dest.writeString(this.pkgName);
            return;
        }
        dest.writeInt(0);
    }
}
