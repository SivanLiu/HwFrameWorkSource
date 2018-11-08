package tmsdkobf;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import tmsdk.common.ErrorCode;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;

public class gb {
    public static Object a(Context context, String str, Object obj, String str2) {
        Object -l_12_R;
        Object obj2 = null;
        if (str == null || str2 == null) {
            return null;
        }
        FileInputStream fileInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            FileInputStream -l_5_R = new FileInputStream(str2);
            try {
                ByteArrayOutputStream -l_6_R = new ByteArrayOutputStream();
                Object -l_7_R;
                try {
                    -l_7_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
                    while (true) {
                        int -l_8_I = -l_5_R.read(-l_7_R);
                        if (-l_8_I == -1) {
                            break;
                        }
                        -l_6_R.write(-l_7_R, 0, -l_8_I);
                    }
                    Object -l_9_R = -l_6_R.toByteArray();
                    Object -l_10_R = new fn();
                    -l_10_R.B("UTF-8");
                    -l_10_R.b(fz.b(-l_9_R, fz.P()));
                    obj2 = -l_10_R.a(str, obj);
                    if (obj2 == null) {
                        mb.o("FileUtil", "wupObject is null");
                    } else {
                        mb.n("FileUtil", obj2.toString());
                    }
                    if (-l_5_R != null) {
                        try {
                            -l_5_R.close();
                        } catch (Object -l_7_R2) {
                            -l_7_R2.printStackTrace();
                        }
                    }
                    if (-l_6_R != null) {
                        try {
                            -l_6_R.close();
                        } catch (Object -l_7_R22) {
                            -l_7_R22.printStackTrace();
                        }
                    }
                    byteArrayOutputStream = -l_6_R;
                    fileInputStream = -l_5_R;
                } catch (FileNotFoundException e) {
                    byteArrayOutputStream = -l_6_R;
                    fileInputStream = -l_5_R;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_7_R222) {
                            -l_7_R222.printStackTrace();
                        }
                    }
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Object -l_7_R2222) {
                            -l_7_R2222.printStackTrace();
                        }
                    }
                    return obj2;
                } catch (IOException e2) {
                    byteArrayOutputStream = -l_6_R;
                    fileInputStream = -l_5_R;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_7_R22222) {
                            -l_7_R22222.printStackTrace();
                        }
                    }
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Object -l_7_R222222) {
                            -l_7_R222222.printStackTrace();
                        }
                    }
                    return obj2;
                } catch (Throwable th) {
                    -l_12_R = th;
                    byteArrayOutputStream = -l_6_R;
                    fileInputStream = -l_5_R;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_13_R) {
                            -l_13_R.printStackTrace();
                        }
                    }
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Object -l_13_R2) {
                            -l_13_R2.printStackTrace();
                        }
                    }
                    throw -l_12_R;
                }
            } catch (FileNotFoundException e3) {
                fileInputStream = -l_5_R;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                return obj2;
            } catch (IOException e4) {
                fileInputStream = -l_5_R;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                return obj2;
            } catch (Throwable th2) {
                -l_12_R = th2;
                fileInputStream = -l_5_R;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                throw -l_12_R;
            }
        } catch (FileNotFoundException e5) {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            return obj2;
        } catch (IOException e6) {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            return obj2;
        } catch (Throwable th3) {
            -l_12_R = th3;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            throw -l_12_R;
        }
        return obj2;
    }

    private static boolean a(File file) {
        if (!file.exists()) {
            Object -l_1_R = file.getParentFile();
            if (!-l_1_R.exists()) {
                -l_1_R.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public static int c(Context context, Object obj, String str, String str2) {
        Object -l_6_R;
        Object -l_10_R;
        int -l_4_I = -2;
        if (obj == null || str == null || str2 == null) {
            return -57;
        }
        FileOutputStream fileOutputStream = null;
        try {
            -l_6_R = new File(str2);
            if (a(-l_6_R)) {
                FileOutputStream -l_5_R = new FileOutputStream(-l_6_R);
                try {
                    Object -l_7_R = new fn();
                    -l_7_R.B("UTF-8");
                    -l_7_R.put(str, obj);
                    Object -l_9_R = fz.a(-l_7_R.l(), fz.P());
                    if (-l_9_R != null) {
                        -l_5_R.write(-l_9_R);
                        -l_4_I = 0;
                    }
                    if (-l_5_R != null) {
                        try {
                            -l_5_R.close();
                        } catch (Object -l_6_R2) {
                            -l_6_R2.printStackTrace();
                        }
                    }
                    fileOutputStream = -l_5_R;
                } catch (FileNotFoundException e) {
                    -l_6_R2 = e;
                    fileOutputStream = -l_5_R;
                    -l_4_I = -1;
                    try {
                        -l_6_R2.printStackTrace();
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (Object -l_6_R22) {
                                -l_6_R22.printStackTrace();
                            }
                        }
                        return -l_4_I;
                    } catch (Throwable th) {
                        -l_10_R = th;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (Object -l_11_R) {
                                -l_11_R.printStackTrace();
                            }
                        }
                        throw -l_10_R;
                    }
                } catch (IOException e2) {
                    -l_6_R22 = e2;
                    fileOutputStream = -l_5_R;
                    -l_4_I = ErrorCode.ERR_FILE_OP;
                    -l_6_R22.printStackTrace();
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (Object -l_6_R222) {
                            -l_6_R222.printStackTrace();
                        }
                    }
                    return -l_4_I;
                } catch (Throwable th2) {
                    -l_10_R = th2;
                    fileOutputStream = -l_5_R;
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    throw -l_10_R;
                }
                return -l_4_I;
            }
            if (null != null) {
                try {
                    fileOutputStream.close();
                } catch (Object -l_8_R) {
                    -l_8_R.printStackTrace();
                }
            }
            return -2;
        } catch (FileNotFoundException e3) {
            -l_6_R222 = e3;
            -l_4_I = -1;
            -l_6_R222.printStackTrace();
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return -l_4_I;
        } catch (IOException e4) {
            -l_6_R222 = e4;
            -l_4_I = ErrorCode.ERR_FILE_OP;
            -l_6_R222.printStackTrace();
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return -l_4_I;
        } catch (Throwable th3) {
            -l_6_R222 = th3;
            -l_6_R222.printStackTrace();
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return -l_4_I;
        }
    }

    public static String e(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }
}
