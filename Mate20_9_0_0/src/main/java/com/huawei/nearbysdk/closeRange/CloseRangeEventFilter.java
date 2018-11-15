package com.huawei.nearbysdk.closeRange;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.huawei.nearbysdk.HwLog;
import java.util.ArrayList;
import java.util.List;

public class CloseRangeEventFilter implements Parcelable {
    public static final Creator<CloseRangeEventFilter> CREATOR = new Creator<CloseRangeEventFilter>() {
        public CloseRangeEventFilter createFromParcel(Parcel source) {
            CloseRangeBusinessType businessType = CloseRangeBusinessType.valueOf(source.readString());
            List<CloseRangeConfigInstance> configInstances = new ArrayList();
            source.readTypedList(configInstances, CloseRangeConfigInstance.CREATOR);
            return new CloseRangeEventFilter(businessType, configInstances, null);
        }

        public CloseRangeEventFilter[] newArray(int size) {
            return new CloseRangeEventFilter[size];
        }
    };
    private static final String TAG = "CloseRangeEventFilter";
    private CloseRangeBusinessType businessType;
    private List<CloseRangeConfigInstance> configInstances;

    /* synthetic */ CloseRangeEventFilter(CloseRangeBusinessType x0, List x1, AnonymousClass1 x2) {
        this(x0, x1);
    }

    private CloseRangeEventFilter(CloseRangeBusinessType businessType, List<CloseRangeConfigInstance> configInstances) {
        this.businessType = businessType;
        this.configInstances = new ArrayList();
        this.configInstances.addAll(configInstances);
    }

    public static CloseRangeEventFilter buildFilter(CloseRangeBusinessType businessType, List<CloseRangeConfigInstance> configInstances) {
        if (businessType != null && configInstances != null) {
            return new CloseRangeEventFilter(businessType, configInstances);
        }
        HwLog.e(TAG, "error when build filter");
        return null;
    }

    public CloseRangeBusinessType getBusinessType() {
        return this.businessType;
    }

    public List<CloseRangeConfigInstance> getConfigInstances() {
        return this.configInstances;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.businessType.name());
        dest.writeTypedList(this.configInstances);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CloseRangeEventFilter)) {
            return false;
        }
        return getBusinessType().equals(((CloseRangeEventFilter) o).getBusinessType());
    }

    public int hashCode() {
        return getBusinessType().getTag();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CloseRangeEventFilter{businessType=");
        stringBuilder.append(this.businessType);
        stringBuilder.append(", configInstances=");
        stringBuilder.append(this.configInstances);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
