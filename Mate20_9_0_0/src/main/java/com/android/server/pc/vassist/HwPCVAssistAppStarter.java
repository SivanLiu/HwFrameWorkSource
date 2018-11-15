package com.android.server.pc.vassist;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.HwPCUtils;
import com.android.server.pc.HwPCManagerService;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

public final class HwPCVAssistAppStarter {
    private static final String APP_CENTER_SEARCH_ACTION = "com.huawei.appmarket.appmarket.intent.action.SearchActivity";
    private static final String APP_CENTER_SEARCH_KEY_WORD = "keyWord";
    private static final String HIAPP_PACKAGE_NAME = "com.huawei.appmarket";
    static final int RESULT_APP_START_FAILED = 41;
    static final int RESULT_APP_START_FAILED_PARAMS = 42;
    static final int RESULT_APP_START_SUCC = -40;
    static final int RESULT_APP_START_SUCC_IN_PHONE = -41;
    static final int RESULT_APP_START_SUCC_ON_EXT_DISPLAY = -42;
    static final int RESULT_APP_START_SUCC_SEARCH_IN_MARKET = -43;
    private static final String TAG = "HwPCVAssistAppStarter";
    private static final String UNICODE_EXTERNAL_FILE_MANAGER = "\\u6211\\u7684\\u6587\\u4ef6";
    private static final String UNICODE_PHONE_FILE_MANAGER = "\\u6587\\u4ef6\\u7ba1\\u7406";
    private HwPCVAssistCmdExecutor mCmdExecutor;
    private Context mContext;
    private HwPCManagerService mService;

    public HwPCVAssistAppStarter(Context context, HwPCVAssistCmdExecutor cmdExecutor, HwPCManagerService service) {
        this.mContext = context;
        this.mCmdExecutor = cmdExecutor;
        this.mService = service;
    }

    private static String string2Unicode(String string) {
        StringBuffer unicode = new StringBuffer();
        int N = string.length();
        for (int i = 0; i < N; i++) {
            char c = string.charAt(i);
            unicode.append("\\u");
            unicode.append(Integer.toHexString(c));
        }
        return unicode.toString();
    }

    private static Intent launchIntentForPhoneFileManager() {
        ComponentName component = new ComponentName("com.huawei.hidisk", "com.huawei.hidisk.filemanager.FileManager");
        Intent intent = new Intent("android.intent.action.MAIN", null);
        intent.setComponent(component);
        intent.setFlags(268435456);
        return intent;
    }

    private static Intent launchIntentForDesktopFileManager() {
        ComponentName component = new ComponentName("com.huawei.desktop.explorer", "com.huawei.filemanager.activities.MainActivity");
        Intent intent = new Intent("android.intent.action.VIEW", null);
        intent.setComponent(component);
        intent.setFlags(402653184);
        return intent;
    }

