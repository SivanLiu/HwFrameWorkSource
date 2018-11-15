package com.huawei.android.pushagent.utils;

import com.huawei.android.pushagent.utils.f.c;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public abstract class e {
    public static byte[] fn(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteArrayInputStream(bArr));
            byte[] bArr2 = new byte[256];
            while (true) {
                int read = gZIPInputStream.read(bArr2);
                if (read < 0) {
                    break;
                }
                byteArrayOutputStream.write(bArr2, 0, read);
            }
        } catch (IOException e) {
            c.eq("PushLog3413", "fail to gzip uncompress");
        }
        return byteArrayOutputStream.toByteArray();
    }
}
