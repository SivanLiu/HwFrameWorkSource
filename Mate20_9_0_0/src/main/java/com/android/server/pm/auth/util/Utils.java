package com.android.server.pm.auth.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
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
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
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
            zipFile = new ZipFile(apkFile);
            Enumeration<?> enumeration = zipFile.entries();
            ZipEntry zipEntry = null;
            while (enumeration.hasMoreElements()) {
                zipEntry = (ZipEntry) enumeration.nextElement();
                if (!zipEntry.isDirectory() && "META-INF/MANIFEST.MF".equals(zipEntry.getName())) {
                    inputStream = zipFile.getInputStream(zipEntry);
                    while (true) {
                        int read = inputStream.read(b);
                        int length = read;
                        if (read <= 0) {
                            break;
                        }
                        os.write(b, 0, length);
                    }
                    catchFlag = true;
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } else if (inputStream != null) {
                    inputStream.close();
                }
            }
            try {
                zipFile.close();
            } catch (IOException e) {
                HwAuthLogger.e("HwCertificationManager", "close stream failed when get manifest file");
            }
            return catchFlag ? os.toByteArray() : new byte[0];
        } catch (IOException e2) {
            try {
                HwAuthLogger.e("HwCertificationManager", "IOException in getManifestFile() in Utils.java ");
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e3) {
                        HwAuthLogger.e("HwCertificationManager", "close stream failed when get manifest file");
                    }
                }
                return null;
            } catch (Throwable th) {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e4) {
                        HwAuthLogger.e("HwCertificationManager", "close stream failed when get manifest file");
                    }
                }
            }
        } catch (Throwable th2) {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static byte[] getManifestFileNew(File apkFile) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        BufferedOutputStream bufOs = null;
        BufferedInputStream bufIn = null;
        byte[] b = new byte[4096];
        ZipFile zipFile = null;
        InputStream inputStream = null;
        boolean catchFlag = false;
        try {
            zipFile = new ZipFile(apkFile);
            ZipEntry ManifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (ManifestEntry == null || ManifestEntry.isDirectory()) {
                HwAuthLogger.e("bailong", "can not find manifest.mf file.");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("can not close inputStream, e is ");
                        stringBuilder.append(e);
                        HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                    }
                }
                if (bufIn != null) {
                    try {
                        bufIn.close();
                    } catch (IOException e2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("can not close bufIn, e is ");
                        stringBuilder.append(e2);
                        HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                    }
                }
                if (bufOs != null) {
                    try {
                        bufOs.close();
                    } catch (IOException e22) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("can not close bufOs, e is ");
                        stringBuilder.append(e22);
                        HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                    }
                }
                try {
                    zipFile.close();
                } catch (IOException e222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close zipFile, e is ");
                    stringBuilder.append(e222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
                return null;
            }
            inputStream = zipFile.getInputStream(ManifestEntry);
            bufIn = new BufferedInputStream(inputStream);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bufOs = new BufferedOutputStream(os);
            while (true) {
                int read = bufIn.read(b, 0, 4096);
                int readLength = read;
                if (read <= 0) {
                    break;
                }
                bufOs.write(b, 0, readLength);
            }
            bufOs.flush();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("can not close inputStream, e is ");
                    stringBuilder2.append(e3);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
                }
            }
            try {
                bufIn.close();
            } catch (IOException e32) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("can not close bufIn, e is ");
                stringBuilder2.append(e32);
                HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
            }
            try {
                bufOs.close();
            } catch (IOException e322) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("can not close bufOs, e is ");
                stringBuilder2.append(e322);
                HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
            }
            try {
                zipFile.close();
            } catch (IOException e3222) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("can not close zipFile, e is ");
                stringBuilder2.append(e3222);
                HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
            }
            return true ? os.toByteArray() : new byte[0];
        } catch (IOException e2222) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("IOException in getManifestFileNew, e is ");
            stringBuilder2.append(e2222);
            HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close inputStream, e is ");
                    stringBuilder.append(e4);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
            }
            if (bufIn != null) {
                try {
                    bufIn.close();
                } catch (IOException e42) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close bufIn, e is ");
                    stringBuilder.append(e42);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
            }
            if (bufOs != null) {
                try {
                    bufOs.close();
                } catch (IOException e422) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close bufOs, e is ");
                    stringBuilder.append(e422);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e4222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close zipFile, e is ");
                    stringBuilder.append(e4222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
            }
            return null;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e32222) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("can not close inputStream, e is ");
                    stringBuilder2.append(e32222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
                }
            }
            if (bufIn != null) {
                try {
                    bufIn.close();
                } catch (IOException e322222) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("can not close bufIn, e is ");
                    stringBuilder2.append(e322222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
                }
            }
            if (bufOs != null) {
                try {
                    bufOs.close();
                } catch (IOException e3222222) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("can not close bufOs, e is ");
                    stringBuilder2.append(e3222222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e32222222) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("can not close zipFile, e is ");
                    stringBuilder3.append(e32222222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder3.toString());
                }
            }
        }
    }

    public static byte[] getManifestFileNew(ZipFile zfile, ZipEntry ManifestEntry) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3;
        if (zfile == null || ManifestEntry == null) {
            return new byte[0];
        }
        BufferedOutputStream bufOs = null;
        BufferedInputStream bufIn = null;
        byte[] b = new byte[4096];
        InputStream inputStream = null;
        boolean catchFlag = false;
        try {
            inputStream = zfile.getInputStream(ManifestEntry);
            bufIn = new BufferedInputStream(inputStream);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bufOs = new BufferedOutputStream(os);
            while (true) {
                int read = bufIn.read(b, 0, 4096);
                int readLength = read;
                if (read <= 0) {
                    break;
                }
                bufOs.write(b, 0, readLength);
            }
            bufOs.flush();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close inputStream, e is ");
                    stringBuilder.append(e);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
            }
            try {
                bufIn.close();
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("can not close bufIn, e is ");
                stringBuilder.append(e2);
                HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            }
            try {
                bufOs.close();
            } catch (IOException e22) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("can not close bufOs, e is ");
                stringBuilder.append(e22);
                HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            }
            return true ? os.toByteArray() : new byte[0];
        } catch (IOException e3) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("IOException in getManifestFileNew, e is ");
            stringBuilder2.append(e3);
            HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("can not close inputStream, e is ");
                    stringBuilder3.append(e4);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder3.toString());
                }
            }
            if (bufIn != null) {
                try {
                    bufIn.close();
                } catch (IOException e42) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("can not close bufIn, e is ");
                    stringBuilder3.append(e42);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder3.toString());
                }
            }
            if (bufOs != null) {
                try {
                    bufOs.close();
                } catch (IOException e422) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("can not close bufOs, e is ");
                    stringBuilder3.append(e422);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder3.toString());
                }
            }
            return null;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close inputStream, e is ");
                    stringBuilder.append(e222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
            }
            if (bufIn != null) {
                try {
                    bufIn.close();
                } catch (IOException e2222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("can not close bufIn, e is ");
                    stringBuilder.append(e2222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                }
            }
            if (bufOs != null) {
                try {
                    bufOs.close();
                } catch (IOException e22222) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("can not close bufOs, e is ");
                    stringBuilder2.append(e22222);
                    HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
                }
            }
        }
    }

    public static boolean isUsingSignatureSchemaV2(ZipFile zFile, ZipEntry entry) {
        int i = 0;
        if (zFile == null || entry == null || entry.isDirectory()) {
            HwAuthLogger.e("HwCertificationManager", "isUsingSignatureSchemaV2 input is null");
            return false;
        }
        boolean catchFlag = false;
        InputStream inputStream = null;
        BufferedReader br = null;
        try {
            inputStream = zFile.getInputStream(entry);
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while (i < 10) {
                String readLine = br.readLine();
                String line = readLine;
                if (readLine != null) {
                    if (line.startsWith(SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME)) {
                        String[] attr = line.split(":");
                        if (attr.length == 2 && attr[1].trim().equals("2")) {
                            catchFlag = true;
                        }
                    } else {
                        i++;
                    }
                }
            }
            try {
                br.close();
            } catch (IOException e) {
                HwAuthLogger.e("HwCertificationManager", "can not close br in isUsingSignatureSchemaV2");
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e2) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream in isUsingSignatureSchemaV2");
                }
            }
        } catch (IOException e3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOException happened in isUsingSignatureSchemaV2, e is");
            stringBuilder.append(e3);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e4) {
                    HwAuthLogger.e("HwCertificationManager", "can not close br in isUsingSignatureSchemaV2");
                }
            }
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Throwable th) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e5) {
                    HwAuthLogger.e("HwCertificationManager", "can not close br in isUsingSignatureSchemaV2");
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e6) {
                    HwAuthLogger.e("HwCertificationManager", "can not close inputStream in isUsingSignatureSchemaV2");
                }
            }
        }
        return catchFlag;
    }

    /* JADX WARNING: Removed duplicated region for block: B:136:0x02e5 A:{SYNTHETIC, Splitter: B:136:0x02e5} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0303 A:{SYNTHETIC, Splitter: B:141:0x0303} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0321 A:{SYNTHETIC, Splitter: B:146:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0288 A:{SYNTHETIC, Splitter: B:118:0x0288} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x02a6 A:{SYNTHETIC, Splitter: B:123:0x02a6} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x02c4 A:{SYNTHETIC, Splitter: B:128:0x02c4} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02e5 A:{SYNTHETIC, Splitter: B:136:0x02e5} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0303 A:{SYNTHETIC, Splitter: B:141:0x0303} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0321 A:{SYNTHETIC, Splitter: B:146:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0288 A:{SYNTHETIC, Splitter: B:118:0x0288} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x02a6 A:{SYNTHETIC, Splitter: B:123:0x02a6} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x02c4 A:{SYNTHETIC, Splitter: B:128:0x02c4} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02e5 A:{SYNTHETIC, Splitter: B:136:0x02e5} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0303 A:{SYNTHETIC, Splitter: B:141:0x0303} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0321 A:{SYNTHETIC, Splitter: B:146:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0288 A:{SYNTHETIC, Splitter: B:118:0x0288} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x02a6 A:{SYNTHETIC, Splitter: B:123:0x02a6} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x02c4 A:{SYNTHETIC, Splitter: B:128:0x02c4} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0288 A:{SYNTHETIC, Splitter: B:118:0x0288} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x02a6 A:{SYNTHETIC, Splitter: B:123:0x02a6} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x02c4 A:{SYNTHETIC, Splitter: B:128:0x02c4} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02e5 A:{SYNTHETIC, Splitter: B:136:0x02e5} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0303 A:{SYNTHETIC, Splitter: B:141:0x0303} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0321 A:{SYNTHETIC, Splitter: B:146:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0288 A:{SYNTHETIC, Splitter: B:118:0x0288} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x02a6 A:{SYNTHETIC, Splitter: B:123:0x02a6} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x02c4 A:{SYNTHETIC, Splitter: B:128:0x02c4} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02e5 A:{SYNTHETIC, Splitter: B:136:0x02e5} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0303 A:{SYNTHETIC, Splitter: B:141:0x0303} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0321 A:{SYNTHETIC, Splitter: B:146:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0288 A:{SYNTHETIC, Splitter: B:118:0x0288} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x02a6 A:{SYNTHETIC, Splitter: B:123:0x02a6} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x02c4 A:{SYNTHETIC, Splitter: B:128:0x02c4} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02e5 A:{SYNTHETIC, Splitter: B:136:0x02e5} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0303 A:{SYNTHETIC, Splitter: B:141:0x0303} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0321 A:{SYNTHETIC, Splitter: B:146:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0288 A:{SYNTHETIC, Splitter: B:118:0x0288} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x02a6 A:{SYNTHETIC, Splitter: B:123:0x02a6} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x02c4 A:{SYNTHETIC, Splitter: B:128:0x02c4} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02e5 A:{SYNTHETIC, Splitter: B:136:0x02e5} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0303 A:{SYNTHETIC, Splitter: B:141:0x0303} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0321 A:{SYNTHETIC, Splitter: B:146:0x0321} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @SuppressLint({"PreferForInArrayList"})
    public static byte[] readManifestAndSkipHwTag(byte[] manifest) {
        ByteArrayInputStream byteIn;
        IOException e;
        BufferedReader bufferedReader;
        Map<String, Attributes> map;
        ArrayList<String> arrayList;
        IOException e2;
        StringBuilder stringBuilder;
        byte[] bArr;
        Throwable th;
        IOException iOException;
        StringBuilder stringBuilder2;
        IOException iOException2;
        StringBuilder stringBuilder3;
        byte[] bArr2 = manifest;
        ByteArrayInputStream byteIn2 = null;
        BufferedWriter bufferWriter = null;
        BufferedReader bufferReader = null;
        Manifest manifestContent = null;
        ArrayList<String> contentsList = new ArrayList();
        boolean findHwCer = false;
        if (bArr2 == null) {
            HwAuthLogger.e("HwCertificationManager", "manifest is null in readManifestAndSkipHwTag.");
            return new byte[0];
        }
        Manifest manifest2;
        try {
            long readbegin = System.currentTimeMillis();
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byteIn = null;
            try {
                bufferWriter = new BufferedWriter(new OutputStreamWriter(byteOut, "UTF-8"));
                byteIn2 = new ByteArrayInputStream(bArr2);
            } catch (IOException e3) {
                e = e3;
                bufferedReader = null;
                manifest2 = null;
                map = null;
                arrayList = null;
                byteIn2 = byteIn;
                e2 = e;
                try {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("readManifestAndSkipHwTag, e is ");
                    stringBuilder.append(e2);
                    HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                    bArr = new byte[0];
                    if (bufferWriter != null) {
                    }
                    if (byteIn2 != null) {
                    }
                    if (bufferReader != null) {
                    }
                    return bArr;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferWriter != null) {
                    }
                    if (byteIn2 != null) {
                    }
                    if (bufferReader != null) {
                    }
                    throw th;
                }
            } catch (Throwable th22) {
                bufferedReader = null;
                manifest2 = null;
                map = null;
                arrayList = null;
                th = th22;
                byteIn2 = byteIn;
                if (bufferWriter != null) {
                }
                if (byteIn2 != null) {
                }
                if (bufferReader != null) {
                }
                throw th;
            }
            try {
                bufferedReader = null;
                try {
                    bufferReader = new BufferedReader(new InputStreamReader(byteIn2, "UTF-8"));
                    try {
                        String readLine;
                        int hwcerLen = HW_CER_TAG.length();
                        while (true) {
                            int hwcerLen2 = hwcerLen;
                            String readLine2 = bufferReader.readLine();
                            String line = readLine2;
                            if (readLine2 == null) {
                                break;
                            }
                            try {
                                if (hwcerLen2 != line.length() || findHwCer) {
                                    manifest2 = manifestContent;
                                } else {
                                    manifest2 = manifestContent;
                                    if (line.charAt(hwcerLen2 - 3) == 'C') {
                                        try {
                                            if (line.charAt(hwcerLen2 - 2) == 'E' && line.charAt(hwcerLen2 - 1) == 'R' && line.lastIndexOf(HW_CER_NAME) != -1) {
                                                String nextName;
                                                findHwCer = true;
                                                HwAuthLogger.w("HwCertificationManager", "find HwCer tag");
                                                boolean findNextName = false;
                                                do {
                                                    readLine = bufferReader.readLine();
                                                    nextName = readLine;
                                                    if (readLine == null) {
                                                        break;
                                                    }
                                                } while (!nextName.startsWith("Name:"));
                                                line = nextName;
                                                boolean findNextName2 = true;
                                                HwAuthLogger.w("HwCertificationManager", "find next block name.");
                                                findNextName = findNextName2;
                                                if (!findNextName) {
                                                    hwcerLen = hwcerLen2;
                                                    manifestContent = manifest2;
                                                }
                                            }
                                        } catch (IOException e4) {
                                            e = e4;
                                            map = null;
                                            arrayList = null;
                                            e2 = e;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("readManifestAndSkipHwTag, e is ");
                                            stringBuilder.append(e2);
                                            HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                                            bArr = new byte[0];
                                            if (bufferWriter != null) {
                                            }
                                            if (byteIn2 != null) {
                                            }
                                            if (bufferReader != null) {
                                            }
                                            return bArr;
                                        } catch (Throwable th222) {
                                            th = th222;
                                            map = null;
                                            arrayList = null;
                                            if (bufferWriter != null) {
                                            }
                                            if (byteIn2 != null) {
                                            }
                                            if (bufferReader != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                }
                                if (line.length() != 0) {
                                    contentsList.add(line);
                                }
                                hwcerLen = hwcerLen2;
                                manifestContent = manifest2;
                            } catch (IOException e5) {
                                e = e5;
                                manifest2 = manifestContent;
                                map = null;
                                arrayList = null;
                                e2 = e;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("readManifestAndSkipHwTag, e is ");
                                stringBuilder.append(e2);
                                HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                                bArr = new byte[0];
                                if (bufferWriter != null) {
                                    try {
                                        bufferWriter.close();
                                    } catch (IOException e6) {
                                        iOException = e6;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("bufferWriter.close, e is ");
                                        stringBuilder2.append(e6);
                                        HwAuthLogger.i("HwCertificationManager", stringBuilder2.toString());
                                    }
                                }
                                if (byteIn2 != null) {
                                    try {
                                        byteIn2.close();
                                    } catch (IOException e62) {
                                        iOException = e62;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("byteIn.close, e is ");
                                        stringBuilder2.append(e62);
                                        HwAuthLogger.i("HwCertificationManager", stringBuilder2.toString());
                                    }
                                }
                                if (bufferReader != null) {
                                    try {
                                        bufferReader.close();
                                    } catch (IOException e622) {
                                        iOException = e622;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("bufferReader.close, e is ");
                                        stringBuilder2.append(e622);
                                        HwAuthLogger.i("HwCertificationManager", stringBuilder2.toString());
                                    }
                                }
                                return bArr;
                            } catch (Throwable th2222) {
                                manifest2 = manifestContent;
                                th = th2222;
                                map = null;
                                arrayList = null;
                                if (bufferWriter != null) {
                                    try {
                                        bufferWriter.close();
                                    } catch (IOException e6222) {
                                        iOException2 = e6222;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("bufferWriter.close, e is ");
                                        stringBuilder3.append(e6222);
                                        HwAuthLogger.i("HwCertificationManager", stringBuilder3.toString());
                                    }
                                }
                                if (byteIn2 != null) {
                                    try {
                                        byteIn2.close();
                                    } catch (IOException e62222) {
                                        iOException2 = e62222;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("byteIn.close, e is ");
                                        stringBuilder3.append(e62222);
                                        HwAuthLogger.i("HwCertificationManager", stringBuilder3.toString());
                                    }
                                }
                                if (bufferReader != null) {
                                    try {
                                        bufferReader.close();
                                    } catch (IOException e622222) {
                                        iOException2 = e622222;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("bufferReader.close, e is ");
                                        stringBuilder.append(e622222);
                                        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                                    }
                                }
                                throw th;
                            }
                        }
                        if (findHwCer) {
                            map = null;
                            arrayList = null;
                            try {
                                Collections.sort(contentsList);
                                Iterator it = contentsList.iterator();
                                while (it.hasNext()) {
                                    readLine = (String) it.next();
                                    bufferWriter.write(readLine, 0, readLine.length());
                                    bufferWriter.write(LINE_SEPERATOR, 0, LINE_SEPERATOR.length());
                                    bArr2 = manifest;
                                }
                                bufferWriter.flush();
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("readManifestAndSkipHwTag, cost time is ");
                                stringBuilder4.append(System.currentTimeMillis() - readbegin);
                                HwAuthLogger.i("HwCertificationManager", stringBuilder4.toString());
                                try {
                                    bufferWriter.close();
                                } catch (IOException e6222222) {
                                    e2 = e6222222;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("bufferWriter.close, e is ");
                                    stringBuilder.append(e6222222);
                                    HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                                }
                                try {
                                    byteIn2.close();
                                } catch (IOException e62222222) {
                                    e2 = e62222222;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("byteIn.close, e is ");
                                    stringBuilder.append(e62222222);
                                    HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                                }
                                try {
                                    bufferReader.close();
                                } catch (IOException e622222222) {
                                    e2 = e622222222;
                                    manifestContent = new StringBuilder();
                                    manifestContent.append("bufferReader.close, e is ");
                                    manifestContent.append(e622222222);
                                    HwAuthLogger.i("HwCertificationManager", manifestContent.toString());
                                }
                                return byteOut.toByteArray();
                            } catch (IOException e7) {
                                e622222222 = e7;
                                e2 = e622222222;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("readManifestAndSkipHwTag, e is ");
                                stringBuilder.append(e2);
                                HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                                bArr = new byte[0];
                                if (bufferWriter != null) {
                                }
                                if (byteIn2 != null) {
                                }
                                if (bufferReader != null) {
                                }
                                return bArr;
                            }
                        }
                        try {
                            bufferWriter.close();
                            map = null;
                            arrayList = null;
                        } catch (IOException e6222222222) {
                            iOException2 = e6222222222;
                            map = null;
                            stringBuilder3 = new StringBuilder();
                            arrayList = null;
                            stringBuilder3.append("bufferWriter.close, e is ");
                            stringBuilder3.append(e6222222222);
                            HwAuthLogger.i("HwCertificationManager", stringBuilder3.toString());
                        }
                        try {
                            byteIn2.close();
                        } catch (IOException e62222222222) {
                            iOException2 = e62222222222;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("byteIn.close, e is ");
                            stringBuilder3.append(e62222222222);
                            HwAuthLogger.i("HwCertificationManager", stringBuilder3.toString());
                        }
                        try {
                            bufferReader.close();
                        } catch (IOException e622222222222) {
                            iOException2 = e622222222222;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("bufferReader.close, e is ");
                            stringBuilder3.append(e622222222222);
                            HwAuthLogger.i("HwCertificationManager", stringBuilder3.toString());
                        }
                        return bArr2;
                    } catch (IOException e8) {
                        e622222222222 = e8;
                        manifest2 = null;
                        map = null;
                        arrayList = null;
                        e2 = e622222222222;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("readManifestAndSkipHwTag, e is ");
                        stringBuilder.append(e2);
                        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                        bArr = new byte[0];
                        if (bufferWriter != null) {
                        }
                        if (byteIn2 != null) {
                        }
                        if (bufferReader != null) {
                        }
                        return bArr;
                    } catch (Throwable th22222) {
                        manifest2 = null;
                        map = null;
                        arrayList = null;
                        th = th22222;
                        if (bufferWriter != null) {
                        }
                        if (byteIn2 != null) {
                        }
                        if (bufferReader != null) {
                        }
                        throw th;
                    }
                } catch (IOException e9) {
                    e622222222222 = e9;
                    manifest2 = null;
                    map = null;
                    arrayList = null;
                    bufferReader = bufferedReader;
                    e2 = e622222222222;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("readManifestAndSkipHwTag, e is ");
                    stringBuilder.append(e2);
                    HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                    bArr = new byte[0];
                    if (bufferWriter != null) {
                    }
                    if (byteIn2 != null) {
                    }
                    if (bufferReader != null) {
                    }
                    return bArr;
                } catch (Throwable th222222) {
                    manifest2 = null;
                    map = null;
                    arrayList = null;
                    th = th222222;
                    bufferReader = bufferedReader;
                    if (bufferWriter != null) {
                    }
                    if (byteIn2 != null) {
                    }
                    if (bufferReader != null) {
                    }
                    throw th;
                }
            } catch (IOException e10) {
                e622222222222 = e10;
                bufferedReader = null;
                manifest2 = null;
                map = null;
                arrayList = null;
                e2 = e622222222222;
                stringBuilder = new StringBuilder();
                stringBuilder.append("readManifestAndSkipHwTag, e is ");
                stringBuilder.append(e2);
                HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                bArr = new byte[0];
                if (bufferWriter != null) {
                }
                if (byteIn2 != null) {
                }
                if (bufferReader != null) {
                }
                return bArr;
            } catch (Throwable th2222222) {
                bufferedReader = null;
                manifest2 = null;
                map = null;
                arrayList = null;
                th = th2222222;
                if (bufferWriter != null) {
                }
                if (byteIn2 != null) {
                }
                if (bufferReader != null) {
                }
                throw th;
            }
        } catch (IOException e11) {
            e622222222222 = e11;
            byteIn = null;
            bufferedReader = null;
            manifest2 = null;
            map = null;
            arrayList = null;
            e2 = e622222222222;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readManifestAndSkipHwTag, e is ");
            stringBuilder.append(e2);
            HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
            bArr = new byte[0];
            if (bufferWriter != null) {
            }
            if (byteIn2 != null) {
            }
            if (bufferReader != null) {
            }
            return bArr;
        } catch (Throwable th22222222) {
            byteIn = null;
            bufferedReader = null;
            manifest2 = null;
            map = null;
            arrayList = null;
            th = th22222222;
            if (bufferWriter != null) {
            }
            if (byteIn2 != null) {
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getManifestFileNew cost, cost time is  ");
        stringBuilder.append(System.currentTimeMillis() - begin);
        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
        byte[] out = readManifestAndSkipHwTag(manifest);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getManifestFileWithoutHwCER begin, cost time is  ");
        stringBuilder2.append(System.currentTimeMillis() - begin);
        HwAuthLogger.i("HwCertificationManager", stringBuilder2.toString());
        return out;
    }

    public static byte[] getManifestFileWithoutHwCER(File apkFile) {
        long begin = System.currentTimeMillis();
        byte[] manifest = getManifestFileNew(apkFile);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getManifestFileWithoutHwCER begin, cost time is  ");
        stringBuilder.append(System.currentTimeMillis() - begin);
        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
        return manifest;
    }

    public static InputStream readHwCertFromApk(String apkPath) {
        ZipFile zipFile = null;
        InputStream input = null;
        try {
            zipFile = new ZipFile(new File(apkPath));
            Enumeration enumeration = zipFile.entries();
            ZipEntry zipEntry = null;
            while (enumeration.hasMoreElements()) {
                zipEntry = (ZipEntry) enumeration.nextElement();
                if (!zipEntry.isDirectory() && CERT_NAME.equals(zipEntry.getName())) {
                    input = zipFile.getInputStream(zipEntry);
                    break;
                }
            }
            try {
                zipFile.close();
            } catch (IOException e) {
                HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
            }
            return input;
        } catch (IOException e2) {
            HwAuthLogger.e("HwCertificationManager", "IOException in readHwCertFromApk() in Utils.java ");
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e3) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e4) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
                }
            }
            return null;
        } catch (Throwable th) {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e5) {
                    HwAuthLogger.e("HwCertificationManager", "close stream failed when read cert from apk");
                }
            }
        }
    }

    public static InputStream readHwCertFromApk(ZipFile zfile, ZipEntry entry) {
        if (zfile == null || entry == null) {
            return null;
        }
        InputStream input = new ByteArrayInputStream(new byte[0]);
        try {
            if (!entry.isDirectory() && CERT_NAME.equals(entry.getName())) {
                input = zfile.getInputStream(entry);
            }
            return input;
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
        char[] hexChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("from ");
        stringBuilder.append(fromString);
        stringBuilder.append(" to ");
        stringBuilder.append(toString);
        return stringBuilder.toString();
    }

    public static boolean isPackageInstalled(String packagename, Context context) {
        try {
            context.getPackageManager().getPackageInfo(packagename, 1);
            return true;
        } catch (NameNotFoundException e) {
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The SF file name is ");
                stringBuilder.append(zipFileNames);
                HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                return zipFileNames;
            }
        }
        return null;
    }
}
