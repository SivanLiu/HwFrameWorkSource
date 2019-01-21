package android.service.resolver;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

@SystemApi
public final class ResolverTarget implements Parcelable {
    public static final Creator<ResolverTarget> CREATOR = new Creator<ResolverTarget>() {
        public ResolverTarget createFromParcel(Parcel source) {
            return new ResolverTarget(source);
        }

        public ResolverTarget[] newArray(int size) {
            return new ResolverTarget[size];
        }
    };
    private static final String TAG = "ResolverTarget";
    private float mChooserScore;
    private float mLaunchScore;
    private float mRecencyScore;
    private float mSelectProbability;
    private float mTimeSpentScore;

    ResolverTarget(Parcel in) {
        this.mRecencyScore = in.readFloat();
        this.mTimeSpentScore = in.readFloat();
        this.mLaunchScore = in.readFloat();
        this.mChooserScore = in.readFloat();
        this.mSelectProbability = in.readFloat();
    }

    public float getRecencyScore() {
        return this.mRecencyScore;
    }

    public void setRecencyScore(float recencyScore) {
        this.mRecencyScore = recencyScore;
    }

    public float getTimeSpentScore() {
        return this.mTimeSpentScore;
    }

    public void setTimeSpentScore(float timeSpentScore) {
        this.mTimeSpentScore = timeSpentScore;
    }

    public float getLaunchScore() {
        return this.mLaunchScore;
    }

    public void setLaunchScore(float launchScore) {
        this.mLaunchScore = launchScore;
    }

    public float getChooserScore() {
        return this.mChooserScore;
    }

    public void setChooserScore(float chooserScore) {
        this.mChooserScore = chooserScore;
    }

    public float getSelectProbability() {
        return this.mSelectProbability;
    }

    public void setSelectProbability(float selectProbability) {
        this.mSelectProbability = selectProbability;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ResolverTarget{");
        stringBuilder.append(this.mRecencyScore);
        stringBuilder.append(", ");
        stringBuilder.append(this.mTimeSpentScore);
        stringBuilder.append(", ");
        stringBuilder.append(this.mLaunchScore);
        stringBuilder.append(", ");
        stringBuilder.append(this.mChooserScore);
        stringBuilder.append(", ");
        stringBuilder.append(this.mSelectProbability);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.mRecencyScore);
        dest.writeFloat(this.mTimeSpentScore);
        dest.writeFloat(this.mLaunchScore);
        dest.writeFloat(this.mChooserScore);
        dest.writeFloat(this.mSelectProbability);
    }
}
