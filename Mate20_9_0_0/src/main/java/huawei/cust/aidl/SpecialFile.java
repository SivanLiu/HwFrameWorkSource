package huawei.cust.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class SpecialFile implements Parcelable {
    public static final Creator<SpecialFile> CREATOR = new Creator<SpecialFile>() {
        public SpecialFile createFromParcel(Parcel in) {
            return new SpecialFile(in);
        }

        public SpecialFile[] newArray(int size) {
            return new SpecialFile[size];
        }
    };
    private String fileId;
    private String filePath;
    private String value;

    public SpecialFile(String filePath, String fileId, String value) {
        this.filePath = filePath;
        this.fileId = fileId;
        this.value = value;
    }

    protected SpecialFile(Parcel in) {
        this.filePath = in.readString();
        this.fileId = in.readString();
        this.value = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.filePath);
        dest.writeString(this.fileId);
        dest.writeString(this.value);
    }

    public String getFileId() {
        return this.fileId;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getValue() {
        return this.value;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SpecialFile{filePath='");
        stringBuilder.append(this.filePath);
        stringBuilder.append('\'');
        stringBuilder.append(", fileId='");
        stringBuilder.append(this.fileId);
        stringBuilder.append('\'');
        stringBuilder.append(", value='");
        stringBuilder.append(givePrintableMsg(this.value));
        stringBuilder.append('\'');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static String givePrintableMsg(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 6) {
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(value.substring(0, 6));
        stringBuilder.append("XXXXXXXXXXX");
        return stringBuilder.toString();
    }
}
