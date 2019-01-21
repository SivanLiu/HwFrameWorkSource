package com.huawei.android.pushagent.datatype.tcp.base;

import com.huawei.android.pushagent.datatype.a.d;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.c;
import com.huawei.android.pushagent.utils.e;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class PushMessage implements IPushMessage {
    protected byte mCmdId = (byte) -1;
    protected byte mSubCmdId = (byte) -1;

    public abstract PushMessage a(InputStream inputStream);

    public abstract byte[] b();

    public abstract String toString();

    public String d() {
        return c.tr(new byte[]{this.mCmdId});
    }

    public PushMessage(byte b) {
        j(b);
    }

    public byte c() {
        return this.mCmdId;
    }

    public void j(byte b) {
        this.mCmdId = b;
    }

    public byte g() {
        return this.mSubCmdId;
    }

    public void k(byte b) {
        this.mSubCmdId = b;
    }

    public d e(InputStream inputStream) {
        byte[] bArr = new byte[2];
        i(inputStream, bArr);
        int yg = com.huawei.android.pushagent.utils.d.yg(bArr);
        a.st("PushLog3414", "msg total lenth is: " + yg);
        bArr = new byte[((yg - 1) - 2)];
        i(inputStream, bArr);
        return new d(bArr);
    }

    public JSONObject f(byte b, d dVar) {
        if ((b & 2) == 2) {
            try {
                byte[] ax = dVar.ax(com.huawei.android.pushagent.utils.d.yg(dVar.ax(2)));
                if (ax == null || ax.length == 0) {
                    return null;
                }
                if ((b & 1) == 1) {
                    ax = e.zt(ax);
                }
                return new JSONObject(new String(ax, "UTF-8"));
            } catch (JSONException e) {
                a.su("PushLog3414", "fail to parser payload");
            } catch (UnsupportedEncodingException e2) {
                a.su("PushLog3414", "unsupported encoding type");
            } catch (ArrayIndexOutOfBoundsException e3) {
                a.su("PushLog3414", "fail to parser payload, array out of bounds");
            }
        }
        return null;
    }

    public boolean h(byte b) {
        boolean z = false;
        if ((b & 12) == 0) {
            z = true;
        }
        if (!z) {
            a.su("PushLog3414", "tlv is not support in current version");
        }
        return z;
    }

    public static void i(InputStream inputStream, byte[] bArr) {
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
