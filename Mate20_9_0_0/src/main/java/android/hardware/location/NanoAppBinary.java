package android.hardware.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@SystemApi
public final class NanoAppBinary implements Parcelable {
    public static final Creator<NanoAppBinary> CREATOR = new Creator<NanoAppBinary>() {
        public NanoAppBinary createFromParcel(Parcel in) {
            return new NanoAppBinary(in, null);
        }

        public NanoAppBinary[] newArray(int size) {
            return new NanoAppBinary[size];
        }
    };
    private static final int EXPECTED_HEADER_VERSION = 1;
    private static final int EXPECTED_MAGIC_VALUE = 1330528590;
    private static final ByteOrder HEADER_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final int HEADER_SIZE_BYTES = 40;
    private static final int NANOAPP_ENCRYPTED_FLAG_BIT = 2;
    private static final int NANOAPP_SIGNED_FLAG_BIT = 1;
    private static final String TAG = "NanoAppBinary";
    private int mFlags;
    private boolean mHasValidHeader;
    private int mHeaderVersion;
    private long mHwHubType;
    private int mMagic;
    private byte[] mNanoAppBinary;
    private long mNanoAppId;
    private int mNanoAppVersion;
    private byte mTargetChreApiMajorVersion;
    private byte mTargetChreApiMinorVersion;

    /* synthetic */ NanoAppBinary(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public NanoAppBinary(byte[] appBinary) {
        this.mHasValidHeader = false;
        this.mNanoAppBinary = appBinary;
        parseBinaryHeader();
    }

    private void parseBinaryHeader() {
        ByteBuffer buf = ByteBuffer.wrap(this.mNanoAppBinary).order(HEADER_ORDER);
        this.mHasValidHeader = false;
        try {
            this.mHeaderVersion = buf.getInt();
            if (this.mHeaderVersion != 1) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected header version ");
                stringBuilder.append(this.mHeaderVersion);
                stringBuilder.append(" while parsing header (expected ");
                stringBuilder.append(1);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString());
                return;
            }
            this.mMagic = buf.getInt();
            this.mNanoAppId = buf.getLong();
            this.mNanoAppVersion = buf.getInt();
            this.mFlags = buf.getInt();
            this.mHwHubType = buf.getLong();
            this.mTargetChreApiMajorVersion = buf.get();
            this.mTargetChreApiMinorVersion = buf.get();
            if (this.mMagic != EXPECTED_MAGIC_VALUE) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unexpected magic value ");
                stringBuilder2.append(String.format("0x%08X", new Object[]{Integer.valueOf(this.mMagic)}));
                stringBuilder2.append("while parsing header (expected ");
                stringBuilder2.append(String.format("0x%08X", new Object[]{Integer.valueOf(EXPECTED_MAGIC_VALUE)}));
                stringBuilder2.append(")");
                Log.e(str2, stringBuilder2.toString());
            } else {
                this.mHasValidHeader = true;
            }
        } catch (BufferUnderflowException e) {
            Log.e(TAG, "Not enough contents in nanoapp header");
        }
    }

    public byte[] getBinary() {
        return this.mNanoAppBinary;
    }

    public byte[] getBinaryNoHeader() {
        if (this.mNanoAppBinary.length >= 40) {
            return Arrays.copyOfRange(this.mNanoAppBinary, 40, this.mNanoAppBinary.length);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NanoAppBinary binary byte size (");
        stringBuilder.append(this.mNanoAppBinary.length);
        stringBuilder.append(") is less than header size (");
        stringBuilder.append(40);
        stringBuilder.append(")");
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    public boolean hasValidHeader() {
        return this.mHasValidHeader;
    }

    public int getHeaderVersion() {
        return this.mHeaderVersion;
    }

    public long getNanoAppId() {
        return this.mNanoAppId;
    }

    public int getNanoAppVersion() {
        return this.mNanoAppVersion;
    }

    public long getHwHubType() {
        return this.mHwHubType;
    }

    public byte getTargetChreApiMajorVersion() {
        return this.mTargetChreApiMajorVersion;
    }

    public byte getTargetChreApiMinorVersion() {
        return this.mTargetChreApiMinorVersion;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public boolean isSigned() {
        return (this.mFlags & 1) != 0;
    }

    public boolean isEncrypted() {
        return (this.mFlags & 2) != 0;
    }

    private NanoAppBinary(Parcel in) {
        this.mHasValidHeader = false;
        this.mNanoAppBinary = new byte[in.readInt()];
        in.readByteArray(this.mNanoAppBinary);
        parseBinaryHeader();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mNanoAppBinary.length);
        out.writeByteArray(this.mNanoAppBinary);
    }
}
