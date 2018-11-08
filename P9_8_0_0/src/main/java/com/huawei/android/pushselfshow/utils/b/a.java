package com.huawei.android.pushselfshow.utils.b;

import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.c;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;

public class a {
    private String a;
    private String b;

    public a(String str, String str2) {
        this.a = str;
        this.b = str2;
    }

    public static File a(String str, String str2) {
        Object -l_4_R;
        Object -l_2_R = str2.split("/");
        Object -l_3_R = new File(str);
        int -l_5_I = 0;
        while (-l_5_I < -l_2_R.length - 1) {
            try {
                -l_4_R = new String(-l_2_R[-l_5_I].getBytes("8859_1"), "GB2312");
                -l_5_I++;
                Object obj = -l_4_R;
                -l_3_R = new File(-l_3_R, -l_4_R);
            } catch (Exception e) {
                Object -l_5_R = e;
            }
        }
        c.a("PushSelfShowLog", "file1 = " + -l_3_R);
        if (!-l_3_R.exists()) {
            c.a("PushSelfShowLog", "getRealFileName ret.mkdirs success");
            if (-l_3_R.mkdirs() == 0) {
                c.a("PushSelfShowLog", "ret.mkdirs faild");
            }
        }
        -l_4_R = new String(-l_2_R[-l_2_R.length - 1].getBytes("8859_1"), "GB2312");
        try {
        } catch (Exception e2) {
            -l_5_R = e2;
            obj = -l_4_R;
        }
        try {
            c.a("PushSelfShowLog", "substr = " + -l_4_R);
            Object -l_3_R2 = new File(-l_3_R, -l_4_R);
            try {
                c.a("PushSelfShowLog", "file2 = " + -l_3_R2);
                return -l_3_R2;
            } catch (Exception e3) {
                -l_5_R = e3;
                obj = -l_4_R;
                -l_3_R = -l_3_R2;
                c.d("PushSelfShowLog", -l_5_R.toString());
                return -l_3_R;
            }
        } catch (Exception e4) {
            -l_5_R = e4;
            obj = -l_4_R;
        }
    }

