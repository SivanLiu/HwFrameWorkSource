package com.android.server.security.fileprotect;

import android.content.Context;
import android.util.Slog;
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
        ArrayList packPolicyList = new ArrayList();
        try {
            JSONObject json = new JSONObject(readFileString(context, fname));
            JSONArray packageList = json.getJSONArray(HwSecDiagnoseConstant.ANTIMAL_APK_LIST);
            int pkgListLen = packageList.length();
            int i = 0;
            while (i < pkgListLen) {
                JSONObject json2;
                JSONArray packageList2;
                JSONObject jsonObj = packageList.getJSONObject(i);
                String pack = jsonObj.getString("name");
                PackStoragePolicy oneApp = new PackStoragePolicy();
                oneApp.packageName = pack;
                if (jsonObj.has("dir")) {
                    JSONArray dirList = jsonObj.getJSONArray("dir");
                    int dirListLen = dirList.length();
                    int j = 0;
                    while (j < dirListLen) {
                        JSONObject dirObj = (JSONObject) dirList.get(j);
                        String path = dirObj.getString("name");
                        String st = dirObj.getString("StorageType");
                        if ("true".equals(dirObj.getString("traversal"))) {
                            json2 = json;
                            packageList2 = packageList;
                            oneApp.policies.add(new PathPolicy(path, st, 17));
                        } else {
                            json2 = json;
                            packageList2 = packageList;
                            oneApp.policies.add(new PathPolicy(path, st, 16));
                        }
                        j++;
                        json = json2;
                        packageList = packageList2;
                    }
                }
                json2 = json;
                packageList2 = packageList;
                if (jsonObj.has("file")) {
                    JSONArray fileList = jsonObj.getJSONArray("file");
                    int fileListLen = fileList.length();
                    for (int j2 = 0; j2 < fileListLen; j2++) {
                        JSONObject fileObj = (JSONObject) fileList.get(j2);
                        oneApp.policies.add(new PathPolicy(fileObj.getString("name"), fileObj.getString("StorageType"), 0));
                    }
                }
                packPolicyList.add(oneApp);
                i++;
                json = json2;
                packageList = packageList2;
            }
        } catch (JSONException e) {
            packPolicyList.clear();
            Slog.e(TAG, e.getMessage());
        }
        return packPolicyList;
    }

    private static String readFileString(Context context, String fname) {
        StringBuilder builder = new StringBuilder();
        String reader = null;
        try {
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(context.getAssets().open(fname), "UTF-8"));
            while (true) {
                String readLine = reader2.readLine();
                String line = readLine;
                if (readLine != null) {
                    builder.append(line);
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        Slog.e(TAG, e.getMessage());
                    }
                }
            }
            reader2.close();
        } catch (IOException e2) {
            Slog.e(TAG, e2.getMessage());
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    Slog.e(TAG, e3.getMessage());
                }
            }
        }
        return builder.toString();
    }
}
