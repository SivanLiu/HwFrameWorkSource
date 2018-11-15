package com.android.server.security.securityprofile;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InstallerDataBase {
    private static InstallerDataBase sInstance = null;
    private final String DB_FILE_PATH = "/data/system/securityprofileinstallerDB.json";
    private final String PACKAGE_NAME = "packageName";
    private final String PACKAGE_NAME_INSTALLER = "installer";
    private final String TAG = "InstallerDataBase";
    private final String databaseName = "";
    private Path mFile = Paths.get("/data/system/securityprofileinstallerDB.json", new String[0]);
    PackageManager mPackageManager = null;

    private class AppInstallerInfo {
        private String installer;
        private String packageName;

        public AppInstallerInfo(String packageName, String installer) {
            this.installer = installer;
            this.packageName = packageName;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public String getInstaller() {
            return this.installer;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public void setInstaller(String installer) {
            this.installer = installer;
        }
    }

    public static InstallerDataBase getInstance() {
        if (sInstance == null) {
            sInstance = new InstallerDataBase();
        }
        return sInstance;
    }

    private InstallerDataBase() {
    }

    private String getInstallerFromSys(Context context, String packageName) {
        String installer = null;
        PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            Log.e("InstallerDataBase", "[getInstaller] mPackageManager is null");
            return null;
        }
        try {
            installer = packageManager.getInstallerPackageName(packageName);
            if (!(installer == null || TextUtils.isEmpty(installer))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[From sys]");
                stringBuilder.append(packageName);
                stringBuilder.append(" got installer from system api installer = ");
                stringBuilder.append(installer);
                Log.d("InstallerDataBase", stringBuilder.toString());
            }
        } catch (IllegalArgumentException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getInstaller IllegalArgumentException:");
            stringBuilder2.append(e.getMessage());
            Log.e("InstallerDataBase", stringBuilder2.toString());
        }
        return installer;
    }

    public JSONArray setInstallerPackageName(Context context, String packageName) {
        if (context == null || packageName == null || TextUtils.isEmpty(packageName)) {
            Log.e("InstallerDataBase", "[addInstallerInfo] context is null or packagename error");
            return null;
        }
        String installer = getInstallerFromSys(context, packageName);
        JSONArray res = null;
        try {
            StringBuilder stringBuilder;
            String allInstallerInfo = readDataBase();
            if (allInstallerInfo == null || TextUtils.isEmpty(allInstallerInfo)) {
                res = new JSONArray();
            } else {
                res = new JSONArray(allInstallerInfo);
                int index = indexOfJSONArray(res, packageName);
                stringBuilder = new StringBuilder();
                stringBuilder.append("index = ");
                stringBuilder.append(index);
                Log.d("InstallerDataBase", stringBuilder.toString());
                if (index > -1) {
                    res.remove(index);
                }
            }
            JSONObject installInfo = new JSONObject();
            installInfo.put("packageName", packageName);
            installInfo.put("installer", installer);
            stringBuilder = new StringBuilder();
            stringBuilder.append("add  jsonObject = ");
            stringBuilder.append(installInfo);
            Log.d("InstallerDataBase", stringBuilder.toString());
            res.put(installInfo);
            writeDataBase(res.toString());
            return res;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getInstaller(Context context, String packageName) {
        String installer = getInstallerFromSys(context, packageName);
        if (installer == null || TextUtils.isEmpty(installer)) {
            installer = getInstallerFromDb(packageName);
            if (!(installer == null || TextUtils.isEmpty(installer))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[From seapp]");
                stringBuilder.append(packageName);
                stringBuilder.append(" got installer from seapp database installer = ");
                stringBuilder.append(installer);
                Log.d("InstallerDataBase", stringBuilder.toString());
            }
        }
        return installer;
    }

    private String getInstallerFromDb(String packageName) {
        String installerList = "";
        if (packageName == null || TextUtils.isEmpty(packageName)) {
            Log.e("InstallerDataBase", "getInstallerFromDb input para error");
            return null;
        }
        installerList = readDataBase();
        if (installerList == null) {
            return null;
        }
        try {
            return (String) parseJSONHashMap(installerList).get(packageName);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readDataBase() {
        String installerJson = "";
        if (Files.exists(this.mFile, new LinkOption[0])) {
            try {
                installerJson = new String(Files.readAllBytes(this.mFile), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return installerJson;
        }
        Log.d("InstallerDataBase", "install db file is not exist");
        return null;
    }

    private void writeDataBase(String installerJson) {
        if (Files.exists(this.mFile, new LinkOption[0])) {
            try {
                Files.write(this.mFile, installerJson.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            Files.write(this.mFile, installerJson.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    private HashMap<String, String> parseJSONHashMap(String installerList) throws JSONException {
        HashMap<String, String> res = new HashMap();
        JSONArray appInstalledInfoArray = new JSONArray(installerList);
        int length = appInstalledInfoArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject appInstalledInfo = (JSONObject) appInstalledInfoArray.get(i);
            res.put(appInstalledInfo.optString("packageName"), appInstalledInfo.optString("installer"));
        }
        return res;
    }

    private ArrayList<AppInstallerInfo> parseJSONArrayList(String installerList) throws JSONException {
        ArrayList<AppInstallerInfo> res = new ArrayList();
        JSONArray appInstalledInfoArray = new JSONArray(installerList);
        int length = appInstalledInfoArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject appInstalledInfo = (JSONObject) appInstalledInfoArray.get(i);
            res.add(new AppInstallerInfo(appInstalledInfo.optString("packageName"), appInstalledInfo.optString("installer")));
        }
        return res;
    }

    private int indexOfJSONArray(JSONArray jsonArray, String value) {
        if (jsonArray == null) {
            return -1;
        }
        int length = jsonArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (jsonObject != null && jsonObject.optString("packageName").equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private JSONArray generateJSON(ArrayList<AppInstallerInfo> appInstalledInfoArray) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        int size = appInstalledInfoArray.size();
        for (int i = 0; i < size; i++) {
            jsonObject.put("packageName", ((AppInstallerInfo) appInstalledInfoArray.get(i)).getPackageName());
            jsonObject.put("installer", ((AppInstallerInfo) appInstalledInfoArray.get(i)).getInstaller());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }
}
