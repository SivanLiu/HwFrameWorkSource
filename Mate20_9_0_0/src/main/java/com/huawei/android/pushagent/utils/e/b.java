package com.huawei.android.pushagent.utils.e;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class b {
    private Key hq;
    private SecureRandom hr = new SecureRandom();

    private b() {
    }

    public static b wa(byte[] bArr) {
        b bVar = new b();
        bVar.wg(bArr);
        return bVar;
    }

    public void wg(byte[] bArr) {
        if (bArr.length == 16) {
            this.hq = new SecretKeySpec(bArr, "AES");
        }
    }

    public void wf(byte[] bArr) {
        this.hr.nextBytes(bArr);
    }

    private byte[] we(byte[] bArr, Key key, int i) {
        int i2 = 16;
        if (bArr == null || key == null) {
            return new byte[0];
        }
        byte[] copyOf;
        Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] bArr2 = new byte[16];
        if (i == 1) {
            wf(bArr2);
            i2 = 0;
        } else if (i != 2) {
            return new byte[0];
        } else {
            if (bArr.length <= 16) {
                return new byte[0];
            }
            for (int i3 = 0; i3 < 16; i3++) {
                bArr2[i3] = bArr[i3];
            }
        }
        instance.init(i, key, new IvParameterSpec(bArr2));
        byte[] doFinal = instance.doFinal(bArr, i2, bArr.length - i2);
        if (i == 1) {
            copyOf = Arrays.copyOf(bArr2, bArr2.length + doFinal.length);
            System.arraycopy(doFinal, 0, copyOf, bArr2.length, doFinal.length);
        } else {
            copyOf = doFinal;
        }
        return copyOf;
    }

    public byte[] wb(byte[] bArr) {
        return we(bArr, this.hq, 1);
    }

    public byte[] wd(byte[] bArr) {
        return we(bArr, this.hq, 2);
    }

    public byte[] wc(byte[] bArr, byte[] bArr2) {
        if (bArr == null || this.hq == null || bArr2 == null) {
            return new byte[0];
        }
        if (16 != bArr2.length) {
            return new byte[0];
        }
        Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
        instance.init(1, this.hq, new IvParameterSpec(bArr2));
        byte[] doFinal = instance.doFinal(bArr);
        byte[] copyOf = Arrays.copyOf(bArr2, bArr2.length + doFinal.length);
        System.arraycopy(doFinal, 0, copyOf, bArr2.length, doFinal.length);
        return copyOf;
    }
}
