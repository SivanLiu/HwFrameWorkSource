package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.provider.SettingsEx.Systemex;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class HwCustHwSIMRecordsImpl extends HwCustHwSIMRecords {
    public static final String DATA_ROAMING_SIM2 = "data_roaming_sim2";
    private static final int EF_OCSGL = 20356;
    private static final int EVENT_GET_OCSGL_DONE = 1;
    private static final boolean HWDBG = true;
    private static String LAST_ICCID = "data_roaming_setting_last_iccid";
    private static final String LOG_TAG = "HwCustHwSIMRecordsImpl";
    private static final int SLOT0 = 0;
    private static final int SLOT1 = 1;
    private static boolean mIsSaveCardTypeLGU = SystemProperties.getBoolean("ro.config.save_cardtype_lgu", false);
    private static boolean mIsSupportCsgSearch = SystemProperties.getBoolean("ro.config.att.csg", false);
    private Handler custHandlerEx = new Handler() {
        /*  JADX ERROR: NullPointerException in pass: ProcessVariables
            java.lang.NullPointerException
            	at jadx.core.dex.visitors.regions.ProcessVariables.addToUsageMap(ProcessVariables.java:278)
            	at jadx.core.dex.visitors.regions.ProcessVariables.access$000(ProcessVariables.java:31)
            	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processInsn(ProcessVariables.java:152)
            	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processBlockTraced(ProcessVariables.java:129)
            	at jadx.core.dex.visitors.regions.TracedRegionVisitor.processBlock(TracedRegionVisitor.java:23)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:53)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
            	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:18)
            	at jadx.core.dex.visitors.regions.ProcessVariables.visit(ProcessVariables.java:183)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
            	at java.util.ArrayList.forEach(ArrayList.java:1257)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
            	at jadx.core.ProcessClass.process(ProcessClass.java:32)
            	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
            	at java.lang.Iterable.forEach(Iterable.java:75)
            	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
            	at jadx.core.ProcessClass.process(ProcessClass.java:37)
            	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
            	at jadx.api.JavaClass.decompile(JavaClass.java:62)
            	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
            */
        public void handleMessage(android.os.Message r11) {
            /*
            r10 = this;
            r0 = 0;
            r1 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;
            r1 = r1.mSIMRecords;
            if (r1 == 0) goto L_0x0039;
        L_0x0007:
            r1 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;
            r1 = r1.mSIMRecords;
            r1 = r1.mDestroyed;
            r1 = r1.get();
            if (r1 == 0) goto L_0x0039;
        L_0x0013:
            r1 = "HwCustHwSIMRecordsImpl";
            r2 = new java.lang.StringBuilder;
            r2.<init>();
            r3 = "Received message ";
            r2.append(r3);
            r2.append(r11);
            r3 = "[";
            r2.append(r3);
            r3 = r11.what;
            r2.append(r3);
            r3 = "]  while being destroyed. Ignoring.";
            r2.append(r3);
            r2 = r2.toString();
            android.telephony.Rlog.e(r1, r2);
            return;
        L_0x0039:
            r1 = r11.what;	 Catch:{ RuntimeException -> 0x00ef }
            r2 = 1;	 Catch:{ RuntimeException -> 0x00ef }
            if (r1 == r2) goto L_0x0058;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x003e:
            r1 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x00ef }
            r2.<init>();	 Catch:{ RuntimeException -> 0x00ef }
            r3 = "unknown Event: ";	 Catch:{ RuntimeException -> 0x00ef }
            r2.append(r3);	 Catch:{ RuntimeException -> 0x00ef }
            r3 = r11.what;	 Catch:{ RuntimeException -> 0x00ef }
            r2.append(r3);	 Catch:{ RuntimeException -> 0x00ef }
            r2 = r2.toString();	 Catch:{ RuntimeException -> 0x00ef }
            r1.log(r2);	 Catch:{ RuntimeException -> 0x00ef }
            goto L_0x00e4;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0058:
            r1 = r11.obj;	 Catch:{ RuntimeException -> 0x00ef }
            r1 = (android.os.AsyncResult) r1;	 Catch:{ RuntimeException -> 0x00ef }
            r2 = r1.exception;	 Catch:{ RuntimeException -> 0x00ef }
            r3 = 0;	 Catch:{ RuntimeException -> 0x00ef }
            if (r2 != 0) goto L_0x00c6;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0061:
            r2 = r1.result;	 Catch:{ RuntimeException -> 0x00ef }
            if (r2 != 0) goto L_0x0066;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0065:
            goto L_0x00c6;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0066:
            r2 = r1.result;	 Catch:{ RuntimeException -> 0x00ef }
            r2 = (java.util.ArrayList) r2;	 Catch:{ RuntimeException -> 0x00ef }
            r4 = 0;	 Catch:{ RuntimeException -> 0x00ef }
            r5 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r5.mEfOcsgl = r3;	 Catch:{ RuntimeException -> 0x00ef }
            r3 = 0;	 Catch:{ RuntimeException -> 0x00ef }
            r5 = r2.size();	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0075:
            if (r3 >= r5) goto L_0x00a7;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0077:
            r6 = r2.get(r3);	 Catch:{ RuntimeException -> 0x00ef }
            r6 = (byte[]) r6;	 Catch:{ RuntimeException -> 0x00ef }
            r7 = 0;	 Catch:{ RuntimeException -> 0x00ef }
            r7 = 0;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x007f:
            r8 = r6.length;	 Catch:{ RuntimeException -> 0x00ef }
            if (r7 >= r8) goto L_0x009e;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0082:
            r8 = r6[r7];	 Catch:{ RuntimeException -> 0x00ef }
            r9 = 255; // 0xff float:3.57E-43 double:1.26E-321;	 Catch:{ RuntimeException -> 0x00ef }
            r8 = r8 & r9;	 Catch:{ RuntimeException -> 0x00ef }
            if (r8 == r9) goto L_0x009b;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x0089:
            r8 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r9 = r6.length;	 Catch:{ RuntimeException -> 0x00ef }
            r9 = java.util.Arrays.copyOf(r6, r9);	 Catch:{ RuntimeException -> 0x00ef }
            r8.mEfOcsgl = r9;	 Catch:{ RuntimeException -> 0x00ef }
            r8 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r9 = "=csg= SIMRecords:  OCSGL not empty.";	 Catch:{ RuntimeException -> 0x00ef }
            r8.log(r9);	 Catch:{ RuntimeException -> 0x00ef }
            goto L_0x009e;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x009b:
            r7 = r7 + 1;	 Catch:{ RuntimeException -> 0x00ef }
            goto L_0x007f;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x009e:
            r8 = r6.length;	 Catch:{ RuntimeException -> 0x00ef }
            if (r7 >= r8) goto L_0x00a2;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x00a1:
            goto L_0x00a7;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x00a2:
            r4 = r4 + 1;	 Catch:{ RuntimeException -> 0x00ef }
            r3 = r3 + 1;	 Catch:{ RuntimeException -> 0x00ef }
            goto L_0x0075;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x00a7:
            r3 = r2.size();	 Catch:{ RuntimeException -> 0x00ef }
            if (r4 < r3) goto L_0x00bc;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x00ad:
            r3 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r5 = 0;	 Catch:{ RuntimeException -> 0x00ef }
            r5 = new byte[r5];	 Catch:{ RuntimeException -> 0x00ef }
            r3.mEfOcsgl = r5;	 Catch:{ RuntimeException -> 0x00ef }
            r3 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r5 = "=csg= SIMRecords:  OCSGL is empty. ";	 Catch:{ RuntimeException -> 0x00ef }
            r3.log(r5);	 Catch:{ RuntimeException -> 0x00ef }
        L_0x00bc:
            r3 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r3 = r3.mSIMRecords;	 Catch:{ RuntimeException -> 0x00ef }
            r3 = r3.mCsgRecordsLoadedRegistrants;	 Catch:{ RuntimeException -> 0x00ef }
            r3.notifyRegistrants();	 Catch:{ RuntimeException -> 0x00ef }
            goto L_0x00e4;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x00c6:
            r2 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r4 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x00ef }
            r4.<init>();	 Catch:{ RuntimeException -> 0x00ef }
            r5 = "=csg= EVENT_GET_OCSGL_DONE exception = ";	 Catch:{ RuntimeException -> 0x00ef }
            r4.append(r5);	 Catch:{ RuntimeException -> 0x00ef }
            r5 = r1.exception;	 Catch:{ RuntimeException -> 0x00ef }
            r4.append(r5);	 Catch:{ RuntimeException -> 0x00ef }
            r4 = r4.toString();	 Catch:{ RuntimeException -> 0x00ef }
            r2.log(r4);	 Catch:{ RuntimeException -> 0x00ef }
            r2 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ RuntimeException -> 0x00ef }
            r2.mEfOcsgl = r3;	 Catch:{ RuntimeException -> 0x00ef }
        L_0x00e4:
            r1 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;
            r1 = r1.mSIMRecords;
            if (r1 == 0) goto L_0x0115;
        L_0x00ea:
            if (r0 == 0) goto L_0x0115;
        L_0x00ec:
            goto L_0x010e;
        L_0x00ed:
            r1 = move-exception;
            goto L_0x0116;
        L_0x00ef:
            r1 = move-exception;
            r2 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;	 Catch:{ all -> 0x00ed }
            r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ed }
            r3.<init>();	 Catch:{ all -> 0x00ed }
            r4 = "Exception parsing SIM record:";	 Catch:{ all -> 0x00ed }
            r3.append(r4);	 Catch:{ all -> 0x00ed }
            r3.append(r1);	 Catch:{ all -> 0x00ed }
            r3 = r3.toString();	 Catch:{ all -> 0x00ed }
            r2.log(r3);	 Catch:{ all -> 0x00ed }
            r1 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;
            r1 = r1.mSIMRecords;
            if (r1 == 0) goto L_0x0115;
        L_0x010c:
            if (r0 == 0) goto L_0x0115;
        L_0x010e:
            r1 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;
            r1 = r1.mSIMRecords;
            r1.onRecordLoaded();
        L_0x0115:
            return;
        L_0x0116:
            r2 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;
            r2 = r2.mSIMRecords;
            if (r2 == 0) goto L_0x0125;
        L_0x011c:
            if (r0 == 0) goto L_0x0125;
        L_0x011e:
            r2 = com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl.this;
            r2 = r2.mSIMRecords;
            r2.onRecordLoaded();
        L_0x0125:
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.uicc.HwCustHwSIMRecordsImpl$AnonymousClass1.handleMessage(android.os.Message):void");
        }
    };
    boolean iccidChanged = false;
    private boolean isCustRoamingOpenArea = false;
    private boolean isCustRoamingOpenArea_SIM2 = false;
    private byte[] mEfOcsgl = null;

    public HwCustHwSIMRecordsImpl(SIMRecords obj, Context mConText) {
        super(obj, mConText);
    }

    public void setVmPriorityModeInClaro(VoiceMailConstants mVmConfig) {
        if (this.mContext != null && this.mSIMRecords != null) {
            int VoicemailPriorityMode = this.mContext.getContentResolver();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("voicemailPrioritySpecial_");
            stringBuilder.append(this.mSIMRecords.getOperatorNumeric());
            VoicemailPriorityMode = Systemex.getInt(VoicemailPriorityMode, stringBuilder.toString(), 0);
            stringBuilder = new StringBuilder();
            stringBuilder.append("The SIM card MCCMNC = ");
            stringBuilder.append(this.mSIMRecords.getOperatorNumeric());
            log(stringBuilder.toString());
            if (VoicemailPriorityMode != 0 && mVmConfig != null) {
                mVmConfig.setVoicemailInClaro(VoicemailPriorityMode);
                stringBuilder = new StringBuilder();
                stringBuilder.append("VoicemailPriorityMode from custom = ");
                stringBuilder.append(VoicemailPriorityMode);
                log(stringBuilder.toString());
            }
        }
    }

    public void refreshDataRoamingSettings() {
        String roamingAreaStr = Systemex.getString(this.mContext.getContentResolver(), "list_roaming_open_area");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("refreshDataRoamingSettings(): roamingAreaStr = ");
        stringBuilder.append(roamingAreaStr);
        log(stringBuilder.toString());
        if (TextUtils.isEmpty(roamingAreaStr) || this.mSIMRecords == null) {
            log("refreshDataRoamingSettings(): roamingAreaStr is empty");
            return;
        }
        SharedPreferences sp = this.mContext.getSharedPreferences("DataRoamingSettingIccid", 0);
        String mIccid = this.mSIMRecords.getIccId();
        if (!TextUtils.isEmpty(mIccid)) {
            String oldIccid;
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(LAST_ICCID);
                stringBuilder2.append(this.mSIMRecords.getSlotId());
                oldIccid = sp.getString(stringBuilder2.toString(), null);
            } else {
                oldIccid = sp.getString(LAST_ICCID, null);
            }
            if (oldIccid != null) {
                try {
                    oldIccid = new String(Base64.decode(oldIccid, 0), "utf-8");
                } catch (UnsupportedEncodingException e) {
                    Rlog.d(LOG_TAG, "refreshDataRoamingSettings(): iccid not UnsupportedEncodingException");
                }
            }
            if (mIccid.equals(oldIccid)) {
                this.iccidChanged = false;
            } else {
                this.iccidChanged = HWDBG;
            }
            if (this.iccidChanged) {
                try {
                    Editor editor = sp.edit();
                    if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(LAST_ICCID);
                        stringBuilder3.append(this.mSIMRecords.getSlotId());
                        editor.putString(stringBuilder3.toString(), new String(Base64.encode(mIccid.getBytes("utf-8"), 0), "utf-8"));
                    } else {
                        editor.putString(LAST_ICCID, new String(Base64.encode(mIccid.getBytes("utf-8"), 0), "utf-8"));
                    }
                    editor.commit();
                } catch (UnsupportedEncodingException e2) {
                    Rlog.d(LOG_TAG, "refreshDataRoamingSettings(): iccid not UnsupportedEncodingException");
                }
                this.isCustRoamingOpenArea = false;
                this.isCustRoamingOpenArea_SIM2 = false;
                String[] areaArray = roamingAreaStr.split(",");
                String operator = this.mSIMRecords.getOperatorNumeric();
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("refreshDataRoamingSettings(): roamingAreaStr : ");
                stringBuilder4.append(roamingAreaStr);
                stringBuilder4.append(" operator : ");
                stringBuilder4.append(operator);
                log(stringBuilder4.toString());
                int length;
                int i;
                String area;
                StringBuilder stringBuilder5;
                if (TelephonyManager.getDefault() != null && !TelephonyManager.getDefault().isMultiSimEnabled()) {
                    length = areaArray.length;
                    i = 0;
                    while (i < length) {
                        area = areaArray[i];
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("refreshDataRoamingSettings(): area : ");
                        stringBuilder5.append(area);
                        log(stringBuilder5.toString());
                        if (!area.equals(operator)) {
                            Global.putInt(this.mContext.getContentResolver(), "data_roaming", 0);
                            i++;
                        } else if (isSkipDataRoamingGid()) {
                            log("refreshDataRoamingSettings(): isSkipDataRoamingGid() returns true");
                        } else {
                            log("refreshDataRoamingSettings(): setting data roaming to true");
                            Systemex.putInt(this.mContext.getContentResolver(), "roaming_saving_on", 1);
                            Global.putInt(this.mContext.getContentResolver(), "data_roaming", 1);
                            this.isCustRoamingOpenArea = HWDBG;
                        }
                    }
                } else if (TelephonyManager.getDefault() != null && TelephonyManager.getDefault().isMultiSimEnabled()) {
                    log("######## MultiSimEnabled");
                    for (String area2 : areaArray) {
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("refreshDataRoamingSettings(): else loop area : ");
                        stringBuilder5.append(area2);
                        log(stringBuilder5.toString());
                        if (area2.equals(operator)) {
                            if (this.mSIMRecords.getSlotId() == 0) {
                                if (isSkipDataRoamingGid()) {
                                    log("refreshDataRoamingSettings(): isSkipDataRoamingGid() returns true for SIM1");
                                } else {
                                    Global.putInt(this.mContext.getContentResolver(), "data_roaming", 1);
                                    this.isCustRoamingOpenArea = HWDBG;
                                    log("refreshDataRoamingSettings(): setting data roaming to true else loop SIM1");
                                }
                            } else if (1 != this.mSIMRecords.getSlotId()) {
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("doesn't contains the carrier");
                                stringBuilder5.append(operator);
                                stringBuilder5.append("for slotId");
                                stringBuilder5.append(this.mSIMRecords.getSlotId());
                                log(stringBuilder5.toString());
                            } else if (isSkipDataRoamingGid()) {
                                log("refreshDataRoamingSettings(): isSkipDataRoamingGid() returns true for SIM2");
                            } else {
                                Global.putInt(this.mContext.getContentResolver(), DATA_ROAMING_SIM2, 1);
                                this.isCustRoamingOpenArea_SIM2 = HWDBG;
                            }
                        } else if (this.mSIMRecords.getSlotId() == 0) {
                            Global.putInt(this.mContext.getContentResolver(), "data_roaming", 0);
                        } else if (1 == this.mSIMRecords.getSlotId()) {
                            Global.putInt(this.mContext.getContentResolver(), DATA_ROAMING_SIM2, 0);
                        }
                    }
                }
                return;
            }
            String str = LOG_TAG;
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("refreshDataRoamingSettings(): iccid not changed");
            stringBuilder6.append(this.iccidChanged);
            Rlog.d(str, stringBuilder6.toString());
        }
    }

    private boolean isSkipDataRoamingGid() {
        String skipDataRoamingGid = Systemex.getString(this.mContext.getContentResolver(), "hw_skip_data_roaming_gid");
        byte[] simGidbytes = this.mSIMRecords != null ? this.mSIMRecords.getGID1() : null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSkipDataRoamingGid(): skipDataRoamingGid : ");
        stringBuilder.append(skipDataRoamingGid);
        stringBuilder.append(" simGidbytes : ");
        stringBuilder.append(simGidbytes);
        log(stringBuilder.toString());
        boolean matched = false;
        if (TextUtils.isEmpty(skipDataRoamingGid) || simGidbytes == null || simGidbytes.length <= 0) {
            return false;
        }
        String[] gidArray = skipDataRoamingGid.split(",");
        String simGid = IccUtils.bytesToHexString(simGidbytes);
        if (simGid == null || simGid.length() < 2) {
            return false;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isSkipDataRoamingGid(): simGid : ");
        stringBuilder2.append(simGid);
        log(stringBuilder2.toString());
        for (String gid : gidArray) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("isSkipDataRoamingGid(): cust gid : ");
            stringBuilder3.append(gid);
            log(stringBuilder3.toString());
            if (simGid.substring(0, 2).equals(gid)) {
                matched = HWDBG;
                break;
            }
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isSkipDataRoamingGid() returning : ");
        stringBuilder2.append(matched);
        log(stringBuilder2.toString());
        return matched;
    }

    public void refreshMobileDataAlwaysOnSettings() {
        String dataAlwaysOnAreaStr = System.getString(this.mContext.getContentResolver(), "list_mobile_data_always_on");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("refreshMobileDataAlwaysOnSettings(): dataAlwaysOnAreaStr = ");
        stringBuilder.append(dataAlwaysOnAreaStr);
        log(stringBuilder.toString());
        if (TextUtils.isEmpty(dataAlwaysOnAreaStr) || this.mSIMRecords == null) {
            log("refreshMobileDataAlwaysOnSettings(): dataAlwaysOnAreaStr is empty");
        } else if (System.getInt(this.mContext.getContentResolver(), "whether_data_alwayson_init", 0) == 1) {
            log("refreshMobileDataAlwaysOnSettings(): whether_data_alwayson_init is 1");
        } else {
            String[] areaArray = dataAlwaysOnAreaStr.split(",");
            String operator = this.mSIMRecords.getOperatorNumeric();
            for (String area : areaArray) {
                if (area.equals(operator)) {
                    System.putInt(this.mContext.getContentResolver(), "power_saving_on", 0);
                    System.putInt(this.mContext.getContentResolver(), "whether_data_alwayson_init", 1);
                    break;
                }
            }
        }
    }

    private void log(String message) {
        Rlog.d(LOG_TAG, message);
    }

    public void custLoadCardSpecialFile(int fileid) {
        if (fileid != EF_OCSGL) {
            Rlog.d(LOG_TAG, "no fileid found for load");
        } else if (mIsSupportCsgSearch) {
            log("=csg= fetchSimRecords => CSG ... ");
            if (this.mSIMRecords != null) {
                this.mSIMRecords.mFh.loadEFLinearFixedAll(EF_OCSGL, this.custHandlerEx.obtainMessage(1));
            } else {
                log("IccRecords is null !!! ");
            }
        }
    }

    public byte[] getOcsgl() {
        if (this.mEfOcsgl == null || this.mSIMRecords == null) {
            if (this.mSIMRecords != null) {
                this.mSIMRecords.setCsglexist(false);
            }
            return new byte[0];
        }
        this.mSIMRecords.setCsglexist(HWDBG);
        if (this.mEfOcsgl.length > 0) {
            return Arrays.copyOf(this.mEfOcsgl, this.mEfOcsgl.length);
        }
        return new byte[0];
    }

    public void refreshCardType() {
        if (mIsSaveCardTypeLGU) {
            StringBuilder stringBuilder;
            int card_type = -1;
            if (this.mSIMRecords.mImsi != null) {
                if (this.mSIMRecords.mImsi.substring(0, 6).equals("450069")) {
                    SIMRecords sIMRecords = this.mSIMRecords;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("LGU SIMRecords: Refresh_card_type card_type = ");
                    stringBuilder.append(3);
                    log(stringBuilder.toString());
                    SystemProperties.set("gsm.sim.card.type", String.valueOf(3));
                    return;
                }
                String countryCode = this.mSIMRecords.mImsi.substring(0, 5);
                SIMRecords sIMRecords2;
                if (countryCode.substring(0, 5).equals("45006")) {
                    sIMRecords2 = this.mSIMRecords;
                    card_type = 0;
                } else if (countryCode.substring(0, 3).equals("450")) {
                    sIMRecords2 = this.mSIMRecords;
                    card_type = 1;
                } else {
                    sIMRecords2 = this.mSIMRecords;
                    card_type = 2;
                }
            }
            SystemProperties.set("gsm.sim.card.type", String.valueOf(card_type));
            stringBuilder = new StringBuilder();
            stringBuilder.append("LGU SIMRecords: card_type = ");
            stringBuilder.append(card_type);
            log(stringBuilder.toString());
            log("LGU SIMRecords: Refresh_card_type exit ");
        }
    }

    public boolean isHwCustDataRoamingOpenArea() {
        StringBuilder stringBuilder;
        if (TelephonyManager.getDefault() != null && !TelephonyManager.getDefault().isMultiSimEnabled()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("single sim, isCustRoamingOpenArea:");
            stringBuilder.append(this.isCustRoamingOpenArea);
            log(stringBuilder.toString());
            return this.isCustRoamingOpenArea;
        } else if (TelephonyManager.getDefault() == null || !TelephonyManager.getDefault().isMultiSimEnabled()) {
            return false;
        } else {
            if (this.mSIMRecords != null && this.mSIMRecords.getSlotId() == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("multi sim, isCustRoamingOpenArea:");
                stringBuilder.append(this.isCustRoamingOpenArea);
                log(stringBuilder.toString());
                return this.isCustRoamingOpenArea;
            } else if (this.mSIMRecords == null || 1 != this.mSIMRecords.getSlotId()) {
                log("isHwCustDataRoamingOpenArea: invalid slotId");
                return false;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("multi sim,isCustRoamingOpenArea_SIM2:");
                stringBuilder.append(this.isCustRoamingOpenArea_SIM2);
                log(stringBuilder.toString());
                return this.isCustRoamingOpenArea_SIM2;
            }
        }
    }
}
