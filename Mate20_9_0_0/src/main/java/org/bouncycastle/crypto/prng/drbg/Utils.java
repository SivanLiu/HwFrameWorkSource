package org.bouncycastle.crypto.prng.drbg;

import java.util.Hashtable;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;
import org.bouncycastle.util.Integers;

class Utils {
    static final Hashtable maxSecurityStrengths = new Hashtable();

    static {
        maxSecurityStrengths.put(McElieceCCA2KeyGenParameterSpec.SHA1, Integers.valueOf(128));
        maxSecurityStrengths.put(McElieceCCA2KeyGenParameterSpec.SHA224, Integers.valueOf(192));
        maxSecurityStrengths.put(McElieceCCA2KeyGenParameterSpec.SHA256, Integers.valueOf(256));
        maxSecurityStrengths.put(McElieceCCA2KeyGenParameterSpec.SHA384, Integers.valueOf(256));
        maxSecurityStrengths.put(McElieceCCA2KeyGenParameterSpec.SHA512, Integers.valueOf(256));
        maxSecurityStrengths.put("SHA-512/224", Integers.valueOf(192));
        maxSecurityStrengths.put("SHA-512/256", Integers.valueOf(256));
    }

    Utils() {
    }

    static int getMaxSecurityStrength(Digest digest) {
        return ((Integer) maxSecurityStrengths.get(digest.getAlgorithmName())).intValue();
    }

    static int getMaxSecurityStrength(Mac mac) {
        String algorithmName = mac.getAlgorithmName();
        return ((Integer) maxSecurityStrengths.get(algorithmName.substring(0, algorithmName.indexOf("/")))).intValue();
    }

    static byte[] hash_df(Digest digest, byte[] bArr, int i) {
        Object obj = new byte[((i + 7) / 8)];
        int length = obj.length / digest.getDigestSize();
        Object obj2 = new byte[digest.getDigestSize()];
        int i2 = 0;
        int i3 = 1;
        for (int i4 = 0; i4 <= length; i4++) {
            digest.update((byte) i3);
            digest.update((byte) (i >> 24));
            digest.update((byte) (i >> 16));
            digest.update((byte) (i >> 8));
            digest.update((byte) i);
            digest.update(bArr, 0, bArr.length);
            digest.doFinal(obj2, 0);
            System.arraycopy(obj2, 0, obj, obj2.length * i4, obj.length - (obj2.length * i4) > obj2.length ? obj2.length : obj.length - (obj2.length * i4));
            i3++;
        }
        i %= 8;
        if (i != 0) {
            int i5 = 8 - i;
            int i6 = 0;
            while (i2 != obj.length) {
                i = obj[i2] & 255;
                obj[i2] = (byte) ((i6 << (8 - i5)) | (i >>> i5));
                i2++;
                i6 = i;
            }
        }
        return obj;
    }

    static boolean isTooLarge(byte[] bArr, int i) {
        return bArr != null && bArr.length > i;
    }
}
