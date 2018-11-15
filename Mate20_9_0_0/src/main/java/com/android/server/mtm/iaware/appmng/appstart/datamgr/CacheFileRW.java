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
        List<String> result = new ArrayList();
        FileInputStream stream = null;
        BufferedReader br = null;
        String readLine;
        try {
            stream = this.mAtomicFile.openRead();
            if (stream == null) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e2) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                return result;
            } else if (stream.available() > 32768) {
                AwareLog.e(TAG, "readFileLines file size is more than 32K!");
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e3) {
                        AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                return result;
            } else {
                br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
                while (true) {
                    readLine = br.readLine();
                    String tmp = readLine;
                    if (readLine != null) {
                        result.add(tmp);
                    } else {
                        try {
                            break;
                        } catch (IOException e5) {
                            AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                        }
                    }
                }
                br.close();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e6) {
                        AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                    }
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("readFileLines result: ");
                stringBuilder.append(result);
                AwareLog.d(str, stringBuilder.toString());
                return result;
            }
        } catch (FileNotFoundException e7) {
            readLine = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("readFileLines file not exist: ");
            stringBuilder2.append(this.mAtomicFile);
            AwareLog.e(readLine, stringBuilder2.toString());
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e8) {
                    AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                }
            }
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e9) {
            AwareLog.e(TAG, "readFileLines catch IOException!");
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e10) {
                    AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                }
            }
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e11) {
                    AwareLog.e(TAG, "readFileLines br catch IOException in finally!");
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e12) {
                    AwareLog.e(TAG, "readFileLines stream catch IOException in finally!");
                }
            }
        }
    }

    void writeFileLines(List<String> lines) {
        try {
            FileOutputStream fos = this.mAtomicFile.startWrite();
            StringBuffer buf = new StringBuffer();
            for (String line : lines) {
                buf.append(line);
                buf.append("\n");
            }
            fos.write(buf.toString().getBytes("utf-8"));
            this.mAtomicFile.finishWrite(fos);
        } catch (IOException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("writeFileLines catch IOException: ");
            stringBuilder.append(ex);
            AwareLog.e(str, stringBuilder.toString());
            this.mAtomicFile.failWrite(null);
        }
    }
}