    private static Intent searchAppFromWeb(String label) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("searchAppFromWeb: label = ");
        stringBuilder.append(label);
        HwPCUtils.log(str, stringBuilder.toString());
        if (label == null) {
            HwPCUtils.log(TAG, "Null input params");
            return null;
        }
        Intent intent = new Intent();
        intent.setAction(APP_CENTER_SEARCH_ACTION);
        intent.addFlags(268468224);
        intent.putExtra(APP_CENTER_SEARCH_KEY_WORD, label);
        intent.setPackage(HIAPP_PACKAGE_NAME);
        return intent;
    }

    private static boolean isApkSupportedInPCMode(Context context, String pkg) {
        String str;
        StringBuilder stringBuilder;
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pkg, 0);
        } catch (NameNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("warning: ");
            stringBuilder.append(e.toString());
            HwPCUtils.log(str, stringBuilder.toString());
        }
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return false;
        }
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("isApkSupportedInPCMode applicationInfo: ");
        stringBuilder.append(applicationInfo);
        HwPCUtils.log(str, stringBuilder.toString());
        if ((applicationInfo.flags & 1) != 0 && (applicationInfo.hwFlags & 33554432) == 0 && (applicationInfo.hwFlags & 67108864) == 0) {
            return false;
        }
        return true;
    }

    public void startApp(VoiceCmd cmd) {
        VoiceCmd voiceCmd = cmd;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startApp cmd = ");
        stringBuilder.append(voiceCmd);
        HwPCUtils.log(str, stringBuilder.toString());
        if (TextUtils.isEmpty(voiceCmd.pkgName) && TextUtils.isEmpty(voiceCmd.extra)) {
            HwPCUtils.log(TAG, "Category is null");
            replyResultForStartApp(42, true, voiceCmd);
            return;
        }
        HwPCUtils.bdReport(this.mContext, 10058, String.format("{appName:%s, pkgName:%s}", new Object[]{voiceCmd.pkgName, voiceCmd.extra}));
        boolean searchedInMarket = false;
        Intent intent = null;
        if (TextUtils.isEmpty(voiceCmd.pkgName) && !TextUtils.isEmpty(voiceCmd.extra)) {
            voiceCmd.pkgName = getPackageName(this.mContext, voiceCmd.extra);
            if (TextUtils.isEmpty(voiceCmd.pkgName)) {
                intent = searchAppFromWeb(voiceCmd.extra);
                if (intent == null) {
                    HwPCUtils.log(TAG, "cannot find this app in market");
                    replyResultForStartApp(42, true, voiceCmd);
                    return;
                }
                searchedInMarket = true;
                replyResultForStartApp(RESULT_APP_START_SUCC_SEARCH_IN_MARKET, true, voiceCmd);
            }
        } else if ("com.huawei.desktop.explorer".equals(voiceCmd.pkgName)) {
            intent = launchIntentForDesktopFileManager();
        } else if (UNICODE_PHONE_FILE_MANAGER.equals(string2Unicode(voiceCmd.extra))) {
            intent = launchIntentForPhoneFileManager();
        } else {
            intent = this.mContext.getPackageManager().getLaunchIntentForPackage(voiceCmd.pkgName);
            if (intent == null) {
                intent = searchAppFromWeb(voiceCmd.extra);
                if (intent == null) {
                    HwPCUtils.log(TAG, "cannot find this app in market");
                    replyResultForStartApp(42, true, voiceCmd);
                    return;
                }
                searchedInMarket = true;
                replyResultForStartApp(RESULT_APP_START_SUCC_SEARCH_IN_MARKET, true, voiceCmd);
            }
        }
        boolean searchedInMarket2 = searchedInMarket;
        if (intent == null) {
            HwPCUtils.log(TAG, "wrong parameters");
            replyResultForStartApp(42, true, voiceCmd);
            return;
        }
        searchedInMarket = false;
        boolean isForceStartedOnExternal = false;
        if (!TextUtils.isEmpty(voiceCmd.pkgName) && !isApkSupportedInPCMode(this.mContext, voiceCmd.pkgName) && this.mService.getPackageSupportPcState(voiceCmd.pkgName) == -1 && voiceCmd.targetDisplay > 0) {
            voiceCmd.targetDisplay = 0;
            HwPCUtils.log(TAG, "startApp package is not supported in external display, ignore displayId.");
            searchedInMarket = true;
        }
        boolean isForceStartedInPhone = searchedInMarket;
        if ("com.huawei.desktop.explorer".equals(voiceCmd.pkgName) && voiceCmd.targetDisplay == 0) {
            voiceCmd.targetDisplay = voiceCmd.castingDisplay;
            isForceStartedOnExternal = true;
        }
        Bundle options = null;
        if (voiceCmd.targetDisplay != 0) {
            options = new Bundle();
            options.putInt("android.activity.launchDisplayId", voiceCmd.targetDisplay);
        }
        try {
            int result = ActivityManager.getService().startActivity(ActivityThread.currentActivityThread().getApplicationThread(), this.mContext.getBasePackageName(), intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), null, null, -1, 0, null, options);
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startApp result = ");
            stringBuilder2.append(result);
            HwPCUtils.log(str, stringBuilder2.toString());
            if (!searchedInMarket2) {
                if (isForceStartedInPhone) {
                    if (result == 99 || result == 98 || !ActivityManager.isStartResultSuccessful(result)) {
                        replyResultForStartApp(result, false, voiceCmd);
                    } else {
                        replyResultForStartApp(RESULT_APP_START_SUCC_IN_PHONE, true, voiceCmd);
                    }
                } else if (isForceStartedOnExternal && ActivityManager.isStartResultSuccessful(result)) {
                    replyResultForStartApp(RESULT_APP_START_SUCC_ON_EXT_DISPLAY, true, voiceCmd);
                } else {
                    replyResultForStartApp(result, false, voiceCmd);
                }
            }
        } catch (RemoteException e) {
            replyResultForStartApp(1, false, voiceCmd);
            HwPCUtils.log(TAG, "startApp RemoteException");
        } catch (Exception e2) {
            replyResultForStartApp(1, false, voiceCmd);
            HwPCUtils.log(TAG, "startApp other Exception");
        }
    }

    private void genJsonReplyForNormalResultSuccess(JSONObject obj) throws JSONException {
        obj.put("errorCode", 0);
        String text = this.mCmdExecutor.getRandomResponseStr(this.mCmdExecutor.mAppStartSuccStrs);
        obj.put("responseText", text);
        obj.put("ttsText", text);
    }

    private void replyResultForStartApp(int errCode, boolean extResult, VoiceCmd cmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("replyResultForStartApp errCode = ");
        stringBuilder.append(errCode);
        stringBuilder.append(", extResult = ");
        stringBuilder.append(extResult);
        stringBuilder.append(", cmd = ");
        stringBuilder.append(cmd);
        HwPCUtils.log(str, stringBuilder.toString());
        JSONObject obj = new JSONObject();
        try {
            obj.put("isFinish", "true");
            String responseStr;
            if (!extResult) {
                switch (errCode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        genJsonReplyForNormalResultSuccess(obj);
                        break;
                    default:
                        switch (errCode) {
                            case 98:
                            case 99:
                                obj.put("errorCode", 0);
                                if (errCode == 98) {
                                    responseStr = this.mContext.getString(33686136);
                                } else {
                                    responseStr = this.mContext.getString(33686134);
                                }
                                responseStr = String.format(responseStr, new Object[]{cmd.extra});
                                obj.put("responseText", responseStr);
                                obj.put("ttsText", responseStr);
                                break;
                            default:
                                if (!ActivityManager.isStartResultSuccessful(errCode)) {
                                    obj.put("errorCode", 1);
                                    obj.put("responseText", this.mContext.getString(33686131));
                                    obj.put("ttsText", this.mContext.getString(33686131));
                                    break;
                                }
                                genJsonReplyForNormalResultSuccess(obj);
                                break;
                        }
                }
            }
            switch (errCode) {
                case RESULT_APP_START_SUCC_SEARCH_IN_MARKET /*-43*/:
                    obj.put("errorCode", 0);
                    responseStr = this.mContext.getString(33686141);
                    obj.put("responseText", responseStr);
                    obj.put("ttsText", responseStr);
                    break;
                case RESULT_APP_START_SUCC_ON_EXT_DISPLAY /*-42*/:
                case RESULT_APP_START_SUCC_IN_PHONE /*-41*/:
                    obj.put("errorCode", 0);
                    if (errCode == RESULT_APP_START_SUCC_IN_PHONE) {
                        responseStr = this.mContext.getString(33686155);
                    } else {
                        responseStr = this.mContext.getString(33686156);
                    }
                    responseStr = String.format(responseStr, new Object[]{cmd.extra});
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("replyResultForStartApp responseStr = ");
                    stringBuilder2.append(responseStr);
                    HwPCUtils.log(str2, stringBuilder2.toString());
                    obj.put("responseText", responseStr);
                    obj.put("ttsText", responseStr);
                    break;
                default:
                    obj.put("errorCode", 1);
                    obj.put("responseText", this.mContext.getString(33686131));
                    obj.put("ttsText", this.mContext.getString(33686131));
                    break;
            }
        } catch (JSONException e) {
            HwPCUtils.log(TAG, "replyResultForStartApp JSONException occurred");
        }
        this.mCmdExecutor.replyResultToVAssist(obj, cmd);
    }

    public static String getPackageName(Context context, String appName) {
        if (context == null || TextUtils.isEmpty(appName)) {
            HwPCUtils.log(TAG, "getPackageName input params is invalid");
            return null;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            HwPCUtils.log(TAG, "getPackageName PackageManager is invalid");
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        if (!UNICODE_EXTERNAL_FILE_MANAGER.equals(string2Unicode(appName))) {
            intent.addCategory("android.intent.category.LAUNCHER");
        }
        List<ResolveInfo> listAppInfo = pm.queryIntentActivities(intent, null);
        if (listAppInfo == null) {
            HwPCUtils.log(TAG, "getPackageName :error: listAppcations is null");
            return null;
        }
        String packageNameString = null;
        for (ResolveInfo app : listAppInfo) {
            String appLabel = (String) app.loadLabel(pm);
            if (!TextUtils.isEmpty(appLabel)) {
                if (TextUtils.equals(appLabel.toUpperCase(Locale.US), appName.toUpperCase(Locale.US))) {
                    packageNameString = app.activityInfo.packageName;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getPackageName packageName = ");
                    stringBuilder.append(packageNameString);
                    stringBuilder.append(", appLabel = ");
                    stringBuilder.append(appLabel);
                    HwPCUtils.log(str, stringBuilder.toString());
                    return packageNameString;
                } else if (appLabel.toUpperCase(Locale.US).contains(appName.toUpperCase(Locale.US))) {
                    packageNameString = app.activityInfo.packageName;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getPackageName packageName = ");
                    stringBuilder2.append(packageNameString);
                    stringBuilder2.append(", appLabel = ");
                    stringBuilder2.append(appLabel);
                    HwPCUtils.log(str2, stringBuilder2.toString());
                }
            }
        }
        listAppInfo.clear();
        return packageNameString;
    }
}
