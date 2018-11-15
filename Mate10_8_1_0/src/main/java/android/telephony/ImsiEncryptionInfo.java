package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.security.keystore.KeyProperties;
import android.util.Log;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

public final class ImsiEncryptionInfo implements Parcelable {
    public static final Creator<ImsiEncryptionInfo> CREATOR = new Creator<ImsiEncryptionInfo>() {
        public ImsiEncryptionInfo createFromParcel(Parcel in) {
            return new ImsiEncryptionInfo(in);
        }

        public ImsiEncryptionInfo[] newArray(int size) {
            return new ImsiEncryptionInfo[size];
        }
    };
    private static final String LOG_TAG = "ImsiEncryptionInfo";
    private final Date expirationTime;
    private final String keyIdentifier;
    private final int keyType;
    private final String mcc;
    private final String mnc;
    private final PublicKey publicKey;

    public ImsiEncryptionInfo(String mcc, String mnc, int keyType, String keyIdentifier, byte[] key, Date expirationTime) {
        this(mcc, mnc, keyType, keyIdentifier, makeKeyObject(key), expirationTime);
    }

    public ImsiEncryptionInfo(String mcc, String mnc, int keyType, String keyIdentifier, PublicKey publicKey, Date expirationTime) {
        this.mcc = mcc;
        this.mnc = mnc;
        this.keyType = keyType;
        this.publicKey = publicKey;
        this.keyIdentifier = keyIdentifier;
        this.expirationTime = expirationTime;
    }

    public ImsiEncryptionInfo(Parcel in) {
        byte[] b = new byte[in.readInt()];
        in.readByteArray(b);
        this.publicKey = makeKeyObject(b);
        this.mcc = in.readString();
        this.mnc = in.readString();
        this.keyIdentifier = in.readString();
        this.keyType = in.readInt();
        this.expirationTime = new Date(in.readLong());
    }

    public String getMnc() {
        return this.mnc;
    }

    public String getMcc() {
        return this.mcc;
    }

    public String getKeyIdentifier() {
        return this.keyIdentifier;
    }

    public int getKeyType() {
        return this.keyType;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public Date getExpirationTime() {
        return this.expirationTime;
    }

    private static PublicKey makeKeyObject(byte[] publicKeyBytes) {
        try {
            return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA).generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (GeneralSecurityException ex) {
            Log.e(LOG_TAG, "Error makeKeyObject: unable to convert into PublicKey", ex);
            throw new IllegalArgumentException();
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        byte[] b = this.publicKey.getEncoded();
        dest.writeInt(b.length);
        dest.writeByteArray(b);
        dest.writeString(this.mcc);
        dest.writeString(this.mnc);
        dest.writeString(this.keyIdentifier);
        dest.writeInt(this.keyType);
        dest.writeLong(this.expirationTime.getTime());
    }

    public String toString() {
        return "[ImsiEncryptionInfo mcc=" + this.mcc + "mnc=" + this.mnc + "publicKey=" + this.publicKey + ", keyIdentifier=" + this.keyIdentifier + ", keyType=" + this.keyType + ", expirationTime=" + this.expirationTime + "]";
    }
}
