package tmsdk.common.module.network;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Date;
import tmsdkobf.ib;

public class NetworkInfoEntity extends ib implements Parcelable, Comparable<NetworkInfoEntity> {
    public static final Creator<NetworkInfoEntity> CREATOR = new Creator<NetworkInfoEntity>() {
        public NetworkInfoEntity[] aX(int i) {
            return new NetworkInfoEntity[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return j(parcel);
        }

        public NetworkInfoEntity j(Parcel parcel) {
            Object -l_2_R = new NetworkInfoEntity();
            -l_2_R.mTotalForMonth = parcel.readLong();
            -l_2_R.mUsedForMonth = parcel.readLong();
            -l_2_R.mUsedTranslateForMonth = parcel.readLong();
            -l_2_R.mUsedReceiveForMonth = parcel.readLong();
            -l_2_R.mRetialForMonth = parcel.readLong();
            -l_2_R.mUsedForDay = parcel.readLong();
            -l_2_R.mUsedTranslateForDay = parcel.readLong();
            -l_2_R.mUsedReceiveForDay = parcel.readLong();
            -l_2_R.mStartDate = (Date) parcel.readSerializable();
            return -l_2_R;
        }

        public /* synthetic */ Object[] newArray(int i) {
            return aX(i);
        }
    };
    public long mRetialForMonth = 0;
    public Date mStartDate = new Date();
    public long mTotalForMonth = 0;
    public long mUsedForDay = 0;
    public long mUsedForMonth = 0;
    public long mUsedReceiveForDay = 0;
    public long mUsedReceiveForMonth = 0;
    public long mUsedTranslateForDay = 0;
    public long mUsedTranslateForMonth = 0;

    public int compareTo(NetworkInfoEntity networkInfoEntity) {
        return this.mStartDate.compareTo(networkInfoEntity.mStartDate);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mTotalForMonth);
        parcel.writeLong(this.mUsedForMonth);
        parcel.writeLong(this.mUsedTranslateForMonth);
        parcel.writeLong(this.mUsedReceiveForMonth);
        parcel.writeLong(this.mRetialForMonth);
        parcel.writeLong(this.mUsedForDay);
        parcel.writeLong(this.mUsedTranslateForDay);
        parcel.writeLong(this.mUsedReceiveForDay);
        parcel.writeSerializable(this.mStartDate);
    }
}
