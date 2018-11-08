package com.huawei.android.pushagent.utils.c;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class d {
    private Key u;
    private SecureRandom v = new SecureRandom();

    private d() {
    }

    public static d bi(byte[] bArr) {
        d dVar = new d();
        dVar.bo(bArr);
        return dVar;
    }

    public void bo(byte[] bArr) {
        if (bArr.length == 16) {
            this.u = new SecretKeySpec(bArr, "AES");
        }
    }

    public void bn(byte[] bArr) {
        this.v.nextBytes(bArr);
    }

    private byte[] bm(byte[] bArr, Key key, int i) {
        int i2 = 16;
        if (bArr == null || key == null) {
            return new byte[0];
        }
        byte[] copyOf;
        Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] bArr2 = new byte[16];
        if (i == 1) {
            bn(bArr2);
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

    public byte[] bj(byte[] bArr) {
        return bm(bArr, this.u, 1);
    }

    public byte[] bl(byte[] bArr) {
        return bm(bArr, this.u, 2);
    }

    public byte[] bk(byte[] bArr, byte[] bArr2) {
        if (bArr == null || this.u == null || bArr2 == null) {
            return new byte[0];
        }
        if (16 != bArr2.length) {
            return new byte[0];
        }
        Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
        instance.init(1, this.u, new IvParameterSpec(bArr2));
        byte[] doFinal = instance.doFinal(bArr);
        byte[] copyOf = Arrays.copyOf(bArr2, bArr2.length + doFinal.length);
        System.arraycopy(doFinal, 0, copyOf, bArr2.length, doFinal.length);
        return copyOf;
    }
}
