package com.android.server.pm.auth.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {
    private static final int BUFFER_LENGTH = 4096;
    public static final String CERT_NAME = "META-INF/HUAWEI.CER";
    private static final String COMMON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int CR_LF_LENGTH = 2;
    private static final String HW_CER_NAME = "HUAWEI.CER";
    private static final String HW_CER_TAG = "Name: META-INF/HUAWEI.CER";
    private static final String LINE_SEPERATOR = "\r\n";
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    private static final String SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME = "X-Android-APK-Signed:";
    private static final String SF_ATTRIBUTE_ANDROID_APK_SIGNED_VALUE = "2";
    public static final String SF_CERT_NAME = "META-INF/CERT.SF";
    public static final String TAG = "HwCertificationManager";

    public static byte[] getManifestFile(File apkFile) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        ZipFile zipFile = null;
        InputStream inputStream = null;
        boolean catchFlag = false;
        try {
            ZipFile zipFile2 = new ZipFile(apkFile);
            Enumeration<?> enumeration = zipFile2.entries();
            while (true) {
                if (enumeration.hasMoreElements()) {
                    try {
                        ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
                        if (!zipEntry.isDirectory() && "META-INF/MANIFEST.MF".equals(zipEntry.getName())) {
                            InputStream inputStream2 = zipFile2.getInputStream(zipEntry);
                            while (true) {
                                int length = inputStream2.read(b);
                                if (length <= 0) {
                                    break;
                                }
                                os.write(b, 0, length);
                            }
                            catchFlag = true;
                            inputStream2.close();
                        }
                    } finally {
                        if (0 != 0) {
                            inputStream.close();
                        }
                    }
                }
            }
            try {
                zipFile2.close();
            } catch (IOException e) {
                HwAuthLogger.e("HwCertificationManager", "close stream failed when get manifest file");
            }
            return catchFlag ? os.toByteArray() : new byte[0];
        } catch (IOException e2) {
            HwAuthLogger.e("HwCertificationManager", "IOException in getManifestFile() in Utils.java ");
            if (0 != 0) {
                try {
                    zipFile.close();
                } catch (IOException e3) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when get manifest file");
                }
            }
            return null;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    zipFile.close();
                } catch (IOException e4) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when get manifest file");
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Multiple debug info for r6v20 'os'  java.io.ByteArrayOutputStream: [D('ManifestEntry' java.util.zip.ZipEntry), D('os' java.io.ByteArrayOutputStream)] */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x01e0 A[SYNTHETIC, Splitter:B:113:0x01e0] */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x01fb A[SYNTHETIC, Splitter:B:118:0x01fb] */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0216 A[SYNTHETIC, Splitter:B:123:0x0216] */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x0231 A[SYNTHETIC, Splitter:B:128:0x0231] */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x0259 A[SYNTHETIC, Splitter:B:137:0x0259] */
    /* JADX WARNING: Removed duplicated region for block: B:142:0x0274 A[SYNTHETIC, Splitter:B:142:0x0274] */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x028f A[SYNTHETIC, Splitter:B:147:0x028f] */
    /* JADX WARNING: Removed duplicated region for block: B:152:0x02aa A[SYNTHETIC, Splitter:B:152:0x02aa] */
    /* JADX WARNING: Removed duplicated region for block: B:163:? A[RETURN, SYNTHETIC] */
    public static byte[] getManifestFileNew(File apkFile) {
        Throwable th;
        ByteArrayOutputStream os = null;
        BufferedOutputStream bufOs = null;
        BufferedInputStream bufIn = null;
        int readLength = 4096;
        byte[] b = new byte[4096];
        ZipFile zipFile = null;
        InputStream inputStream = null;
        int readLength2 = 0;
        try {
            try {
                zipFile = new ZipFile(apkFile);
                ZipEntry ManifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
                if (ManifestEntry != null) {
                    try {
                        if (!ManifestEntry.isDirectory()) {
                            inputStream = zipFile.getInputStream(ManifestEntry);
                            bufIn = new BufferedInputStream(inputStream);
                            ByteArrayOutputStream os2 = new ByteArrayOutputStream();
                            try {
                                os = os2;
                                try {
                                    bufOs = new BufferedOutputStream(os);
                                    while (true) {
                                        try {
                                            int readLength3 = bufIn.read(b, 0, readLength);
                                            if (readLength3 <= 0) {
                                                break;
                                            }
                                            try {
                                                bufOs.write(b, 0, readLength3);
                                                readLength2 = readLength3;
                                                readLength = readLength;
                                            } catch (IOException e) {
                                                e = e;
                                                try {
                                                    StringBuilder sb = new StringBuilder();
                                                    try {
                                                        sb.append("IOException in getManifestFileNew, e is ");
                                                        sb.append(e);
                                                        HwAuthLogger.e("HwCertificationManager", sb.toString());
                                                        if (inputStream != null) {
                                                        }
                                                        if (bufIn != null) {
                                                        }
                                                        if (bufOs != null) {
                                                        }
                                                        if (zipFile == null) {
                                                        }
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                    }
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    if (inputStream != null) {
                                                    }
                                                    if (bufIn != null) {
                                                    }
                                                    if (bufOs != null) {
                                                    }
                                                    if (zipFile != null) {
                                                    }
                                                    throw th;
                                                }
                                            } catch (Throwable th4) {
                                                th = th4;
                                                if (inputStream != null) {
                                                }
                                                if (bufIn != null) {
                                                }
                                                if (bufOs != null) {
                                                }
                                                if (zipFile != null) {
                                                }
                                                throw th;
                                            }
                                        } catch (IOException e2) {
                                            e = e2;
                                            StringBuilder sb2 = new StringBuilder();
                                            sb2.append("IOException in getManifestFileNew, e is ");
                                            sb2.append(e);
                                            HwAuthLogger.e("HwCertificationManager", sb2.toString());
                                            if (inputStream != null) {
                                            }
                                            if (bufIn != null) {
                                            }
                                            if (bufOs != null) {
                                            }
                                            if (zipFile == null) {
                                            }
                                        } catch (Throwable th5) {
                                            th = th5;
                                            if (inputStream != null) {
                                            }
                                            if (bufIn != null) {
                                            }
                                            if (bufOs != null) {
                                            }
                                            if (zipFile != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    bufOs.flush();
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (IOException e3) {
                                            HwAuthLogger.e("HwCertificationManager", "can not close inputStream, e is " + e3);
                                        }
                                    }
                                    try {
                                        bufIn.close();
                                    } catch (IOException e4) {
                                        HwAuthLogger.e("HwCertificationManager", "can not close bufIn, e is " + e4);
                                    }
                                    try {
                                        bufOs.close();
                                    } catch (IOException e5) {
                                        HwAuthLogger.e("HwCertificationManager", "can not close bufOs, e is " + e5);
                                    }
                                    try {
                                        zipFile.close();
                                    } catch (IOException e6) {
                                        HwAuthLogger.e("HwCertificationManager", "can not close zipFile, e is " + e6);
                                    }
                                    return 1 == 1 ? os.toByteArray() : new byte[0];
                                } catch (IOException e7) {
                                    e = e7;
                                    StringBuilder sb22 = new StringBuilder();
                                    sb22.append("IOException in getManifestFileNew, e is ");
                                    sb22.append(e);
                                    HwAuthLogger.e("HwCertificationManager", sb22.toString());
                                    if (inputStream != null) {
                                    }
                                    if (bufIn != null) {
                                    }
                                    if (bufOs != null) {
                                    }
                                    if (zipFile == null) {
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    if (inputStream != null) {
                                    }
                                    if (bufIn != null) {
                                    }
                                    if (bufOs != null) {
                                    }
                                    if (zipFile != null) {
                                    }
                                    throw th;
                                }
                            } catch (IOException e8) {
                                e = e8;
                                os = os2;
                                StringBuilder sb222 = new StringBuilder();
                                sb222.append("IOException in getManifestFileNew, e is ");
                                sb222.append(e);
                                HwAuthLogger.e("HwCertificationManager", sb222.toString());
                                if (inputStream != null) {
                                }
                                if (bufIn != null) {
                                }
                                if (bufOs != null) {
                                }
                                if (zipFile == null) {
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                if (inputStream != null) {
                                }
                                if (bufIn != null) {
                                }
                                if (bufOs != null) {
                                }
                                if (zipFile != null) {
                                }
                                throw th;
                            }
                        }
                    } catch (IOException e9) {
                        e = e9;
                        os = null;
                        StringBuilder sb2222 = new StringBuilder();
                        sb2222.append("IOException in getManifestFileNew, e is ");
                        sb2222.append(e);
                        HwAuthLogger.e("HwCertificationManager", sb2222.toString());
                        if (inputStream != null) {
                        }
                        if (bufIn != null) {
                        }
                        if (bufOs != null) {
                        }
                        if (zipFile == null) {
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        if (inputStream != null) {
                        }
                        if (bufIn != null) {
                        }
                        if (bufOs != null) {
                        }
                        if (zipFile != null) {
                        }
                        throw th;
                    }
                }
                try {
                    HwAuthLogger.e("bailong", "can not find manifest.mf file.");
                    if (0 != 0) {
                        try {
                            inputStream.close();
                        } catch (IOException e10) {
                            HwAuthLogger.e("HwCertificationManager", "can not close inputStream, e is " + e10);
                        }
                    }
                    if (0 != 0) {
                        try {
                            bufIn.close();
                        } catch (IOException e11) {
                            HwAuthLogger.e("HwCertificationManager", "can not close bufIn, e is " + e11);
                        }
                    }
                    if (0 != 0) {
                        try {
                            bufOs.close();
                        } catch (IOException e12) {
                            HwAuthLogger.e("HwCertificationManager", "can not close bufOs, e is " + e12);
                        }
                    }
                    try {
                        zipFile.close();
                        return null;
                    } catch (IOException e13) {
                        HwAuthLogger.e("HwCertificationManager", "can not close zipFile, e is " + e13);
                        return null;
                    }
                } catch (IOException e14) {
                    e = e14;
                    os = null;
                } catch (Throwable th9) {
                    th = th9;
                    if (inputStream != null) {
                    }
                    if (bufIn != null) {
                    }
                    if (bufOs != null) {
                    }
                    if (zipFile != null) {
                    }
                    throw th;
                }
            } catch (IOException e15) {
                e = e15;
                StringBuilder sb22222 = new StringBuilder();
                sb22222.append("IOException in getManifestFileNew, e is ");
                sb22222.append(e);
                HwAuthLogger.e("HwCertificationManager", sb22222.toString());
                if (inputStream != null) {
                }
                if (bufIn != null) {
                }
                if (bufOs != null) {
                }
                if (zipFile == null) {
                }
            } catch (Throwable th10) {
                th = th10;
                th = th;
                if (inputStream != null) {
                }
                if (bufIn != null) {
                }
                if (bufOs != null) {
                }
                if (zipFile != null) {
                }
                throw th;
            }
        } catch (IOException e16) {
            e = e16;
            StringBuilder sb222222 = new StringBuilder();
            sb222222.append("IOException in getManifestFileNew, e is ");
            sb222222.append(e);
            HwAuthLogger.e("HwCertificationManager", sb222222.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e17) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream, e is " + e17);
                }
            }
            if (bufIn != null) {
                try {
                    bufIn.close();
                } catch (IOException e18) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufIn, e is " + e18);
                }
            }
            if (bufOs != null) {
                try {
                    bufOs.close();
                } catch (IOException e19) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufOs, e is " + e19);
                }
            }
            if (zipFile == null) {
                return null;
            }
            try {
                zipFile.close();
                return null;
            } catch (IOException e20) {
                HwAuthLogger.e("HwCertificationManager", "can not close zipFile, e is " + e20);
                return null;
            }
        } catch (Throwable th11) {
            th = th11;
            th = th;
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e21) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream, e is " + e21);
                }
            }
            if (bufIn != null) {
                try {
                    bufIn.close();
                } catch (IOException e22) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufIn, e is " + e22);
                }
            }
            if (bufOs != null) {
                try {
                    bufOs.close();
                } catch (IOException e23) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufOs, e is " + e23);
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e24) {
                    HwAuthLogger.e("HwCertificationManager", "can not close zipFile, e is " + e24);
                }
            }
            throw th;
        }
    }

    public static byte[] getManifestFileNew(ZipFile zfile, ZipEntry ManifestEntry) {
        if (zfile == null || ManifestEntry == null) {
            return new byte[0];
        }
        BufferedOutputStream bufOs = null;
        BufferedInputStream bufIn = null;
        byte[] b = new byte[4096];
        InputStream inputStream = null;
        try {
            InputStream inputStream2 = zfile.getInputStream(ManifestEntry);
            BufferedInputStream bufIn2 = new BufferedInputStream(inputStream2);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            BufferedOutputStream bufOs2 = new BufferedOutputStream(os);
            while (true) {
                int readLength = bufIn2.read(b, 0, 4096);
                if (readLength <= 0) {
                    break;
                }
                bufOs2.write(b, 0, readLength);
            }
            bufOs2.flush();
            if (inputStream2 != null) {
                try {
                    inputStream2.close();
                } catch (IOException e) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream, e is " + e);
                }
            }
            try {
                bufIn2.close();
            } catch (IOException e2) {
                HwAuthLogger.e("HwCertificationManager", "can not close bufIn, e is " + e2);
            }
            try {
                bufOs2.close();
            } catch (IOException e3) {
                HwAuthLogger.e("HwCertificationManager", "can not close bufOs, e is " + e3);
            }
            return 1 == 1 ? os.toByteArray() : new byte[0];
        } catch (IOException e4) {
            HwAuthLogger.e("HwCertificationManager", "IOException in getManifestFileNew, e is " + e4);
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream, e is " + e5);
                }
            }
            if (0 != 0) {
                try {
                    bufIn.close();
                } catch (IOException e6) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufIn, e is " + e6);
                }
            }
            if (0 != 0) {
                try {
                    bufOs.close();
                } catch (IOException e7) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufOs, e is " + e7);
                }
            }
            return null;
        } catch (Throwable e8) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e9) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream, e is " + e9);
                }
            }
            if (0 != 0) {
                try {
                    bufIn.close();
                } catch (IOException e10) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufIn, e is " + e10);
                }
            }
            if (0 != 0) {
                try {
                    bufOs.close();
                } catch (IOException e11) {
                    HwAuthLogger.e("HwCertificationManager", "can not close bufOs, e is " + e11);
                }
            }
            throw e8;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:?, code lost:
        r7.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0060, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "can not close br in isUsingSignatureSchemaV2");
     */
    public static boolean isUsingSignatureSchemaV2(ZipFile zFile, ZipEntry entry) {
        InputStream inputStream;
        String line;
        if (zFile == null || entry == null || entry.isDirectory()) {
            HwAuthLogger.e("HwCertificationManager", "isUsingSignatureSchemaV2 input is null");
            return false;
        }
        boolean catchFlag = false;
        InputStream inputStream2 = null;
        BufferedReader br = null;
        try {
            inputStream = zFile.getInputStream(entry);
            BufferedReader br2 = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            int i = 0;
            while (true) {
                if (i < 10 && (line = br2.readLine()) != null) {
                    if (line.startsWith(SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME)) {
                        String[] attr = line.split(AwarenessInnerConstants.COLON_KEY);
                        if (attr.length == 2 && attr[1].trim().equals("2")) {
                            catchFlag = true;
                        }
                    } else {
                        i++;
                    }
                }
            }
        } catch (IOException e) {
            HwAuthLogger.e("HwCertificationManager", "IOException happened in isUsingSignatureSchemaV2, e is" + e);
            if (0 != 0) {
                try {
                    br.close();
                } catch (IOException e2) {
                    HwAuthLogger.e("HwCertificationManager", "can not close br in isUsingSignatureSchemaV2");
                }
            }
            if (0 != 0) {
                inputStream2.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    br.close();
                } catch (IOException e3) {
                    HwAuthLogger.e("HwCertificationManager", "can not close br in isUsingSignatureSchemaV2");
                }
            }
            if (0 != 0) {
                try {
                    inputStream2.close();
                } catch (IOException e4) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream in isUsingSignatureSchemaV2");
                }
            }
            throw th;
        }
        return catchFlag;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e5) {
                HwAuthLogger.e("HwCertificationManager", "can not close inputStream in isUsingSignatureSchemaV2");
            }
        }
        return catchFlag;
    }

    /* JADX INFO: Multiple debug info for r14v17 'line'  java.lang.String: [D('line' java.lang.String), D('namesList' java.util.ArrayList<java.lang.String>)] */
    /* JADX INFO: Multiple debug info for r12v19 java.lang.String: [D('hwcerLen' int), D('nextName' java.lang.String)] */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00bf, code lost:
        r16 = r12;
        r13 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:?, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.w("HwCertificationManager", "find next block name.");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x00c8, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x00c9, code lost:
        r1 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x00d0, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x00d1, code lost:
        r7 = r25;
     */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x033b A[SYNTHETIC, Splitter:B:153:0x033b] */
    /* JADX WARNING: Removed duplicated region for block: B:158:0x0356 A[SYNTHETIC, Splitter:B:158:0x0356] */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x0371 A[SYNTHETIC, Splitter:B:163:0x0371] */
    /* JADX WARNING: Removed duplicated region for block: B:171:0x0393 A[SYNTHETIC, Splitter:B:171:0x0393] */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x03ae A[SYNTHETIC, Splitter:B:176:0x03ae] */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x03c9 A[SYNTHETIC, Splitter:B:181:0x03c9] */
    @SuppressLint({"PreferForInArrayList"})
    public static byte[] readManifestAndSkipHwTag(byte[] manifest) {
        ByteArrayInputStream byteIn;
        BufferedReader bufferReader;
        Throwable th;
        ByteArrayOutputStream byteOut;
        ArrayList<String> namesList;
        int hwcerLen;
        String line;
        String nextName;
        ByteArrayOutputStream byteOut2 = null;
        BufferedWriter bufferWriter = null;
        ArrayList<String> namesList2 = null;
        ArrayList<String> contentsList = new ArrayList<>();
        String line2 = null;
        boolean findHwCer = false;
        if (manifest == null) {
            HwAuthLogger.e("HwCertificationManager", "manifest is null in readManifestAndSkipHwTag.");
            return new byte[0];
        }
        try {
            long readbegin = System.currentTimeMillis();
            byteOut2 = new ByteArrayOutputStream();
            try {
            } catch (IOException e) {
                e = e;
                byteIn = null;
                bufferWriter = null;
                bufferReader = null;
                try {
                    HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                    byte[] bArr = new byte[0];
                    if (bufferWriter != null) {
                    }
                    if (byteIn != null) {
                    }
                    if (bufferReader != null) {
                    }
                    return bArr;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferWriter != null) {
                    }
                    if (byteIn != null) {
                    }
                    if (bufferReader != null) {
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                byteIn = null;
                bufferWriter = null;
                bufferReader = null;
                if (bufferWriter != null) {
                }
                if (byteIn != null) {
                }
                if (bufferReader != null) {
                }
                throw th;
            }
            try {
                bufferWriter = new BufferedWriter(new OutputStreamWriter(byteOut2, "UTF-8"));
                try {
                    byteIn = new ByteArrayInputStream(manifest);
                    try {
                        try {
                            bufferReader = new BufferedReader(new InputStreamReader(byteIn, "UTF-8"));
                            try {
                                int hwcerLen2 = HW_CER_TAG.length();
                                while (true) {
                                    String readLine = bufferReader.readLine();
                                    line2 = readLine;
                                    if (readLine == null) {
                                        break;
                                    }
                                    try {
                                        if (hwcerLen2 != line2.length() || findHwCer) {
                                            byteOut = byteOut2;
                                            hwcerLen = hwcerLen2;
                                            namesList = namesList2;
                                            line = line2;
                                        } else {
                                            namesList = namesList2;
                                            line = line2;
                                            try {
                                                byteOut = byteOut2;
                                                if (line.charAt(hwcerLen2 - 3) == 'C') {
                                                    try {
                                                        if (line.charAt(hwcerLen2 - 2) == 'E' && line.charAt(hwcerLen2 - 1) == 'R') {
                                                            if (line.lastIndexOf(HW_CER_NAME) != -1) {
                                                                findHwCer = true;
                                                                HwAuthLogger.w("HwCertificationManager", "find HwCer tag");
                                                                boolean findNextName = false;
                                                                while (true) {
                                                                    String nextName2 = bufferReader.readLine();
                                                                    if (nextName2 == null) {
                                                                        hwcerLen = hwcerLen2;
                                                                        nextName = nextName2;
                                                                        line2 = line;
                                                                        break;
                                                                    }
                                                                    hwcerLen = hwcerLen2;
                                                                    nextName = nextName2;
                                                                    try {
                                                                        if (nextName.startsWith("Name:")) {
                                                                            break;
                                                                        }
                                                                        hwcerLen2 = hwcerLen;
                                                                    } catch (IOException e2) {
                                                                        e = e2;
                                                                        line2 = line;
                                                                        byteOut2 = byteOut;
                                                                        HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                                                                        byte[] bArr2 = new byte[0];
                                                                        if (bufferWriter != null) {
                                                                        }
                                                                        if (byteIn != null) {
                                                                        }
                                                                        if (bufferReader != null) {
                                                                        }
                                                                        return bArr2;
                                                                    } catch (Throwable th4) {
                                                                        th = th4;
                                                                        if (bufferWriter != null) {
                                                                        }
                                                                        if (byteIn != null) {
                                                                        }
                                                                        if (bufferReader != null) {
                                                                        }
                                                                        throw th;
                                                                    }
                                                                }
                                                                if (!findNextName) {
                                                                    hwcerLen2 = hwcerLen;
                                                                    namesList2 = namesList;
                                                                    byteOut2 = byteOut;
                                                                } else {
                                                                    line = line2;
                                                                }
                                                            } else {
                                                                hwcerLen = hwcerLen2;
                                                            }
                                                        }
                                                    } catch (IOException e3) {
                                                        e = e3;
                                                        line2 = line;
                                                        byteOut2 = byteOut;
                                                        HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                                                        byte[] bArr22 = new byte[0];
                                                        if (bufferWriter != null) {
                                                        }
                                                        if (byteIn != null) {
                                                        }
                                                        if (bufferReader != null) {
                                                        }
                                                        return bArr22;
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        if (bufferWriter != null) {
                                                        }
                                                        if (byteIn != null) {
                                                        }
                                                        if (bufferReader != null) {
                                                        }
                                                        throw th;
                                                    }
                                                }
                                                hwcerLen = hwcerLen2;
                                            } catch (IOException e4) {
                                                e = e4;
                                                line2 = line;
                                                HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                                                byte[] bArr222 = new byte[0];
                                                if (bufferWriter != null) {
                                                }
                                                if (byteIn != null) {
                                                }
                                                if (bufferReader != null) {
                                                }
                                                return bArr222;
                                            } catch (Throwable th6) {
                                                th = th6;
                                                if (bufferWriter != null) {
                                                }
                                                if (byteIn != null) {
                                                }
                                                if (bufferReader != null) {
                                                }
                                                throw th;
                                            }
                                        }
                                        if (line.length() != 0) {
                                            contentsList.add(line);
                                        }
                                        hwcerLen2 = hwcerLen;
                                        namesList2 = namesList;
                                        byteOut2 = byteOut;
                                    } catch (IOException e5) {
                                        e = e5;
                                        HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                                        byte[] bArr2222 = new byte[0];
                                        if (bufferWriter != null) {
                                            try {
                                                bufferWriter.close();
                                            } catch (IOException e6) {
                                                HwAuthLogger.i("HwCertificationManager", "bufferWriter.close, e is " + e6);
                                            }
                                        }
                                        if (byteIn != null) {
                                            try {
                                                byteIn.close();
                                            } catch (IOException e7) {
                                                HwAuthLogger.i("HwCertificationManager", "byteIn.close, e is " + e7);
                                            }
                                        }
                                        if (bufferReader != null) {
                                            try {
                                                bufferReader.close();
                                            } catch (IOException e8) {
                                                HwAuthLogger.i("HwCertificationManager", "bufferReader.close, e is " + e8);
                                            }
                                        }
                                        return bArr2222;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        if (bufferWriter != null) {
                                            try {
                                                bufferWriter.close();
                                            } catch (IOException e9) {
                                                HwAuthLogger.i("HwCertificationManager", "bufferWriter.close, e is " + e9);
                                            }
                                        }
                                        if (byteIn != null) {
                                            try {
                                                byteIn.close();
                                            } catch (IOException e10) {
                                                HwAuthLogger.i("HwCertificationManager", "byteIn.close, e is " + e10);
                                            }
                                        }
                                        if (bufferReader != null) {
                                            try {
                                                bufferReader.close();
                                            } catch (IOException e11) {
                                                HwAuthLogger.i("HwCertificationManager", "bufferReader.close, e is " + e11);
                                            }
                                        }
                                        throw th;
                                    }
                                }
                                if (!findHwCer) {
                                    try {
                                        bufferWriter.close();
                                    } catch (IOException e12) {
                                        HwAuthLogger.i("HwCertificationManager", "bufferWriter.close, e is " + e12);
                                    }
                                    try {
                                        byteIn.close();
                                    } catch (IOException e13) {
                                        HwAuthLogger.i("HwCertificationManager", "byteIn.close, e is " + e13);
                                    }
                                    try {
                                        bufferReader.close();
                                    } catch (IOException e14) {
                                        HwAuthLogger.i("HwCertificationManager", "bufferReader.close, e is " + e14);
                                    }
                                    return manifest;
                                }
                                Collections.sort(contentsList);
                                Iterator<String> it = contentsList.iterator();
                                while (it.hasNext()) {
                                    String name = it.next();
                                    bufferWriter.write(name, 0, name.length());
                                    bufferWriter.write(LINE_SEPERATOR, 0, LINE_SEPERATOR.length());
                                }
                                bufferWriter.flush();
                                HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, cost time is " + (System.currentTimeMillis() - readbegin));
                                try {
                                    bufferWriter.close();
                                } catch (IOException e15) {
                                    HwAuthLogger.i("HwCertificationManager", "bufferWriter.close, e is " + e15);
                                }
                                try {
                                    byteIn.close();
                                } catch (IOException e16) {
                                    HwAuthLogger.i("HwCertificationManager", "byteIn.close, e is " + e16);
                                }
                                try {
                                    bufferReader.close();
                                } catch (IOException e17) {
                                    HwAuthLogger.i("HwCertificationManager", "bufferReader.close, e is " + e17);
                                }
                                return byteOut2.toByteArray();
                            } catch (IOException e18) {
                                e = e18;
                                HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                                byte[] bArr22222 = new byte[0];
                                if (bufferWriter != null) {
                                }
                                if (byteIn != null) {
                                }
                                if (bufferReader != null) {
                                }
                                return bArr22222;
                            } catch (Throwable th8) {
                                th = th8;
                                if (bufferWriter != null) {
                                }
                                if (byteIn != null) {
                                }
                                if (bufferReader != null) {
                                }
                                throw th;
                            }
                        } catch (IOException e19) {
                            e = e19;
                            bufferReader = null;
                            HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                            byte[] bArr222222 = new byte[0];
                            if (bufferWriter != null) {
                            }
                            if (byteIn != null) {
                            }
                            if (bufferReader != null) {
                            }
                            return bArr222222;
                        } catch (Throwable th9) {
                            th = th9;
                            bufferReader = null;
                            if (bufferWriter != null) {
                            }
                            if (byteIn != null) {
                            }
                            if (bufferReader != null) {
                            }
                            throw th;
                        }
                    } catch (IOException e20) {
                        e = e20;
                        bufferReader = null;
                        HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                        byte[] bArr2222222 = new byte[0];
                        if (bufferWriter != null) {
                        }
                        if (byteIn != null) {
                        }
                        if (bufferReader != null) {
                        }
                        return bArr2222222;
                    } catch (Throwable th10) {
                        th = th10;
                        bufferReader = null;
                        if (bufferWriter != null) {
                        }
                        if (byteIn != null) {
                        }
                        if (bufferReader != null) {
                        }
                        throw th;
                    }
                } catch (IOException e21) {
                    e = e21;
                    byteIn = null;
                    bufferReader = null;
                    HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                    byte[] bArr22222222 = new byte[0];
                    if (bufferWriter != null) {
                    }
                    if (byteIn != null) {
                    }
                    if (bufferReader != null) {
                    }
                    return bArr22222222;
                } catch (Throwable th11) {
                    th = th11;
                    byteIn = null;
                    bufferReader = null;
                    if (bufferWriter != null) {
                    }
                    if (byteIn != null) {
                    }
                    if (bufferReader != null) {
                    }
                    throw th;
                }
            } catch (IOException e22) {
                e = e22;
                byteIn = null;
                bufferWriter = null;
                bufferReader = null;
                HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
                byte[] bArr222222222 = new byte[0];
                if (bufferWriter != null) {
                }
                if (byteIn != null) {
                }
                if (bufferReader != null) {
                }
                return bArr222222222;
            } catch (Throwable th12) {
                th = th12;
                byteIn = null;
                bufferWriter = null;
                bufferReader = null;
                if (bufferWriter != null) {
                }
                if (byteIn != null) {
                }
                if (bufferReader != null) {
                }
                throw th;
            }
        } catch (IOException e23) {
            e = e23;
            byteIn = null;
            bufferReader = null;
            HwAuthLogger.i("HwCertificationManager", "readManifestAndSkipHwTag, e is " + e);
            byte[] bArr2222222222 = new byte[0];
            if (bufferWriter != null) {
            }
            if (byteIn != null) {
            }
            if (bufferReader != null) {
            }
            return bArr2222222222;
        } catch (Throwable th13) {
            th = th13;
            byteIn = null;
            bufferReader = null;
            if (bufferWriter != null) {
            }
            if (byteIn != null) {
            }
            if (bufferReader != null) {
            }
            throw th;
        }
    }

    public static byte[] getManifestFileWithoutHwCER(ZipFile zfile, ZipEntry entry) {
        if (zfile == null || entry == null) {
            return new byte[0];
        }
        long begin = System.currentTimeMillis();
        byte[] manifest = getManifestFileNew(zfile, entry);
        HwAuthLogger.i("HwCertificationManager", "getManifestFileNew cost, cost time is  " + (System.currentTimeMillis() - begin));
        byte[] out = readManifestAndSkipHwTag(manifest);
        HwAuthLogger.i("HwCertificationManager", "getManifestFileWithoutHwCER begin, cost time is  " + (System.currentTimeMillis() - begin));
        return out;
    }

    public static byte[] getManifestFileWithoutHwCER(File apkFile) {
        long begin = System.currentTimeMillis();
        byte[] manifest = getManifestFileNew(apkFile);
        HwAuthLogger.i("HwCertificationManager", "getManifestFileWithoutHwCER begin, cost time is  " + (System.currentTimeMillis() - begin));
        return manifest;
    }

    public static InputStream readHwCertFromApk(String apkPath) {
        ZipFile zipFile = null;
        InputStream input = null;
        try {
            ZipFile zipFile2 = new ZipFile(new File(apkPath));
            Enumeration enumeration = zipFile2.entries();
            while (true) {
                if (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
                    if (!zipEntry.isDirectory() && CERT_NAME.equals(zipEntry.getName())) {
                        input = zipFile2.getInputStream(zipEntry);
                        break;
                    }
                }
            }
            try {
                zipFile2.close();
            } catch (IOException e) {
                HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
            }
            return input;
        } catch (IOException e2) {
            HwAuthLogger.e("HwCertificationManager", "IOException in readHwCertFromApk() in Utils.java ");
            if (0 != 0) {
                try {
                    input.close();
                } catch (IOException e3) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
                }
            }
            if (0 != 0) {
                try {
                    zipFile.close();
                } catch (IOException e4) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
                }
            }
            return null;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    zipFile.close();
                } catch (IOException e5) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
                }
            }
            throw th;
        }
    }

    public static InputStream readHwCertFromApk(ZipFile zfile, ZipEntry entry) {
        if (zfile == null || entry == null) {
            return null;
        }
        InputStream input = new ByteArrayInputStream(new byte[0]);
        try {
            if (entry.isDirectory() || !CERT_NAME.equals(entry.getName())) {
                return input;
            }
            return zfile.getInputStream(entry);
        } catch (IOException e) {
            HwAuthLogger.e("HwCertificationManager", "IOException in readHwCertFromApk() in Utils.java ");
            try {
                input.close();
            } catch (IOException e2) {
                HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
            }
            return null;
        }
    }

    public static byte[] stringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[(len / 2)];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToString(byte[] bytes) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] chars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int byteValue = bytes[j] & 255;
            chars[j * 2] = hexChars[byteValue >>> 4];
            chars[(j * 2) + 1] = hexChars[byteValue & 15];
        }
        return new String(chars);
    }

    public static Date convertStringToDate(String dateString) throws ParseException {
        return new SimpleDateFormat(COMMON_DATE_FORMAT).parse(dateString);
    }

    public static String convertDateToString(Date from, Date to) {
        if (from == null || to == null) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(COMMON_DATE_FORMAT);
        String fromString = dateFormat.format(from);
        String toString = dateFormat.format(to);
        return "from " + fromString + " to " + toString;
    }

    public static boolean isPackageInstalled(String packagename, Context context) {
        try {
            context.getPackageManager().getPackageInfo(packagename, 1);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isMultiSimEnabled() {
        try {
            return TelephonyManager.getDefault().isMultiSimEnabled();
        } catch (Exception e) {
            HwAuthLogger.w("HwCertificationManager", "isMultiSimEnabled Exception.");
            return false;
        }
    }

    public static boolean isCDMAPhone(int phoneType) {
        return 2 == phoneType;
    }

    public static String getSfFileName(ZipFile zFile) {
        if (zFile.getEntry(SF_CERT_NAME) != null) {
            return SF_CERT_NAME;
        }
        Enumeration<? extends ZipEntry> entries = zFile.entries();
        while (entries.hasMoreElements()) {
            String zipFileNames = ((ZipEntry) entries.nextElement()).getName();
            if (zipFileNames.contains(".SF") && zipFileNames.contains("META-INF/")) {
                HwAuthLogger.i("HwCertificationManager", "The SF file name is " + zipFileNames);
                return zipFileNames;
            }
        }
        return null;
    }
}
