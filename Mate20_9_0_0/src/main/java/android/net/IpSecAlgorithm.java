package android.net;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public final class IpSecAlgorithm implements Parcelable {
    public static final String AUTH_CRYPT_AES_GCM = "rfc4106(gcm(aes))";
    public static final String AUTH_HMAC_MD5 = "hmac(md5)";
    public static final String AUTH_HMAC_SHA1 = "hmac(sha1)";
    public static final String AUTH_HMAC_SHA256 = "hmac(sha256)";
    public static final String AUTH_HMAC_SHA384 = "hmac(sha384)";
    public static final String AUTH_HMAC_SHA512 = "hmac(sha512)";
    public static final Creator<IpSecAlgorithm> CREATOR = new Creator<IpSecAlgorithm>() {
        public IpSecAlgorithm createFromParcel(Parcel in) {
            return new IpSecAlgorithm(in.readString(), in.createByteArray(), in.readInt());
        }

        public IpSecAlgorithm[] newArray(int size) {
            return new IpSecAlgorithm[size];
        }
    };
    public static final String CRYPT_AES_CBC = "cbc(aes)";
    public static final String CRYPT_NULL = "ecb(cipher_null)";
    private static final String TAG = "IpSecAlgorithm";
    private final byte[] mKey;
    private final String mName;
    private final int mTruncLenBits;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AlgorithmName {
    }

    public IpSecAlgorithm(String algorithm, byte[] key) {
        this(algorithm, key, 0);
    }

    public IpSecAlgorithm(String algorithm, byte[] key, int truncLenBits) {
        this.mName = algorithm;
        this.mKey = (byte[]) key.clone();
        this.mTruncLenBits = truncLenBits;
        checkValidOrThrow(this.mName, this.mKey.length * 8, this.mTruncLenBits);
    }

    public String getName() {
        return this.mName;
    }

    public byte[] getKey() {
        return (byte[]) this.mKey.clone();
    }

    public int getTruncationLengthBits() {
        return this.mTruncLenBits;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mName);
        out.writeByteArray(this.mKey);
        out.writeInt(this.mTruncLenBits);
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void checkValidOrThrow(String name, int keyLen, int truncLen) {
        Object obj;
        boolean isValidLen;
        StringBuilder stringBuilder;
        boolean isValidTruncLen = true;
        boolean z = false;
        switch (name.hashCode()) {
            case -1137603038:
                if (name.equals(AUTH_CRYPT_AES_GCM)) {
                    obj = 6;
                    break;
                }
            case 394796030:
                if (name.equals(CRYPT_AES_CBC)) {
                    obj = null;
                    break;
                }
            case 559425185:
                if (name.equals(AUTH_HMAC_SHA256)) {
                    obj = 3;
                    break;
                }
            case 559457797:
                if (name.equals(AUTH_HMAC_SHA384)) {
                    obj = 4;
                    break;
                }
            case 559510590:
                if (name.equals(AUTH_HMAC_SHA512)) {
                    obj = 5;
                    break;
                }
            case 759177996:
                if (name.equals(AUTH_HMAC_MD5)) {
                    obj = 1;
                    break;
                }
            case 2065384259:
                if (name.equals(AUTH_HMAC_SHA1)) {
                    obj = 2;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
                if (keyLen == 128 || keyLen == 192 || keyLen == 256) {
                    z = true;
                }
                isValidLen = z;
                break;
            case 1:
                isValidLen = keyLen == 128;
                if (truncLen >= 96 && truncLen <= 128) {
                    z = true;
                }
                isValidTruncLen = z;
                break;
            case 2:
                isValidLen = keyLen == 160;
                if (truncLen >= 96 && truncLen <= 160) {
                    z = true;
                }
                isValidTruncLen = z;
                break;
            case 3:
                isValidLen = keyLen == 256;
                if (truncLen >= 96 && truncLen <= 256) {
                    z = true;
                }
                isValidTruncLen = z;
                break;
            case 4:
                isValidLen = keyLen == 384;
                if (truncLen >= 192 && truncLen <= 384) {
                    z = true;
                }
                isValidTruncLen = z;
                break;
            case 5:
                isValidLen = keyLen == 512;
                if (truncLen >= 256 && truncLen <= 512) {
                    z = true;
                }
                isValidTruncLen = z;
                break;
            case 6:
                boolean z2 = keyLen == 160 || keyLen == 224 || keyLen == R.styleable.Theme_dialogCustomTitleDecorLayout;
                isValidLen = z2;
                if (truncLen == 64 || truncLen == 96 || truncLen == 128) {
                    z = true;
                }
                isValidTruncLen = z;
                break;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't find an algorithm: ");
                stringBuilder.append(name);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
        if (!isValidLen) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid key material keyLength: ");
            stringBuilder.append(keyLen);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!isValidTruncLen) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid truncation keyLength: ");
            stringBuilder.append(truncLen);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isAuthentication() {
        boolean z;
        String name = getName();
        switch (name.hashCode()) {
            case 559425185:
                if (name.equals(AUTH_HMAC_SHA256)) {
                    z = true;
                    break;
                }
            case 559457797:
                if (name.equals(AUTH_HMAC_SHA384)) {
                    z = true;
                    break;
                }
            case 559510590:
                if (name.equals(AUTH_HMAC_SHA512)) {
                    z = true;
                    break;
                }
            case 759177996:
                if (name.equals(AUTH_HMAC_MD5)) {
                    z = false;
                    break;
                }
            case 2065384259:
                if (name.equals(AUTH_HMAC_SHA1)) {
                    z = true;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
            case true:
            case true:
            case true:
            case true:
                return true;
            default:
                return false;
        }
    }

    public boolean isEncryption() {
        return getName().equals(CRYPT_AES_CBC);
    }

    public boolean isAead() {
        return getName().equals(AUTH_CRYPT_AES_GCM);
    }

    private static boolean isUnsafeBuild() {
        return Build.IS_DEBUGGABLE && Build.IS_ENG;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{mName=");
        stringBuilder.append(this.mName);
        stringBuilder.append(", mKey=");
        stringBuilder.append(isUnsafeBuild() ? HexDump.toHexString(this.mKey) : "<hidden>");
        stringBuilder.append(", mTruncLenBits=");
        stringBuilder.append(this.mTruncLenBits);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    @VisibleForTesting
    public static boolean equals(IpSecAlgorithm lhs, IpSecAlgorithm rhs) {
        boolean z = false;
        if (lhs == null || rhs == null) {
            if (lhs == rhs) {
                z = true;
            }
            return z;
        }
        if (lhs.mName.equals(rhs.mName) && Arrays.equals(lhs.mKey, rhs.mKey) && lhs.mTruncLenBits == rhs.mTruncLenBits) {
            z = true;
        }
        return z;
    }
}
