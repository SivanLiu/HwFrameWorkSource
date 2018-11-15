package android.telephony;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import com.android.internal.telephony.uicc.IccUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class UiccAccessRule implements Parcelable {
    public static final Creator<UiccAccessRule> CREATOR = new Creator<UiccAccessRule>() {
        public UiccAccessRule createFromParcel(Parcel in) {
            return new UiccAccessRule(in);
        }

        public UiccAccessRule[] newArray(int size) {
            return new UiccAccessRule[size];
        }
    };
    private static final int ENCODING_VERSION = 1;
    private static final String TAG = "UiccAccessRule";
    private final long mAccessType;
    private final byte[] mCertificateHash;
    private final String mPackageName;

    public static byte[] encodeRules(UiccAccessRule[] accessRules) {
        if (accessRules == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(baos);
            output.writeInt(1);
            output.writeInt(accessRules.length);
            for (UiccAccessRule accessRule : accessRules) {
                output.writeInt(accessRule.mCertificateHash.length);
                output.write(accessRule.mCertificateHash);
                if (accessRule.mPackageName != null) {
                    output.writeBoolean(true);
                    output.writeUTF(accessRule.mPackageName);
                } else {
                    output.writeBoolean(false);
                }
                output.writeLong(accessRule.mAccessType);
            }
            output.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("ByteArrayOutputStream should never lead to an IOException", e);
        }
    }

    public static UiccAccessRule[] decodeRules(byte[] encodedRules) {
        IOException e;
        Throwable th;
        if (encodedRules == null) {
            return null;
        }
        Throwable th2 = null;
        DataInputStream dataInputStream = null;
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(encodedRules));
            try {
                input.readInt();
                int count = input.readInt();
                UiccAccessRule[] accessRules = new UiccAccessRule[count];
                for (int i = 0; i < count; i++) {
                    byte[] certificateHash = new byte[input.readInt()];
                    input.readFully(certificateHash);
                    accessRules[i] = new UiccAccessRule(certificateHash, input.readBoolean() ? input.readUTF() : null, input.readLong());
                }
                input.close();
                if (input != null) {
                    try {
                        input.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return accessRules;
                }
                try {
                    throw th2;
                } catch (IOException e2) {
                    e = e2;
                    dataInputStream = input;
                }
            } catch (Throwable th4) {
                th = th4;
                dataInputStream = input;
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    try {
                        throw th2;
                    } catch (IOException e3) {
                        e = e3;
                        throw new IllegalStateException("ByteArrayInputStream should never lead to an IOException", e);
                    }
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
    }

    public UiccAccessRule(byte[] certificateHash, String packageName, long accessType) {
        this.mCertificateHash = certificateHash;
        this.mPackageName = packageName;
        this.mAccessType = accessType;
    }

    UiccAccessRule(Parcel in) {
        this.mCertificateHash = in.createByteArray();
        this.mPackageName = in.readString();
        this.mAccessType = in.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.mCertificateHash);
        dest.writeString(this.mPackageName);
        dest.writeLong(this.mAccessType);
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
            throw new IllegalArgumentException("Must use GET_SIGNATURES when looking up package info");
        }
        for (Signature sig : packageInfo.signatures) {
            int accessStatus = getCarrierPrivilegeStatus(sig, packageInfo.packageName);
            if (accessStatus != 0) {
                return accessStatus;
            }
        }
        return 0;
    }

    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        byte[] certHash = getCertHash(signature, KeyProperties.DIGEST_SHA1);
        byte[] certHash256 = getCertHash(signature, KeyProperties.DIGEST_SHA256);
        if (matches(certHash, packageName) || matches(certHash256, packageName)) {
            return 1;
        }
        return 0;
    }

    private boolean matches(byte[] certHash, String packageName) {
        if (certHash == null || !Arrays.equals(this.mCertificateHash, certHash)) {
            return false;
        }
        return !TextUtils.isEmpty(this.mPackageName) ? this.mPackageName.equals(packageName) : true;
    }

    public String toString() {
        return "cert: " + IccUtils.bytesToHexString(this.mCertificateHash) + " pkg: " + this.mPackageName + " access: " + this.mAccessType;
    }

    public int describeContents() {
        return 0;
    }

    private static byte[] getCertHash(Signature signature, String algo) {
        try {
            return MessageDigest.getInstance(algo).digest(signature.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            Rlog.e(TAG, "NoSuchAlgorithmException: " + ex);
            return null;
        }
    }
}
