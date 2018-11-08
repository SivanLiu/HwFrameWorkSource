package tmsdk.common.module.aresengine;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import tmsdkobf.ib;

public class MmsHeader extends ib implements Parcelable {
    public static final Creator<MmsHeader> CREATOR = new Creator<MmsHeader>() {
        public MmsHeader[] aQ(int i) {
            return new MmsHeader[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return e(parcel);
        }

        public MmsHeader e(Parcel parcel) {
            return new MmsHeader(parcel);
        }

        public /* synthetic */ Object[] newArray(int i) {
            return aQ(i);
        }
    };
    public int messageType;
    public byte[] messageclass;
    public int mmsVersion;
    public int phonenumCharset;
    public String subject;
    public int subjectCharset;
    public byte[] transactionId;

    public MmsHeader(Parcel parcel) {
        this.phonenumCharset = parcel.readInt();
        this.subject = parcel.readString();
        this.subjectCharset = parcel.readInt();
        this.messageclass = parcel.createByteArray();
        this.messageType = parcel.readInt();
        this.transactionId = parcel.createByteArray();
        this.mmsVersion = parcel.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.phonenumCharset);
        parcel.writeString(this.subject);
        parcel.writeInt(this.subjectCharset);
        parcel.writeByteArray(this.messageclass);
        parcel.writeInt(this.messageType);
        parcel.writeByteArray(this.transactionId);
        parcel.writeInt(this.mmsVersion);
    }
}