    public void a() {
        Object -l_2_R;
        FileOutputStream fileOutputStream;
        OutputStream outputStream;
        InputStream inputStream;
        InputStream inputStream2;
        Object -l_10_R;
        OutputStream outputStream2;
        Object -l_11_R;
        ZipFile zipFile = null;
        try {
            if (!this.b.endsWith("/")) {
                this.b += "/";
            }
            ZipFile -l_1_R = new ZipFile(new File(this.a));
            try {
                -l_2_R = -l_1_R.entries();
                Object -l_4_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
                while (-l_2_R.hasMoreElements()) {
                    ZipEntry -l_3_R = (ZipEntry) -l_2_R.nextElement();
                    c.a("PushSelfShowLog", "ze.isDirectory()=" + -l_3_R.isDirectory() + "ze.getName() = " + -l_3_R.getName());
                    if (-l_3_R.isDirectory()) {
                        Object -l_5_R = new String((this.b + -l_3_R.getName()).getBytes("8859_1"), "GB2312");
                        c.a("PushSelfShowLog", "str = " + -l_5_R);
                        if (new File(-l_5_R).mkdir() != 0) {
                            continue;
                        }
                    }
                    Object -l_5_R2;
                    if (TextUtils.isEmpty(-l_3_R.getName())) {
                        c.a("PushSelfShowLog", "ze.getName() is empty= ");
                        if (-l_1_R != null) {
                            try {
                                -l_1_R.close();
                            } catch (Object -l_5_R22) {
                                c.a("PushSelfShowLog", "zfile.close error:" + -l_5_R22.getMessage());
                            }
                        }
                        return;
                    }
                    if (!-l_3_R.getName().contains("..\\")) {
                        if (!-l_3_R.getName().contains("../")) {
                            -l_5_R22 = a(this.b, -l_3_R.getName());
                            if (-l_5_R22.isDirectory()) {
                                if (-l_1_R != null) {
                                    try {
                                        -l_1_R.close();
                                    } catch (Object -l_6_R) {
                                        c.a("PushSelfShowLog", "zfile.close error:" + -l_6_R.getMessage());
                                    }
                                }
                                return;
                            }
                            c.a("PushSelfShowLog", ",output file :" + -l_5_R22.getAbsolutePath());
                            fileOutputStream = null;
                            outputStream = null;
                            inputStream = null;
                            inputStream2 = null;
                            try {
                                inputStream2 = -l_1_R.getInputStream(-l_3_R);
                                OutputStream fileOutputStream2 = new FileOutputStream(-l_5_R22);
                                try {
                                    try {
                                        fileOutputStream2 = new BufferedOutputStream(fileOutputStream2);
                                        try {
                                            InputStream -l_8_R = new BufferedInputStream(inputStream2);
                                            while (true) {
                                                try {
                                                    int -l_10_I = -l_8_R.read(-l_4_R, 0, IncomingSmsFilterConsts.PAY_SMS);
                                                    if (-l_10_I == -1) {
                                                        break;
                                                    }
                                                    try {
                                                        fileOutputStream2.write(-l_4_R, 0, -l_10_I);
                                                    } catch (IOException e) {
                                                        -l_10_R = e;
                                                        inputStream = -l_8_R;
                                                        outputStream = fileOutputStream2;
                                                        fileOutputStream = fileOutputStream2;
                                                    } catch (IllegalStateException e2) {
                                                        -l_10_R = e2;
                                                        inputStream = -l_8_R;
                                                        outputStream = fileOutputStream2;
                                                        outputStream2 = fileOutputStream2;
                                                    } catch (IndexOutOfBoundsException e3) {
                                                        -l_10_R = e3;
                                                        inputStream = -l_8_R;
                                                        outputStream = fileOutputStream2;
                                                        outputStream2 = fileOutputStream2;
                                                    } catch (Throwable th) {
                                                        -l_11_R = th;
                                                        inputStream = -l_8_R;
                                                        outputStream = fileOutputStream2;
                                                        outputStream2 = fileOutputStream2;
                                                    }
                                                } catch (IOException e4) {
                                                    -l_10_R = e4;
                                                    inputStream = -l_8_R;
                                                    outputStream = fileOutputStream2;
                                                    outputStream2 = fileOutputStream2;
                                                } catch (IllegalStateException e5) {
                                                    -l_10_R = e5;
                                                    inputStream = -l_8_R;
                                                    outputStream = fileOutputStream2;
                                                    outputStream2 = fileOutputStream2;
                                                } catch (IndexOutOfBoundsException e6) {
                                                    -l_10_R = e6;
                                                    inputStream = -l_8_R;
                                                    outputStream = fileOutputStream2;
                                                    outputStream2 = fileOutputStream2;
                                                } catch (Throwable th2) {
                                                    -l_11_R = th2;
                                                    inputStream = -l_8_R;
                                                    outputStream = fileOutputStream2;
                                                    outputStream2 = fileOutputStream2;
                                                }
                                            }
                                            if (inputStream2 != null) {
                                                try {
                                                    inputStream2.close();
                                                } catch (Object -l_10_R2) {
                                                    c.a("PushSelfShowLog", "zFileIn.close error:" + -l_10_R2.getMessage());
                                                }
                                            }
                                            if (-l_8_R != null) {
                                                try {
                                                    -l_8_R.close();
                                                } catch (Object -l_10_R22) {
                                                    c.a("PushSelfShowLog", "is.close error:" + -l_10_R22.getMessage());
                                                }
                                            }
                                            if (fileOutputStream2 != null) {
                                                try {
                                                    fileOutputStream2.close();
                                                } catch (Object -l_10_R222) {
                                                    c.a("PushSelfShowLog", "os.close error:" + -l_10_R222.getMessage());
                                                }
                                            }
                                            if (fileOutputStream2 != null) {
                                                try {
                                                    fileOutputStream2.close();
                                                } catch (Object -l_10_R2222) {
                                                    c.a("PushSelfShowLog", "tempFOS.close error:" + -l_10_R2222.getMessage());
                                                }
                                            }
                                            inputStream = -l_8_R;
                                            outputStream = fileOutputStream2;
                                            outputStream2 = fileOutputStream2;
                                        } catch (IOException e7) {
                                            -l_10_R2222 = e7;
                                            outputStream = fileOutputStream2;
                                            outputStream2 = fileOutputStream2;
                                            c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222.getMessage());
                                            if (inputStream2 != null) {
                                                try {
                                                    inputStream2.close();
                                                } catch (Object -l_10_R22222) {
                                                    c.a("PushSelfShowLog", "zFileIn.close error:" + -l_10_R22222.getMessage());
                                                }
                                            }
                                            if (inputStream != null) {
                                                try {
                                                    inputStream.close();
                                                } catch (Object -l_10_R222222) {
                                                    c.a("PushSelfShowLog", "is.close error:" + -l_10_R222222.getMessage());
                                                }
                                            }
                                            if (outputStream != null) {
                                                try {
                                                    outputStream.close();
                                                } catch (Object -l_10_R2222222) {
                                                    c.a("PushSelfShowLog", "os.close error:" + -l_10_R2222222.getMessage());
                                                }
                                            }
                                            if (fileOutputStream == null) {
                                                continue;
                                            } else {
                                                try {
                                                    fileOutputStream.close();
                                                } catch (Object -l_10_R22222222) {
                                                    c.a("PushSelfShowLog", "tempFOS.close error:" + -l_10_R22222222.getMessage());
                                                }
                                            }
                                        } catch (IllegalStateException e8) {
                                            -l_10_R22222222 = e8;
                                            outputStream = fileOutputStream2;
                                            outputStream2 = fileOutputStream2;
                                            c.a("PushSelfShowLog", "os.write error:" + -l_10_R22222222.getMessage());
                                            if (inputStream2 != null) {
                                                try {
                                                    inputStream2.close();
                                                } catch (Object -l_10_R222222222) {
                                                    c.a("PushSelfShowLog", "zFileIn.close error:" + -l_10_R222222222.getMessage());
                                                }
                                            }
                                            if (inputStream != null) {
                                                try {
                                                    inputStream.close();
                                                } catch (Object -l_10_R2222222222) {
                                                    c.a("PushSelfShowLog", "is.close error:" + -l_10_R2222222222.getMessage());
                                                }
                                            }
                                            if (outputStream != null) {
                                                try {
                                                    outputStream.close();
                                                } catch (Object -l_10_R22222222222) {
                                                    c.a("PushSelfShowLog", "os.close error:" + -l_10_R22222222222.getMessage());
                                                }
                                            }
                                            if (fileOutputStream == null) {
                                                continue;
                                            } else {
                                                try {
                                                    fileOutputStream.close();
                                                } catch (Object -l_10_R222222222222) {
                                                    c.a("PushSelfShowLog", "tempFOS.close error:" + -l_10_R222222222222.getMessage());
                                                }
                                            }
                                        } catch (IndexOutOfBoundsException e9) {
                                            -l_10_R222222222222 = e9;
                                            outputStream = fileOutputStream2;
                                            outputStream2 = fileOutputStream2;
                                            try {
                                                c.a("PushSelfShowLog", "os.write error:" + -l_10_R222222222222.getMessage());
                                                if (inputStream2 != null) {
                                                    try {
                                                        inputStream2.close();
                                                    } catch (Object -l_10_R2222222222222) {
                                                        c.a("PushSelfShowLog", "zFileIn.close error:" + -l_10_R2222222222222.getMessage());
                                                    }
                                                }
                                                if (inputStream != null) {
                                                    try {
                                                        inputStream.close();
                                                    } catch (Object -l_10_R22222222222222) {
                                                        c.a("PushSelfShowLog", "is.close error:" + -l_10_R22222222222222.getMessage());
                                                    }
                                                }
                                                if (outputStream != null) {
                                                    try {
                                                        outputStream.close();
                                                    } catch (Object -l_10_R222222222222222) {
                                                        c.a("PushSelfShowLog", "os.close error:" + -l_10_R222222222222222.getMessage());
                                                    }
                                                }
                                                if (fileOutputStream == null) {
                                                    continue;
                                                } else {
                                                    try {
                                                        fileOutputStream.close();
                                                    } catch (Object -l_10_R2222222222222222) {
                                                        c.a("PushSelfShowLog", "tempFOS.close error:" + -l_10_R2222222222222222.getMessage());
                                                    }
                                                }
                                            } catch (Throwable th3) {
                                                -l_11_R = th3;
                                            }
                                        } catch (Throwable th4) {
                                            -l_11_R = th4;
                                            outputStream = fileOutputStream2;
                                            outputStream2 = fileOutputStream2;
                                        }
                                    } catch (IOException e10) {
                                        -l_10_R2222222222222222 = e10;
                                        outputStream2 = fileOutputStream2;
                                        c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                        if (inputStream2 != null) {
                                            inputStream2.close();
                                        }
                                        if (inputStream != null) {
                                            inputStream.close();
                                        }
                                        if (outputStream != null) {
                                            outputStream.close();
                                        }
                                        if (fileOutputStream == null) {
                                            fileOutputStream.close();
                                        } else {
                                            continue;
                                        }
                                    } catch (IllegalStateException e11) {
                                        -l_10_R2222222222222222 = e11;
                                        outputStream2 = fileOutputStream2;
                                        c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                        if (inputStream2 != null) {
                                            inputStream2.close();
                                        }
                                        if (inputStream != null) {
                                            inputStream.close();
                                        }
                                        if (outputStream != null) {
                                            outputStream.close();
                                        }
                                        if (fileOutputStream == null) {
                                            fileOutputStream.close();
                                        } else {
                                            continue;
                                        }
                                    } catch (IndexOutOfBoundsException e12) {
                                        -l_10_R2222222222222222 = e12;
                                        outputStream2 = fileOutputStream2;
                                        c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                        if (inputStream2 != null) {
                                            inputStream2.close();
                                        }
                                        if (inputStream != null) {
                                            inputStream.close();
                                        }
                                        if (outputStream != null) {
                                            outputStream.close();
                                        }
                                        if (fileOutputStream == null) {
                                            fileOutputStream.close();
                                        } else {
                                            continue;
                                        }
                                    } catch (Throwable th5) {
                                        -l_11_R = th5;
                                        outputStream2 = fileOutputStream2;
                                    }
                                } catch (IOException e13) {
                                    -l_10_R2222222222222222 = e13;
                                    outputStream2 = fileOutputStream2;
                                    c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                    if (inputStream2 != null) {
                                        inputStream2.close();
                                    }
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    if (fileOutputStream == null) {
                                        fileOutputStream.close();
                                    } else {
                                        continue;
                                    }
                                } catch (IllegalStateException e14) {
                                    -l_10_R2222222222222222 = e14;
                                    outputStream2 = fileOutputStream2;
                                    c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                    if (inputStream2 != null) {
                                        inputStream2.close();
                                    }
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    if (fileOutputStream == null) {
                                        fileOutputStream.close();
                                    } else {
                                        continue;
                                    }
                                } catch (IndexOutOfBoundsException e15) {
                                    -l_10_R2222222222222222 = e15;
                                    outputStream2 = fileOutputStream2;
                                    c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                    if (inputStream2 != null) {
                                        inputStream2.close();
                                    }
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    if (fileOutputStream == null) {
                                        fileOutputStream.close();
                                    } else {
                                        continue;
                                    }
                                } catch (Throwable th6) {
                                    -l_11_R = th6;
                                    outputStream2 = fileOutputStream2;
                                }
                            } catch (IOException e16) {
                                -l_10_R2222222222222222 = e16;
                                c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                if (inputStream2 != null) {
                                    inputStream2.close();
                                }
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                if (fileOutputStream == null) {
                                    continue;
                                } else {
                                    fileOutputStream.close();
                                }
                            } catch (IllegalStateException e17) {
                                -l_10_R2222222222222222 = e17;
                                c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                if (inputStream2 != null) {
                                    inputStream2.close();
                                }
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                if (fileOutputStream == null) {
                                    continue;
                                } else {
                                    fileOutputStream.close();
                                }
                            } catch (IndexOutOfBoundsException e18) {
                                -l_10_R2222222222222222 = e18;
                                c.a("PushSelfShowLog", "os.write error:" + -l_10_R2222222222222222.getMessage());
                                if (inputStream2 != null) {
                                    inputStream2.close();
                                }
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                if (fileOutputStream == null) {
                                    continue;
                                } else {
                                    fileOutputStream.close();
                                }
                            }
                        }
                    }
                    c.c("PushSelfShowLog", "upZipFile, path is invalid!");
                    if (-l_1_R != null) {
                        try {
                            -l_1_R.close();
                        } catch (Object -l_5_R222) {
                            c.a("PushSelfShowLog", "zfile.close error:" + -l_5_R222.getMessage());
                        }
                    }
                    return;
                }
                if (-l_1_R != null) {
                    try {
                        -l_1_R.close();
                    } catch (Object -l_2_R2) {
                        c.a("PushSelfShowLog", "zfile.close error:" + -l_2_R2.getMessage());
                    }
                }
                zipFile = -l_1_R;
            } catch (ZipException e19) {
                -l_2_R2 = e19;
                zipFile = -l_1_R;
            } catch (IOException e20) {
                -l_2_R2 = e20;
                zipFile = -l_1_R;
            } catch (IllegalStateException e21) {
                -l_2_R2 = e21;
                zipFile = -l_1_R;
            } catch (NoSuchElementException e22) {
                -l_2_R2 = e22;
                zipFile = -l_1_R;
            } catch (Throwable th7) {
                -l_13_R = th7;
                zipFile = -l_1_R;
            }
        } catch (ZipException e23) {
            -l_2_R2 = e23;
            try {
                c.a("PushSelfShowLog", "upZipFile error:" + -l_2_R2.getMessage());
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Object -l_2_R22) {
                        c.a("PushSelfShowLog", "zfile.close error:" + -l_2_R22.getMessage());
                    }
                }
                return;
            } catch (Throwable th8) {
                Object -l_13_R;
                -l_13_R = th8;
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Object -l_14_R) {
                        c.a("PushSelfShowLog", "zfile.close error:" + -l_14_R.getMessage());
                    }
                }
                throw -l_13_R;
            }
        } catch (IOException e24) {
            -l_2_R22 = e24;
            c.a("PushSelfShowLog", "upZipFile error:" + -l_2_R22.getMessage());
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Object -l_2_R222) {
                    c.a("PushSelfShowLog", "zfile.close error:" + -l_2_R222.getMessage());
                }
            }
            return;
        } catch (IllegalStateException e25) {
            -l_2_R222 = e25;
            c.a("PushSelfShowLog", "upZipFile error:" + -l_2_R222.getMessage());
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Object -l_2_R2222) {
                    c.a("PushSelfShowLog", "zfile.close error:" + -l_2_R2222.getMessage());
                }
            }
            return;
        } catch (NoSuchElementException e26) {
            -l_2_R2222 = e26;
            c.a("PushSelfShowLog", "upZipFile error:" + -l_2_R2222.getMessage());
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Object -l_2_R22222) {
                    c.a("PushSelfShowLog", "zfile.close error:" + -l_2_R22222.getMessage());
                }
            }
            return;
        }
        return;
        if (inputStream2 != null) {
            try {
                inputStream2.close();
            } catch (Object -l_12_R) {
                c.a("PushSelfShowLog", "zFileIn.close error:" + -l_12_R.getMessage());
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Object -l_12_R2) {
                c.a("PushSelfShowLog", "is.close error:" + -l_12_R2.getMessage());
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Object -l_12_R22) {
                c.a("PushSelfShowLog", "os.close error:" + -l_12_R22.getMessage());
            }
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (Object -l_12_R222) {
                c.a("PushSelfShowLog", "tempFOS.close error:" + -l_12_R222.getMessage());
            }
        }
        throw -l_11_R;
        if (inputStream != null) {
            inputStream.close();
        }
        if (outputStream != null) {
            outputStream.close();
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
        throw -l_11_R;
        if (outputStream != null) {
            outputStream.close();
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
        throw -l_11_R;
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
        throw -l_11_R;
        throw -l_11_R;
    }
}
