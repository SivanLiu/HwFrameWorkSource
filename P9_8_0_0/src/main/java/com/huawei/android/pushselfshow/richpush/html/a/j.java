package com.huawei.android.pushselfshow.richpush.html.a;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.richpush.html.api.NativeToJsMessageQueue;
import com.huawei.android.pushselfshow.richpush.html.api.b;
import com.huawei.android.pushselfshow.richpush.html.api.d.a;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import org.json.JSONException;
import org.json.JSONObject;

public class j implements g {
    private NativeToJsMessageQueue a;
    private String b;
    private Context c;
    private String d = null;

    public j(Context context) {
        c.e("PushSelfShowLog", "init VideoPlayer");
        this.c = context;
    }

    private void a(JSONObject jSONObject) {
        if (this.a != null) {
            if (jSONObject != null && jSONObject.has(CheckVersionField.CHECK_VERSION_SERVER_URL)) {
                try {
                    String -l_2_R = jSONObject.getString(CheckVersionField.CHECK_VERSION_SERVER_URL);
                    Object -l_4_R = b.a(this.a.a(), -l_2_R);
                    if (-l_4_R != null) {
                        if (-l_4_R.length() > 0) {
                            this.d = -l_4_R;
                            Object -l_5_R = "video/*";
                            if (jSONObject.has("mime-type")) {
                                try {
                                    Object -l_6_R = jSONObject.getString("mime-type");
                                    c.e("PushSelfShowLog", "the custom mimetype is " + -l_6_R);
                                    if (-l_6_R.startsWith("video/")) {
                                        -l_5_R = -l_6_R;
                                    }
                                } catch (JSONException e) {
                                    c.e("PushSelfShowLog", "get mime-type error");
                                } catch (Exception e2) {
                                    c.e("PushSelfShowLog", "get mime-type error");
                                }
                            }
                            Intent -l_6_R2 = new Intent("android.intent.action.VIEW");
                            -l_6_R2.setDataAndType(Uri.parse(this.d), -l_5_R);
                            if (jSONObject.has("package-name")) {
                                try {
                                    Object -l_8_R = jSONObject.getString("package-name");
                                    c.e("PushSelfShowLog", "the custom packageName is " + -l_8_R);
                                    if (b.a(this.c, -l_6_R2).contains(-l_8_R)) {
                                        -l_6_R2.setPackage(-l_8_R);
                                    }
                                } catch (JSONException e3) {
                                    c.e("PushSelfShowLog", "get packageName error");
                                }
                            }
                            this.c.startActivity(-l_6_R2);
                            this.a.a(this.b, a.OK, "success", null);
                        }
                    }
                    c.e("PushSelfShowLog", -l_2_R + "File not exist");
                    this.a.a(this.b, a.AUDIO_ONLY_SUPPORT_HTTP, "error", null);
                } catch (Object -l_2_R2) {
                    c.e("PushSelfShowLog", "startPlaying failed ", -l_2_R2);
                    this.a.a(this.b, a.JSON_EXCEPTION, "error", null);
                } catch (Object -l_2_R22) {
                    c.e("PushSelfShowLog", "startPlaying failed ", -l_2_R22);
                    this.a.a(this.b, a.JSON_EXCEPTION, "error", null);
                }
            } else {
                this.a.a(this.b, a.JSON_EXCEPTION, "error", null);
            }
            return;
        }
        c.a("PushSelfShowLog", "jsMessageQueue is null while run into Video Player exec");
    }

    public String a(String str, JSONObject jSONObject) {
        return null;
    }

    public void a(int i, int i2, Intent intent) {
    }

    public void a(NativeToJsMessageQueue nativeToJsMessageQueue, String str, String str2, JSONObject jSONObject) {
        if (nativeToJsMessageQueue != null) {
            this.a = nativeToJsMessageQueue;
            if ("playVideo".equals(str)) {
                d();
                if (str2 == null) {
                    c.a("PushSelfShowLog", "Audio exec callback is null ");
                } else {
                    this.b = str2;
                    a(jSONObject);
                }
            } else {
                nativeToJsMessageQueue.a(str2, a.METHOD_NOT_FOUND_EXCEPTION, "error", null);
            }
            return;
        }
        c.a("PushSelfShowLog", "jsMessageQueue is null while run into Video Player exec");
    }

    public void b() {
    }

    public void c() {
        d();
    }

    public void d() {
        this.b = null;
        this.d = null;
    }
}
