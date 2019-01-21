package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.c;
import com.huawei.android.pushagent.utils.d;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PushDataRspMessage extends PushMessage {
    private static final long serialVersionUID = -8613876352739508266L;
    private byte control;
    private byte[] cookie;
    private byte[] msgId;
    private byte result;
    private byte[] tokenBytes;

    private static byte q() {
        return (byte) 69;
    }

    public PushDataRspMessage() {
        super(q());
    }

    public PushDataRspMessage(byte[] bArr, byte b, byte b2, byte[] bArr2, byte[] bArr3) {
        this();
        if (bArr == null) {
            this.msgId = new byte[0];
        } else {
            this.msgId = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.msgId, 0, bArr.length);
        }
        this.result = b;
        this.control = (byte) (b2 & 48);
        p(bArr2);
        if (bArr3 == null) {
            bArr3 = new byte[0];
        }
        this.tokenBytes = bArr3;
    }

    private void p(byte[] bArr) {
        if (bArr == null) {
            bArr = new byte[0];
        }
        if ((this.control & 32) == 32) {
            this.cookie = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.cookie, 0, bArr.length);
        }
    }

    public PushMessage a(InputStream inputStream) {
        return null;
    }

    public void m(byte b) {
        this.result = b;
    }

    public byte o() {
        return this.result;
    }

    public byte[] b() {
        if (this.msgId == null) {
            a.su("PushLog3414", "encode error, mMsgId is null ");
            return new byte[0];
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(c());
            int length = ((((this.msgId.length + 4) + 2) + this.tokenBytes.length) + 1) + 1;
            if ((this.control & 32) == 32) {
                length += this.cookie.length + 2;
            }
            byteArrayOutputStream.write(d.zr(length));
            byteArrayOutputStream.write(this.msgId.length);
            byteArrayOutputStream.write(this.msgId);
            byteArrayOutputStream.write(d.zr(this.tokenBytes.length));
            byteArrayOutputStream.write(this.tokenBytes);
            byteArrayOutputStream.write(this.result);
            byteArrayOutputStream.write(this.control);
            if ((this.control & 32) == 32) {
                byteArrayOutputStream.write(d.zr(this.cookie.length));
                byteArrayOutputStream.write(this.cookie);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            a.su("PushLog3414", "encode error " + e.toString());
            return new byte[0];
        }
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(",cmdId:").append(d()).append(",msgId:").append(c.tr(this.msgId)).append(",flag:").append(this.result).toString();
    }

    public byte[] n() {
        return this.msgId;
    }
}
