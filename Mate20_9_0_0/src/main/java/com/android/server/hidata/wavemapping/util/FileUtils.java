package com.android.server.hidata.wavemapping.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileUtils {
    public static final String ERROR_RET = "FILE_BIGGER";
    private static final String TAG;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(FileUtils.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public static boolean delFile(String path) {
        if (path == null || path.equals("")) {
            LogUtil.d(" delFile path=null || path== \"\" ");
            return false;
        }
        StringBuilder stringBuilder;
        try {
            File file = new File(path);
            if (file.exists() && !file.delete()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("delFile failure.path:");
                stringBuilder.append(path);
                LogUtil.e(stringBuilder.toString());
            }
            return true;
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception delFile----");
            stringBuilder.append(e);
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    public static boolean mkdir(String tileFilePath) {
        boolean ret = false;
        if (tileFilePath == null || tileFilePath.equals("")) {
            LogUtil.e(" mkdir tileFilePath=null or tileFilePath == ");
            return false;
        }
        try {
            File file = new File(tileFilePath);
            if (!file.exists() && !file.isDirectory() && !file.mkdirs()) {
                return false;
            }
            if (file.exists()) {
                ret = true;
            }
            return ret;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception mkdir");
            stringBuilder.append(e);
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    public static boolean addFileHead(String filePath, String content) {
        StringBuilder stringBuilder;
        IOException e;
        FileOutputStream fout = null;
        OutputStreamWriter osw = null;
        try {
            if (!new File(filePath).exists()) {
                fout = new FileOutputStream(new File(filePath), true);
                osw = new OutputStreamWriter(fout, "UTF-8");
                osw.write(content);
            }
            if (osw != null) {
                try {
                    osw.flush();
                } catch (IOException e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("saveFile:");
                    stringBuilder.append(e2);
                    LogUtil.e(stringBuilder.toString());
                }
                closeFileStreamNotThrow(osw);
            }
            if (fout != null) {
                try {
                    fout.flush();
                } catch (IOException e3) {
                    e2 = e3;
                    stringBuilder = new StringBuilder();
                }
                closeFileStreamNotThrow(fout);
            }
        } catch (Exception e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("addFileHead failure.filePath:");
            stringBuilder.append(filePath);
            stringBuilder.append(",content:");
            stringBuilder.append(content);
            LogUtil.e(stringBuilder.toString());
            if (osw != null) {
                try {
                    osw.flush();
                } catch (IOException e22) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("saveFile:");
                    stringBuilder.append(e22);
                    LogUtil.e(stringBuilder.toString());
                }
                closeFileStreamNotThrow(osw);
            }
            if (fout != null) {
                try {
                    fout.flush();
                } catch (IOException e5) {
                    e22 = e5;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (osw != null) {
                try {
                    osw.flush();
                } catch (IOException e222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("saveFile:");
                    stringBuilder.append(e222);
                    LogUtil.e(stringBuilder.toString());
                }
                closeFileStreamNotThrow(osw);
            }
            if (fout != null) {
                try {
                    fout.flush();
                } catch (IOException e2222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("saveFile:");
                    stringBuilder.append(e2222);
                    LogUtil.e(stringBuilder.toString());
                }
                closeFileStreamNotThrow(fout);
            }
        }
        return true;
        stringBuilder.append("saveFile:");
        stringBuilder.append(e2222);
        LogUtil.e(stringBuilder.toString());
        closeFileStreamNotThrow(fout);
        return true;
    }

    public static boolean writeFile(String filePath, String content) {
        StringBuilder stringBuilder;
        IOException e;
        StringBuilder stringBuilder2;
        File tgFile = new File(filePath);
        FileOutputStream fout = null;
        OutputStreamWriter osw = null;
        boolean ret = false;
        try {
            if (!tgFile.exists()) {
                tgFile = new File(filePath);
            }
            fout = new FileOutputStream(tgFile, true);
            osw = new OutputStreamWriter(fout, "UTF-8");
            osw.write(content);
            ret = true;
            try {
                osw.flush();
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveFile:");
                stringBuilder.append(e2);
                LogUtil.e(stringBuilder.toString());
            }
            closeFileStreamNotThrow(osw);
            try {
                fout.flush();
            } catch (IOException e3) {
                e2 = e3;
                stringBuilder = new StringBuilder();
            }
        } catch (Exception e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("writeFile failure.filePath:");
            stringBuilder.append(filePath);
            stringBuilder.append(",content:");
            stringBuilder.append(content);
            LogUtil.e(stringBuilder.toString());
            if (osw != null) {
                try {
                    osw.flush();
                } catch (IOException e22) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("saveFile:");
                    stringBuilder.append(e22);
                    LogUtil.e(stringBuilder.toString());
                }
                closeFileStreamNotThrow(osw);
            }
            if (fout != null) {
                try {
                    fout.flush();
                } catch (IOException e5) {
                    e22 = e5;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (osw != null) {
                try {
                    osw.flush();
                } catch (IOException e6) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saveFile:");
                    stringBuilder2.append(e6);
                    LogUtil.e(stringBuilder2.toString());
                }
                closeFileStreamNotThrow(osw);
            }
            if (fout != null) {
                try {
                    fout.flush();
                } catch (IOException e62) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saveFile:");
                    stringBuilder2.append(e62);
                    LogUtil.e(stringBuilder2.toString());
                }
                closeFileStreamNotThrow(fout);
            }
        }
        closeFileStreamNotThrow(fout);
        return ret;
        stringBuilder.append("saveFile:");
        stringBuilder.append(e22);
        LogUtil.e(stringBuilder.toString());
        closeFileStreamNotThrow(fout);
        return ret;
    }

    public static boolean saveFile(String filePath, String content) {
        StringBuilder stringBuilder;
        IOException e;
        StringBuilder stringBuilder2;
        BufferedWriter bfw = null;
        boolean ret = false;
        FileOutputStream writerStream = null;
        try {
            writerStream = new FileOutputStream(filePath);
            bfw = new BufferedWriter(new OutputStreamWriter(writerStream, "UTF-8"));
            bfw.write(content);
            bfw.newLine();
            ret = true;
            try {
                bfw.flush();
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveFile:");
                stringBuilder.append(e2);
                LogUtil.e(stringBuilder.toString());
            }
            closeFileStreamNotThrow(bfw);
            try {
                writerStream.flush();
            } catch (IOException e3) {
                e2 = e3;
                stringBuilder = new StringBuilder();
            }
        } catch (IOException e22) {
            e22.printStackTrace();
            if (bfw != null) {
                try {
                    bfw.flush();
                } catch (IOException e222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("saveFile:");
                    stringBuilder.append(e222);
                    LogUtil.e(stringBuilder.toString());
                }
                closeFileStreamNotThrow(bfw);
            }
            if (writerStream != null) {
                try {
                    writerStream.flush();
                } catch (IOException e4) {
                    e222 = e4;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Exception e5) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("saveFile,e");
            stringBuilder.append(e5.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (bfw != null) {
                try {
                    bfw.flush();
                } catch (IOException e2222) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("saveFile:");
                    stringBuilder.append(e2222);
                    LogUtil.e(stringBuilder.toString());
                }
                closeFileStreamNotThrow(bfw);
            }
            if (writerStream != null) {
                try {
                    writerStream.flush();
                } catch (IOException e6) {
                    e2222 = e6;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (bfw != null) {
                try {
                    bfw.flush();
                } catch (IOException e7) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saveFile:");
                    stringBuilder2.append(e7);
                    LogUtil.e(stringBuilder2.toString());
                }
                closeFileStreamNotThrow(bfw);
            }
            if (writerStream != null) {
                try {
                    writerStream.flush();
                } catch (IOException e72) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saveFile:");
                    stringBuilder2.append(e72);
                    LogUtil.e(stringBuilder2.toString());
                }
                closeFileStreamNotThrow(writerStream);
            }
        }
        closeFileStreamNotThrow(writerStream);
        return ret;
        stringBuilder.append("saveFile:");
        stringBuilder.append(e2222);
        LogUtil.e(stringBuilder.toString());
        closeFileStreamNotThrow(writerStream);
        return ret;
    }

    public static String getFileContent(String fPath) {
        String res = "";
        if (fPath == null || fPath.equals("")) {
            return res;
        }
        FileInputStream fin = null;
        try {
            if (new File(fPath).isFile()) {
                fin = new FileInputStream(fPath);
                int length = fin.available();
                if (length > 25000000) {
                    String str = ERROR_RET;
                    closeFileStreamNotThrow(fin);
                    return str;
                }
                byte[] buffer = new byte[length];
                if (fin.read(buffer) != -1) {
                    res = new String(buffer, "utf-8");
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFileContent,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            closeFileStreamNotThrow(fin);
        }
        closeFileStreamNotThrow(fin);
        return res;
    }

    public static boolean isFileExists(String fPath) {
        if (fPath == null || fPath.equals("")) {
            return false;
        }
        String res = "";
        try {
            if (new File(fPath).isFile()) {
                return true;
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isFileExists:");
            stringBuilder.append(e);
            LogUtil.e(stringBuilder.toString());
        }
        return false;
    }

    public static long getDirSize(File file) {
        if (file == null) {
            try {
                LogUtil.e(" getDirSize file=null ");
                return 0;
            } catch (Exception e) {
                if (file != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getDirSize e:");
                    stringBuilder.append(e);
                    stringBuilder.append(",file=");
                    stringBuilder.append(file.getAbsolutePath());
                    LogUtil.e(stringBuilder.toString());
                }
                return 0;
            }
        } else if (!file.exists()) {
            LogUtil.d("getDirSize file do not exist!");
            return 0;
        } else if (!file.isDirectory()) {
            return file.length();
        } else {
            File[] children = file.listFiles();
            long size = 0;
            if (children != null) {
                for (File f : children) {
                    size += getDirSize(f);
                }
            }
            return size;
        }
    }

    private static void closeFileStreamNotThrow(Closeable fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
            }
        }
    }
}
