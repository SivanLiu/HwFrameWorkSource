package com.huawei.server.security.behaviorcollect.bean;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

public class OnceData implements Parcelable {
    public static final Creator<OnceData> CREATOR = new Creator<OnceData>() {
        /* class com.huawei.server.security.behaviorcollect.bean.OnceData.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public OnceData createFromParcel(Parcel source) {
            return new OnceData(source);
        }

        @Override // android.os.Parcelable.Creator
        public OnceData[] newArray(int size) {
            return new OnceData[size];
        }
    };
    private boolean isActiveTouched;
    private ArrayList<SensorData> sensorDatas;
    private ArrayList<TouchPoint> touchPoints;

    public OnceData(boolean isActiveTouched2, ArrayList<SensorData> sensorDatas2, ArrayList<TouchPoint> touchPoints2) {
        this.isActiveTouched = isActiveTouched2;
        Object sensorDataObj = sensorDatas2.clone();
        if (sensorDataObj instanceof ArrayList) {
            this.sensorDatas = (ArrayList) sensorDataObj;
        }
        Object touchDataObj = touchPoints2.clone();
        if (touchDataObj instanceof ArrayList) {
            this.touchPoints = (ArrayList) touchDataObj;
        }
    }

    public OnceData(Parcel source) {
        this.isActiveTouched = source.readBoolean();
        this.sensorDatas = source.readArrayList(SensorData.class.getClassLoader());
        this.touchPoints = source.readArrayList(TouchPoint.class.getClassLoader());
    }

    public boolean isActiveTouched() {
        return this.isActiveTouched;
    }

    public ArrayList<SensorData> getSensorDatas() {
        return this.sensorDatas;
    }

    public ArrayList<TouchPoint> getTouchPoints() {
        return this.touchPoints;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.isActiveTouched);
        dest.writeList(this.sensorDatas);
        dest.writeList(this.touchPoints);
    }
}
