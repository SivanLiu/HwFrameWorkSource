package com.huawei.motionservice.common;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class HuaweiMotionEvent implements Parcelable {
    public static final Creator<HuaweiMotionEvent> CREATOR = new Creator<HuaweiMotionEvent>() {
        public HuaweiMotionEvent createFromParcel(Parcel source) {
            return new HuaweiMotionEvent(source);
        }

        public HuaweiMotionEvent[] newArray(int size) {
            return new HuaweiMotionEvent[size];
        }
    };
    private int mActivityRunSteps = 0;
    private int mActivityState = 0;
    private int mActivityTotalSteps = 0;
    private int mActivityWalkSteps = 0;
    private int mMotionDirection = 0;
    public Bundle mMotionExtras = null;
    private int mMotionRecoResult = 0;
    private int mMotionType = 0;

    public Bundle getMotionExtras() {
        return this.mMotionExtras;
    }

    public void setMotionExtras(Bundle Extras) {
        this.mMotionExtras = Extras;
    }

    public int getMotionType() {
        return this.mMotionType;
    }

    public void setMotionType(int motionType) {
        this.mMotionType = motionType;
    }

    public int getMotionRecoResult() {
        return this.mMotionRecoResult;
    }

    public void setMotionRecoResult(int motionRecoResult) {
        this.mMotionRecoResult = motionRecoResult;
    }

    public int getMotionDirection() {
        return this.mMotionDirection;
    }

    public void setMotionDirection(int motionDirect) {
        this.mMotionDirection = motionDirect;
    }

    public int getActivityState() {
        return this.mActivityState;
    }

    public void setActivityState(int activityState) {
        this.mActivityState = activityState;
    }

    public int getTotalSteps() {
        return this.mActivityTotalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.mActivityTotalSteps = totalSteps;
    }

    public int getActivityWalkSteps() {
        return this.mActivityWalkSteps;
    }

    public void setActivityWalkSteps(int walkSteps) {
        this.mActivityWalkSteps = walkSteps;
    }

    public int getActivityRunSteps() {
        return this.mActivityRunSteps;
    }

    public void setActivityRunSteps(int runSteps) {
        this.mActivityRunSteps = runSteps;
    }

    public HuaweiMotionEvent(Parcel in) {
        this.mMotionType = in.readInt();
        this.mMotionRecoResult = in.readInt();
        this.mMotionDirection = in.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMotionType);
        dest.writeInt(this.mMotionRecoResult);
        dest.writeInt(this.mMotionDirection);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HuaweiMotionEvent{mMotionExtras=");
        stringBuilder.append(this.mMotionExtras);
        stringBuilder.append(", mMotionType=");
        stringBuilder.append(this.mMotionType);
        stringBuilder.append(", mMotionRecoResult=");
        stringBuilder.append(this.mMotionRecoResult);
        stringBuilder.append(", mMotionDirection=");
        stringBuilder.append(this.mMotionDirection);
        stringBuilder.append(", mActivityState=");
        stringBuilder.append(this.mActivityState);
        stringBuilder.append(", mActivityTotalSteps=");
        stringBuilder.append(this.mActivityTotalSteps);
        stringBuilder.append(", mActivityWalkSteps=");
        stringBuilder.append(this.mActivityWalkSteps);
        stringBuilder.append(", mActivityRunSteps=");
        stringBuilder.append(this.mActivityRunSteps);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
