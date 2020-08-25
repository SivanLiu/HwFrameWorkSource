package com.huawei.nb.query;

import android.os.Parcel;
import android.os.Parcelable;

public class QueryContainer implements Parcelable {
    public static final Creator<QueryContainer> CREATOR = new Creator<QueryContainer>() {
        /* class com.huawei.nb.query.QueryContainer.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public QueryContainer createFromParcel(Parcel in) {
            return new QueryContainer(in);
        }

        @Override // android.os.Parcelable.Creator
        public QueryContainer[] newArray(int size) {
            return new QueryContainer[size];
        }
    };
    private String pkgName;
    private IQuery query;

    public QueryContainer(IQuery query2, String pkgName2) {
        this.query = query2;
        this.pkgName = pkgName2;
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

    public IQuery getQuery() {
        return this.query;
    }

    public String getPkgName() {
        return this.pkgName;
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
