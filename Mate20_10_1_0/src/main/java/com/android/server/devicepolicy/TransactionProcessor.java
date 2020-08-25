package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.pm.HwPackageManagerService;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class TransactionProcessor {
    private static final String BDREPORT_MDM_KEY_APINAME = "apiName";
    private static final String BDREPORT_MDM_KEY_PACKAGE = "package";
    private static final int[] CODE_INDEXS = {1004, 1006, HwArbitrationDEFS.MSG_CELL_STATE_DISCONNECT, HwArbitrationDEFS.MSG_SCREEN_IS_TURNOFF, HwArbitrationDEFS.MSG_DATA_ROAMING_DISABLE, HwArbitrationDEFS.MSG_STATE_NO_CONNECTION, HwArbitrationDEFS.MSG_VPN_STATE_CHANGED, HwArbitrationDEFS.MSG_STATE_OUT_OF_SERVICE, 1022, 1024, 1026, 1028, HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK, 1032, 1034, 1036, 1038, 1501, 1502, 1504, 1506, 1507, 1508, 1509, 1510, 1511, 1512, 1513, 1514, 2001, 2501, 2502, 2503, 2504, 2505, 2508, 2509, 2511, 2512, 2514, 2515, 3001, HwArbitrationDEFS.MSG_Display_Start_Monitor_Network, HwArbitrationDEFS.MSG_Display_Start_Monitor_SmartMP, HwArbitrationDEFS.MSG_Display_Stop_Monitor_SmartMP, 3007, 3501, 3503, 5001, 5002, 5003, 5006, 5007, 5008, 5011, 5017, 5018, 5020, 6001, 7004, 7005};
    protected static final boolean HWDBG = false;
    protected static final boolean HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String[] POLICY_NAMES = {"setWifiDisabled", "setWifiApDisabled", "setUSBDataDisabled", "setExternalStorageDisabled", "setNFCDisabled", "setDataConnectivityDisabled", "setVoiceDisabled", "setSMSDisabled", "setStatusBarExpandPanelDisabled", "setBluetoothDisabled", "setGPSDisabled", "setAdbDisabled", "setUSBOtgDisabled", "setSafeModeDisabled", "setTaskButtonDisabled", "setHomeButtonDisabled", "setBackButtonDisabled", "shutdownDevice", "rebootDevice", "turnOnGPS", "setDefaultLauncher", "clearDefaultLauncher", "setCustomSettingsMenu", "captureScreen", "setSysTime", "setDeviceOwnerApp", "clearDeviceOwnerApp", "turnOnMobiledata", "setDefaultDataCard", "hangupCalling", "installPackage", "uninstallPackage", "clearPackageData", "enableInstallPackage", "disableInstallSource", "addInstallPackageWhiteList", "removeInstallPackageWhiteList", "addDisallowedUninstallPackages", "removeDisallowedUninstallPackages", "addDisabledDeactivateMdmPackages", "removeDisabledDeactivateMdmPackages", "addPersistentApp", "removePersistentApp", "addDisallowedRunningApp", "removeDisallowedRunningApp", "killApplicationProcess", "configExchangeMail", "resetNetworkSetting", "addApn", "deleteApn", "updateApn", "setPreferApn", "addNetworkAccessWhitelist", "removeNetworkAccessWhitelist", "setSDCardDecryptionDisabled", "formatSDCard", "setAccountDisabled", "installCertificateWithType", "setSilentActiveAdmin", "setCarrierLockScreenPassword", "clearCarrierLockScreenPassword"};
    protected static final String TAG = "TransactionProcessor";
    private IHwDevicePolicyManager mService;

    TransactionProcessor(IHwDevicePolicyManager service) {
        this.mService = service;
    }

    /* access modifiers changed from: package-private */
    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Failed to find switch 'out' block
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:786)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:50)
        */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x03a6  */
    /* JADX WARNING: Removed duplicated region for block: B:209:0x0446  */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x04cc  */
    /* JADX WARNING: Removed duplicated region for block: B:252:0x055f  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0197  */
    public boolean processTransaction(int r27, Parcel r28, Parcel r29) {
        /*
            r26 = this;
            r10 = r26
            r11 = r27
            r12 = r28
            r13 = r29
            r0 = 2001(0x7d1, float:2.804E-42)
            java.lang.String r1 = "com.huawei.android.app.admin.hwdevicepolicymanagerex"
            r14 = 1
            java.lang.String r15 = "TransactionProcessor"
            if (r11 == r0) goto L_0x0650
            r0 = 6001(0x1771, float:8.409E-42)
            if (r11 == r0) goto L_0x0650
            r0 = 7004(0x1b5c, float:9.815E-42)
            r9 = 0
            if (r11 == r0) goto L_0x0604
            r0 = 7005(0x1b5d, float:9.816E-42)
            if (r11 == r0) goto L_0x05be
            switch(r11) {
                case 1004: goto L_0x0197;
                case 1005: goto L_0x055f;
                case 1006: goto L_0x0197;
                case 1007: goto L_0x055f;
                case 1008: goto L_0x0197;
                case 1009: goto L_0x055f;
                case 1010: goto L_0x0197;
                case 1011: goto L_0x055f;
                case 1012: goto L_0x0197;
                case 1013: goto L_0x055f;
                case 1014: goto L_0x0197;
                case 1015: goto L_0x055f;
                case 1016: goto L_0x0197;
                case 1017: goto L_0x055f;
                case 1018: goto L_0x0197;
                case 1019: goto L_0x055f;
                case 1020: goto L_0x0197;
                case 1021: goto L_0x055f;
                case 1022: goto L_0x0197;
                case 1023: goto L_0x055f;
                case 1024: goto L_0x0197;
                case 1025: goto L_0x055f;
                case 1026: goto L_0x0197;
                case 1027: goto L_0x055f;
                case 1028: goto L_0x0197;
                case 1029: goto L_0x055f;
                case 1030: goto L_0x0197;
                case 1031: goto L_0x055f;
                case 1032: goto L_0x0197;
                case 1033: goto L_0x055f;
                case 1034: goto L_0x0197;
                case 1035: goto L_0x055f;
                case 1036: goto L_0x0197;
                case 1037: goto L_0x055f;
                case 1038: goto L_0x0197;
                case 1039: goto L_0x055f;
                default: goto L_0x0021;
            }
        L_0x0021:
            java.lang.String r0 = ", user: "
            switch(r11) {
                case 1501: goto L_0x0650;
                case 1502: goto L_0x0650;
                case 1503: goto L_0x055f;
                case 1504: goto L_0x0197;
                case 1505: goto L_0x055f;
                case 1506: goto L_0x050f;
                case 1507: goto L_0x0650;
                case 1508: goto L_0x04cc;
                case 1509: goto L_0x0484;
                case 1510: goto L_0x0446;
                case 1511: goto L_0x0446;
                case 1512: goto L_0x0650;
                case 1513: goto L_0x0197;
                default: goto L_0x0026;
            }
        L_0x0026:
            switch(r11) {
                case 2501: goto L_0x0446;
                case 2502: goto L_0x03db;
                case 2503: goto L_0x0446;
                case 2504: goto L_0x0650;
                case 2505: goto L_0x04cc;
                case 2506: goto L_0x055f;
                case 2507: goto L_0x03a6;
                case 2508: goto L_0x04cc;
                case 2509: goto L_0x04cc;
                case 2510: goto L_0x03a6;
                case 2511: goto L_0x04cc;
                case 2512: goto L_0x04cc;
                case 2513: goto L_0x03a6;
                case 2514: goto L_0x04cc;
                case 2515: goto L_0x04cc;
                case 2516: goto L_0x03a6;
                default: goto L_0x0029;
            }
        L_0x0029:
            switch(r11) {
                case 3001: goto L_0x04cc;
                case 3002: goto L_0x04cc;
                case 3003: goto L_0x03a6;
                case 3004: goto L_0x04cc;
                case 3005: goto L_0x04cc;
                case 3006: goto L_0x03a6;
                case 3007: goto L_0x0446;
                default: goto L_0x002c;
            }
        L_0x002c:
            switch(r11) {
                case 3501: goto L_0x034c;
                case 3502: goto L_0x0301;
                case 3503: goto L_0x0650;
                default: goto L_0x002f;
            }
        L_0x002f:
            switch(r11) {
                case 4001: goto L_0x055f;
                case 4002: goto L_0x055f;
                case 4003: goto L_0x055f;
                case 4004: goto L_0x03a6;
                case 4005: goto L_0x03a6;
                case 4006: goto L_0x03a6;
                case 4007: goto L_0x03a6;
                case 4008: goto L_0x03a6;
                case 4009: goto L_0x055f;
                case 4010: goto L_0x03a6;
                case 4011: goto L_0x055f;
                case 4012: goto L_0x055f;
                case 4013: goto L_0x055f;
                case 4014: goto L_0x055f;
                case 4015: goto L_0x055f;
                case 4016: goto L_0x055f;
                case 4017: goto L_0x055f;
                case 4018: goto L_0x055f;
                case 4019: goto L_0x03a6;
                case 4020: goto L_0x03a6;
                case 4021: goto L_0x055f;
                case 4022: goto L_0x055f;
                case 4023: goto L_0x055f;
                case 4024: goto L_0x055f;
                case 4025: goto L_0x055f;
                case 4026: goto L_0x055f;
                case 4027: goto L_0x03a6;
                case 4028: goto L_0x03a6;
                default: goto L_0x0032;
            }
        L_0x0032:
            java.lang.String r0 = "HwDPMS received ConstantValue.transaction_updateApn, user: "
            r2 = 0
            switch(r11) {
                case 5001: goto L_0x02a6;
                case 5002: goto L_0x0446;
                case 5003: goto L_0x024b;
                case 5004: goto L_0x01fd;
                case 5005: goto L_0x01a9;
                case 5006: goto L_0x0446;
                case 5007: goto L_0x04cc;
                case 5008: goto L_0x04cc;
                case 5009: goto L_0x03a6;
                case 5010: goto L_0x019a;
                case 5011: goto L_0x0197;
                case 5012: goto L_0x055f;
                default: goto L_0x0038;
            }
        L_0x0038:
            switch(r11) {
                case 5017: goto L_0x0154;
                case 5018: goto L_0x0110;
                case 5019: goto L_0x00d4;
                case 5020: goto L_0x0040;
                case 5021: goto L_0x055f;
                case 5022: goto L_0x055f;
                default: goto L_0x003b;
            }
        L_0x003b:
            boolean r0 = r26.processTransactionEx(r27, r28, r29)
            return r0
        L_0x0040:
            r12.enforceInterface(r1)
            r0 = 0
            int r16 = r28.readInt()
            if (r16 == 0) goto L_0x0050
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r7 = r0
            goto L_0x0051
        L_0x0050:
            r7 = r0
        L_0x0051:
            int r17 = r28.readInt()
            int r6 = r28.readInt()
            byte[] r5 = new byte[r6]
            r12.readByteArray(r5)
            java.lang.String r18 = r28.readString()
            java.lang.String r19 = r28.readString()
            int r20 = r28.readInt()
            int r0 = r28.readInt()
            if (r0 != r14) goto L_0x0072
            r8 = r14
            goto L_0x0073
        L_0x0072:
            r8 = r9
        L_0x0073:
            int r21 = r28.readInt()
            r22 = 0
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x00c1, Exception -> 0x00af }
            r10.bdReportMdmPolicy(r7, r0)     // Catch:{ RuntimeException -> 0x00c1, Exception -> 0x00af }
            r1 = r26
            r2 = r7
            r3 = r17
            r4 = r5
            r23 = r5
            r5 = r18
            r24 = r6
            r6 = r19
            r25 = r7
            r7 = r20
            r0 = r9
            r9 = r21
            boolean r1 = r1.installCertificateWithType(r2, r3, r4, r5, r6, r7, r8, r9)     // Catch:{ RuntimeException -> 0x00ab, Exception -> 0x00a7 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x00a5, Exception -> 0x00a3 }
            if (r1 != r14) goto L_0x009f
            r0 = r14
        L_0x009f:
            r13.writeInt(r0)     // Catch:{ RuntimeException -> 0x00a5, Exception -> 0x00a3 }
            goto L_0x00d2
        L_0x00a3:
            r0 = move-exception
            goto L_0x00b8
        L_0x00a5:
            r0 = move-exception
            goto L_0x00ca
        L_0x00a7:
            r0 = move-exception
            r1 = r22
            goto L_0x00b8
        L_0x00ab:
            r0 = move-exception
            r1 = r22
            goto L_0x00ca
        L_0x00af:
            r0 = move-exception
            r23 = r5
            r24 = r6
            r25 = r7
            r1 = r22
        L_0x00b8:
            java.lang.String r2 = "install user cert exception"
            android.util.Log.e(r15, r2)
            r13.writeException(r0)
            goto L_0x00d3
        L_0x00c1:
            r0 = move-exception
            r23 = r5
            r24 = r6
            r25 = r7
            r1 = r22
        L_0x00ca:
            java.lang.String r2 = "install user cert  occur RuntimeException"
            android.util.Log.e(r15, r2)
            r13.writeException(r0)
        L_0x00d2:
        L_0x00d3:
            return r14
        L_0x00d4:
            r0 = r9
            r12.enforceInterface(r1)
            r1 = 0
            int r2 = r28.readInt()
            if (r2 == 0) goto L_0x00e3
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x00e3:
            java.lang.String r3 = r28.readString()
            int r4 = r28.readInt()
            r5 = 0
            boolean r6 = r10.isAccountDisabled(r1, r3, r4)     // Catch:{ RuntimeException -> 0x0105, Exception -> 0x00fb }
            r5 = r6
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x0105, Exception -> 0x00fb }
            if (r5 != r14) goto L_0x00f7
            r0 = r14
        L_0x00f7:
            r13.writeInt(r0)     // Catch:{ RuntimeException -> 0x0105, Exception -> 0x00fb }
            goto L_0x010e
        L_0x00fb:
            r0 = move-exception
            java.lang.String r6 = "isAccountDisabled exception"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
            goto L_0x010f
        L_0x0105:
            r0 = move-exception
            java.lang.String r6 = "isAccountDisabled occur RuntimeException"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
        L_0x010e:
        L_0x010f:
            return r14
        L_0x0110:
            r0 = r9
            r12.enforceInterface(r1)
            r1 = 0
            int r2 = r28.readInt()
            if (r2 == 0) goto L_0x011f
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x011f:
            java.lang.String r3 = r28.readString()
            int r4 = r28.readInt()
            if (r4 != r14) goto L_0x012a
            r0 = r14
        L_0x012a:
            r4 = r0
            int r5 = r28.readInt()
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x0148, Exception -> 0x013d }
            r10.bdReportMdmPolicy(r1, r0)     // Catch:{ RuntimeException -> 0x0148, Exception -> 0x013d }
            r10.setAccountDisabled(r1, r3, r4, r5)     // Catch:{ RuntimeException -> 0x0148, Exception -> 0x013d }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x0148, Exception -> 0x013d }
            goto L_0x0152
        L_0x013d:
            r0 = move-exception
            java.lang.String r6 = "setAccountDisabled exception"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
            goto L_0x0153
        L_0x0148:
            r0 = move-exception
            java.lang.String r6 = "setAccountDisabled occur RuntimeException"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
        L_0x0152:
        L_0x0153:
            return r14
        L_0x0154:
            r0 = r9
            r12.enforceInterface(r1)
            r1 = 0
            int r2 = r28.readInt()
            if (r2 == 0) goto L_0x0163
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x0163:
            java.lang.String r3 = r28.readString()
            int r4 = r28.readInt()
            r5 = 0
            java.lang.String r6 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x018c, Exception -> 0x0182 }
            r10.bdReportMdmPolicy(r1, r6)     // Catch:{ RuntimeException -> 0x018c, Exception -> 0x0182 }
            boolean r6 = r10.formatSDCard(r1, r3, r4)     // Catch:{ RuntimeException -> 0x018c, Exception -> 0x0182 }
            r5 = r6
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x018c, Exception -> 0x0182 }
            if (r5 != r14) goto L_0x017e
            r0 = r14
        L_0x017e:
            r13.writeInt(r0)     // Catch:{ RuntimeException -> 0x018c, Exception -> 0x0182 }
            goto L_0x0195
        L_0x0182:
            r0 = move-exception
            java.lang.String r6 = "formatSDCard exception"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
            goto L_0x0196
        L_0x018c:
            r0 = move-exception
            java.lang.String r6 = "formatSDCard occur RuntimeException"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
        L_0x0195:
        L_0x0196:
            return r14
        L_0x0197:
            r2 = r9
            goto L_0x057d
        L_0x019a:
            r12.enforceInterface(r1)
            r0 = 0
            int r0 = r26.getSDCardEncryptionStatus(r27)
            r29.writeNoException()
            r13.writeInt(r0)
            return r14
        L_0x01a9:
            r12.enforceInterface(r1)
            r1 = 0
            int r3 = r28.readInt()
            if (r3 == 0) goto L_0x01b7
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x01b7:
            java.util.HashMap r4 = new java.util.HashMap
            r4.<init>()
            r12.readMap(r4, r2)
            int r2 = r28.readInt()
            boolean r5 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            if (r5 == 0) goto L_0x01d9
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            r5.<init>()     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            r5.append(r0)     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            r5.append(r2)     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            java.lang.String r0 = r5.toString()     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            android.util.Log.i(r15, r0)     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
        L_0x01d9:
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            java.util.List r0 = r0.queryApn(r1, r4, r2)     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            r13.writeStringList(r0)     // Catch:{ RuntimeException -> 0x01f1, Exception -> 0x01e6 }
            goto L_0x01fb
        L_0x01e6:
            r0 = move-exception
            java.lang.String r5 = "queryApn exception"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
            goto L_0x01fc
        L_0x01f1:
            r0 = move-exception
            java.lang.String r5 = "queryApn occur RuntimeException"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
        L_0x01fb:
        L_0x01fc:
            return r14
        L_0x01fd:
            r12.enforceInterface(r1)
            r1 = 0
            int r2 = r28.readInt()
            if (r2 == 0) goto L_0x020b
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x020b:
            java.lang.String r3 = r28.readString()
            int r4 = r28.readInt()
            boolean r5 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            if (r5 == 0) goto L_0x0229
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            r5.<init>()     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            r5.append(r0)     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            r5.append(r4)     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            java.lang.String r0 = r5.toString()     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            android.util.Log.i(r15, r0)     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
        L_0x0229:
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            java.util.Map r0 = r0.getApnInfo(r1, r3, r4)     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            r13.writeMap(r0)     // Catch:{ RuntimeException -> 0x0240, Exception -> 0x0236 }
            goto L_0x0249
        L_0x0236:
            r0 = move-exception
            java.lang.String r5 = "getApnInfo exception"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
            goto L_0x024a
        L_0x0240:
            r0 = move-exception
            java.lang.String r5 = "getApnInfo occur RuntimeException"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
        L_0x0249:
        L_0x024a:
            return r14
        L_0x024b:
            r12.enforceInterface(r1)
            r1 = 0
            int r3 = r28.readInt()
            if (r3 == 0) goto L_0x0259
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x0259:
            java.util.HashMap r4 = new java.util.HashMap
            r4.<init>()
            r12.readMap(r4, r2)
            java.lang.String r2 = r28.readString()
            int r5 = r28.readInt()
            boolean r6 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            if (r6 == 0) goto L_0x027f
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            r6.<init>()     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            r6.append(r0)     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            r6.append(r5)     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            java.lang.String r0 = r6.toString()     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            android.util.Log.i(r15, r0)     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
        L_0x027f:
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            r10.bdReportMdmPolicy(r1, r0)     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            r0.updateApn(r1, r4, r2, r5)     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x029a, Exception -> 0x028f }
            goto L_0x02a4
        L_0x028f:
            r0 = move-exception
            java.lang.String r6 = "updateApn exception"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
            goto L_0x02a5
        L_0x029a:
            r0 = move-exception
            java.lang.String r6 = "updateApn occur RuntimeException"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
        L_0x02a4:
        L_0x02a5:
            return r14
        L_0x02a6:
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x02b6
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r3 = r0
            goto L_0x02b7
        L_0x02b6:
            r3 = r0
        L_0x02b7:
            java.util.HashMap r0 = new java.util.HashMap
            r0.<init>()
            r4 = r0
            r12.readMap(r4, r2)
            int r2 = r28.readInt()
            boolean r0 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            if (r0 == 0) goto L_0x02dc
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            r0.<init>()     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            java.lang.String r5 = "HwDPMS received ConstantValue.transaction_addApn, user: "
            r0.append(r5)     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            r0.append(r2)     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            java.lang.String r0 = r0.toString()     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            android.util.Log.i(r15, r0)     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
        L_0x02dc:
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            r10.bdReportMdmPolicy(r3, r0)     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            r0.addApn(r3, r4, r2)     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x02f6, Exception -> 0x02ec }
            goto L_0x02ff
        L_0x02ec:
            r0 = move-exception
            java.lang.String r5 = "addApn exception"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
            goto L_0x0300
        L_0x02f6:
            r0 = move-exception
            java.lang.String r5 = "addApn occur RuntimeException"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
        L_0x02ff:
        L_0x0300:
            return r14
        L_0x0301:
            r2 = r9
            r12.enforceInterface(r1)
            r1 = 0
            int r3 = r28.readInt()
            if (r3 == 0) goto L_0x0310
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x0310:
            java.lang.String r4 = r28.readString()
            int r5 = r28.readInt()
            boolean r6 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW
            if (r6 == 0) goto L_0x0336
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "HwDPMS received transaction_configExchangeMail domain: "
            r6.append(r7)
            r6.append(r4)
            r6.append(r0)
            r6.append(r5)
            java.lang.String r0 = r6.toString()
            android.util.Log.i(r15, r0)
        L_0x0336:
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService
            android.os.Bundle r0 = r0.getMailProviderForDomain(r1, r4, r5)
            r29.writeNoException()
            if (r0 == 0) goto L_0x0348
            r13.writeInt(r14)
            r0.writeToParcel(r13, r2)
            goto L_0x034b
        L_0x0348:
            r13.writeInt(r2)
        L_0x034b:
            return r14
        L_0x034c:
            r12.enforceInterface(r1)
            r0 = 0
            android.os.Bundle r1 = new android.os.Bundle
            r1.<init>()
            int r2 = r28.readInt()
            if (r2 == 0) goto L_0x0361
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r3 = r0
            goto L_0x0362
        L_0x0361:
            r3 = r0
        L_0x0362:
            r1.readFromParcel(r12)
            int r4 = r28.readInt()
            boolean r0 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            if (r0 == 0) goto L_0x0381
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            r0.<init>()     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            java.lang.String r5 = "HwDPMS received ConstantValue.transaction_configExchangeMail, user: "
            r0.append(r5)     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            r0.append(r4)     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            java.lang.String r0 = r0.toString()     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            android.util.Log.i(r15, r0)     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
        L_0x0381:
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            r10.bdReportMdmPolicy(r3, r0)     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            r0.configExchangeMailProvider(r3, r1, r4)     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x039b, Exception -> 0x0391 }
            goto L_0x03a4
        L_0x0391:
            r0 = move-exception
            java.lang.String r5 = "configExchangeMail exception"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
            goto L_0x03a5
        L_0x039b:
            r0 = move-exception
            java.lang.String r5 = "configExchangeMail occur RuntimeException"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
        L_0x03a4:
        L_0x03a5:
            return r14
        L_0x03a6:
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x03b6
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r2 = r0
            goto L_0x03b7
        L_0x03b6:
            r2 = r0
        L_0x03b7:
            int r3 = r28.readInt()
            java.util.List r0 = r10.getListCommand(r11, r2, r3)     // Catch:{ RuntimeException -> 0x03d0, Exception -> 0x03c6 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x03d0, Exception -> 0x03c6 }
            r13.writeStringList(r0)     // Catch:{ RuntimeException -> 0x03d0, Exception -> 0x03c6 }
            goto L_0x03da
        L_0x03c6:
            r0 = move-exception
            java.lang.String r4 = "getListCommand exception"
            android.util.Log.e(r15, r4)
            r13.writeException(r0)
            goto L_0x03da
        L_0x03d0:
            r0 = move-exception
            java.lang.String r4 = "getListCommand occur RuntimeException"
            android.util.Log.e(r15, r4)
            r13.writeException(r0)
        L_0x03da:
            return r14
        L_0x03db:
            r2 = r9
            r12.enforceInterface(r1)
            r1 = 0
            int r3 = r28.readInt()
            if (r3 == 0) goto L_0x03ea
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x03ea:
            java.lang.String r4 = r28.readString()
            int r5 = r28.readInt()
            if (r5 != r14) goto L_0x03f5
            r2 = r14
        L_0x03f5:
            int r5 = r28.readInt()
            boolean r6 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            if (r6 == 0) goto L_0x041f
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r6.<init>()     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            java.lang.String r7 = "HwDPMS received transaction_uninstallPackage packageName: "
            r6.append(r7)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r6.append(r4)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            java.lang.String r7 = ", keepData: "
            r6.append(r7)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r6.append(r2)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r6.append(r0)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r6.append(r5)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            java.lang.String r0 = r6.toString()     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            android.util.Log.i(r15, r0)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
        L_0x041f:
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r10.bdReportMdmPolicy(r1, r0)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r0.uninstallPackage(r1, r4, r2, r5)     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x043a, Exception -> 0x042f }
            goto L_0x0444
        L_0x042f:
            r0 = move-exception
            java.lang.String r6 = "uninstallPackage exception"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
            goto L_0x0445
        L_0x043a:
            r0 = move-exception
            java.lang.String r6 = "uninstallPackage occur RuntimeException"
            android.util.Log.e(r15, r6)
            r13.writeException(r0)
        L_0x0444:
        L_0x0445:
            return r14
        L_0x0446:
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x0456
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r2 = r0
            goto L_0x0457
        L_0x0456:
            r2 = r0
        L_0x0457:
            java.lang.String r3 = r28.readString()
            int r4 = r28.readInt()
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x0478, Exception -> 0x046d }
            r10.bdReportMdmPolicy(r2, r0)     // Catch:{ RuntimeException -> 0x0478, Exception -> 0x046d }
            r10.execCommand(r11, r2, r3, r4)     // Catch:{ RuntimeException -> 0x0478, Exception -> 0x046d }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x0478, Exception -> 0x046d }
            goto L_0x0482
        L_0x046d:
            r0 = move-exception
            java.lang.String r5 = "setPreferApn exception"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
            goto L_0x0483
        L_0x0478:
            r0 = move-exception
            java.lang.String r5 = "setPreferApn occur RuntimeException"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
        L_0x0482:
        L_0x0483:
            return r14
        L_0x0484:
            r2 = r9
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x0493
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
        L_0x0493:
            int r3 = r28.readInt()
            boolean r4 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW
            if (r4 == 0) goto L_0x04af
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "HwDPMS received transaction_captureScreen user : "
            r4.append(r5)
            r4.append(r3)
            java.lang.String r4 = r4.toString()
            android.util.Log.i(r15, r4)
        L_0x04af:
            java.lang.String r4 = r26.getPolicyNameForBdReport(r27)
            r10.bdReportMdmPolicy(r0, r4)
            com.android.server.devicepolicy.IHwDevicePolicyManager r4 = r10.mService
            android.graphics.Bitmap r4 = r4.captureScreen(r0, r3)
            r29.writeNoException()
            if (r4 == 0) goto L_0x04c8
            r13.writeInt(r14)
            r4.writeToParcel(r13, r2)
            goto L_0x04cb
        L_0x04c8:
            r13.writeInt(r2)
        L_0x04cb:
            return r14
        L_0x04cc:
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x04dc
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r2 = r0
            goto L_0x04dd
        L_0x04dc:
            r2 = r0
        L_0x04dd:
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r3 = r0
            r12.readStringList(r3)
            int r4 = r28.readInt()
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x0503, Exception -> 0x04f8 }
            r10.bdReportMdmPolicy(r2, r0)     // Catch:{ RuntimeException -> 0x0503, Exception -> 0x04f8 }
            r10.execCommand(r11, r2, r3, r4)     // Catch:{ RuntimeException -> 0x0503, Exception -> 0x04f8 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x0503, Exception -> 0x04f8 }
            goto L_0x050d
        L_0x04f8:
            r0 = move-exception
            java.lang.String r5 = "setCustomSettingsMenu exception"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
            goto L_0x050e
        L_0x0503:
            r0 = move-exception
            java.lang.String r5 = "setCustomSettingsMenu occur RuntimeException"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
        L_0x050d:
        L_0x050e:
            return r14
        L_0x050f:
            r12.enforceInterface(r1)
            r1 = 0
            int r2 = r28.readInt()
            if (r2 == 0) goto L_0x051d
            android.content.ComponentName r1 = android.content.ComponentName.readFromParcel(r28)
        L_0x051d:
            java.lang.String r3 = r28.readString()
            java.lang.String r4 = r28.readString()
            int r5 = r28.readInt()
            boolean r6 = com.android.server.devicepolicy.TransactionProcessor.HWFLOW
            if (r6 == 0) goto L_0x054f
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "HwDPMS received transaction_setDefaultLauncher packageName: "
            r6.append(r7)
            r6.append(r3)
            java.lang.String r7 = ", className: "
            r6.append(r7)
            r6.append(r4)
            r6.append(r0)
            r6.append(r5)
            java.lang.String r0 = r6.toString()
            android.util.Log.i(r15, r0)
        L_0x054f:
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)
            r10.bdReportMdmPolicy(r1, r0)
            com.android.server.devicepolicy.IHwDevicePolicyManager r0 = r10.mService
            r0.setDefaultLauncher(r1, r3, r4, r5)
            r29.writeNoException()
            return r14
        L_0x055f:
            r12.enforceInterface(r1)
            r0 = 0
            r1 = 0
            int r2 = r28.readInt()
            if (r2 == 0) goto L_0x056e
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
        L_0x056e:
            int r3 = r28.readInt()
            boolean r1 = r10.isFunctionDisabled(r11, r0, r3)
            r29.writeNoException()
            r13.writeInt(r1)
            return r14
        L_0x057d:
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x058d
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r3 = r0
            goto L_0x058e
        L_0x058d:
            r3 = r0
        L_0x058e:
            int r0 = r28.readInt()
            if (r0 != r14) goto L_0x0595
            r2 = r14
        L_0x0595:
            int r4 = r28.readInt()
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x05b2, Exception -> 0x05a7 }
            r10.bdReportMdmPolicy(r3, r0)     // Catch:{ RuntimeException -> 0x05b2, Exception -> 0x05a7 }
            r10.setFunctionDisabled(r11, r3, r2, r4)     // Catch:{ RuntimeException -> 0x05b2, Exception -> 0x05a7 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x05b2, Exception -> 0x05a7 }
            goto L_0x05bc
        L_0x05a7:
            r0 = move-exception
            java.lang.String r5 = "setFunctionDisabled exception"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
            goto L_0x05bd
        L_0x05b2:
            r0 = move-exception
            java.lang.String r5 = "setFunctionDisabled occur RuntimeException"
            android.util.Log.e(r15, r5)
            r13.writeException(r0)
        L_0x05bc:
        L_0x05bd:
            return r14
        L_0x05be:
            r2 = r9
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x05cf
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r3 = r0
            goto L_0x05d0
        L_0x05cf:
            r3 = r0
        L_0x05d0:
            java.lang.String r4 = r28.readString()
            int r5 = r28.readInt()
            r6 = 0
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x05f9, Exception -> 0x05ef }
            r10.bdReportMdmPolicy(r3, r0)     // Catch:{ RuntimeException -> 0x05f9, Exception -> 0x05ef }
            boolean r0 = r10.clearCarrierLockScreenPassword(r3, r4, r5)     // Catch:{ RuntimeException -> 0x05f9, Exception -> 0x05ef }
            r6 = r0
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x05f9, Exception -> 0x05ef }
            if (r6 != r14) goto L_0x05eb
            r2 = r14
        L_0x05eb:
            r13.writeInt(r2)     // Catch:{ RuntimeException -> 0x05f9, Exception -> 0x05ef }
            goto L_0x0602
        L_0x05ef:
            r0 = move-exception
            java.lang.String r2 = "clear carrierlockscreenpassword exception"
            android.util.Log.e(r15, r2)
            r13.writeException(r0)
            goto L_0x0603
        L_0x05f9:
            r0 = move-exception
            java.lang.String r2 = "clear carrierlockscreenpassword occur RuntimeException"
            android.util.Log.e(r15, r2)
            r13.writeException(r0)
        L_0x0602:
        L_0x0603:
            return r14
        L_0x0604:
            r2 = r9
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x0615
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r3 = r0
            goto L_0x0616
        L_0x0615:
            r3 = r0
        L_0x0616:
            java.lang.String r4 = r28.readString()
            java.lang.String r5 = r28.readString()
            int r6 = r28.readInt()
            r7 = 0
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x0644, Exception -> 0x0639 }
            r10.bdReportMdmPolicy(r3, r0)     // Catch:{ RuntimeException -> 0x0644, Exception -> 0x0639 }
            boolean r0 = r10.setCarrierLockScreenPassword(r3, r4, r5, r6)     // Catch:{ RuntimeException -> 0x0644, Exception -> 0x0639 }
            r7 = r0
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x0644, Exception -> 0x0639 }
            if (r7 != r14) goto L_0x0635
            r2 = r14
        L_0x0635:
            r13.writeInt(r2)     // Catch:{ RuntimeException -> 0x0644, Exception -> 0x0639 }
            goto L_0x064e
        L_0x0639:
            r0 = move-exception
            java.lang.String r2 = "set carrierlockscreenpassword exception"
            android.util.Log.e(r15, r2)
            r13.writeException(r0)
            goto L_0x064f
        L_0x0644:
            r0 = move-exception
            java.lang.String r2 = "set carrierLockScreenPassword occur RuntimeException"
            android.util.Log.e(r15, r2)
            r13.writeException(r0)
        L_0x064e:
        L_0x064f:
            return r14
        L_0x0650:
            r12.enforceInterface(r1)
            r0 = 0
            int r1 = r28.readInt()
            if (r1 == 0) goto L_0x0660
            android.content.ComponentName r0 = android.content.ComponentName.readFromParcel(r28)
            r2 = r0
            goto L_0x0661
        L_0x0660:
            r2 = r0
        L_0x0661:
            int r3 = r28.readInt()
            java.lang.String r0 = r26.getPolicyNameForBdReport(r27)     // Catch:{ RuntimeException -> 0x067d, Exception -> 0x0673 }
            r10.bdReportMdmPolicy(r2, r0)     // Catch:{ RuntimeException -> 0x067d, Exception -> 0x0673 }
            r10.execCommand(r11, r2, r3)     // Catch:{ RuntimeException -> 0x067d, Exception -> 0x0673 }
            r29.writeNoException()     // Catch:{ RuntimeException -> 0x067d, Exception -> 0x0673 }
            goto L_0x0686
        L_0x0673:
            r0 = move-exception
            java.lang.String r4 = "clearDefaultLauncher exception"
            android.util.Log.e(r15, r4)
            r13.writeException(r0)
            goto L_0x0687
        L_0x067d:
            r0 = move-exception
            java.lang.String r4 = "clearDefaultLauncher occur RuntimeException"
            android.util.Log.e(r15, r4)
            r13.writeException(r0)
        L_0x0686:
        L_0x0687:
            return r14
            switch-data {1004->0x0197, 1005->0x055f, 1006->0x0197, 1007->0x055f, 1008->0x0197, 1009->0x055f, 1010->0x0197, 1011->0x055f, 1012->0x0197, 1013->0x055f, 1014->0x0197, 1015->0x055f, 1016->0x0197, 1017->0x055f, 1018->0x0197, 1019->0x055f, 1020->0x0197, 1021->0x055f, 1022->0x0197, 1023->0x055f, 1024->0x0197, 1025->0x055f, 1026->0x0197, 1027->0x055f, 1028->0x0197, 1029->0x055f, 1030->0x0197, 1031->0x055f, 1032->0x0197, 1033->0x055f, 1034->0x0197, 1035->0x055f, 1036->0x0197, 1037->0x055f, 1038->0x0197, 1039->0x055f, }
            switch-data {1501->0x0650, 1502->0x0650, 1503->0x055f, 1504->0x0197, 1505->0x055f, 1506->0x050f, 1507->0x0650, 1508->0x04cc, 1509->0x0484, 1510->0x0446, 1511->0x0446, 1512->0x0650, 1513->0x0197, }
            switch-data {2501->0x0446, 2502->0x03db, 2503->0x0446, 2504->0x0650, 2505->0x04cc, 2506->0x055f, 2507->0x03a6, 2508->0x04cc, 2509->0x04cc, 2510->0x03a6, 2511->0x04cc, 2512->0x04cc, 2513->0x03a6, 2514->0x04cc, 2515->0x04cc, 2516->0x03a6, }
            switch-data {3001->0x04cc, 3002->0x04cc, 3003->0x03a6, 3004->0x04cc, 3005->0x04cc, 3006->0x03a6, 3007->0x0446, }
            switch-data {3501->0x034c, 3502->0x0301, 3503->0x0650, }
            switch-data {4001->0x055f, 4002->0x055f, 4003->0x055f, 4004->0x03a6, 4005->0x03a6, 4006->0x03a6, 4007->0x03a6, 4008->0x03a6, 4009->0x055f, 4010->0x03a6, 4011->0x055f, 4012->0x055f, 4013->0x055f, 4014->0x055f, 4015->0x055f, 4016->0x055f, 4017->0x055f, 4018->0x055f, 4019->0x03a6, 4020->0x03a6, 4021->0x055f, 4022->0x055f, 4023->0x055f, 4024->0x055f, 4025->0x055f, 4026->0x055f, 4027->0x03a6, 4028->0x03a6, }
            switch-data {5001->0x02a6, 5002->0x0446, 5003->0x024b, 5004->0x01fd, 5005->0x01a9, 5006->0x0446, 5007->0x04cc, 5008->0x04cc, 5009->0x03a6, 5010->0x019a, 5011->0x0197, 5012->0x055f, }
            switch-data {5017->0x0154, 5018->0x0110, 5019->0x00d4, 5020->0x0040, 5021->0x055f, 5022->0x055f, }
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.TransactionProcessor.processTransaction(int, android.os.Parcel, android.os.Parcel):boolean");
    }

    private boolean processTransactionEx(int code, Parcel data, Parcel reply) {
        int i = 0;
        if (code != 1514) {
            return false;
        }
        data.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
        ComponentName who = null;
        if (data.readInt() != 0) {
            who = ComponentName.readFromParcel(data);
        }
        Message response = null;
        if (data.readInt() != 0) {
            response = (Message) Message.CREATOR.createFromParcel(data);
        }
        int slotId = data.readInt();
        int userHandle = data.readInt();
        try {
            bdReportMdmPolicy(who, getPolicyNameForBdReport(code));
            boolean result = setDefaultDataCard(who, slotId, response, userHandle);
            reply.writeNoException();
            if (result) {
                i = 1;
            }
            reply.writeInt(i);
        } catch (RuntimeException e) {
            Log.e(TAG, "Set Default Data Card occur RuntimeException");
            reply.writeException(e);
        } catch (Exception e2) {
            Log.e(TAG, "Set Default Data Card occur exception");
            reply.writeException(e2);
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void setFunctionDisabled(int code, ComponentName who, boolean isDisabled, int userHandle) {
        switch (code) {
            case 1004:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setWifiDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setWifiDisabled(who, isDisabled, userHandle);
                return;
            case 1006:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setWifiApDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setWifiApDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_CELL_STATE_DISABLE /*{ENCODED_INT: 1008}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setBootLoaderDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setBootLoaderDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_CELL_STATE_DISCONNECT /*{ENCODED_INT: 1010}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setUSBDataDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setUSBDataDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_SCREEN_IS_TURNOFF /*{ENCODED_INT: 1012}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setExternalStorageDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setExternalStorageDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_DATA_ROAMING_DISABLE /*{ENCODED_INT: 1014}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setNFCDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setNFCDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_STATE_NO_CONNECTION /*{ENCODED_INT: 1016}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setDataConnectivityDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setDataConnectivityDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_VPN_STATE_CHANGED /*{ENCODED_INT: 1018}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setVoiceDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setVoiceDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_STATE_OUT_OF_SERVICE /*{ENCODED_INT: 1020}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setSMSDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setSMSDisabled(who, isDisabled, userHandle);
                return;
            case 1022:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setStatusBarExpandPanelDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setStatusBarExpandPanelDisabled(who, isDisabled, userHandle);
                return;
            case 1024:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setBluetoothDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setBluetoothDisabled(who, isDisabled, userHandle);
                return;
            case 1026:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setGPSDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setGPSDisabled(who, isDisabled, userHandle);
                return;
            case 1028:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setAdbDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setAdbDisabled(who, isDisabled, userHandle);
                return;
            case HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK /*{ENCODED_INT: 1030}*/:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setUSBOtgDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setUSBOtgDisabled(who, isDisabled, userHandle);
                return;
            case 1032:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setSafeModeDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setSafeModeDisabled(who, isDisabled, userHandle);
                return;
            case 1034:
                this.mService.setTaskButtonDisabled(who, isDisabled, userHandle);
                return;
            case 1036:
                this.mService.setHomeButtonDisabled(who, isDisabled, userHandle);
                return;
            case 1038:
                this.mService.setBackButtonDisabled(who, isDisabled, userHandle);
                return;
            case 1504:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_turnOnGPS, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.turnOnGPS(who, isDisabled, userHandle);
                return;
            case 1513:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_turnOnMobiledata, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.turnOnMobiledata(who, isDisabled, userHandle);
                return;
            case 5011:
                if (HWFLOW) {
                    Log.i(TAG, "HwDPMS received transaction_setSDcardDecryptionDisabled, isDisabled: " + isDisabled + ", user: " + userHandle);
                }
                this.mService.setSDCardDecryptionDisabled(who, isDisabled, userHandle);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    /* access modifiers changed from: package-private */
    public boolean isFunctionDisabled(int code, ComponentName who, int userHandle) {
        if (code == 5012) {
            boolean isDisabled = this.mService.isSDCardDecryptionDisabled(who, userHandle);
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_isSDcardDecryptionDisabled, the ret: " + isDisabled);
            }
            return isDisabled;
        } else if (userHandle != 0) {
            return false;
        } else {
            if (!(code == 5021 || code == 5022)) {
                switch (code) {
                    case 1005:
                        boolean isDisabled2 = this.mService.isWifiDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isWifiDisabled, the ret: " + isDisabled2);
                        }
                        return isDisabled2;
                    case HwArbitrationDEFS.MSG_CELL_STATE_ENABLE /*{ENCODED_INT: 1007}*/:
                        boolean isDisabled3 = this.mService.isWifiApDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isWifiApDisabled, the ret: " + isDisabled3);
                        }
                        return isDisabled3;
                    case HwArbitrationDEFS.MSG_CELL_STATE_CONNECTED /*{ENCODED_INT: 1009}*/:
                        boolean isDisabled4 = this.mService.isBootLoaderDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isBootLoaderDisabled, the ret: " + isDisabled4);
                        }
                        return isDisabled4;
                    case HwPackageManagerService.TRANSACTION_CODE_GET_HDB_KEY /*{ENCODED_INT: 1011}*/:
                        boolean isDisabled5 = this.mService.isUSBDataDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isUSBDataDisabled, the ret: " + isDisabled5);
                        }
                        return isDisabled5;
                    case HwArbitrationDEFS.MSG_DATA_ROAMING_ENABLE /*{ENCODED_INT: 1013}*/:
                        boolean isDisabled6 = this.mService.isExternalStorageDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isExternalStorageDisabled, the ret: " + isDisabled6);
                        }
                        return isDisabled6;
                    case HwArbitrationDEFS.MSG_STATE_IS_ROAMING /*{ENCODED_INT: 1015}*/:
                        boolean isDisabled7 = this.mService.isNFCDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isNFCDisabled, the ret: " + isDisabled7);
                        }
                        return isDisabled7;
                    case HwArbitrationDEFS.MSG_SCREEN_IS_ON /*{ENCODED_INT: 1017}*/:
                        boolean isDisabled8 = this.mService.isDataConnectivityDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isDataConnectivityDisabled, the ret: " + isDisabled8);
                        }
                        return isDisabled8;
                    case HwArbitrationDEFS.MSG_STATE_IN_SERVICE /*{ENCODED_INT: 1019}*/:
                        boolean isDisabled9 = this.mService.isVoiceDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isVoiceDisabled, the ret: " + isDisabled9);
                        }
                        return isDisabled9;
                    case 1021:
                        boolean isDisabled10 = this.mService.isSMSDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isSMSDisabled, the ret: " + isDisabled10);
                        }
                        return isDisabled10;
                    case 1023:
                        boolean isDisabled11 = this.mService.isStatusBarExpandPanelDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isStatusBarExpandPanelDisabled, the ret: " + isDisabled11);
                        }
                        return isDisabled11;
                    case 1025:
                        boolean isDisabled12 = this.mService.isBluetoothDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isBluetoothDisabled, the ret: " + isDisabled12);
                        }
                        return isDisabled12;
                    case 1027:
                        boolean isDisabled13 = this.mService.isGPSDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isGPSDisabled, the ret: " + isDisabled13);
                        }
                        return isDisabled13;
                    case 1029:
                        boolean isDisabled14 = this.mService.isAdbDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isAdbDisabled, the ret: " + isDisabled14);
                        }
                        return isDisabled14;
                    case 1031:
                        boolean isDisabled15 = this.mService.isUSBOtgDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isUSBOtgDisabled, the ret: " + isDisabled15);
                        }
                        return isDisabled15;
                    case 1033:
                        boolean isDisabled16 = this.mService.isSafeModeDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isSafeModeDisabled, the ret: " + isDisabled16);
                        }
                        return isDisabled16;
                    case 1035:
                        return this.mService.isTaskButtonDisabled(who, userHandle);
                    case 1037:
                        return this.mService.isHomeButtonDisabled(who, userHandle);
                    case 1039:
                        return this.mService.isBackButtonDisabled(who, userHandle);
                    case 1503:
                        boolean isDisabled17 = this.mService.isRooted(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isRooted, the ret: " + isDisabled17);
                        }
                        return isDisabled17;
                    case 1505:
                        boolean isDisabled18 = this.mService.isGPSTurnOn(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isGPSTurnOn, the ret: " + isDisabled18);
                        }
                        return isDisabled18;
                    case 2506:
                        boolean isDisabled19 = this.mService.isInstallSourceDisabled(who, userHandle);
                        if (HWFLOW) {
                            Log.i(TAG, "HwDPMS received transaction_isStatusBarExpandPanelDisabled, the ret: " + isDisabled19);
                        }
                        return isDisabled19;
                    case 4009:
                        break;
                    default:
                        switch (code) {
                            case 4001:
                            case 4002:
                            case 4003:
                                break;
                            default:
                                switch (code) {
                                    case 4011:
                                        boolean isDisabled20 = this.mService.isSafeModeDisabled(null, userHandle);
                                        if (HWFLOW) {
                                            Log.i(TAG, "HwDPMS received transaction_isSafeModeDisabled, the ret: " + isDisabled20);
                                        }
                                        return isDisabled20;
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
                                                return false;
                                        }
                                }
                        }
                }
            }
            boolean isDisabled21 = this.mService.getHwAdminCachedValue(code);
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_isHwFrameworkAdminAllowed, the ret: " + isDisabled21);
            }
            return isDisabled21;
        }
    }

    /* access modifiers changed from: package-private */
    public void execCommand(int code, ComponentName who, int userHandle) {
        if (code == 1501 || code == 1502) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_rebootDevice, user: " + userHandle);
            }
            this.mService.shutdownOrRebootDevice(code, who, userHandle);
        } else if (code == 1507) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_clearDefaultLauncher, user: " + userHandle);
            }
            this.mService.clearDefaultLauncher(who, userHandle);
        } else if (code == 1512) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_clearDeviceOwnerApp, user: " + userHandle);
            }
            this.mService.clearDeviceOwnerApp(userHandle);
        } else if (code == 2001) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_hangupCalling, user: " + userHandle);
            }
            this.mService.hangupCalling(who, userHandle);
        } else if (code == 2504) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_enableInstallPackage, user: " + userHandle);
            }
            this.mService.enableInstallPackage(who, userHandle);
        } else if (code == 3503) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_resetNetworkSetting, user: " + userHandle);
            }
            this.mService.resetNetorkSetting(who, userHandle);
        } else if (code == 6001) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_setSilentActiveAdmin, user: " + userHandle);
            }
            this.mService.setSilentActiveAdmin(who, userHandle);
        }
    }

    /* access modifiers changed from: package-private */
    public void execCommand(int code, ComponentName who, String param, int userHandle) {
        if (code == 1510) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_setSysTime, packageName: " + param + ", user: " + userHandle);
            }
            try {
                this.mService.setSysTime(who, Long.parseLong(param), userHandle);
            } catch (NumberFormatException e) {
                Log.e(TAG, "setSysTime : NumberFormatException");
            }
        } else if (code == 1511) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_setDeviceOwnerApp, ownnerName: " + param + ", user: " + userHandle);
            }
            this.mService.setDeviceOwnerApp(who, param, userHandle);
        } else if (code == 2501) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_installPackage packagePath: " + param + ", user: " + userHandle);
            }
            this.mService.installPackage(who, param, userHandle);
        } else if (code == 2503) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_clearPackageData, packageName: " + param + ", user: " + userHandle);
            }
            this.mService.clearPackageData(who, param, userHandle);
        } else if (code == 3007) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_killApplicationProcess packageName: " + param + ", user: " + userHandle);
            }
            this.mService.killApplicationProcess(who, param, userHandle);
        } else if (code == 5002) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_deleteApn, apnId: " + param + ", user: " + userHandle);
            }
            this.mService.deleteApn(who, param, userHandle);
        } else if (code == 5006) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_setPreferApn, apnId: " + param + ", user: " + userHandle);
            }
            this.mService.setPreferApn(who, param, userHandle);
        }
    }

    /* access modifiers changed from: package-private */
    public void execCommand(int code, ComponentName who, List<String> param, int userHandle) {
        if (code == 1508) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_setCustomSettingsMenu user: " + userHandle);
            }
            this.mService.setCustomSettingsMenu(who, param, userHandle);
        } else if (code == 2505) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_disableInstallPackage user: " + userHandle);
            }
            this.mService.disableInstallSource(who, param, userHandle);
        } else if (code == 2508) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_addInstallPackageWhiteList user: " + userHandle);
            }
            this.mService.addInstallPackageWhiteList(who, param, userHandle);
        } else if (code == 2509) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_removeInstallPackageWhiteList user: " + userHandle);
            }
            this.mService.removeInstallPackageWhiteList(who, param, userHandle);
        } else if (code == 2511) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_addDisallowedUninstallPackages user: " + userHandle);
            }
            this.mService.addDisallowedUninstallPackages(who, param, userHandle);
        } else if (code == 2512) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_removeDisallowedUninstallPackages user: " + userHandle);
            }
            this.mService.removeDisallowedUninstallPackages(who, param, userHandle);
        } else if (code == 2514) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_addDisabledDeactivateMdmPackages user: " + userHandle);
            }
            this.mService.addDisabledDeactivateMdmPackages(who, param, userHandle);
        } else if (code == 2515) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_removeDisabledDeactivateMdmPackages user: " + userHandle);
            }
            this.mService.removeDisabledDeactivateMdmPackages(who, param, userHandle);
        } else if (code == 3001) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_addPersistentApp user: " + userHandle);
            }
            this.mService.addPersistentApp(who, param, userHandle);
        } else if (code == 3002) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_removePersistentApp user: " + userHandle);
            }
            this.mService.removePersistentApp(who, param, userHandle);
        } else if (code == 3004) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_addDisallowedRunningApp user: " + userHandle);
            }
            this.mService.addDisallowedRunningApp(who, param, userHandle);
        } else if (code == 3005) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_removeDisallowedRunningApp user: " + userHandle);
            }
            this.mService.removeDisallowedRunningApp(who, param, userHandle);
        } else if (code == 5007) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_addNetworkAccessWhitelist user: " + userHandle);
            }
            this.mService.addNetworkAccessWhitelist(who, param, userHandle);
        } else if (code == 5008) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_removeNetworkAccessWhitelist user: " + userHandle);
            }
            this.mService.removeNetworkAccessWhitelist(who, param, userHandle);
        }
    }

    /* access modifiers changed from: package-private */
    public List<String> getListCommand(int code, ComponentName who, int userHandle) {
        if (userHandle != 0) {
            return null;
        }
        if (code == 2507) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_getInstallPackageSourceWhiteList user: " + userHandle);
            }
            return this.mService.getInstallPackageSourceWhiteList(who, userHandle);
        } else if (code == 2510) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_getInstallPackageWhiteList user: " + userHandle);
            }
            return this.mService.getInstallPackageWhiteList(who, userHandle);
        } else if (code == 2513) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_getDisallowedUninstallPackageList user: " + userHandle);
            }
            return this.mService.getDisallowedUninstallPackageList(who, userHandle);
        } else if (code == 2516) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_getDisabledDeactivateMdmPackageList user: " + userHandle);
            }
            return this.mService.getDisabledDeactivateMdmPackageList(who, userHandle);
        } else if (code == 3003) {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_getPersistentApp user: " + userHandle);
            }
            return this.mService.getPersistentApp(who, userHandle);
        } else if (code != 3006) {
            if (code != 4010) {
                if (code == 5009) {
                    if (HWFLOW) {
                        Log.i(TAG, "HwDPMS received transaction_getNetworkAccessWhitelist user: " + userHandle);
                    }
                    return this.mService.getNetworkAccessWhitelist(who, userHandle);
                } else if (!(code == 4019 || code == 4020 || code == 4027 || code == 4028)) {
                    switch (code) {
                        case 4004:
                        case 4005:
                        case 4006:
                        case 4007:
                        case 4008:
                            break;
                        default:
                            return null;
                    }
                }
            }
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_getHwFrameworkAdminList user: " + userHandle + AwarenessInnerConstants.COLON_KEY + code);
            }
            return this.mService.getHwAdminCachedList(code);
        } else {
            if (HWFLOW) {
                Log.i(TAG, "HwDPMS received transaction_getDisallowedRunningApp user: " + userHandle);
            }
            return this.mService.getDisallowedRunningApp(who, userHandle);
        }
    }

    /* access modifiers changed from: package-private */
    public int getSDCardEncryptionStatus(int code) {
        if (code != 5010) {
            return 0;
        }
        return this.mService.getSDCardEncryptionStatus();
    }

    /* access modifiers changed from: package-private */
    public boolean processTransactionWithPolicyName(int code, Parcel data, Parcel reply) {
        ComponentName whoGetPolicy;
        ComponentName whoSetPolicy;
        ComponentName whoRemovePolicy;
        int i = 0;
        switch (code) {
            case 5013:
                data.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                if (data.readInt() != 0) {
                    whoGetPolicy = ComponentName.readFromParcel(data);
                } else {
                    whoGetPolicy = null;
                }
                try {
                    Bundle getPolicyData = getPolicy(whoGetPolicy, data.readString(), data.readBundle(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeBundle(getPolicyData);
                } catch (RuntimeException e) {
                    Log.e(TAG, "getPolicy occur RuntimeException");
                    reply.writeException(e);
                } catch (Exception e2) {
                    Log.e(TAG, "getPolicy exception");
                    reply.writeException(e2);
                }
                return true;
            case 5014:
                data.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                if (data.readInt() != 0) {
                    whoSetPolicy = ComponentName.readFromParcel(data);
                } else {
                    whoSetPolicy = null;
                }
                try {
                    int setPolicyResult = setPolicy(whoSetPolicy, data.readString(), data.readBundle(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(setPolicyResult);
                } catch (RuntimeException e3) {
                    Log.e(TAG, "setPolicy occur RuntimeException");
                    reply.writeException(e3);
                } catch (Exception e4) {
                    Log.e(TAG, "setPolicy exception");
                    reply.writeException(e4);
                }
                return true;
            case 5015:
                data.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                if (data.readInt() != 0) {
                    whoRemovePolicy = ComponentName.readFromParcel(data);
                } else {
                    whoRemovePolicy = null;
                }
                try {
                    int removePolicyResult = removePolicy(whoRemovePolicy, data.readString(), data.readBundle(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(removePolicyResult);
                } catch (RuntimeException e5) {
                    Log.e(TAG, "removePolicy occur RuntimeException");
                    reply.writeException(e5);
                } catch (Exception e6) {
                    Log.e(TAG, "removePolicy exception");
                    reply.writeException(e6);
                }
                return true;
            case 5016:
                data.enforceInterface("com.huawei.android.app.admin.hwdevicepolicymanagerex");
                try {
                    boolean isDisabled = hasHwPolicy(data.readInt());
                    reply.writeNoException();
                    if (isDisabled) {
                        i = 1;
                    }
                    reply.writeInt(i);
                } catch (RuntimeException e7) {
                    Log.e(TAG, "hasHwPolicy occur RuntimeException");
                    reply.writeException(e7);
                } catch (Exception e8) {
                    Log.e(TAG, "hasHwPolicy exception");
                    reply.writeException(e8);
                }
                return true;
            default:
                return false;
        }
    }

    /* access modifiers changed from: package-private */
    public Bundle getPolicy(ComponentName who, String policyName, Bundle keyWords, int userHandle, int type) {
        if (type == 0) {
            return this.mService.getPolicy(who, policyName, userHandle);
        }
        if (type == 1) {
            char c = 65535;
            int hashCode = policyName.hashCode();
            if (hashCode != -1078880066) {
                if (hashCode != 830361577) {
                    if (hashCode == 1115753445 && policyName.equals("queryBrowsingHistory")) {
                        c = 0;
                    }
                } else if (policyName.equals("config-vpn")) {
                    c = 1;
                }
            } else if (policyName.equals("policy-top-packagename")) {
                c = 2;
            }
            if (c == 0) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("value", this.mService.queryBrowsingHistory(who, userHandle));
                return bundle;
            } else if (c == 1) {
                if (HWFLOW) {
                    Log.i(TAG, "receive get config-vpn policy: " + policyName);
                }
                if (keyWords == null) {
                    return this.mService.getVpnList(who, null, userHandle);
                }
                return this.mService.getVpnProfile(who, keyWords, userHandle);
            } else if (c == 2) {
                if (HWFLOW) {
                    Log.i(TAG, "get top application packagename policy: " + policyName);
                }
                return this.mService.getTopAppPackageName(who, userHandle);
            } else if (HWFLOW) {
                Log.i(TAG, "don't have this get policy: " + policyName);
            }
        } else if (type == 2) {
            return this.mService.getHwAdminCachedBundle(policyName);
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public int setPolicy(ComponentName who, String policyName, Bundle policyData, int userHandle, int type) {
        bdReportMdmPolicy(who, policyName);
        if (type == 0) {
            return this.mService.setPolicy(who, policyName, policyData, userHandle);
        }
        if (type != 1) {
            return 0;
        }
        char c = 65535;
        int hashCode = policyName.hashCode();
        if (hashCode != 830361577) {
            if (hashCode == 1456366027 && policyName.equals("set-system-language")) {
                c = 1;
            }
        } else if (policyName.equals("config-vpn")) {
            c = 0;
        }
        if (c == 0) {
            if (HWFLOW) {
                Log.i(TAG, "receive set config-vpn policy: " + policyName);
            }
            return this.mService.configVpnProfile(who, policyData, userHandle);
        } else if (c != 1) {
            if (HWFLOW) {
                Log.i(TAG, "don't have this set policy: " + policyName);
            }
            return 0;
        } else {
            if (HWFLOW) {
                Log.i(TAG, "receive set system language policy: " + policyName);
            }
            return this.mService.setSystemLanguage(who, policyData, userHandle);
        }
    }

    /* access modifiers changed from: package-private */
    public int removePolicy(ComponentName who, String policyName, Bundle policyData, int userHandle, int type) {
        if (type == 0) {
            return this.mService.removePolicy(who, policyName, policyData, userHandle);
        }
        if (type == 1) {
            char c = 65535;
            if (policyName.hashCode() == 830361577 && policyName.equals("config-vpn")) {
                c = 0;
            }
            if (c == 0) {
                if (HWFLOW) {
                    Log.i(TAG, "receive remove config-vpn policy: " + policyName);
                }
                return this.mService.removeVpnProfile(who, policyData, userHandle);
            } else if (HWFLOW) {
                Log.i(TAG, "don't have this remove policy: " + policyName);
            }
        }
        return 0;
    }

    private void bdReportMdmPolicy(ComponentName who, String policyName) {
        if (!TextUtils.isEmpty(policyName)) {
            try {
                JSONObject obj = new JSONObject();
                obj.put(BDREPORT_MDM_KEY_PACKAGE, who == null ? "" : who.getPackageName());
                obj.put(BDREPORT_MDM_KEY_APINAME, policyName);
                if (this.mService != null) {
                    this.mService.bdReport(CPUFeature.MSG_SET_CPUSETCONFIG_SCREENON, obj.toString());
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException can not put on obj");
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasHwPolicy(int userHandle) {
        return this.mService.hasHwPolicy(userHandle);
    }

    /* access modifiers changed from: package-private */
    public void setAccountDisabled(ComponentName who, String accountType, boolean isDisabled, int userHandle) {
        this.mService.setAccountDisabled(who, accountType, isDisabled, userHandle);
    }

    /* access modifiers changed from: package-private */
    public boolean isAccountDisabled(ComponentName who, String accountType, int userHandle) {
        return this.mService.isAccountDisabled(who, accountType, userHandle);
    }

    /* access modifiers changed from: package-private */
    public boolean formatSDCard(ComponentName who, String diskId, int userHandle) {
        return this.mService.formatSDCard(who, diskId, userHandle);
    }

    /* access modifiers changed from: package-private */
    public boolean installCertificateWithType(ComponentName who, int type, byte[] certBuffer, String name, String password, int flag, boolean isRequestAccess, int userHandle) {
        return this.mService.installCertificateWithType(who, type, certBuffer, name, password, flag, isRequestAccess, userHandle);
    }

    /* access modifiers changed from: package-private */
    public boolean setCarrierLockScreenPassword(ComponentName who, String password, String phoneNumber, int userHandle) {
        return this.mService.setCarrierLockScreenPassword(who, password, phoneNumber, userHandle);
    }

    /* access modifiers changed from: package-private */
    public boolean clearCarrierLockScreenPassword(ComponentName who, String password, int userHandle) {
        return this.mService.clearCarrierLockScreenPassword(who, password, userHandle);
    }

    /* access modifiers changed from: package-private */
    public boolean setDefaultDataCard(ComponentName who, int slot, Message response, int userHandle) {
        return this.mService.setDefaultDataCard(who, slot, response, userHandle);
    }

    private String getPolicyNameForBdReport(int code) {
        int index = -1;
        int i = 0;
        while (true) {
            try {
                if (i >= CODE_INDEXS.length) {
                    break;
                } else if (CODE_INDEXS[i] == code) {
                    index = i;
                    break;
                } else {
                    i++;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "ArrayIndexOutOfBoundsException when getPolicyNameForBdReport");
                return "";
            }
        }
        if (index != -1) {
            return POLICY_NAMES[index];
        }
        return "";
    }
}
