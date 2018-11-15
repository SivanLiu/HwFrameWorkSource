package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareLog;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileContent {
    private static final String TAG = "FileContent";

    public static void closeBufferedReader(BufferedReader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("closeBufferedReader exception ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    public static void closeInputStreamReader(InputStreamReader isr) {
        if (isr != null) {
            try {
                isr.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("closeInputStreamReader exception ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    public static void closeFileInputStream(FileInputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("closeFileInputStream exception ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }
}
