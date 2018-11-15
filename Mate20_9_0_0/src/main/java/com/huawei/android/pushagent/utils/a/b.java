package com.huawei.android.pushagent.utils.a;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class b {
    private Key a;
    private SecureRandom b = new SecureRandom();

    private b() {
    }

    public static b g(byte[] bArr) {
        b bVar = new b();
        bVar.i(bArr);
        return bVar;
    }

    public void i(byte[] bArr) {
        if (bArr.length == 16) {
            this.a = new SecretKeySpec(bArr, "AES");
        }
    }

    public void h(byte[] bArr) {
        this.b.nextBytes(bArr);
    }

    private byte[] c(byte[] bArr, Key key, int i) {
        int i2 = 16;
        if (bArr == null || key == null) {
            return new byte[0];
        }
        byte[] copyOf;
        Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] bArr2 = new byte[16];
        if (i == 1) {
            h(bArr2);
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

    public byte[] e(byte[] bArr) {
        return c(bArr, this.a, 1);
    }

    public byte[] d(byte[] bArr) {
        return c(bArr, this.a, 2);
    }

    public byte[] f(byte[] bArr, byte[] bArr2) {
        if (bArr == null || this.a == null || bArr2 == null) {
            return new byte[0];
        }
        if (16 != bArr2.length) {
            return new byte[0];
        }
        Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
        instance.init(1, this.a, new IvParameterSpec(bArr2));
        byte[] doFinal = instance.doFinal(bArr);
        byte[] copyOf = Arrays.copyOf(bArr2, bArr2.length + doFinal.length);
        System.arraycopy(doFinal, 0, copyOf, bArr2.length, doFinal.length);
        return copyOf;
    }
}
