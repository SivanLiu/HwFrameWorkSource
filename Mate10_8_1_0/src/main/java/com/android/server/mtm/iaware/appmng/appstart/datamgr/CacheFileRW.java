package com.android.server.mtm.iaware.appmng.appstart.datamgr;

import android.rms.iaware.AwareLog;
import android.util.AtomicFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class CacheFileRW {
    private static final String TAG = "CacheFileRW";
    private AtomicFile mAtomicFile;

    CacheFileRW(File file) {
        this.mAtomicFile = new AtomicFile(file);
    }

    List<String> readFileLines() {
        Throwable th;
        List<String> result = new ArrayList();
        FileInputStream fileInputStream = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = this.mAtomicFile.openRead();
            if (fileInputStream == null) {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                return result;
            } else if (fileInputStream.available() > 32768) {
                AwareLog.e(TAG, "readFileLines file size is more than 32K!");
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e2) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                return result;
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream, "utf-8"));
                while (true) {
                    try {
                        String tmp = br.readLine();
                        if (tmp == null) {
                            break;
                        }
                        result.add(tmp);
                    } catch (FileNotFoundException e3) {
                        bufferedReader = br;
                    } catch (IOException e4) {
                        bufferedReader = br;
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedReader = br;
                    }
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e5) {
                        AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e6) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                AwareLog.d(TAG, "readFileLines result: " + result);
                return result;
            }
        } catch (FileNotFoundException e7) {
            try {
                AwareLog.e(TAG, "readFileLines file not exist: " + this.mAtomicFile);
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e8) {
                        AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e9) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                AwareLog.d(TAG, "readFileLines result: " + result);
                return result;
            } catch (Throwable th3) {
                th = th3;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e10) {
                        AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e11) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                throw th;
            }
        } catch (IOException e12) {
            AwareLog.e(TAG, "readFileLines catch IOException!");
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e13) {
                    AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e14) {
                    AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                }
            }
            AwareLog.d(TAG, "readFileLines result: " + result);
            return result;
        }
    }

    void writeFileLines(List<String> lines) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = this.mAtomicFile.startWrite();
            StringBuffer buf = new StringBuffer();
            for (String line : lines) {
                buf.append(line).append("\n");
            }
            fileOutputStream.write(buf.toString().getBytes("utf-8"));
            this.mAtomicFile.finishWrite(fileOutputStream);
        } catch (IOException ex) {
            AwareLog.e(TAG, "writeFileLines catch IOException: " + ex);
            this.mAtomicFile.failWrite(fileOutputStream);
        }
    }
}
