package tmsdkobf;

import android.os.Environment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.utils.f;

public class kl {
    public static boolean a(byte[] bArr, String str) {
        Object -l_5_R;
        if (bArr == null) {
            return false;
        }
        int -l_2_I = 0;
        FileOutputStream fileOutputStream = null;
        try {
            Object -l_4_R = new File(str);
            if (!-l_4_R.exists()) {
                -l_4_R.getAbsoluteFile().getParentFile().mkdirs();
                -l_4_R.createNewFile();
            }
            FileOutputStream -l_3_R = new FileOutputStream(-l_4_R, false);
            try {
                -l_3_R.write(bArr);
                -l_2_I = 1;
                if (-l_3_R != null) {
                    try {
                        -l_3_R.close();
                    } catch (IOException e) {
                        fileOutputStream = -l_3_R;
                    }
                }
                fileOutputStream = -l_3_R;
            } catch (Throwable th) {
                -l_5_R = th;
                fileOutputStream = -l_3_R;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e2) {
                    }
                }
                throw -l_5_R;
            }
        } catch (Throwable th2) {
            -l_5_R = th2;
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            throw -l_5_R;
        }
        return -l_2_I;
    }

    public static byte[] aV(String str) {
        Object -l_6_R;
        byte[] -l_1_R = null;
        FileInputStream fileInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            FileInputStream -l_2_R = new FileInputStream(str);
            Object -l_4_R;
            try {
                ByteArrayOutputStream -l_3_R = new ByteArrayOutputStream(-l_2_R.available());
                try {
                    -l_4_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
                    while (true) {
                        int -l_5_I = -l_2_R.read(-l_4_R);
                        if (-l_5_I < 0) {
                            break;
                        }
                        -l_3_R.write(-l_4_R, 0, -l_5_I);
                    }
                    -l_1_R = -l_3_R.toByteArray();
                    if (-l_3_R != null) {
                        try {
                            -l_3_R.close();
                        } catch (Object -l_4_R2) {
                            -l_4_R2.printStackTrace();
                        }
                    }
                    if (-l_2_R != null) {
                        try {
                            -l_2_R.close();
                        } catch (Object -l_4_R22) {
                            -l_4_R22.printStackTrace();
                        }
                    }
                    byteArrayOutputStream = -l_3_R;
                    fileInputStream = -l_2_R;
                } catch (Throwable th) {
                    -l_6_R = th;
                    byteArrayOutputStream = -l_3_R;
                    fileInputStream = -l_2_R;
                }
            } catch (Throwable th2) {
                -l_6_R = th2;
                fileInputStream = -l_2_R;
                if (byteArrayOutputStream != null) {
                    try {
                        byteArrayOutputStream.close();
                    } catch (Object -l_7_R) {
                        -l_7_R.printStackTrace();
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Object -l_7_R2) {
                        -l_7_R2.printStackTrace();
                    }
                }
                throw -l_6_R;
            }
        } catch (Throwable th3) {
            -l_6_R = th3;
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            throw -l_6_R;
        }
        if (-l_1_R == null) {
        }
    }

    public static boolean b(File file) {
        int -l_3_I = 0;
        int -l_1_I = 1;
        if (file.isDirectory()) {
            Object -l_2_R = file.list();
            Object -l_3_R = -l_2_R;
            int -l_4_I = -l_2_R.length;
            for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
                if (b(new File(file, -l_3_R[-l_5_I])) == 0) {
                    -l_1_I = 0;
                }
            }
        }
        int -l_2_I = file.delete();
        if (!(-l_1_I == 0 || -l_2_I == 0)) {
            -l_3_I = 1;
        }
        if (-l_3_I == 0) {
            f.f("FileUtil", "delete failed: " + file.getAbsolutePath());
        }
        return -l_3_I;
    }

    public static int cK() {
        return lu.eF() ? cL() ? 0 : 2 : 1;
    }

    public static boolean cL() {
        Object -l_0_R = Environment.getExternalStorageDirectory().toString() + "/DCIM";
        Object -l_1_R = new File(-l_0_R);
        if (!-l_1_R.isDirectory() && !-l_1_R.mkdirs()) {
            return false;
        }
        Object -l_2_R = new File(-l_0_R, ".probe");
        try {
            if (-l_2_R.exists()) {
                -l_2_R.delete();
            }
            if (!-l_2_R.createNewFile()) {
                return false;
            }
            -l_2_R.delete();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean copyFile(File file, File file2) {
        OutputStream -l_3_R;
        Object -l_8_R;
        if (file.isFile()) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            BufferedInputStream bufferedInputStream = null;
            BufferedOutputStream bufferedOutputStream = null;
            try {
                InputStream -l_2_R = new FileInputStream(file);
                try {
                    -l_3_R = new FileOutputStream(file2);
                } catch (Exception e) {
                    inputStream = -l_2_R;
                    if (bufferedInputStream != null) {
                        try {
                            bufferedInputStream.close();
                        } catch (Exception e2) {
                        }
                    }
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (Exception e3) {
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e4) {
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception e5) {
                        }
                    }
                    return true;
                } catch (Throwable th) {
                    -l_8_R = th;
                    inputStream = -l_2_R;
                    if (bufferedInputStream != null) {
                        try {
                            bufferedInputStream.close();
                        } catch (Exception e6) {
                        }
                    }
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (Exception e7) {
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e8) {
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception e9) {
                        }
                    }
                    throw -l_8_R;
                }
                try {
                    try {
                        BufferedInputStream -l_4_R = new BufferedInputStream(-l_2_R);
                        try {
                            try {
                                BufferedOutputStream -l_5_R = new BufferedOutputStream(-l_3_R);
                                try {
                                    Object -l_6_R = new byte[8192];
                                    while (true) {
                                        int -l_7_I = -l_4_R.read(-l_6_R);
                                        if (-l_7_I == -1) {
                                            break;
                                        }
                                        -l_5_R.write(-l_6_R, 0, -l_7_I);
                                    }
                                    -l_5_R.flush();
                                    if (-l_4_R != null) {
                                        try {
                                            -l_4_R.close();
                                        } catch (Exception e10) {
                                        }
                                    }
                                    if (-l_5_R != null) {
                                        try {
                                            -l_5_R.close();
                                        } catch (Exception e11) {
                                        }
                                    }
                                    if (-l_2_R != null) {
                                        try {
                                            -l_2_R.close();
                                        } catch (Exception e12) {
                                        }
                                    }
                                    if (-l_3_R != null) {
                                        try {
                                            -l_3_R.close();
                                        } catch (Exception e13) {
                                        }
                                    }
                                    bufferedOutputStream = -l_5_R;
                                    bufferedInputStream = -l_4_R;
                                    outputStream = -l_3_R;
                                    inputStream = -l_2_R;
                                } catch (Exception e14) {
                                    bufferedOutputStream = -l_5_R;
                                    bufferedInputStream = -l_4_R;
                                    outputStream = -l_3_R;
                                    inputStream = -l_2_R;
                                    if (bufferedInputStream != null) {
                                        bufferedInputStream.close();
                                    }
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.close();
                                    }
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    return true;
                                } catch (Throwable th2) {
                                    -l_8_R = th2;
                                    bufferedOutputStream = -l_5_R;
                                    bufferedInputStream = -l_4_R;
                                    outputStream = -l_3_R;
                                    inputStream = -l_2_R;
                                    if (bufferedInputStream != null) {
                                        bufferedInputStream.close();
                                    }
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.close();
                                    }
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    throw -l_8_R;
                                }
                            } catch (Exception e15) {
                                bufferedInputStream = -l_4_R;
                                outputStream = -l_3_R;
                                inputStream = -l_2_R;
                                if (bufferedInputStream != null) {
                                    bufferedInputStream.close();
                                }
                                if (bufferedOutputStream != null) {
                                    bufferedOutputStream.close();
                                }
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                return true;
                            } catch (Throwable th3) {
                                -l_8_R = th3;
                                bufferedInputStream = -l_4_R;
                                outputStream = -l_3_R;
                                inputStream = -l_2_R;
                                if (bufferedInputStream != null) {
                                    bufferedInputStream.close();
                                }
                                if (bufferedOutputStream != null) {
                                    bufferedOutputStream.close();
                                }
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                throw -l_8_R;
                            }
                        } catch (Exception e16) {
                            bufferedInputStream = -l_4_R;
                            outputStream = -l_3_R;
                            inputStream = -l_2_R;
                            if (bufferedInputStream != null) {
                                bufferedInputStream.close();
                            }
                            if (bufferedOutputStream != null) {
                                bufferedOutputStream.close();
                            }
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            if (outputStream != null) {
                                outputStream.close();
                            }
                            return true;
                        } catch (Throwable th4) {
                            -l_8_R = th4;
                            bufferedInputStream = -l_4_R;
                            outputStream = -l_3_R;
                            inputStream = -l_2_R;
                            if (bufferedInputStream != null) {
                                bufferedInputStream.close();
                            }
                            if (bufferedOutputStream != null) {
                                bufferedOutputStream.close();
                            }
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            if (outputStream != null) {
                                outputStream.close();
                            }
                            throw -l_8_R;
                        }
                    } catch (Exception e17) {
                        outputStream = -l_3_R;
                        inputStream = -l_2_R;
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        if (bufferedOutputStream != null) {
                            bufferedOutputStream.close();
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        return true;
                    } catch (Throwable th5) {
                        -l_8_R = th5;
                        outputStream = -l_3_R;
                        inputStream = -l_2_R;
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        if (bufferedOutputStream != null) {
                            bufferedOutputStream.close();
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        throw -l_8_R;
                    }
                } catch (Exception e18) {
                    outputStream = -l_3_R;
                    inputStream = -l_2_R;
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                    if (bufferedOutputStream != null) {
                        bufferedOutputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    return true;
                } catch (Throwable th6) {
                    -l_8_R = th6;
                    outputStream = -l_3_R;
                    inputStream = -l_2_R;
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                    if (bufferedOutputStream != null) {
                        bufferedOutputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    throw -l_8_R;
                }
            } catch (Exception e19) {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                return true;
            } catch (Throwable th7) {
                -l_8_R = th7;
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                throw -l_8_R;
            }
        } else if (file.isDirectory()) {
            Object -l_2_R2 = file.listFiles();
            file2.mkdir();
            if (-l_2_R2 != null) {
                for (int -l_3_I = 0; -l_3_I < -l_2_R2.length; -l_3_I++) {
                    copyFile(-l_2_R2[-l_3_I].getAbsoluteFile(), new File(file2.getAbsoluteFile() + File.separator + -l_2_R2[-l_3_I].getName()));
                }
            }
        }
        return true;
    }
}
