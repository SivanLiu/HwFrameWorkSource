package com.huawei.android.pushagent.datatype.tcp.base;

import com.huawei.android.pushagent.datatype.a.d;
import com.huawei.android.pushagent.utils.e;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class PushMessage implements IPushMessage {
    protected byte mCmdId = (byte) -1;
    protected byte mSubCmdId = (byte) -1;

    public abstract byte[] is();

    public abstract PushMessage jc(InputStream inputStream);

    public abstract String toString();

    public String iw() {
        return b.el(new byte[]{this.mCmdId});
    }

    public PushMessage(byte b) {
        jb(b);
    }

    public byte it() {
        return this.mCmdId;
    }

    public void jb(byte b) {
        this.mCmdId = b;
    }

    public byte iu() {
        return this.mSubCmdId;
    }

    public void ix(byte b) {
        this.mSubCmdId = b;
    }

    public d iy(InputStream inputStream) {
        byte[] bArr = new byte[2];
        jd(inputStream, bArr);
        int gx = g.gx(bArr);
        c.er("PushLog3413", "msg total lenth is: " + gx);
        bArr = new byte[((gx - 1) - 2)];
        jd(inputStream, bArr);
        return new d(bArr);
    }

    public JSONObject ja(byte b, d dVar) {
        if ((b & 2) == 2) {
            try {
                byte[] kp = dVar.kp(g.gx(dVar.kp(2)));
                if (kp == null || kp.length == 0) {
                    return null;
                }
                if ((b & 1) == 1) {
                    kp = e.fn(kp);
                }
                return new JSONObject(new String(kp, "UTF-8"));
            } catch (JSONException e) {
                c.eq("PushLog3413", "fail to parser payload");
            } catch (UnsupportedEncodingException e2) {
                c.eq("PushLog3413", "unsupported encoding type");
            } catch (ArrayIndexOutOfBoundsException e3) {
                c.eq("PushLog3413", "fail to parser payload, array out of bounds");
            }
        }
        return null;
    }

    public boolean iz(byte b) {
        boolean z = false;
        if ((b & 12) == 0) {
            z = true;
        }
        if (!z) {
            c.eq("PushLog3413", "tlv is not support in current version");
        }
        return z;
    }

    public static void jd(InputStream inputStream, byte[] bArr) {
        int i = 0;
        while (i < bArr.length) {
            int read = inputStream.read(bArr, i, bArr.length - i);
            if (-1 == read) {
                throw new IOException("read -1 reached");
            }
            i += read;
        }
    }
}
