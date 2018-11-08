package tmsdk.common.module.network;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;

public final class TrafficEntity implements Parcelable {
    public static Creator<TrafficEntity> CREATOR = new Creator<TrafficEntity>() {
        public TrafficEntity[] aY(int i) {
            return new TrafficEntity[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return k(parcel);
        }

        public TrafficEntity k(Parcel parcel) {
            Object -l_2_R = new TrafficEntity();
            -l_2_R.mPkg = parcel.readString();
            -l_2_R.mLastUpValue = parcel.readLong();
            -l_2_R.mLastDownValue = parcel.readLong();
            -l_2_R.mMobileUpValue = parcel.readLong();
            -l_2_R.mMobileDownValue = parcel.readLong();
            -l_2_R.mWIFIUpValue = parcel.readLong();
            -l_2_R.mWIFIDownValue = parcel.readLong();
            return -l_2_R;
        }

        public /* synthetic */ Object[] newArray(int i) {
            return aY(i);
        }
    };
    public long mLastDownValue = 0;
    public long mLastUpValue = 0;
    public long mMobileDownValue = 0;
    public long mMobileUpValue = 0;
    public String mPkg;
    public long mWIFIDownValue = 0;
    public long mWIFIUpValue = 0;

    public static TrafficEntity fromString(String str) {
        Object -l_1_R = null;
        if (!TextUtils.isEmpty(str)) {
            -l_1_R = new TrafficEntity();
            Object -l_2_R = str.trim().split("[,:]");
            try {
                -l_1_R.mPkg = -l_2_R[0];
                -l_1_R.mLastUpValue = Long.parseLong(-l_2_R[1]);
                -l_1_R.mLastDownValue = Long.parseLong(-l_2_R[2]);
                -l_1_R.mMobileUpValue = Long.parseLong(-l_2_R[3]);
                -l_1_R.mMobileDownValue = Long.parseLong(-l_2_R[4]);
                -l_1_R.mWIFIUpValue = Long.parseLong(-l_2_R[5]);
                -l_1_R.mWIFIDownValue = Long.parseLong(-l_2_R[6]);
            } catch (NumberFormatException e) {
                return null;
            } catch (ArrayIndexOutOfBoundsException e2) {
                return null;
            } catch (Exception e3) {
                return null;
            }
        }
        return -l_1_R;
    }

    public static String toString(TrafficEntity trafficEntity) {
        return String.format("%s,%s,%s,%s,%s,%s,%s", new Object[]{trafficEntity.mPkg, Long.valueOf(trafficEntity.mLastUpValue), Long.valueOf(trafficEntity.mLastDownValue), Long.valueOf(trafficEntity.mMobileUpValue), Long.valueOf(trafficEntity.mMobileDownValue), Long.valueOf(trafficEntity.mWIFIUpValue), Long.valueOf(trafficEntity.mWIFIDownValue)});
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        return toString(this);
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPkg);
        parcel.writeLong(this.mLastUpValue);
        parcel.writeLong(this.mLastDownValue);
        parcel.writeLong(this.mMobileUpValue);
        parcel.writeLong(this.mMobileDownValue);
        parcel.writeLong(this.mWIFIUpValue);
        parcel.writeLong(this.mWIFIDownValue);
    }
}
