package com.android.server.security.hwkeychain;

import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.securitydiagnose.HwSecurityDiagnoseManager;
import android.util.Flog;
import android.util.Log;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.core.IHwSecurityPlugin.Creator;
import huawei.android.security.IHwKeychainManager.Stub;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class HwKeychainService extends Stub implements IHwSecurityPlugin {
    private static final String AUTOFILL_FLAG_FILE_NAME = "autofill_flag";
    private static final String AUTOFILL_SERVICE_KEY = "autofill_service";
    private static final String AUTOFILL_SERVICE_VALUE_HW = "com.huawei.securitymgr/com.huawei.keychain.service.HwAutofillService";
    public static final Creator CREATOR = new Creator() {
        public IHwSecurityPlugin createPlugin(Context context) {
            Log.d(HwKeychainService.TAG, "create HwKeychainService");
            return new HwKeychainService(context);
        }

        public String getPluginPermission() {
            return null;
        }
    };
    private static final String IS_DECISION_REPORT_KEY = "isDecisionReport";
    private static final String IS_FILLING_USED_KEY = "isFillingUsed";
    private static final String IS_FIRST_REMIND_KEY = "isFirstRemind";
    private static final String IS_KEYCHAIN_ENABLE_KEY = "isKeychainEnable";
    private static final String IS_PRIMARY_USER_KEY = "isPrimaryUser";
    private static final String IS_ROOT_KEY = "isroot";
    private static final String IS_USED_KEY = "isUsed";
    private static final String OP_KEY = "OP";
    private static final String SYSTEM_DATA_PATH = "system";
    private static final String TAG = "HwKeychainService";
    private static final String WEEK_REMIND_KEY = "weekRemind";
    private Context mContext;

    public HwKeychainService(Context context) {
        this.mContext = context;
    }

    public IBinder asBinder() {
        return this;
    }

    public void onStart() {
    }

    public void onStop() {
    }

    private boolean isHwAutofillService(Context context) {
        if (context == null) {
            Log.e(TAG, "context is null in isHwAutofillService");
            return false;
        }
        boolean resultValue = false;
        String setting = Secure.getString(context.getContentResolver(), AUTOFILL_SERVICE_KEY);
        if (setting != null) {
            ComponentName componentName = ComponentName.unflattenFromString(setting);
            if (componentName != null) {
                resultValue = AUTOFILL_SERVICE_VALUE_HW.equals(componentName.flattenToString());
            } else {
                resultValue = false;
            }
        }
        return resultValue;
    }

    public void recordCurrentInfo(int userId) {
        Log.d(TAG, "start recordCurrentInfo");
        JSONObject dict = getConfigureDictionary();
        boolean isHwAutofillService = isHwAutofillService(this.mContext);
        boolean isChanged = false;
        if (checkFirstRemind(this.mContext, dict, isHwAutofillService)) {
            isChanged = true;
        }
        if (checkWeekRemind(this.mContext, dict, isHwAutofillService)) {
            isChanged = true;
        }
        if (isDecisionReport(dict)) {
            Log.d(TAG, "DecisionUtil: bindservice");
            Map<String, Object> extras = new HashMap();
            extras.put(IS_PRIMARY_USER_KEY, Boolean.valueOf(userId == 0));
            extras.put(IS_KEYCHAIN_ENABLE_KEY, Boolean.valueOf(isHwAutofillService));
            extras.put(IS_FILLING_USED_KEY, Boolean.valueOf(isUsedFlag(dict)));
            DecisionUtil.autoExecuteEvent(extras);
            setDecisionReport(dict);
            isChanged = true;
        }
        if (isChanged) {
            saveConfigureDictionary(dict);
        }
    }

    private File getAutofillFlagFile() {
        return new File(new File(Environment.getDataDirectory(), SYSTEM_DATA_PATH), AUTOFILL_FLAG_FILE_NAME);
    }

    private JSONObject getConfigureDictionary() {
        String str;
        StringBuilder stringBuilder;
        String str2;
        StringBuilder stringBuilder2;
        IOException e;
        StringBuilder stringBuilder3;
        File file = getAutofillFlagFile();
        if (!file.exists()) {
            return new JSONObject();
        }
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        FileInputStream fileInputStream = null;
        String readLine;
        try {
            fileInputStream = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(fileInputStream, "utf-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder4 = new StringBuilder();
            while (true) {
                readLine = bufferedReader.readLine();
                String line = readLine;
                if (readLine == null) {
                    break;
                }
                stringBuilder4.append(line);
                stringBuilder4.append("\n");
            }
            readLine = stringBuilder4.toString();
            bufferedReader.close();
            bufferedReader = null;
            inputStreamReader.close();
            inputStreamReader = null;
            fileInputStream.close();
            fileInputStream = null;
            JSONObject jSONObject = new JSONObject(readLine);
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("close bufferedReader IOException in getConfigureDictionary: ");
                    stringBuilder.append(e2.getMessage());
                    Log.e(str, stringBuilder.toString());
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e22) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("close fileReader IOException in getConfigureDictionary: ");
                    stringBuilder.append(e22.getMessage());
                    Log.e(str, stringBuilder.toString());
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("close fileInputStream IOException in getConfigureDictionary: ");
                    stringBuilder.append(e222.getMessage());
                    Log.e(str, stringBuilder.toString());
                }
            }
            return jSONObject;
        } catch (FileNotFoundException e3) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("FileNotFoundException in getConfigureDictionary: ");
            stringBuilder2.append(e3.getMessage());
            Log.e(str2, stringBuilder2.toString());
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e4) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("close bufferedReader IOException in getConfigureDictionary: ");
                    stringBuilder2.append(e4.getMessage());
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e42) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("close fileReader IOException in getConfigureDictionary: ");
                    stringBuilder2.append(e42.getMessage());
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e5) {
                    e42 = e5;
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                }
            }
            return new JSONObject();
        } catch (IOException e422) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("IOException in getConfigureDictionary: ");
            stringBuilder2.append(e422.getMessage());
            Log.e(str2, stringBuilder2.toString());
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e4222) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("close bufferedReader IOException in getConfigureDictionary: ");
                    stringBuilder2.append(e4222.getMessage());
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e42222) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("close fileReader IOException in getConfigureDictionary: ");
                    stringBuilder2.append(e42222.getMessage());
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e6) {
                    e42222 = e6;
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                }
            }
            return new JSONObject();
        } catch (JSONException e7) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("JSONException in getConfigureDictionary: ");
            stringBuilder2.append(e7.getMessage());
            Log.e(str2, stringBuilder2.toString());
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e422222) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("close bufferedReader IOException in getConfigureDictionary: ");
                    stringBuilder2.append(e422222.getMessage());
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e4222222) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("close fileReader IOException in getConfigureDictionary: ");
                    stringBuilder2.append(e4222222.getMessage());
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e8) {
                    e4222222 = e8;
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                }
            }
            return new JSONObject();
        } catch (Throwable th) {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e9) {
                    readLine = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("close bufferedReader IOException in getConfigureDictionary: ");
                    stringBuilder3.append(e9.getMessage());
                    Log.e(readLine, stringBuilder3.toString());
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e92) {
                    readLine = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("close fileReader IOException in getConfigureDictionary: ");
                    stringBuilder3.append(e92.getMessage());
                    Log.e(readLine, stringBuilder3.toString());
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e922) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("close fileInputStream IOException in getConfigureDictionary: ");
                    stringBuilder2.append(e922.getMessage());
                    Log.e(TAG, stringBuilder2.toString());
                }
            }
        }
        stringBuilder2.append("close fileInputStream IOException in getConfigureDictionary: ");
        stringBuilder2.append(e4222222.getMessage());
        Log.e(str2, stringBuilder2.toString());
        return new JSONObject();
    }

    private void saveConfigureDictionary(JSONObject dict) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        if (dict != null) {
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(getAutofillFlagFile());
                outputStream.write(dict.toString().getBytes("utf-8"));
                outputStream.flush();
                outputStream.close();
                outputStream = null;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                        e = e2;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (FileNotFoundException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("FileNotFoundException in saveConfigureDictionary: ");
                stringBuilder.append(e3.getMessage());
                Log.e(str, stringBuilder.toString());
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e4) {
                        e = e4;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (IOException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IOException in saveConfigureDictionary: ");
                stringBuilder.append(e5.getMessage());
                Log.e(str, stringBuilder.toString());
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e6) {
                        e5 = e6;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e7) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("close outputStream IOException in saveConfigureDictionary: ");
                        stringBuilder.append(e7.getMessage());
                        Log.e(TAG, stringBuilder.toString());
                    }
                }
            }
        }
        return;
        stringBuilder.append("close outputStream IOException in saveConfigureDictionary: ");
        stringBuilder.append(e5.getMessage());
        Log.e(str, stringBuilder.toString());
    }

    private boolean checkFirstRemind(Context context, JSONObject dict, boolean isHwAutofillService) {
        String str;
        StringBuilder stringBuilder;
        boolean isFirstRemind = true;
        if (dict.has(IS_FIRST_REMIND_KEY)) {
            try {
                isFirstRemind = dict.getBoolean(IS_FIRST_REMIND_KEY);
            } catch (JSONException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("JSON exception in checkFirstRemind get isFirstRemind: ");
                stringBuilder2.append(e.getMessage());
                Log.e(str2, stringBuilder2.toString());
            }
        }
        if (isFirstRemind) {
            JSONObject eventDict = new JSONObject();
            try {
                eventDict.put(OP_KEY, isHwAutofillService);
                eventDict.put(IS_ROOT_KEY, isRoot());
            } catch (JSONException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("JSON exception in checkFirstRemind event isFirstRemind: ");
                stringBuilder.append(e2.getMessage());
                Log.e(str, stringBuilder.toString());
            }
            Flog.bdReport(context, 700, eventDict);
            try {
                dict.put(IS_FIRST_REMIND_KEY, false);
            } catch (JSONException e22) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("JSON exception in checkFirstRemind update isFirstRemind: ");
                stringBuilder.append(e22.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
        return isFirstRemind;
    }

    private boolean checkWeekRemind(Context context, JSONObject dict, boolean isHwAutofillService) {
        String str;
        StringBuilder stringBuilder;
        String weekRemind = null;
        if (dict.has(WEEK_REMIND_KEY)) {
            try {
                weekRemind = dict.getString(WEEK_REMIND_KEY);
            } catch (JSONException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("JSON exception in checkWeekRemind get weekRemind: ");
                stringBuilder2.append(e.getMessage());
                Log.e(str2, stringBuilder2.toString());
            }
        }
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(3);
        int year = calendar.get(1);
        String currentWeekRemind = new StringBuilder();
        currentWeekRemind.append(year);
        currentWeekRemind.append("-");
        currentWeekRemind.append(week);
        currentWeekRemind = currentWeekRemind.toString();
        if (currentWeekRemind.equals(weekRemind)) {
            return false;
        }
        JSONObject eventDict = new JSONObject();
        try {
            eventDict.put(OP_KEY, isHwAutofillService);
            eventDict.put(IS_ROOT_KEY, isRoot());
        } catch (JSONException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("JSON exception in checkWeekRemind event: ");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        Flog.bdReport(context, 701, eventDict);
        try {
            dict.put(WEEK_REMIND_KEY, currentWeekRemind);
        } catch (JSONException e22) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("JSON exception in checkWeekRemind update: ");
            stringBuilder.append(e22.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return true;
    }

    private boolean isUsedFlag(JSONObject dict) {
        try {
            if (dict.has(IS_USED_KEY) && dict.getBoolean(IS_USED_KEY)) {
                return true;
            }
        } catch (JSONException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("JSON exception in isUsedFlag: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return false;
    }

    private boolean isDecisionReport(JSONObject dict) {
        String decisionReport = null;
        if (dict.has(IS_DECISION_REPORT_KEY)) {
            try {
                decisionReport = dict.getString(IS_DECISION_REPORT_KEY);
            } catch (JSONException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("JSON exception in isDecisionReport get decisionReport: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(1);
        int month = calendar.get(2);
        int day = calendar.get(5);
        String currentDecisionReport = new StringBuilder();
        currentDecisionReport.append(year);
        currentDecisionReport.append("-");
        currentDecisionReport.append(month);
        currentDecisionReport.append("-");
        currentDecisionReport.append(day);
        return true ^ currentDecisionReport.toString().equals(decisionReport);
    }

    private void setDecisionReport(JSONObject dict) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(1);
        int month = calendar.get(2);
        int day = calendar.get(5);
        String currentDecisionReport = new StringBuilder();
        currentDecisionReport.append(year);
        currentDecisionReport.append("-");
        currentDecisionReport.append(month);
        currentDecisionReport.append("-");
        currentDecisionReport.append(day);
        try {
            dict.put(IS_DECISION_REPORT_KEY, currentDecisionReport.toString());
        } catch (JSONException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("JSON exception in setDecisionReport: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private boolean isRoot() {
        long identity = Binder.clearCallingIdentity();
        boolean z = true;
        int rootstatus = 1;
        try {
            HwSecurityDiagnoseManager mSdm = HwSecurityDiagnoseManager.getInstance();
            if (mSdm == null) {
                Binder.restoreCallingIdentity(identity);
                return true;
            }
            rootstatus = mSdm.getRootStatusSync();
            Binder.restoreCallingIdentity(identity);
            if (rootstatus == 0) {
                z = false;
            }
            return z;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isRoot error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
