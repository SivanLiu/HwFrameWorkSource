package com.huawei.android.pushselfshow.utils.c;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.b.b;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class a {
    public Bitmap a(Context context, Bitmap bitmap, float f, float f2) {
        try {
            int -l_5_I = bitmap.getWidth();
            int -l_6_I = bitmap.getHeight();
            float -l_7_F = f / ((float) -l_5_I);
            float -l_8_F = f2 / ((float) -l_6_I);
            Object -l_9_R = new Matrix();
            -l_9_R.postScale(-l_7_F, -l_8_F);
            Object -l_10_R = Bitmap.createBitmap(bitmap, 0, 0, -l_5_I, -l_6_I, -l_9_R, true);
            if (-l_10_R != null) {
                c.a("PushSelfShowLog", "reScaleBitmap success");
                return -l_10_R;
            }
        } catch (Object -l_5_R) {
            c.d("PushSelfShowLog", "reScaleBitmap fail ,error ：" + -l_5_R, -l_5_R);
        }
        return bitmap;
    }

    public Bitmap a(Context context, String str) {
        Object -l_6_R;
        Object -l_12_R;
        Bitmap bitmap = null;
        InputStream -l_4_R = null;
        File file = null;
        try {
            -l_6_R = "image" + System.currentTimeMillis();
            Object -l_7_R = b.a(context);
            Object -l_8_R = new File(-l_7_R);
            if (!-l_8_R.exists()) {
                c.a("PushSelfShowLog", "mkdir: " + -l_8_R.getAbsolutePath());
                if (!-l_8_R.mkdirs()) {
                    c.a("PushSelfShowLog", "file mkdir failed ,path is " + -l_8_R.getPath());
                }
            }
            Object -l_9_R = -l_7_R + File.separator + -l_6_R;
            c.a("PushSelfShowLog", "try to download image to " + -l_9_R);
            if (new b().b(context, str, -l_9_R)) {
                c.a("PushSelfShowLog", "download successed");
                Object -l_11_R = new Options();
                -l_11_R.inDither = false;
                -l_11_R.inPurgeable = true;
                -l_11_R.inSampleSize = 1;
                -l_11_R.inPreferredConfig = Config.RGB_565;
                File -l_5_R = new File(-l_9_R);
                try {
                    try {
                        InputStream -l_4_R2 = new FileInputStream(-l_5_R);
                        try {
                            bitmap = BitmapFactory.decodeStream(-l_4_R2, null, -l_11_R);
                            file = -l_5_R;
                            -l_4_R = -l_4_R2;
                        } catch (Exception e) {
                            -l_6_R = e;
                            file = -l_5_R;
                            -l_4_R = -l_4_R2;
                            try {
                                c.d("PushSelfShowLog", "getRemoteImage  failed  ,errorinfo is " + -l_6_R.toString(), -l_6_R);
                                if (-l_4_R != null) {
                                    -l_4_R.close();
                                }
                                if (file != null) {
                                    try {
                                        c.a("PushSelfShowLog", "image delete success");
                                    } catch (Object -l_6_R2) {
                                        c.d("PushSelfShowLog", "is.close() error" + -l_6_R2.toString(), -l_6_R2);
                                    }
                                }
                                return bitmap;
                            } catch (Throwable th) {
                                -l_12_R = th;
                                if (-l_4_R != null) {
                                    try {
                                        -l_4_R.close();
                                    } catch (Object -l_13_R) {
                                        c.d("PushSelfShowLog", "is.close() error" + -l_13_R.toString(), -l_13_R);
                                        throw -l_12_R;
                                    }
                                }
                                c.a("PushSelfShowLog", "image delete success");
                                throw -l_12_R;
                            }
                        } catch (Throwable th2) {
                            -l_12_R = th2;
                            file = -l_5_R;
                            -l_4_R = -l_4_R2;
                            if (-l_4_R != null) {
                                -l_4_R.close();
                            }
                            c.a("PushSelfShowLog", "image delete success");
                            throw -l_12_R;
                        }
                    } catch (Exception e2) {
                        -l_6_R2 = e2;
                        file = -l_5_R;
                        c.d("PushSelfShowLog", "getRemoteImage  failed  ,errorinfo is " + -l_6_R2.toString(), -l_6_R2);
                        if (-l_4_R != null) {
                            -l_4_R.close();
                        }
                        if (file != null) {
                            if (file.isFile() && file.delete()) {
                                c.a("PushSelfShowLog", "image delete success");
                            }
                        }
                        return bitmap;
                    } catch (Throwable th3) {
                        -l_12_R = th3;
                        file = -l_5_R;
                        if (-l_4_R != null) {
                            -l_4_R.close();
                        }
                        if (file != null && file.isFile() && file.delete()) {
                            c.a("PushSelfShowLog", "image delete success");
                        }
                        throw -l_12_R;
                    }
                } catch (Exception e3) {
                    -l_6_R2 = e3;
                    file = -l_5_R;
                    c.d("PushSelfShowLog", "getRemoteImage  failed  ,errorinfo is " + -l_6_R2.toString(), -l_6_R2);
                    if (-l_4_R != null) {
                        -l_4_R.close();
                    }
                    if (file != null) {
                        c.a("PushSelfShowLog", "image delete success");
                    }
                    return bitmap;
                } catch (Throwable th4) {
                    -l_12_R = th4;
                    file = -l_5_R;
                    if (-l_4_R != null) {
                        -l_4_R.close();
                    }
                    c.a("PushSelfShowLog", "image delete success");
                    throw -l_12_R;
                }
            }
            c.a("PushSelfShowLog", "download failed");
            if (-l_4_R != null) {
                try {
                    -l_4_R.close();
                } catch (Object -l_6_R22) {
                    c.d("PushSelfShowLog", "is.close() error" + -l_6_R22.toString(), -l_6_R22);
                }
            }
            if (file != null && file.isFile() && file.delete()) {
                c.a("PushSelfShowLog", "image delete success");
            }
        } catch (Exception e4) {
            -l_6_R22 = e4;
            c.d("PushSelfShowLog", "getRemoteImage  failed  ,errorinfo is " + -l_6_R22.toString(), -l_6_R22);
            if (-l_4_R != null) {
                -l_4_R.close();
            }
            if (file != null) {
                c.a("PushSelfShowLog", "image delete success");
            }
            return bitmap;
        }
        return bitmap;
    }
}
