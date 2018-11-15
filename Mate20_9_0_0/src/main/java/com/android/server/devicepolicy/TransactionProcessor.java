package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.location.HwGpsPowerTracker;
import com.android.server.pm.HwPackageManagerService;
import com.android.server.rms.iaware.cpu.CPUFeature;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionProcessor {
    protected static final boolean HWDBG = false;
    protected static final boolean HWFLOW;
    private static final String TAG = "TransactionProcessor";
    private IHwDevicePolicyManager mService;

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    TransactionProcessor(IHwDevicePolicyManager service) {
        this.mService = service;
    }

    /* JADX WARNING: Missing block: B:166:0x0435, code:
            r12.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:167:0x043f, code:
            if (r27.readInt() == 0) goto L_0x0445;
     */
    /* JADX WARNING: Missing block: B:168:0x0441, code:
            r0 = android.content.ComponentName.readFromParcel(r27);
     */
    /* JADX WARNING: Missing block: B:171:?, code:
            r0 = getListCommand(r11, r0, r27.readInt());
            r28.writeNoException();
            r13.writeStringList(r0);
     */
    /* JADX WARNING: Missing block: B:172:0x0456, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:173:0x0457, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("getListCommand exception is ");
            r5.append(r0);
            android.util.Log.e(r4, r5.toString());
            r13.writeException(r0);
     */
    /* JADX WARNING: Missing block: B:190:0x04e1, code:
            r12.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:191:0x04eb, code:
            if (r27.readInt() == 0) goto L_0x04f1;
     */
    /* JADX WARNING: Missing block: B:192:0x04ed, code:
            r0 = android.content.ComponentName.readFromParcel(r27);
     */
    /* JADX WARNING: Missing block: B:195:?, code:
            execCommand(r11, r0, r27.readString(), r27.readInt());
            r28.writeNoException();
     */
    /* JADX WARNING: Missing block: B:196:0x0502, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:197:0x0503, code:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("execCommand exception is ");
            r6.append(r0);
            android.util.Log.e(r5, r6.toString());
            r13.writeException(r0);
     */
    /* JADX WARNING: Missing block: B:210:0x0561, code:
            r12.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:211:0x056b, code:
            if (r27.readInt() == 0) goto L_0x0571;
     */
    /* JADX WARNING: Missing block: B:212:0x056d, code:
            r0 = android.content.ComponentName.readFromParcel(r27);
     */
    /* JADX WARNING: Missing block: B:213:0x0571, code:
            r2 = r0;
            r3 = new java.util.ArrayList();
            r12.readStringList(r3);
     */
    /* JADX WARNING: Missing block: B:215:?, code:
            execCommand(r11, r2, r3, r27.readInt());
            r28.writeNoException();
     */
    /* JADX WARNING: Missing block: B:216:0x0587, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:217:0x0588, code:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("execCommand exception is ");
            r6.append(r0);
            android.util.Log.e(r5, r6.toString());
            r13.writeException(r0);
     */
    /* JADX WARNING: Missing block: B:227:0x05f1, code:
            r12.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:228:0x05fb, code:
            if (r27.readInt() == 0) goto L_0x0601;
     */
    /* JADX WARNING: Missing block: B:229:0x05fd, code:
            r0 = android.content.ComponentName.readFromParcel(r27);
     */
    /* JADX WARNING: Missing block: B:232:?, code:
            execCommand(r11, r0, r27.readInt());
            r28.writeNoException();
     */
    /* JADX WARNING: Missing block: B:233:0x060e, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:234:0x060f, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("execCommand exception is ");
            r5.append(r0);
            android.util.Log.e(r4, r5.toString());
            r13.writeException(r0);
     */
    /* JADX WARNING: Missing block: B:236:0x0629, code:
            r12.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:237:0x0634, code:
            if (r27.readInt() == 0) goto L_0x063a;
     */
    /* JADX WARNING: Missing block: B:238:0x0636, code:
            r0 = android.content.ComponentName.readFromParcel(r27);
     */
    /* JADX WARNING: Missing block: B:239:0x063a, code:
            r1 = isFunctionDisabled(r11, r0, r27.readInt());
            r28.writeNoException();
            r13.writeInt(r1);
     */
    /* JADX WARNING: Missing block: B:240:0x0648, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:241:0x0649, code:
            r12.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:242:0x0653, code:
            if (r27.readInt() == 0) goto L_0x0659;
     */
    /* JADX WARNING: Missing block: B:243:0x0655, code:
            r1 = android.content.ComponentName.readFromParcel(r27);
     */
    /* JADX WARNING: Missing block: B:245:0x065d, code:
            if (r27.readInt() != 1) goto L_0x0661;
     */
    /* JADX WARNING: Missing block: B:246:0x065f, code:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:249:?, code:
            setFunctionDisabled(r11, r1, r0, r27.readInt());
            r28.writeNoException();
     */
    /* JADX WARNING: Missing block: B:250:0x066e, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:251:0x066f, code:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("setFunctionDisabled exception is ");
            r6.append(r0);
            android.util.Log.e(r5, r6.toString());
            r13.writeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean processTransaction(int code, Parcel data, Parcel reply) {
        String str;
        StringBuilder stringBuilder;
        Exception e;
        StringBuilder stringBuilder2;
        int i = code;
        Parcel parcel = data;
        Parcel parcel2 = reply;
        boolean z = false;
        switch (i) {
            case 1004:
            case 1006:
            case 1008:
            case 1010:
            case 1012:
            case 1014:
            case 1016:
            case 1018:
            case 1020:
            case 1022:
            case 1024:
            case 1026:
            case 1028:
            case HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK /*1030*/:
            case 1032:
            case 1034:
            case 1036:
            case 1038:
                break;
            case 1005:
            case 1007:
            case 1009:
            case HwPackageManagerService.TRANSACTION_CODE_GET_HDB_KEY /*1011*/:
            case 1013:
            case 1015:
            case 1017:
            case 1019:
            case 1021:
            case 1023:
            case 1025:
            case 1027:
            case 1029:
            case 1031:
            case 1033:
            case 1035:
            case 1037:
            case 1039:
                break;
            default:
                ComponentName who;
                String packageName;
                String className;
                int userHandle;
                String str2;
                StringBuilder stringBuilder3;
                ComponentName who2;
                StringBuilder stringBuilder4;
                switch (i) {
                    case 1501:
                    case 1502:
                    case 1507:
                    case 1512:
                        break;
                    case 1503:
                    case 1505:
                        break;
                    case 1504:
                    case 1513:
                        break;
                    case 1506:
                        parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                        who = null;
                        if (data.readInt() != 0) {
                            who = ComponentName.readFromParcel(data);
                        }
                        packageName = data.readString();
                        className = data.readString();
                        userHandle = data.readInt();
                        if (HWFLOW) {
                            str2 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("HwDPMS received transaction_setDefaultLauncher packageName: ");
                            stringBuilder3.append(packageName);
                            stringBuilder3.append(", className: ");
                            stringBuilder3.append(className);
                            stringBuilder3.append(", user: ");
                            stringBuilder3.append(userHandle);
                            Log.i(str2, stringBuilder3.toString());
                        }
                        this.mService.setDefaultLauncher(who, packageName, className, userHandle);
                        reply.writeNoException();
                        return true;
                    case 1508:
                        break;
                    case 1509:
                        parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                        who2 = null;
                        if (data.readInt() != 0) {
                            who2 = ComponentName.readFromParcel(data);
                        }
                        int userHandle2 = data.readInt();
                        if (HWFLOW) {
                            String str3 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("HwDPMS received transaction_captureScreen user : ");
                            stringBuilder4.append(userHandle2);
                            Log.i(str3, stringBuilder4.toString());
                        }
                        Bitmap bitmapScreen = this.mService.captureScreen(who2, userHandle2);
                        reply.writeNoException();
                        if (bitmapScreen != null) {
                            parcel2.writeInt(1);
                            bitmapScreen.writeToParcel(parcel2, 0);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 1510:
                    case 1511:
                        break;
                    default:
                        int userHandle3;
                        String str4;
                        switch (i) {
                            case 2501:
                            case 2503:
                                break;
                            case 2502:
                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                who2 = null;
                                if (data.readInt() != 0) {
                                    who2 = ComponentName.readFromParcel(data);
                                }
                                className = data.readString();
                                if (data.readInt() == 1) {
                                    z = true;
                                }
                                boolean keepData = z;
                                userHandle3 = data.readInt();
                                try {
                                    if (HWFLOW) {
                                        str4 = TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("HwDPMS received transaction_uninstallPackage packageName: ");
                                        stringBuilder3.append(className);
                                        stringBuilder3.append(", keepData: ");
                                        stringBuilder3.append(keepData);
                                        stringBuilder3.append(", user: ");
                                        stringBuilder3.append(userHandle3);
                                        Log.i(str4, stringBuilder3.toString());
                                    }
                                    this.mService.uninstallPackage(who2, className, keepData, userHandle3);
                                    reply.writeNoException();
                                } catch (Exception e2) {
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("execCommand exception is ");
                                    stringBuilder.append(e2);
                                    Log.e(str, stringBuilder.toString());
                                    parcel2.writeException(e2);
                                }
                                return true;
                            case 2504:
                                break;
                            case 2505:
                            case 2508:
                            case 2509:
                            case 2511:
                            case 2512:
                            case 2514:
                            case 2515:
                                break;
                            case 2506:
                                break;
                            case 2507:
                            case 2510:
                            case 2513:
                            case 2516:
                                break;
                            default:
                                switch (i) {
                                    case HwGpsPowerTracker.EVENT_REMOVE_PACKAGE_LOCATION /*3001*/:
                                    case HwArbitrationDEFS.MSG_Display_Start_Monitor_Network /*3002*/:
                                    case 3004:
                                    case 3005:
                                        break;
                                    case HwArbitrationDEFS.MSG_Display_stop_Monotor_network /*3003*/:
                                    case 3006:
                                        break;
                                    case 3007:
                                        break;
                                    default:
                                        ComponentName who3;
                                        switch (i) {
                                            case 3501:
                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                who = null;
                                                Bundle paraex = new Bundle();
                                                if (data.readInt() != 0) {
                                                    who = ComponentName.readFromParcel(data);
                                                }
                                                who3 = who;
                                                paraex.readFromParcel(parcel);
                                                userHandle = data.readInt();
                                                try {
                                                    if (HWFLOW) {
                                                        str4 = TAG;
                                                        stringBuilder4 = new StringBuilder();
                                                        stringBuilder4.append("HwDPMS received ConstantValue.transaction_configExchangeMail, user: ");
                                                        stringBuilder4.append(userHandle);
                                                        Log.i(str4, stringBuilder4.toString());
                                                    }
                                                    this.mService.configExchangeMailProvider(who3, paraex, userHandle);
                                                    reply.writeNoException();
                                                } catch (Exception e22) {
                                                    str2 = TAG;
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("configExchangeMailProvider exception is ");
                                                    stringBuilder3.append(e22);
                                                    Log.e(str2, stringBuilder3.toString());
                                                    parcel2.writeException(e22);
                                                }
                                                return true;
                                            case 3502:
                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                who2 = null;
                                                if (data.readInt() != 0) {
                                                    who2 = ComponentName.readFromParcel(data);
                                                }
                                                className = data.readString();
                                                userHandle = data.readInt();
                                                if (HWFLOW) {
                                                    str2 = TAG;
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("HwDPMS received transaction_configExchangeMail domain: ");
                                                    stringBuilder3.append(className);
                                                    stringBuilder3.append(", user: ");
                                                    stringBuilder3.append(userHandle);
                                                    Log.i(str2, stringBuilder3.toString());
                                                }
                                                Bundle para = this.mService.getMailProviderForDomain(who2, className, userHandle);
                                                reply.writeNoException();
                                                if (para != null) {
                                                    parcel2.writeInt(1);
                                                    para.writeToParcel(parcel2, 0);
                                                } else {
                                                    parcel2.writeInt(0);
                                                }
                                                return true;
                                            default:
                                                switch (i) {
                                                    case 4001:
                                                    case 4002:
                                                    case 4003:
                                                    case 4009:
                                                    case 4011:
                                                    case 4012:
                                                    case 4013:
                                                    case 4014:
                                                    case 4015:
                                                    case 4016:
                                                    case 4017:
                                                    case 4018:
                                                    case 4021:
                                                    case 4022:
                                                    case 4023:
                                                    case 4024:
                                                    case 4025:
                                                    case 4026:
                                                        break;
                                                    case 4004:
                                                    case 4005:
                                                    case 4006:
                                                    case 4007:
                                                    case 4008:
                                                    case 4010:
                                                    case 4019:
                                                    case 4020:
                                                    case 4027:
                                                    case 4028:
                                                        break;
                                                    default:
                                                        HashMap apnInfo;
                                                        int userHandle4;
                                                        int encryptionStatus;
                                                        switch (i) {
                                                            case 5001:
                                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                who = null;
                                                                if (data.readInt() != 0) {
                                                                    who = ComponentName.readFromParcel(data);
                                                                }
                                                                who3 = who;
                                                                apnInfo = new HashMap();
                                                                parcel.readMap(apnInfo, null);
                                                                userHandle4 = data.readInt();
                                                                try {
                                                                    if (HWFLOW) {
                                                                        str4 = TAG;
                                                                        stringBuilder4 = new StringBuilder();
                                                                        stringBuilder4.append("HwDPMS received ConstantValue.transaction_addApn, user: ");
                                                                        stringBuilder4.append(userHandle4);
                                                                        Log.i(str4, stringBuilder4.toString());
                                                                    }
                                                                    this.mService.addApn(who3, apnInfo, userHandle4);
                                                                    reply.writeNoException();
                                                                } catch (Exception e222) {
                                                                    str2 = TAG;
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("addApn exception is ");
                                                                    stringBuilder3.append(e222);
                                                                    Log.e(str2, stringBuilder3.toString());
                                                                    parcel2.writeException(e222);
                                                                }
                                                                return true;
                                                            case 5002:
                                                            case 5006:
                                                                break;
                                                            case 5003:
                                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                who = null;
                                                                if (data.readInt() != 0) {
                                                                    who = ComponentName.readFromParcel(data);
                                                                }
                                                                who3 = who;
                                                                apnInfo = new HashMap();
                                                                parcel.readMap(apnInfo, null);
                                                                String apnId = data.readString();
                                                                userHandle3 = data.readInt();
                                                                try {
                                                                    if (HWFLOW) {
                                                                        str4 = TAG;
                                                                        stringBuilder3 = new StringBuilder();
                                                                        stringBuilder3.append("HwDPMS received ConstantValue.transaction_updateApn, user: ");
                                                                        stringBuilder3.append(userHandle3);
                                                                        Log.i(str4, stringBuilder3.toString());
                                                                    }
                                                                    this.mService.updateApn(who3, apnInfo, apnId, userHandle3);
                                                                    reply.writeNoException();
                                                                } catch (Exception e2222) {
                                                                    str = TAG;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("addApn exception is ");
                                                                    stringBuilder.append(e2222);
                                                                    Log.e(str, stringBuilder.toString());
                                                                    parcel2.writeException(e2222);
                                                                }
                                                                return true;
                                                            case 5004:
                                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                who = null;
                                                                if (data.readInt() != 0) {
                                                                    who = ComponentName.readFromParcel(data);
                                                                }
                                                                ComponentName who4 = who;
                                                                className = data.readString();
                                                                userHandle = data.readInt();
                                                                try {
                                                                    if (HWFLOW) {
                                                                        str4 = TAG;
                                                                        stringBuilder4 = new StringBuilder();
                                                                        stringBuilder4.append("HwDPMS received ConstantValue.transaction_updateApn, user: ");
                                                                        stringBuilder4.append(userHandle);
                                                                        Log.i(str4, stringBuilder4.toString());
                                                                    }
                                                                    Map<String, String> apnInfo2 = this.mService.getApnInfo(who4, className, userHandle);
                                                                    reply.writeNoException();
                                                                    parcel2.writeMap(apnInfo2);
                                                                } catch (Exception e22222) {
                                                                    str2 = TAG;
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("addApn exception is ");
                                                                    stringBuilder3.append(e22222);
                                                                    Log.e(str2, stringBuilder3.toString());
                                                                    parcel2.writeException(e22222);
                                                                }
                                                                return true;
                                                            case 5005:
                                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                who = null;
                                                                if (data.readInt() != 0) {
                                                                    who = ComponentName.readFromParcel(data);
                                                                }
                                                                who3 = who;
                                                                apnInfo = new HashMap();
                                                                parcel.readMap(apnInfo, null);
                                                                userHandle4 = data.readInt();
                                                                try {
                                                                    if (HWFLOW) {
                                                                        str4 = TAG;
                                                                        stringBuilder4 = new StringBuilder();
                                                                        stringBuilder4.append("HwDPMS received ConstantValue.transaction_updateApn, user: ");
                                                                        stringBuilder4.append(userHandle4);
                                                                        Log.i(str4, stringBuilder4.toString());
                                                                    }
                                                                    List<String> ids = this.mService.queryApn(who3, apnInfo, userHandle4);
                                                                    reply.writeNoException();
                                                                    parcel2.writeStringList(ids);
                                                                } catch (Exception e222222) {
                                                                    str2 = TAG;
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("addApn exception is ");
                                                                    stringBuilder3.append(e222222);
                                                                    Log.e(str2, stringBuilder3.toString());
                                                                    parcel2.writeException(e222222);
                                                                }
                                                                return true;
                                                            case 5007:
                                                            case 5008:
                                                                break;
                                                            case 5009:
                                                                break;
                                                            case 5010:
                                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                encryptionStatus = getSDCardEncryptionStatus(code);
                                                                reply.writeNoException();
                                                                parcel2.writeInt(encryptionStatus);
                                                                return true;
                                                            case 5011:
                                                                break;
                                                            case 5012:
                                                                break;
                                                            default:
                                                                boolean formatResult;
                                                                switch (i) {
                                                                    case 5017:
                                                                        parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                        who2 = null;
                                                                        if (data.readInt() != 0) {
                                                                            who2 = ComponentName.readFromParcel(data);
                                                                        }
                                                                        formatResult = false;
                                                                        try {
                                                                            formatResult = formatSDCard(who2, data.readString(), data.readInt());
                                                                            reply.writeNoException();
                                                                            if (formatResult) {
                                                                                encryptionStatus = 1;
                                                                            }
                                                                            parcel2.writeInt(encryptionStatus);
                                                                        } catch (Exception e2222222) {
                                                                            str = TAG;
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("formatSDCard exception is ");
                                                                            stringBuilder.append(e2222222);
                                                                            Log.e(str, stringBuilder.toString());
                                                                            parcel2.writeException(e2222222);
                                                                        }
                                                                        return true;
                                                                    case 5018:
                                                                        parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                        who2 = null;
                                                                        if (data.readInt() != 0) {
                                                                            who2 = ComponentName.readFromParcel(data);
                                                                        }
                                                                        className = data.readString();
                                                                        if (data.readInt() == 1) {
                                                                            z = true;
                                                                        }
                                                                        try {
                                                                            setAccountDisabled(who2, className, z, data.readInt());
                                                                            reply.writeNoException();
                                                                        } catch (Exception e22222222) {
                                                                            str = TAG;
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("formatSDCard exception is ");
                                                                            stringBuilder.append(e22222222);
                                                                            Log.e(str, stringBuilder.toString());
                                                                            parcel2.writeException(e22222222);
                                                                        }
                                                                        return true;
                                                                    case 5019:
                                                                        parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                        who2 = null;
                                                                        if (data.readInt() != 0) {
                                                                            who2 = ComponentName.readFromParcel(data);
                                                                        }
                                                                        formatResult = false;
                                                                        try {
                                                                            formatResult = isAccountDisabled(who2, data.readString(), data.readInt());
                                                                            reply.writeNoException();
                                                                            if (formatResult) {
                                                                                encryptionStatus = 1;
                                                                            }
                                                                            parcel2.writeInt(encryptionStatus);
                                                                        } catch (Exception e222222222) {
                                                                            str = TAG;
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("formatSDCard exception is ");
                                                                            stringBuilder.append(e222222222);
                                                                            Log.e(str, stringBuilder.toString());
                                                                            parcel2.writeException(e222222222);
                                                                        }
                                                                        return true;
                                                                    case 5020:
                                                                        parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                        who2 = null;
                                                                        if (data.readInt() != 0) {
                                                                            who2 = ComponentName.readFromParcel(data);
                                                                        }
                                                                        ComponentName who5 = who2;
                                                                        int type = data.readInt();
                                                                        int len = data.readInt();
                                                                        byte[] certBuffer = new byte[len];
                                                                        parcel.readByteArray(certBuffer);
                                                                        boolean installResult = false;
                                                                        boolean installResult2;
                                                                        try {
                                                                            installResult2 = installCertificateWithType(who5, type, certBuffer, data.readString(), data.readString(), data.readInt(), data.readInt() == 1, data.readInt());
                                                                            try {
                                                                                reply.writeNoException();
                                                                                if (installResult2) {
                                                                                    encryptionStatus = 1;
                                                                                }
                                                                                parcel2.writeInt(encryptionStatus);
                                                                            } catch (Exception e3) {
                                                                                e222222222 = e3;
                                                                                packageName = TAG;
                                                                                stringBuilder2 = new StringBuilder();
                                                                                stringBuilder2.append("install user cert exception is ");
                                                                                stringBuilder2.append(e222222222);
                                                                                Log.e(packageName, stringBuilder2.toString());
                                                                                parcel2.writeException(e222222222);
                                                                                return true;
                                                                            }
                                                                        } catch (Exception e4) {
                                                                            e222222222 = e4;
                                                                            installResult2 = installResult;
                                                                            packageName = TAG;
                                                                            stringBuilder2 = new StringBuilder();
                                                                            stringBuilder2.append("install user cert exception is ");
                                                                            stringBuilder2.append(e222222222);
                                                                            Log.e(packageName, stringBuilder2.toString());
                                                                            parcel2.writeException(e222222222);
                                                                            return true;
                                                                        }
                                                                        return true;
                                                                    case 5021:
                                                                    case 5022:
                                                                        break;
                                                                    default:
                                                                        switch (i) {
                                                                            case 7004:
                                                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                                who2 = null;
                                                                                if (data.readInt() != 0) {
                                                                                    who2 = ComponentName.readFromParcel(data);
                                                                                }
                                                                                boolean formatResult2 = false;
                                                                                try {
                                                                                    formatResult2 = setCarrierLockScreenPassword(who2, data.readString(), data.readString(), data.readInt());
                                                                                    reply.writeNoException();
                                                                                    if (formatResult2) {
                                                                                        encryptionStatus = 1;
                                                                                    }
                                                                                    parcel2.writeInt(encryptionStatus);
                                                                                } catch (Exception e2222222222) {
                                                                                    String str5 = TAG;
                                                                                    StringBuilder stringBuilder5 = new StringBuilder();
                                                                                    stringBuilder5.append("set carrierlockscreenpassword exception is ");
                                                                                    stringBuilder5.append(e2222222222);
                                                                                    Log.e(str5, stringBuilder5.toString());
                                                                                    parcel2.writeException(e2222222222);
                                                                                }
                                                                                return true;
                                                                            case 7005:
                                                                                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                                                                                who2 = null;
                                                                                if (data.readInt() != 0) {
                                                                                    who2 = ComponentName.readFromParcel(data);
                                                                                }
                                                                                formatResult = false;
                                                                                try {
                                                                                    formatResult = clearCarrierLockScreenPassword(who2, data.readString(), data.readInt());
                                                                                    reply.writeNoException();
                                                                                    if (formatResult) {
                                                                                        encryptionStatus = 1;
                                                                                    }
                                                                                    parcel2.writeInt(encryptionStatus);
                                                                                } catch (Exception e22222222222) {
                                                                                    str = TAG;
                                                                                    stringBuilder = new StringBuilder();
                                                                                    stringBuilder.append("clear carrierlockscreenpassword exception is ");
                                                                                    stringBuilder.append(e22222222222);
                                                                                    Log.e(str, stringBuilder.toString());
                                                                                    parcel2.writeException(e22222222222);
                                                                                }
                                                                                return true;
                                                                            default:
                                                                                switch (i) {
                                                                                    case 2001:
                                                                                    case 6001:
                                                                                        break;
                                                                                    default:
                                                                                        return false;
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
        return true;
        return true;
        return true;
        return true;
        return true;
    }

    void setFunctionDisabled(int code, ComponentName who, boolean disabled, int userHandle) {
        String str;
        StringBuilder stringBuilder;
        switch (code) {
            case 1004:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setWifiDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setWifiDisabled(who, disabled, userHandle);
                return;
            case 1006:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setWifiApDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setWifiApDisabled(who, disabled, userHandle);
                return;
            case 1008:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setBootLoaderDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setBootLoaderDisabled(who, disabled, userHandle);
                return;
            case 1010:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setUSBDataDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setUSBDataDisabled(who, disabled, userHandle);
                return;
            case 1012:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setExternalStorageDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setExternalStorageDisabled(who, disabled, userHandle);
                return;
            case 1014:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setNFCDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setNFCDisabled(who, disabled, userHandle);
                return;
            case 1016:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setDataConnectivityDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setDataConnectivityDisabled(who, disabled, userHandle);
                return;
            case 1018:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setVoiceDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setVoiceDisabled(who, disabled, userHandle);
                return;
            case 1020:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setSMSDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setSMSDisabled(who, disabled, userHandle);
                return;
            case 1022:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setStatusBarExpandPanelDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setStatusBarExpandPanelDisabled(who, disabled, userHandle);
                return;
            case 1024:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setBluetoothDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setBluetoothDisabled(who, disabled, userHandle);
                return;
            case 1026:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setGPSDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setGPSDisabled(who, disabled, userHandle);
                return;
            case 1028:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setAdbDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setAdbDisabled(who, disabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK /*1030*/:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setUSBOtgDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setUSBOtgDisabled(who, disabled, userHandle);
                return;
            case 1032:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setSafeModeDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setSafeModeDisabled(who, disabled, userHandle);
                return;
            case 1034:
                this.mService.setTaskButtonDisabled(who, disabled, userHandle);
                return;
            case 1036:
                this.mService.setHomeButtonDisabled(who, disabled, userHandle);
                return;
            case 1038:
                this.mService.setBackButtonDisabled(who, disabled, userHandle);
                return;
            case 1504:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_turnOnGPS, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.turnOnGPS(who, disabled, userHandle);
                return;
            case 1513:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_turnOnMobiledata, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.turnOnMobiledata(who, disabled, userHandle);
                return;
            case 5011:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setSDcardDecryptionDisabled, disabled: ");
                    stringBuilder.append(disabled);
                    stringBuilder.append(", user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setSDCardDecryptionDisabled(who, disabled, userHandle);
                return;
            default:
                return;
        }
    }

    boolean isFunctionDisabled(int code, ComponentName who, int userHandle) {
        boolean bDisabled;
        String str;
        StringBuilder stringBuilder;
        if (code == 5012) {
            bDisabled = this.mService.isSDCardDecryptionDisabled(who, userHandle);
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_isSDcardDecryptionDisabled, the ret: ");
                stringBuilder.append(bDisabled);
                Log.i(str, stringBuilder.toString());
            }
            return bDisabled;
        } else if (userHandle != 0) {
            return false;
        } else {
            switch (code) {
                case 4001:
                case 4002:
                case 4003:
                    break;
                default:
                    switch (code) {
                        case 4011:
                            bDisabled = this.mService.isSafeModeDisabled(null, userHandle);
                            if (HWFLOW) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("HwDPMS received transaction_isSafeModeDisabled, the ret: ");
                                stringBuilder.append(bDisabled);
                                Log.i(str, stringBuilder.toString());
                            }
                            return bDisabled;
                        case 4012:
                        case 4013:
                        case 4014:
                        case 4015:
                        case 4016:
                        case 4017:
                        case 4018:
                            break;
                        default:
                            switch (code) {
                                case 4021:
                                case 4022:
                                case 4023:
                                case 4024:
                                case 4025:
                                case 4026:
                                    break;
                                default:
                                    switch (code) {
                                        case 5021:
                                        case 5022:
                                            break;
                                        default:
                                            switch (code) {
                                                case 1005:
                                                    bDisabled = this.mService.isWifiDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isWifiDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1007:
                                                    bDisabled = this.mService.isWifiApDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isWifiApDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1009:
                                                    bDisabled = this.mService.isBootLoaderDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isBootLoaderDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case HwPackageManagerService.TRANSACTION_CODE_GET_HDB_KEY /*1011*/:
                                                    bDisabled = this.mService.isUSBDataDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isUSBDataDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1013:
                                                    bDisabled = this.mService.isExternalStorageDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isExternalStorageDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1015:
                                                    bDisabled = this.mService.isNFCDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isNFCDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1017:
                                                    bDisabled = this.mService.isDataConnectivityDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isDataConnectivityDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1019:
                                                    bDisabled = this.mService.isVoiceDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isVoiceDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1021:
                                                    bDisabled = this.mService.isSMSDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isSMSDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1023:
                                                    bDisabled = this.mService.isStatusBarExpandPanelDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isStatusBarExpandPanelDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1025:
                                                    bDisabled = this.mService.isBluetoothDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isBluetoothDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1027:
                                                    bDisabled = this.mService.isGPSDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isGPSDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1029:
                                                    bDisabled = this.mService.isAdbDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isAdbDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1031:
                                                    bDisabled = this.mService.isUSBOtgDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isUSBOtgDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1033:
                                                    bDisabled = this.mService.isSafeModeDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isSafeModeDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1035:
                                                    return this.mService.isTaskButtonDisabled(who, userHandle);
                                                case 1037:
                                                    return this.mService.isHomeButtonDisabled(who, userHandle);
                                                case 1039:
                                                    return this.mService.isBackButtonDisabled(who, userHandle);
                                                case 1503:
                                                    bDisabled = this.mService.isRooted(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isRooted, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 1505:
                                                    bDisabled = this.mService.isGPSTurnOn(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isGPSTurnOn, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 2506:
                                                    bDisabled = this.mService.isInstallSourceDisabled(who, userHandle);
                                                    if (HWFLOW) {
                                                        str = TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("HwDPMS received transaction_isStatusBarExpandPanelDisabled, the ret: ");
                                                        stringBuilder.append(bDisabled);
                                                        Log.i(str, stringBuilder.toString());
                                                    }
                                                    return bDisabled;
                                                case 4009:
                                                    break;
                                                default:
                                                    return false;
                                            }
                                    }
                            }
                    }
            }
            bDisabled = this.mService.getHwAdminCachedValue(code);
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_isHwFrameworkAdminAllowed, the ret: ");
                stringBuilder.append(bDisabled);
                Log.i(str, stringBuilder.toString());
            }
            return bDisabled;
        }
    }

    void execCommand(int code, ComponentName who, int userHandle) {
        String str;
        StringBuilder stringBuilder;
        if (code == 1507) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_clearDefaultLauncher, user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.clearDefaultLauncher(who, userHandle);
        } else if (code == 1512) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_clearDeviceOwnerApp, user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.clearDeviceOwnerApp(userHandle);
        } else if (code == 2001) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_hangupCalling, user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.hangupCalling(who, userHandle);
        } else if (code == 2504) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_enableInstallPackage, user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.enableInstallPackage(who, userHandle);
        } else if (code != 6001) {
            switch (code) {
                case 1501:
                case 1502:
                    if (HWFLOW) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("HwDPMS received transaction_rebootDevice, user: ");
                        stringBuilder.append(userHandle);
                        Log.i(str, stringBuilder.toString());
                    }
                    this.mService.shutdownOrRebootDevice(code, who, userHandle);
                    return;
                default:
                    return;
            }
        } else {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_setSilentActiveAdmin, user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.setSilentActiveAdmin(who, userHandle);
        }
    }

    void execCommand(int code, ComponentName who, String param, int userHandle) {
        String str;
        StringBuilder stringBuilder;
        if (code == 2501) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_installPackage packagePath: ");
                stringBuilder.append(param);
                stringBuilder.append(", user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.installPackage(who, param, userHandle);
        } else if (code == 2503) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_clearPackageData, packageName: ");
                stringBuilder.append(param);
                stringBuilder.append(", user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.clearPackageData(who, param, userHandle);
        } else if (code == 3007) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_killApplicationProcess packageName: ");
                stringBuilder.append(param);
                stringBuilder.append(", user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.killApplicationProcess(who, param, userHandle);
        } else if (code == 5002) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_deleteApn, apnId: ");
                stringBuilder.append(param);
                stringBuilder.append(", user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.deleteApn(who, param, userHandle);
        } else if (code != 5006) {
            switch (code) {
                case 1510:
                    if (HWFLOW) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("HwDPMS received transaction_setSysTime, packageName: ");
                        stringBuilder.append(param);
                        stringBuilder.append(", user: ");
                        stringBuilder.append(userHandle);
                        Log.i(str, stringBuilder.toString());
                    }
                    this.mService.setSysTime(who, Long.parseLong(param), userHandle);
                    return;
                case 1511:
                    if (HWFLOW) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("HwDPMS received transaction_setDeviceOwnerApp, ownnerName: ");
                        stringBuilder.append(param);
                        stringBuilder.append(", user: ");
                        stringBuilder.append(userHandle);
                        Log.i(str, stringBuilder.toString());
                    }
                    this.mService.setDeviceOwnerApp(who, param, userHandle);
                    return;
                default:
                    return;
            }
        } else {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwDPMS received transaction_setPreferApn, apnId: ");
                stringBuilder.append(param);
                stringBuilder.append(", user: ");
                stringBuilder.append(userHandle);
                Log.i(str, stringBuilder.toString());
            }
            this.mService.setPreferApn(who, param, userHandle);
        }
    }

    void execCommand(int code, ComponentName who, List<String> param, int userHandle) {
        String str;
        StringBuilder stringBuilder;
        switch (code) {
            case 1508:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_setCustomSettingsMenu user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.setCustomSettingsMenu(who, param, userHandle);
                return;
            case 2505:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_disableInstallPackage user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.disableInstallSource(who, param, userHandle);
                return;
            case 2508:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_addInstallPackageWhiteList user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.addInstallPackageWhiteList(who, param, userHandle);
                return;
            case 2509:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_removeInstallPackageWhiteList user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.removeInstallPackageWhiteList(who, param, userHandle);
                return;
            case 2511:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_addDisallowedUninstallPackages user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.addDisallowedUninstallPackages(who, param, userHandle);
                return;
            case 2512:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_removeDisallowedUninstallPackages user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.removeDisallowedUninstallPackages(who, param, userHandle);
                return;
            case 2514:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_addDisabledDeactivateMdmPackages user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.addDisabledDeactivateMdmPackages(who, param, userHandle);
                return;
            case 2515:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_removeDisabledDeactivateMdmPackages user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.removeDisabledDeactivateMdmPackages(who, param, userHandle);
                return;
            case HwGpsPowerTracker.EVENT_REMOVE_PACKAGE_LOCATION /*3001*/:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_addPersistentApp user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.addPersistentApp(who, param, userHandle);
                return;
            case HwArbitrationDEFS.MSG_Display_Start_Monitor_Network /*3002*/:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_removePersistentApp user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.removePersistentApp(who, param, userHandle);
                return;
            case 3004:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_addDisallowedRunningApp user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.addDisallowedRunningApp(who, param, userHandle);
                return;
            case 3005:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_removeDisallowedRunningApp user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.removeDisallowedRunningApp(who, param, userHandle);
                return;
            case 5007:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_addNetworkAccessWhitelist user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.addNetworkAccessWhitelist(who, param, userHandle);
                return;
            case 5008:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_removeNetworkAccessWhitelist user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                this.mService.removeNetworkAccessWhitelist(who, param, userHandle);
                return;
            default:
                return;
        }
    }

    List<String> getListCommand(int code, ComponentName who, int userHandle) {
        if (userHandle != 0) {
            return null;
        }
        String str;
        StringBuilder stringBuilder;
        switch (code) {
            case 2507:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getInstallPackageSourceWhiteList user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getInstallPackageSourceWhiteList(who, userHandle);
            case 2510:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getInstallPackageWhiteList user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getInstallPackageWhiteList(who, userHandle);
            case 2513:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getDisallowedUninstallPackageList user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getDisallowedUninstallPackageList(who, userHandle);
            case 2516:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getDisabledDeactivateMdmPackageList user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getDisabledDeactivateMdmPackageList(who, userHandle);
            case HwArbitrationDEFS.MSG_Display_stop_Monotor_network /*3003*/:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getPersistentApp user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getPersistentApp(who, userHandle);
            case 3006:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getDisallowedRunningApp user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getDisallowedRunningApp(who, userHandle);
            case 4004:
            case 4005:
            case 4006:
            case 4007:
            case 4008:
            case 4010:
            case 4019:
            case 4020:
            case 4027:
            case 4028:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getHwFrameworkAdminList user: ");
                    stringBuilder.append(userHandle);
                    stringBuilder.append(":");
                    stringBuilder.append(code);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getHwAdminCachedList(code);
            case 5009:
                if (HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwDPMS received transaction_getNetworkAccessWhitelist user: ");
                    stringBuilder.append(userHandle);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.getNetworkAccessWhitelist(who, userHandle);
            default:
                return null;
        }
    }

    int getSDCardEncryptionStatus(int code) {
        if (code != 5010) {
            return 0;
        }
        return this.mService.getSDCardEncryptionStatus();
    }

    boolean processTransactionWithPolicyName(int code, Parcel data, Parcel reply) {
        String str;
        StringBuilder stringBuilder;
        Parcel parcel = data;
        Parcel parcel2 = reply;
        int i = 0;
        ComponentName whoGetPolicy;
        ComponentName whoSetPolicy;
        int setPolicyUser;
        switch (code) {
            case 5013:
                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                whoGetPolicy = null;
                if (data.readInt() != 0) {
                    whoGetPolicy = ComponentName.readFromParcel(data);
                }
                ComponentName whoGetPolicy2 = whoGetPolicy;
                int getPolicyUser = data.readInt();
                try {
                    Bundle getPolicyData = getPolicy(whoGetPolicy2, data.readString(), data.readBundle(), getPolicyUser, data.readInt());
                    reply.writeNoException();
                    parcel2.writeBundle(getPolicyData);
                } catch (Exception e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getPolicy exception is ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    parcel2.writeException(e);
                }
                return true;
            case 5014:
                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                whoGetPolicy = null;
                if (data.readInt() != 0) {
                    whoGetPolicy = ComponentName.readFromParcel(data);
                }
                whoSetPolicy = whoGetPolicy;
                setPolicyUser = data.readInt();
                try {
                    i = setPolicy(whoSetPolicy, data.readString(), data.readBundle(), setPolicyUser, data.readInt());
                    reply.writeNoException();
                    parcel2.writeInt(i);
                } catch (Exception e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setPolicy exception is ");
                    stringBuilder.append(e2);
                    Log.e(str, stringBuilder.toString());
                    parcel2.writeException(e2);
                }
                return true;
            case 5015:
                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                whoGetPolicy = null;
                if (data.readInt() != 0) {
                    whoGetPolicy = ComponentName.readFromParcel(data);
                }
                whoSetPolicy = whoGetPolicy;
                setPolicyUser = data.readInt();
                try {
                    i = removePolicy(whoSetPolicy, data.readString(), data.readBundle(), setPolicyUser, data.readInt());
                    reply.writeNoException();
                    parcel2.writeInt(i);
                } catch (Exception e22) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("removePolicy exception is ");
                    stringBuilder.append(e22);
                    Log.e(str, stringBuilder.toString());
                    parcel2.writeException(e22);
                }
                return true;
            case 5016:
                parcel.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                try {
                    boolean isDisabled = hasHwPolicy(data.readInt());
                    reply.writeNoException();
                    if (isDisabled) {
                        i = 1;
                    }
                    parcel2.writeInt(i);
                } catch (Exception e222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("hasHwPolicy exception is ");
                    stringBuilder.append(e222);
                    Log.e(str, stringBuilder.toString());
                    parcel2.writeException(e222);
                }
                return true;
            default:
                return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0034  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0079  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x004f  */
    /* JADX WARNING: Missing block: B:13:0x002d, code:
            if (r7.equals("config-vpn") != false) goto L_0x0031;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    Bundle getPolicy(ComponentName who, String policyName, Bundle keyWords, int userHandle, int type) {
        if (type == 0) {
            return this.mService.getPolicy(who, policyName, userHandle);
        }
        Object obj = 1;
        if (type == 1) {
            int hashCode = policyName.hashCode();
            if (hashCode != 830361577) {
                if (hashCode == 1115753445 && policyName.equals("queryBrowsingHistory")) {
                    obj = null;
                    String str;
                    StringBuilder stringBuilder;
                    switch (obj) {
                        case null:
                            Bundle bundle = new Bundle();
                            bundle.putStringArrayList("value", this.mService.queryBrowsingHistory(who, userHandle));
                            return bundle;
                        case 1:
                            if (HWFLOW) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("receive get config-vpn policy: ");
                                stringBuilder.append(policyName);
                                Log.i(str, stringBuilder.toString());
                            }
                            if (keyWords == null) {
                                return this.mService.getVpnList(who, null, userHandle);
                            }
                            return this.mService.getVpnProfile(who, keyWords, userHandle);
                        default:
                            if (HWFLOW) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("don't have this get policy: ");
                                stringBuilder.append(policyName);
                                Log.i(str, stringBuilder.toString());
                                break;
                            }
                            break;
                    }
                }
            }
            obj = -1;
            switch (obj) {
                case null:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        } else if (type == 2) {
            return this.mService.getHwAdminCachedBundle(policyName);
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0091  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0070  */
    /* JADX WARNING: Missing block: B:11:0x0044, code:
            if (r7.equals("set-system-language") == false) goto L_0x0051;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int setPolicy(ComponentName who, String policyName, Bundle policyData, int userHandle, int type) {
        IHwDevicePolicyManager iHwDevicePolicyManager = this.mService;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("policyName: ");
        stringBuilder.append(policyName);
        stringBuilder.append(", type: ");
        stringBuilder.append(type);
        iHwDevicePolicyManager.bdReport(CPUFeature.MSG_SET_CPUSETCONFIG_SCREENON, stringBuilder.toString());
        if (type == 0) {
            return this.mService.setPolicy(who, policyName, policyData, userHandle);
        }
        int i = 1;
        if (type == 1) {
            int hashCode = policyName.hashCode();
            if (hashCode != 830361577) {
                if (hashCode == 1456366027) {
                }
            } else if (policyName.equals("config-vpn")) {
                i = 0;
                String str;
                switch (i) {
                    case 0:
                        if (HWFLOW) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("receive set config-vpn policy: ");
                            stringBuilder.append(policyName);
                            Log.i(str, stringBuilder.toString());
                        }
                        return this.mService.configVpnProfile(who, policyData, userHandle);
                    case 1:
                        if (HWFLOW) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("receive set system language policy: ");
                            stringBuilder.append(policyName);
                            Log.i(str, stringBuilder.toString());
                        }
                        return this.mService.setSystemLanguage(who, policyData, userHandle);
                    default:
                        if (HWFLOW) {
                            str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("don't have this set policy: ");
                            stringBuilder2.append(policyName);
                            Log.i(str, stringBuilder2.toString());
                        }
                        return 0;
                }
            }
            i = -1;
            switch (i) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
        return 0;
    }

    int removePolicy(ComponentName who, String policyName, Bundle policyData, int userHandle, int type) {
        if (type == 0) {
            return this.mService.removePolicy(who, policyName, policyData, userHandle);
        }
        if (type == 1) {
            int i = -1;
            if (policyName.hashCode() == 830361577 && policyName.equals("config-vpn")) {
                i = 0;
            }
            String str;
            if (i == 0) {
                if (HWFLOW) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("receive remove config-vpn policy: ");
                    stringBuilder.append(policyName);
                    Log.i(str, stringBuilder.toString());
                }
                return this.mService.removeVpnProfile(who, policyData, userHandle);
            } else if (HWFLOW) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("don't have this remove policy: ");
                stringBuilder2.append(policyName);
                Log.i(str, stringBuilder2.toString());
            }
        }
        return 0;
    }

    boolean hasHwPolicy(int userHandle) {
        return this.mService.hasHwPolicy(userHandle);
    }

    void setAccountDisabled(ComponentName who, String accountType, boolean disabled, int userHandle) {
        this.mService.setAccountDisabled(who, accountType, disabled, userHandle);
    }

    boolean isAccountDisabled(ComponentName who, String accountType, int userHandle) {
        return this.mService.isAccountDisabled(who, accountType, userHandle);
    }

    boolean formatSDCard(ComponentName who, String diskId, int userHandle) {
        return this.mService.formatSDCard(who, diskId, userHandle);
    }

    boolean installCertificateWithType(ComponentName who, int type, byte[] certBuffer, String name, String password, int flag, boolean requestAccess, int userHandle) {
        return this.mService.installCertificateWithType(who, type, certBuffer, name, password, flag, requestAccess, userHandle);
    }

    boolean setCarrierLockScreenPassword(ComponentName who, String password, String phoneNumber, int userHandle) {
        return this.mService.setCarrierLockScreenPassword(who, password, phoneNumber, userHandle);
    }

    boolean clearCarrierLockScreenPassword(ComponentName who, String password, int userHandle) {
        return this.mService.clearCarrierLockScreenPassword(who, password, userHandle);
    }
}
