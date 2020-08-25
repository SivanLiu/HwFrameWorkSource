package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class NearAdvertiseOption implements Parcelable {
    public static final Creator<NearAdvertiseOption> CREATOR = new Creator<NearAdvertiseOption>() {
        /* class com.huawei.nearbysdk.softnet.NearAdvertiseOption.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearAdvertiseOption createFromParcel(Parcel in) {
            return new NearAdvertiseOption(in);
        }

        @Override // android.os.Parcelable.Creator
        public NearAdvertiseOption[] newArray(int size) {
            return new NearAdvertiseOption[size];
        }
    };
    /* access modifiers changed from: private */
    public int mCount;
    /* access modifiers changed from: private */
    public List<NearServiceDesc> mInfos;
    /* access modifiers changed from: private */
    public NearStrategy mStrategy;
    /* access modifiers changed from: private */
    public int mTimeout;

    protected NearAdvertiseOption(Parcel in) {
        this.mStrategy = (NearStrategy) in.readParcelable(NearStrategy.class.getClassLoader());
        if (this.mInfos == null) {
            this.mInfos = new ArrayList();
        }
        in.readList(this.mInfos, NearServiceDesc.class.getClassLoader());
        this.mTimeout = in.readInt();
        this.mCount = in.readInt();
    }

    private NearAdvertiseOption() {
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mStrategy, 0);
        dest.writeList(this.mInfos);
        dest.writeInt(this.mTimeout);
        dest.writeInt(this.mCount);
    }

    public List<NearServiceDesc> getInfos() {
        return this.mInfos;
    }

    public int getCount() {
        return this.mCount;
    }

    public NearStrategy getStrategy() {
        return this.mStrategy;
    }

    public int getTimeout() {
        return this.mTimeout;
    }

    public static class Builder {
        private NearAdvertiseOption option = new NearAdvertiseOption();

        public Builder strategy(NearStrategy strategy) {
            NearStrategy unused = this.option.mStrategy = strategy;
            return this;
        }

        public Builder infos(List<NearServiceDesc> infos) {
            List unused = this.option.mInfos = infos;
            return this;
        }

        public Builder timeout(int timeout) {
            int unused = this.option.mTimeout = timeout;
            return this;
        }

        public Builder count(int count) {
            int unused = this.option.mCount = count;
            return this;
        }

        public NearAdvertiseOption build() {
            return this.option;
        }
    }
}
