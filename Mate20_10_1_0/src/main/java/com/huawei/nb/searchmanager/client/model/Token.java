package com.huawei.nb.searchmanager.client.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Token implements Parcelable {
    public static final Creator<Token> CREATOR = new Creator<Token>() {
        /* class com.huawei.nb.searchmanager.client.model.Token.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public Token createFromParcel(Parcel in) {
            return new Token(in);
        }

        @Override // android.os.Parcelable.Creator
        public Token[] newArray(int size) {
            return new Token[size];
        }
    };
    private boolean finish;
    private String tokenDescription;

    public Token() {
        this.finish = false;
        this.finish = false;
        this.tokenDescription = toString() + System.currentTimeMillis();
    }

    private Token(Parcel in) {
        boolean z = true;
        this.finish = false;
        this.finish = in.readInt() != 1 ? false : z;
        this.tokenDescription = in.readString();
    }

    public boolean isFinish() {
        return this.finish;
    }

    public void setFinish(boolean finish2) {
        this.finish = finish2;
    }

    public String getTokenDescription() {
        return this.tokenDescription;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.finish ? 1 : 0);
        parcel.writeString(this.tokenDescription);
    }

    public void readFromParcel(Parcel parcel) {
        boolean z = true;
        if (parcel.readInt() != 1) {
            z = false;
        }
        this.finish = z;
        this.tokenDescription = parcel.readString();
    }
}
