package com.huawei.android.pushagent.datatype.tcp;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.a.d;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.a.f;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import org.json.JSONObject;

public class PushDataReqMessage extends PushMessage {
    private static final long serialVersionUID = -6661677033729532404L;
    private byte control;
    private byte[] mCookie;
    private byte[] msgId;
    private JSONObject payload;
    private byte[] tokenBytes;

    public static final byte jy() {
        return (byte) 68;
    }

    public PushDataReqMessage() {
        super(jy());
    }

    public byte[] jw() {
        if (this.mCookie == null || this.mCookie.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.mCookie.length];
        System.arraycopy(this.mCookie, 0, bArr, 0, this.mCookie.length);
        return bArr;
    }

    public byte jv() {
        return this.control;
    }

    public byte[] ju() {
        if (this.msgId == null || this.msgId.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.msgId.length];
        System.arraycopy(this.msgId, 0, bArr, 0, this.msgId.length);
        return bArr;
    }

    public byte[] is() {
        return new byte[0];
    }

    public boolean isValid() {
        boolean z;
        CharSequence pkgName = getPkgName();
        if (this.tokenBytes == null || this.tokenBytes.length <= 0) {
            z = false;
        } else {
            z = true;
        }
        int i;
        if (this.msgId == null || this.msgId.length <= 0) {
            i = 0;
        } else {
            i = 1;
        }
        if (z && !TextUtils.isEmpty(pkgName) && (i ^ 1) == 0 && jt().length != 0) {
            return true;
        }
        c.eq("PushLog3413", "token pkgName msgId msgBody exist null");
        return false;
    }

    public PushMessage jc(InputStream inputStream) {
        d iy = iy(inputStream);
        this.msgId = iy.kp(g.gy(iy.kp(1)[0]));
        this.tokenBytes = iy.kp(g.gx(iy.kp(2)));
        this.control = iy.kp(1)[0];
        if (!iz(this.control)) {
            return null;
        }
        c.ep("PushLog3413", "control is: " + this.control);
        if ((this.control & 32) == 32) {
            int gx = g.gx(iy.kp(2));
            c.ep("PushLog3413", "cookieLen is: " + gx);
            this.mCookie = iy.kp(gx);
        }
        this.payload = ja(this.control, iy);
        return this;
    }

    public int getUserId() {
        if (this.payload == null) {
            c.eq("PushLog3413", "payload is null");
            return 0;
        } else if (this.payload.has("userId")) {
            try {
                return this.payload.getInt("userId");
            } catch (JSONException e) {
                c.eq("PushLog3413", "fail to get userid from payload");
            }
        } else {
            c.eq("PushLog3413", "payload not has userid");
            return 0;
        }
    }

    public String getPkgName() {
        if (this.payload == null) {
            c.eq("PushLog3413", "payload is null");
            return null;
        } else if (this.payload.has("pkgName")) {
            try {
                return this.payload.getString("pkgName");
            } catch (JSONException e) {
                c.eq("PushLog3413", "fail to get pkgname from payload");
            }
        } else {
            c.eq("PushLog3413", "payload not has pkgname");
            return null;
        }
    }

    public byte[] js() {
        if (this.tokenBytes == null || this.tokenBytes.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.tokenBytes.length];
        System.arraycopy(this.tokenBytes, 0, bArr, 0, this.tokenBytes.length);
        return bArr;
    }

    public int jx() {
        if (this.payload == null) {
            c.eq("PushLog3413", "payload is null");
            return -1;
        } else if (this.payload.has("msgType")) {
            try {
                return this.payload.getInt("msgType");
            } catch (JSONException e) {
                c.eq("PushLog3413", "fail to get msgType from payload");
            }
        } else {
            c.eq("PushLog3413", "payload not has msgType");
            return -1;
        }
    }

    public byte[] jt() {
        if (this.payload == null) {
            c.eq("PushLog3413", "payload is null");
            return new byte[0];
        }
        if (this.payload.has("msg")) {
            try {
                String string = this.payload.getString("msg");
                if (string != null) {
                    return string.getBytes("UTF-8");
                }
            } catch (JSONException e) {
                c.eq("PushLog3413", "fail to get msg from payload");
            } catch (UnsupportedEncodingException e2) {
                c.eq("PushLog3413", "fail to get msg from payload, unsupport encoding type");
            }
        } else {
            c.eq("PushLog3413", "payload not has msg");
        }
        return new byte[0];
    }

    public String toString() {
        return "payload is" + f.u(this.payload);
    }
}
