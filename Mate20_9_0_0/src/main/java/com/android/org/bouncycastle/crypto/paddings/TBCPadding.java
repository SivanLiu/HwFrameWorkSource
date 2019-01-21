package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import java.security.SecureRandom;

public class TBCPadding implements BlockCipherPadding {
    public void init(SecureRandom random) throws IllegalArgumentException {
    }

    public String getPaddingName() {
        return "TBC";
    }

    public int addPadding(byte[] in, int inOff) {
        byte code;
        int count = in.length - inOff;
        int code2 = 0;
        if (inOff > 0) {
            if ((in[inOff - 1] & 1) == 0) {
                code = (byte) -1;
            }
            code = (byte) code;
        } else {
            if ((in[in.length - 1] & 1) == 0) {
                code2 = 255;
            }
            code = (byte) code2;
        }
        while (inOff < in.length) {
            in[inOff] = code;
            inOff++;
        }
        return count;
    }

    public int padCount(byte[] in) throws InvalidCipherTextException {
        byte code = in[in.length - 1];
        int index = in.length - 1;
        while (index > 0 && in[index - 1] == code) {
            index--;
        }
        return in.length - index;
    }
}
