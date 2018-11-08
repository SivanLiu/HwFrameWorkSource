package com.android.server.security.fileprotect;

import android.content.Context;
import android.util.Slog;
import com.android.server.devicepolicy.StorageUtils;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PackStoragePolicy {
    private static final String TAG = "PackStoragePolicy";
    public String packageName;
    public List<PathPolicy> policies = new ArrayList();

    PackStoragePolicy() {
    }

    public static List<PackStoragePolicy> parse(Context context, String fname) {
        List<PackStoragePolicy> packPolicyList = new ArrayList();
        try {
            JSONArray packageList = new JSONObject(readFileString(context, fname)).getJSONArray(HwSecDiagnoseConstant.ANTIMAL_APK_LIST);
            int pkgListLen = packageList.length();
            for (int i = 0; i < pkgListLen; i++) {
                int j;
                JSONObject jsonObj = packageList.getJSONObject(i);
                String pack = jsonObj.getString("name");
                PackStoragePolicy oneApp = new PackStoragePolicy();
                oneApp.packageName = pack;
                if (jsonObj.has("dir")) {
                    JSONArray dirList = jsonObj.getJSONArray("dir");
                    int dirListLen = dirList.length();
                    for (j = 0; j < dirListLen; j++) {
                        JSONObject dirObj = (JSONObject) dirList.get(j);
                        String path = dirObj.getString("name");
                        String st = dirObj.getString("StorageType");
                        if (StorageUtils.SDCARD_ROMOUNTED_STATE.equals(dirObj.getString("traversal"))) {
                            oneApp.policies.add(new PathPolicy(path, st, 17));
                        } else {
                            oneApp.policies.add(new PathPolicy(path, st, 16));
                        }
                    }
                }
                if (jsonObj.has("file")) {
                    JSONArray fileList = jsonObj.getJSONArray("file");
                    int fileListLen = fileList.length();
                    for (j = 0; j < fileListLen; j++) {
                        JSONObject fileObj = (JSONObject) fileList.get(j);
                        oneApp.policies.add(new PathPolicy(fileObj.getString("name"), fileObj.getString("StorageType"), 0));
                    }
                }
                packPolicyList.add(oneApp);
            }
        } catch (JSONException e) {
            packPolicyList.clear();
            Slog.e(TAG, e.getMessage());
        }
        return packPolicyList;
    }

    private static String readFileString(Context context, String fname) {
        IOException e;
        Throwable th;
        StringBuilder builder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fname), "UTF-8"));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    builder.append(line);
                } catch (IOException e2) {
                    e = e2;
                    bufferedReader = reader;
                } catch (Throwable th2) {
                    th = th2;
                    bufferedReader = reader;
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    Slog.e(TAG, e3.getMessage());
                }
            }
        } catch (IOException e4) {
            e3 = e4;
            try {
                Slog.e(TAG, e3.getMessage());
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e32) {
                        Slog.e(TAG, e32.getMessage());
                    }
                }
                return builder.toString();
            } catch (Throwable th3) {
                th = th3;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e322) {
                        Slog.e(TAG, e322.getMessage());
                    }
                }
                throw th;
            }
        }
        return builder.toString();
    }
}
