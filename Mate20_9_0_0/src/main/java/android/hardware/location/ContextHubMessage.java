package android.hardware.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;

@SystemApi
@Deprecated
public class ContextHubMessage implements Parcelable {
    public static final Creator<ContextHubMessage> CREATOR = new Creator<ContextHubMessage>() {
        public ContextHubMessage createFromParcel(Parcel in) {
            return new ContextHubMessage(in, null);
        }

        public ContextHubMessage[] newArray(int size) {
            return new ContextHubMessage[size];
        }
    };
    private static final int DEBUG_LOG_NUM_BYTES = 16;
    private byte[] mData;
    private int mType;
    private int mVersion;

    /* synthetic */ ContextHubMessage(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public int getMsgType() {
        return this.mType;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public byte[] getData() {
        return Arrays.copyOf(this.mData, this.mData.length);
    }

    public void setMsgType(int msgType) {
        this.mType = msgType;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public void setMsgData(byte[] data) {
        this.mData = Arrays.copyOf(data, data.length);
    }

    public ContextHubMessage(int msgType, int version, byte[] data) {
        this.mType = msgType;
        this.mVersion = version;
        this.mData = Arrays.copyOf(data, data.length);
    }

    public int describeContents() {
        return 0;
    }

    private ContextHubMessage(Parcel in) {
        this.mType = in.readInt();
        this.mVersion = in.readInt();
        this.mData = new byte[in.readInt()];
        in.readByteArray(this.mData);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mType);
        out.writeInt(this.mVersion);
        out.writeInt(this.mData.length);
        out.writeByteArray(this.mData);
    }

    public String toString() {
        StringBuilder stringBuilder;
        int length = this.mData.length;
        String ret = new StringBuilder();
        ret.append("ContextHubMessage[type = ");
        ret.append(this.mType);
        ret.append(", length = ");
        ret.append(this.mData.length);
        ret.append(" bytes](");
        ret = ret.toString();
        if (length > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("data = 0x");
            ret = stringBuilder.toString();
        }
        for (int i = 0; i < Math.min(length, 16); i++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(ret);
            stringBuilder2.append(Byte.toHexString(this.mData[i], true));
            ret = stringBuilder2.toString();
            if ((i + 1) % 4 == 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(ret);
                stringBuilder2.append(" ");
                ret = stringBuilder2.toString();
            }
        }
        if (length > 16) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("...");
            ret = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(ret);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
