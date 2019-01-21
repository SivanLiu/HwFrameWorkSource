package huawei.cust.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.List;

public class SimMatchRule implements Parcelable {
    public static final Creator<SimMatchRule> CREATOR = new Creator<SimMatchRule>() {
        public SimMatchRule createFromParcel(Parcel in) {
            return new SimMatchRule(in);
        }

        public SimMatchRule[] newArray(int size) {
            return new SimMatchRule[size];
        }
    };
    private int rule;
    List<SpecialFile> specialFiles = new ArrayList();

    protected SimMatchRule(Parcel in) {
        this.rule = in.readInt();
        this.specialFiles = in.createTypedArrayList(SpecialFile.CREATOR);
    }

    public int getRule() {
        return this.rule;
    }

    public void setRule(int rule) {
        this.rule = rule;
    }

    public List<SpecialFile> getSpecialFiles() {
        return this.specialFiles;
    }

    public void setSpecialFiles(List<SpecialFile> specialFiles) {
        this.specialFiles = specialFiles;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.rule);
        dest.writeTypedList(this.specialFiles);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SimMatchRule{rule=");
        stringBuilder.append(this.rule);
        stringBuilder.append(", specialFiles=");
        stringBuilder.append(this.specialFiles);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
