package huawei.android.hwutil;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    private static final int BUFFER_BYTE = 4096;
    private static final int BUFF_SIZE = 10240;
    private static final String EXCLUDE_ENTRY = "..";
    private static final int MAX_ENTRY_THEME_SIZE = 200000000;
    private static final String TAG = "ZipUtil";

    public static boolean zipFiles(Collection<File> resFileList, File zipFile) {
        ZipOutputStream zipout = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(zipFile);
            bos = new BufferedOutputStream(fos, BUFF_SIZE);
            zipout = new ZipOutputStream(bos);
            for (File resFile : resFileList) {
                zipFile(resFile, zipout, "");
            }
            try {
                zipout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            try {
                bos.close();
            } catch (IOException e22) {
                e22.printStackTrace();
            }
            return true;
        } catch (FileNotFoundException e3) {
            e3.printStackTrace();
            if (zipout != null) {
                try {
                    zipout.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e42) {
                    e42.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e422) {
                    e422.printStackTrace();
                }
            }
            return false;
        } catch (Throwable th) {
            if (zipout != null) {
                try {
                    zipout.close();
                } catch (IOException e222) {
                    e222.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2222) {
                    e2222.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e22222) {
                    e22222.printStackTrace();
                }
            }
        }
    }

    public static void zipFiles(Collection<File> resFileList, File zipFile, String comment) throws IOException {
        ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), BUFF_SIZE));
        for (File resFile : resFileList) {
            zipFile(resFile, zipout, "");
        }
        zipout.setComment(comment);
        zipout.close();
    }

    public static void unZipFile(File zipFile, String folderPath) {
        File desDir = new File(folderPath);
        if (desDir.exists() || desDir.mkdirs()) {
            int i = 0;
            ZipFile zf = null;
            OutputStream out;
            InputStream in;
            try {
                zf = new ZipFile(zipFile);
                Enumeration<?> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if (entry.getName() == null || !entry.getName().contains(EXCLUDE_ENTRY)) {
                        String str = new StringBuilder();
                        str.append(folderPath);
                        str.append(File.separator);
                        str.append(entry.getName());
                        str = new String(str.toString().getBytes("8859_1"), "UTF-8");
                        i++;
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(i);
                        stringBuilder.append("======file=====:");
                        stringBuilder.append(str);
                        Log.i(str2, stringBuilder.toString());
                        File desFile = new File(str);
                        if (!entry.isDirectory()) {
                            boolean newOk = true;
                            if (!desFile.exists()) {
                                File fileParentDir = desFile.getParentFile();
                                if (!(fileParentDir == null || fileParentDir.exists())) {
                                    newOk = fileParentDir.mkdirs();
                                }
                                if (newOk) {
                                    newOk = desFile.createNewFile();
                                }
                            }
                            if (newOk || desFile.exists()) {
                                out = null;
                                in = null;
                                in = zf.getInputStream(entry);
                                out = new FileOutputStream(desFile);
                                byte[] buffer = new byte[10240];
                                while (true) {
                                    int read = in.read(buffer);
                                    int realLength = read;
                                    if (read <= 0) {
                                        break;
                                    }
                                    out.write(buffer, 0, realLength);
                                }
                                out.flush();
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                try {
                                    out.close();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    }
                }
                Log.i(TAG, "unzip end");
                try {
                    zf.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            } catch (Exception e4) {
                try {
                    e4.printStackTrace();
                    if (zf != null) {
                        zf.close();
                    }
                } catch (Throwable th) {
                    if (zf != null) {
                        try {
                            zf.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                        }
                    }
                }
            } catch (Throwable th2) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e22) {
                        e22.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e222) {
                        e222.printStackTrace();
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:24:0x0060, code skipped:
            r4 = r4 + r8;
     */
    /* JADX WARNING: Missing block: B:25:0x0061, code skipped:
            if (r4 < MAX_ENTRY_THEME_SIZE) goto L_0x008e;
     */
    /* JADX WARNING: Missing block: B:27:?, code skipped:
            r3 = TAG;
            r10 = new java.lang.StringBuilder();
            r10.append("isZipError total checkZipIsSize true ");
            r10.append(r13);
            android.util.Log.e(r3, r10.toString());
     */
    /* JADX WARNING: Missing block: B:28:0x007a, code skipped:
            if (r2 == null) goto L_0x0085;
     */
    /* JADX WARNING: Missing block: B:30:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:31:0x0080, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:32:0x0081, code skipped:
            r3.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:39:?, code skipped:
            r2.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isZipError(String zipFile) {
        ZipFile zf;
        if (zipFile == null) {
            return true;
        }
        zf = null;
        InputStream in = null;
        int total = 0;
        try {
            zf = new ZipFile(zipFile);
            Enumeration<?> entries = zf.entries();
            while (entries.hasMoreElements()) {
                in = zf.getInputStream((ZipEntry) entries.nextElement());
                int entrySzie = 0;
                byte[] buffer = new byte[4096];
                while (true) {
                    int read = in.read(buffer);
                    int bytesRead = read;
                    if (read < 0) {
                        break;
                    }
                    entrySzie += bytesRead;
                    if (entrySzie >= MAX_ENTRY_THEME_SIZE) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("isZipError entry checkZipIsSize true ");
                        stringBuilder.append(zipFile);
                        Log.e(str, stringBuilder.toString());
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            zf.close();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                        return true;
                    }
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            try {
                zf.close();
            } catch (Exception e22) {
                e22.printStackTrace();
            }
            return false;
        } catch (IOException e4) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
            }
            if (zf != null) {
                try {
                    zf.close();
                } catch (Exception e23) {
                    e23.printStackTrace();
                }
            }
            return true;
        } catch (Exception e6) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e52) {
                    e52.printStackTrace();
                }
            }
            if (zf != null) {
                try {
                    zf.close();
                } catch (Exception e232) {
                    e232.printStackTrace();
                }
            }
            return true;
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e7) {
                    e7.printStackTrace();
                }
            }
            if (zf != null) {
                try {
                    zf.close();
                } catch (Exception e24) {
                    e24.printStackTrace();
                }
            }
        }
        try {
            zf.close();
        } catch (Exception e242) {
            e242.printStackTrace();
        }
        return true;
        return true;
    }

    public static void unZipDirectory(String zipFile, String entryName, String desPath) {
        File desDir = new File(desPath);
        if (desDir.exists() || desDir.mkdirs()) {
            ZipFile zf = null;
            OutputStream out;
            InputStream in;
            try {
                zf = new ZipFile(zipFile);
                Enumeration<?> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if (!(entry.getName() == null || entry.getName().contains(EXCLUDE_ENTRY) || !entry.getName().contains(entryName))) {
                        String str = new StringBuilder();
                        str.append(desPath);
                        str.append(File.separator);
                        str.append(entry.getName());
                        File desFile = new File(new String(str.toString().getBytes("8859_1"), "UTF-8"));
                        if (!entry.isDirectory()) {
                            boolean newOk = true;
                            if (!desFile.exists()) {
                                File fileParentDir = desFile.getParentFile();
                                if (!(fileParentDir == null || fileParentDir.exists())) {
                                    newOk = fileParentDir.mkdirs();
                                }
                                if (newOk) {
                                    newOk = desFile.createNewFile();
                                }
                            }
                            if (newOk || desFile.exists()) {
                                out = null;
                                in = null;
                                in = zf.getInputStream(entry);
                                out = new FileOutputStream(desFile);
                                byte[] buffer = new byte[10240];
                                while (true) {
                                    int read = in.read(buffer);
                                    int realLength = read;
                                    if (read <= 0) {
                                        break;
                                    }
                                    out.write(buffer, 0, realLength);
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                try {
                                    out.close();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    }
                }
                try {
                    zf.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            } catch (Exception e4) {
                try {
                    e4.printStackTrace();
                    if (zf != null) {
                        zf.close();
                    }
                } catch (Throwable th) {
                    if (zf != null) {
                        try {
                            zf.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                        }
                    }
                }
            } catch (Throwable th2) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e22) {
                        e22.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e222) {
                        e222.printStackTrace();
                    }
                }
            }
        }
    }

    public static boolean upZipSelectedFile(File form, String to, String obj) throws ZipException, IOException {
        IOException iOException;
        StringBuilder stringBuilder;
        String str;
        StringBuilder stringBuilder2;
        IOException iOException2;
        Throwable th;
        File file;
        InputStream inStream;
        File file2 = form;
        String str2 = to;
        if (file2 == null) {
            return false;
        }
        File targetDir = new File(str2);
        if (!targetDir.exists()) {
            Log.d("ive", "add audio dir");
            if (!targetDir.mkdir()) {
                return false;
            }
        }
        ZipFile zf = new ZipFile(file2);
        Enumeration<?> entries = zf.entries();
        while (true) {
            Enumeration<?> entries2 = entries;
            if (!entries2.hasMoreElements()) {
                break;
            }
            ZipEntry entry = (ZipEntry) entries2.nextElement();
            if (entry.getName().contains(obj)) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str2);
                stringBuilder3.append(entry.getName());
                String fileName = stringBuilder3.toString();
                File desFile = new File(fileName);
                if (!(fileName.contains(EXCLUDE_ENTRY) || entry.isDirectory())) {
                    boolean newOk = true;
                    if (!desFile.exists()) {
                        File fileParentDir = desFile.getParentFile();
                        if (!(fileParentDir == null || fileParentDir.exists())) {
                            newOk = fileParentDir.mkdirs();
                        }
                        boolean newOk2 = newOk;
                        if (newOk2) {
                            try {
                                newOk2 = desFile.createNewFile();
                            } catch (IOException e) {
                                IOException iOException3 = e;
                                Log.e(TAG, " create new file failed");
                            }
                        }
                        if (newOk2) {
                            InputStream inStream2 = null;
                            OutputStream outStream = null;
                            InputStream inStream3;
                            try {
                                byte[] buffer = new byte[4096];
                                inStream2 = zf.getInputStream(entry);
                                outStream = new FileOutputStream(desFile);
                                while (true) {
                                    int read = inStream2.read(buffer);
                                    int realLength = read;
                                    if (read <= 0) {
                                        break;
                                    }
                                    read = realLength;
                                    outStream.write(buffer, 0, read);
                                    int i = read;
                                    file2 = form;
                                }
                                if (inStream2 != null) {
                                    try {
                                        inStream2.close();
                                    } catch (IOException e2) {
                                        IOException iOException4 = e2;
                                        String str3 = TAG;
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("ioe is ");
                                        stringBuilder4.append(e2);
                                        Log.e(str3, stringBuilder4.toString());
                                    } catch (Throwable th2) {
                                    }
                                }
                                inStream3 = null;
                                try {
                                    outStream.close();
                                } catch (IOException e22) {
                                    iOException = e22;
                                    str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("ioexception is ");
                                    stringBuilder.append(e22);
                                    Log.e(str2, stringBuilder.toString());
                                } catch (Throwable th3) {
                                }
                            } catch (IOException e222) {
                                str = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("IOException is ");
                                stringBuilder2.append(e222);
                                Log.e(str, stringBuilder2.toString());
                                if (inStream2 != null) {
                                    try {
                                        inStream2.close();
                                    } catch (IOException e2222) {
                                        iOException2 = e2222;
                                        str = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("ioe is ");
                                        stringBuilder2.append(e2222);
                                        Log.e(str, stringBuilder2.toString());
                                    } catch (Throwable th4) {
                                    }
                                }
                                inStream3 = null;
                                if (outStream != null) {
                                    try {
                                        outStream.close();
                                    } catch (IOException e22222) {
                                        iOException = e22222;
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("ioexception is ");
                                        stringBuilder.append(e22222);
                                        Log.e(str2, stringBuilder.toString());
                                    } catch (Throwable th5) {
                                    }
                                }
                            } catch (Exception e3) {
                                str = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Exception is ");
                                stringBuilder2.append(e3);
                                Log.e(str, stringBuilder2.toString());
                                if (inStream2 != null) {
                                    try {
                                        inStream2.close();
                                    } catch (IOException e222222) {
                                        iOException2 = e222222;
                                        str = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("ioe is ");
                                        stringBuilder2.append(e222222);
                                        Log.e(str, stringBuilder2.toString());
                                    } catch (Throwable th6) {
                                    }
                                }
                                inStream3 = null;
                                if (outStream != null) {
                                    try {
                                        outStream.close();
                                    } catch (IOException e2222222) {
                                        iOException = e2222222;
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("ioexception is ");
                                        stringBuilder.append(e2222222);
                                        Log.e(str2, stringBuilder.toString());
                                    } catch (Throwable th7) {
                                    }
                                }
                            } catch (Throwable th8) {
                                Throwable th9 = th8;
                                if (inStream2 != null) {
                                    try {
                                        inStream2.close();
                                        file = targetDir;
                                    } catch (IOException e22222222) {
                                        iOException = e22222222;
                                        inStream = TAG;
                                        stringBuilder = new StringBuilder();
                                        file = targetDir;
                                        stringBuilder.append("ioe is ");
                                        stringBuilder.append(e22222222);
                                        Log.e(inStream, stringBuilder.toString());
                                    } catch (Throwable th10) {
                                        th8 = th10;
                                    }
                                }
                                inStream = null;
                                if (outStream != null) {
                                    try {
                                        outStream.close();
                                    } catch (IOException e222222222) {
                                        IOException iOException5 = e222222222;
                                        String str4 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("ioexception is ");
                                        stringBuilder.append(e222222222);
                                        Log.e(str4, stringBuilder.toString());
                                    } catch (Throwable th11) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
            entries = entries2;
            targetDir = targetDir;
            file2 = form;
            str2 = to;
        }
        String str5 = obj;
        file = targetDir;
        try {
            zf.close();
        } catch (IOException e2222222222) {
            iOException2 = e2222222222;
            e2222222222.printStackTrace();
        }
        return false;
        throw th8;
    }

    public static ArrayList<String> getEntriesNames(File zipFile) throws ZipException, IOException {
        Exception e1;
        String str;
        StringBuilder stringBuilder;
        ArrayList<String> entryNames = new ArrayList();
        ZipFile zf = new ZipFile(zipFile);
        try {
            Enumeration<?> entries = zf.entries();
            while (entries.hasMoreElements()) {
                String entryName = getEntryName((ZipEntry) entries.nextElement());
                if (entryName != null) {
                    entryNames.add(entryName);
                }
            }
            try {
                zf.close();
            } catch (Exception e) {
                e1 = e;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (Exception e12) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception e: ");
            stringBuilder.append(e12);
            Log.e(str, stringBuilder.toString());
            try {
                zf.close();
            } catch (Exception e2) {
                e12 = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (Throwable th) {
            try {
                zf.close();
            } catch (Exception e13) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception e1: ");
                stringBuilder.append(e13);
                Log.e(TAG, stringBuilder.toString());
            }
            throw th;
        }
        return entryNames;
        stringBuilder.append("Exception e1: ");
        stringBuilder.append(e12);
        Log.e(str, stringBuilder.toString());
        return entryNames;
    }

    public static String getEntryComment(ZipEntry entry) throws UnsupportedEncodingException {
        return new String(entry.getComment().getBytes("GB2312"), "8859_1");
    }

    public static String getEntryName(ZipEntry entry) throws UnsupportedEncodingException {
        if (entry.getName() == null || !entry.getName().contains(EXCLUDE_ENTRY)) {
            return new String(entry.getName().getBytes("GB2312"), "8859_1");
        }
        return null;
    }

    private static void zipFile(File resFile, ZipOutputStream zipout, String rootpath) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rootpath);
        stringBuilder.append(rootpath.trim().length() == 0 ? "" : File.separator);
        stringBuilder.append(resFile.getName());
        rootpath = stringBuilder.toString();
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            rootpath = new String(rootpath.getBytes("8859_1"), "GB2312");
            int i = 0;
            if (resFile.isDirectory()) {
                File[] fileList = resFile.listFiles();
                if (fileList != null) {
                    int length = fileList.length;
                    while (i < length) {
                        zipFile(fileList[i], zipout, rootpath);
                        i++;
                    }
                }
                zipout.putNextEntry(new ZipEntry(rootpath));
            } else {
                byte[] buffer = new byte[BUFF_SIZE];
                fis = new FileInputStream(resFile);
                bis = new BufferedInputStream(fis, BUFF_SIZE);
                zipout.putNextEntry(new ZipEntry(rootpath));
                while (true) {
                    int read = bis.read(buffer);
                    int realLength = read;
                    if (read == -1) {
                        break;
                    }
                    zipout.write(buffer, 0, realLength);
                }
                bis.close();
                zipout.flush();
                zipout.closeEntry();
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e22) {
                    e22.printStackTrace();
                }
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e42) {
                    e42.printStackTrace();
                }
            }
        }
    }

    public static void unZipFile(String zipFile, String entryName, String desPath) {
        ZipFile zf = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            zf = new ZipFile(zipFile);
            ZipEntry zEntry = zf.getEntry(entryName);
            if (!(zEntry == null || entryName == null)) {
                if (!entryName.contains(EXCLUDE_ENTRY)) {
                    in = zf.getInputStream(zEntry);
                    String str = new StringBuilder();
                    str.append(desPath);
                    str.append(File.separator);
                    str.append(entryName);
                    out = new FileOutputStream(new String(str.toString().getBytes("8859_1"), "GB2312"));
                    byte[] buffer = new byte[4096];
                    while (true) {
                        int read = in.read(buffer);
                        int realLength = read;
                        if (read <= 0) {
                            break;
                        }
                        out.write(buffer, 0, realLength);
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        out.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    try {
                        zf.close();
                    } catch (IOException zEntry2) {
                        zEntry2.printStackTrace();
                    }
                    return;
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e32) {
                    e32.printStackTrace();
                }
            }
            try {
                zf.close();
            } catch (IOException e322) {
                e322.printStackTrace();
            }
        } catch (Exception e4) {
            e4.printStackTrace();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e22) {
                    e22.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e222) {
                    e222.printStackTrace();
                }
            }
            if (zf != null) {
                zf.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3222) {
                    e3222.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e32222) {
                    e32222.printStackTrace();
                }
            }
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e322222) {
                    e322222.printStackTrace();
                }
            }
        }
    }

    public static void unZipFile(File zipFile, String desPath, String entryName) {
        if (zipFile != null && entryName != null && !entryName.contains(EXCLUDE_ENTRY)) {
            InputStream inStream = null;
            OutputStream outStream = null;
            ZipFile zf = null;
            try {
                zf = new ZipFile(zipFile);
                ZipEntry zEntry = zf.getEntry(entryName);
                if (zEntry == null) {
                    try {
                        zf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    if (outStream != null) {
                        try {
                            outStream.close();
                        } catch (IOException e22) {
                            e22.printStackTrace();
                        }
                    }
                    return;
                }
                inStream = zf.getInputStream(zEntry);
                String str = new StringBuilder();
                str.append(desPath);
                str.append(File.separator);
                str.append(entryName);
                outStream = new FileOutputStream(new String(str.toString().getBytes("8859_1"), "GB2312"));
                byte[] buffer = new byte[4096];
                while (true) {
                    int read = inStream.read(buffer);
                    int realLength = read;
                    if (read > 0) {
                        outStream.write(buffer, 0, realLength);
                    } else {
                        try {
                            break;
                        } catch (IOException e222) {
                            e222.printStackTrace();
                        }
                    }
                }
                zf.close();
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e2222) {
                        e2222.printStackTrace();
                    }
                }
                try {
                    outStream.close();
                } catch (IOException e22222) {
                    e22222.printStackTrace();
                }
            } catch (Exception e3) {
                e3.printStackTrace();
                if (zf != null) {
                    try {
                        zf.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                }
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e42) {
                        e42.printStackTrace();
                    }
                }
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e422) {
                        e422.printStackTrace();
                    }
                }
            } catch (Throwable th) {
                if (zf != null) {
                    try {
                        zf.close();
                    } catch (IOException e4222) {
                        e4222.printStackTrace();
                    }
                }
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e42222) {
                        e42222.printStackTrace();
                    }
                }
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e422222) {
                        e422222.printStackTrace();
                    }
                }
            }
        }
    }
}
