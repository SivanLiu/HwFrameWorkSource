package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.e.c;
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

    public byte[] b() {
        if (this.payload == null) {
            a.su("PushLog3414", "encode error, payload is null");
            return new byte[0];
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(c());
            byte[] bytes = this.payload.toString().getBytes("UTF-8");
            byteArrayOutputStream.write(d.zr(bytes.length + 6));
            byteArrayOutputStream.write(2);
            byteArrayOutputStream.write(d.zr(bytes.length));
            byteArrayOutputStream.write(bytes);
            return byteArrayOutputStream.toByteArray();
        } catch (UnsupportedEncodingException e) {
            a.su("PushLog3414", "unsupported encoding type");
        } catch (IOException e2) {
            a.su("PushLog3414", "io exception");
        }
        return new byte[0];
    }

    public PushMessage a(InputStream inputStream) {
        com.huawei.android.pushagent.datatype.a.d e = e(inputStream);
        byte b = e.ax(1)[0];
        if (!h(b)) {
            return null;
        }
        this.payload = f(b, e);
        t(this.payload);
        return this;
    }

    private void t(JSONObject jSONObject) {
        a.sv("PushLog3414", "parse decoupled msg payload.");
        if (jSONObject != null && jSONObject.has("cmdid")) {
            try {
                this.mSubCmdId = (byte) jSONObject.getInt("cmdid");
            } catch (Exception e) {
                a.su("PushLog3414", "parse decoupled msg payload exception");
            }
        }
    }

    public String toString() {
        return "payload is" + c.wi(this.payload);
    }

    public JSONObject s() {
        return this.payload;
    }

    public void r(JSONObject jSONObject) {
        this.payload = jSONObject;
    }
}
