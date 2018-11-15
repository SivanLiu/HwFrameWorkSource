package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.a.d;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.a.f;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONObject;

public class DecoupledPushMessage extends PushMessage {
    private static final long serialVersionUID = -1186347017264161627L;
    private JSONObject payload = new JSONObject();

    public DecoupledPushMessage(byte b) {
        super(b);
    }

    public byte[] is() {
        if (this.payload == null) {
            c.eq("PushLog3413", "encode error, payload is null");
            return new byte[0];
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(it());
            byte[] bytes = this.payload.toString().getBytes("UTF-8");
            byteArrayOutputStream.write(g.gd(bytes.length + 6));
            byteArrayOutputStream.write(2);
            byteArrayOutputStream.write(g.gd(bytes.length));
            byteArrayOutputStream.write(bytes);
            return byteArrayOutputStream.toByteArray();
        } catch (UnsupportedEncodingException e) {
            c.eq("PushLog3413", "unsupported encoding type");
        } catch (IOException e2) {
            c.eq("PushLog3413", "io exception");
        }
        return new byte[0];
    }

    public PushMessage jc(InputStream inputStream) {
        d iy = iy(inputStream);
        byte b = iy.kp(1)[0];
        if (!iz(b)) {
            return null;
        }
        this.payload = ja(b, iy);
        jo(this.payload);
        return this;
    }

    private void jo(JSONObject jSONObject) {
        c.ep("PushLog3413", "parse decoupled msg payload.");
        if (jSONObject != null && jSONObject.has("cmdid")) {
            try {
                this.mSubCmdId = (byte) jSONObject.getInt("cmdid");
            } catch (Exception e) {
                c.eq("PushLog3413", "parse decoupled msg payload exception");
            }
        }
    }

    public String toString() {
        return "payload is" + f.u(this.payload);
    }

    public JSONObject jn() {
        return this.payload;
    }

    public void jm(JSONObject jSONObject) {
        this.payload = jSONObject;
    }
}
