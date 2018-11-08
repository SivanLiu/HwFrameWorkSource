package com.huawei.android.pushselfshow.richpush.tools;

import android.content.Context;
import com.huawei.android.pushselfshow.utils.a;
import java.io.File;
import java.io.FileOutputStream;

public class c {
    private String a;
    private Context b;

    public c(Context context, String str) {
        this.a = str;
        this.b = context;
    }

    private String b() {
        return "﻿<!DOCTYPE html>\t\t<html>\t\t   <head>\t\t     <meta charset=\"utf-8\">\t\t     <title></title>\t\t     <style type=\"text/css\">\t\t\t\t html { height:100%;}\t\t\t\t body { height:100%; text-align:center;}\t    \t    .centerDiv { display:inline-block; zoom:1; *display:inline; vertical-align:top; text-align:left; width:200px; padding:10px;margin-top:100px;}\t\t\t   .hiddenDiv { height:100%; overflow:hidden; display:inline-block; width:1px; overflow:hidden; margin-left:-1px; zoom:1; *display:inline; *margin-top:-1px; _margin-top:0; vertical-align:middle;}\t\t  \t</style>    \t  </head>\t\t <body>\t\t\t<div id =\"container\" class=\"centerDiv\">";
    }

    private String c() {
        return "﻿\t\t</div>  \t\t<div class=\"hiddenDiv\"></div>\t  </body>   </html>";
    }

    public String a() {
        Throwable -l_7_R;
        Object -l_10_R;
        FileOutputStream -l_1_R = null;
        if (this.b != null) {
            Object -l_2_R = b() + this.a + c();
            Object -l_3_R = this.b.getFilesDir().getPath() + File.separator + "PushService" + File.separator + "richpush";
            Object -l_4_R = "error.html";
            Object -l_5_R = new File(-l_3_R);
            File -l_6_R = new File(-l_3_R + File.separator + -l_4_R);
            try {
                if (!-l_5_R.exists()) {
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "Create the path:" + -l_3_R);
                    if (!-l_5_R.mkdirs()) {
                        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "!path.mkdirs()");
                        if (-l_1_R != null) {
                            try {
                                -l_1_R.close();
                            } catch (Throwable -l_8_R) {
                                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "stream.close() error ", -l_8_R);
                            }
                        }
                        return null;
                    }
                }
                if (-l_6_R.exists()) {
                    a.a(-l_6_R);
                }
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "Create the file:" + -l_4_R);
                if (-l_6_R.createNewFile()) {
                    FileOutputStream -l_1_R2 = new FileOutputStream(-l_6_R);
                    try {
                        -l_1_R2.write(-l_2_R.getBytes("UTF-8"));
                        if (-l_1_R2 != null) {
                            try {
                                -l_1_R2.close();
                            } catch (Throwable -l_7_R2) {
                                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "stream.close() error ", -l_7_R2);
                            }
                        }
                        return -l_6_R.getAbsolutePath();
                    } catch (Exception e) {
                        -l_7_R2 = e;
                        -l_1_R = -l_1_R2;
                        try {
                            com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "Create html error ", -l_7_R2);
                            if (-l_1_R != null) {
                                try {
                                    -l_1_R.close();
                                } catch (Throwable -l_9_R) {
                                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "stream.close() error ", -l_9_R);
                                }
                            }
                            return null;
                        } catch (Throwable th) {
                            -l_10_R = th;
                            if (-l_1_R != null) {
                                try {
                                    -l_1_R.close();
                                } catch (Throwable -l_11_R) {
                                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "stream.close() error ", -l_11_R);
                                }
                            }
                            throw -l_10_R;
                        }
                    } catch (Throwable th2) {
                        -l_10_R = th2;
                        -l_1_R = -l_1_R2;
                        if (-l_1_R != null) {
                            -l_1_R.close();
                        }
                        throw -l_10_R;
                    }
                }
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "!file.createNewFile()");
                if (-l_1_R != null) {
                    try {
                        -l_1_R.close();
                    } catch (Throwable -l_8_R2) {
                        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "stream.close() error ", -l_8_R2);
                    }
                }
                return null;
            } catch (Exception e2) {
                -l_7_R2 = e2;
                com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "Create html error ", -l_7_R2);
                if (-l_1_R != null) {
                    -l_1_R.close();
                }
                return null;
            }
        }
        com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "CreateHtmlFile fail ,context is null");
        return null;
    }
}
