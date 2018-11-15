package com.android.server.wifi.HwQoE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.wifi.HwQoE.HiDataTracfficInfo.HiDataApInfo;
import com.android.server.wifi.HwWifiStateMachine;
import java.util.ArrayList;
import java.util.List;

public class HwQoEQualityManager {
    private static HwQoEQualityManager mHwQoEQualityManager = null;
    private SQLiteDatabase mDatabase;
    private HwQoEQualityDataBase mHelper;
    private Object mSQLLock = new Object();

    private boolean checkAPHistoryRecordExist(java.lang.String r11, int r12, int r13) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x004a in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r10 = this;
        r9 = 0;
        r3 = 0;
        r0 = 0;
        r4 = r10.mDatabase;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r5 = "SELECT * FROM HwQoEWeChatAPRecordTable where (SSID like ?) and (authType = ?) and (appType = ?)";	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r6 = 3;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r6 = new java.lang.String[r6];	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r7 = 0;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r6[r7] = r11;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r7 = java.lang.String.valueOf(r12);	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r8 = 1;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r6[r8] = r7;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r7 = java.lang.String.valueOf(r13);	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r8 = 2;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r6[r8] = r7;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r0 = r4.rawQuery(r5, r6);	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r2 = r0.getCount();	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        if (r2 <= 0) goto L_0x0027;
    L_0x0026:
        r3 = 1;
    L_0x0027:
        if (r0 == 0) goto L_0x002c;
    L_0x0029:
        r0.close();
    L_0x002c:
        return r3;
    L_0x002d:
        r1 = move-exception;
        r4 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r4.<init>();	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r5 = "checkAPHistoryRecordExist error:";	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r4 = r4.append(r1);	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        r4 = r4.toString();	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        com.android.server.wifi.HwQoE.HwQoEUtils.logE(r4);	 Catch:{ SQLException -> 0x002d, all -> 0x004b }
        if (r0 == 0) goto L_0x004a;
    L_0x0047:
        r0.close();
    L_0x004a:
        return r9;
    L_0x004b:
        r4 = move-exception;
        if (r0 == 0) goto L_0x0051;
    L_0x004e:
        r0.close();
    L_0x0051:
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HwQoE.HwQoEQualityManager.checkAPHistoryRecordExist(java.lang.String, int, int):boolean");
    }

    private boolean checkHistoryRecordExist(com.android.server.wifi.HwQoE.HwQoEQualityInfo r11) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0050 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r10 = this;
        r9 = 0;
        r3 = 0;
        r0 = 0;
        r4 = r10.mDatabase;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r5 = "SELECT * FROM HwQoEQualityRecordTable where (BSSID like ?) and (RSSI = ?) and (APPType = ?)";	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r6 = 3;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r6 = new java.lang.String[r6];	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r7 = r11.mBSSID;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r8 = 0;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r6[r8] = r7;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r7 = r11.mRSSI;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r7 = java.lang.String.valueOf(r7);	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r8 = 1;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r6[r8] = r7;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r7 = r11.mAPPType;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r7 = java.lang.String.valueOf(r7);	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r8 = 2;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r6[r8] = r7;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r0 = r4.rawQuery(r5, r6);	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r2 = r0.getCount();	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        if (r2 <= 0) goto L_0x002d;
    L_0x002c:
        r3 = 1;
    L_0x002d:
        if (r0 == 0) goto L_0x0032;
    L_0x002f:
        r0.close();
    L_0x0032:
        return r3;
    L_0x0033:
        r1 = move-exception;
        r4 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r4.<init>();	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r5 = "checkHistoryRecordExist error:";	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r4 = r4.append(r1);	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        r4 = r4.toString();	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        com.android.server.wifi.HwQoE.HwQoEUtils.logE(r4);	 Catch:{ SQLException -> 0x0033, all -> 0x0051 }
        if (r0 == 0) goto L_0x0050;
    L_0x004d:
        r0.close();
    L_0x0050:
        return r9;
    L_0x0051:
        r4 = move-exception;
        if (r0 == 0) goto L_0x0057;
    L_0x0054:
        r0.close();
    L_0x0057:
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HwQoE.HwQoEQualityManager.checkHistoryRecordExist(com.android.server.wifi.HwQoE.HwQoEQualityInfo):boolean");
    }

    public com.android.server.wifi.HwQoE.HiDataTracfficInfo.HiDataApInfo queryAPUseType(java.lang.String r9, int r10, int r11) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0082 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r8 = this;
        r2 = new com.android.server.wifi.HwQoE.HiDataTracfficInfo$HiDataApInfo;
        r2.<init>();
        r3 = r8.mDatabase;
        if (r3 == 0) goto L_0x0013;
    L_0x0009:
        r3 = r8.mDatabase;
        r3 = r3.isOpen();
        r3 = r3 ^ 1;
        if (r3 == 0) goto L_0x001a;
    L_0x0013:
        r3 = "queryAPUseType database error.";
        com.android.server.wifi.HwQoE.HwQoEUtils.logE(r3);
        return r2;
    L_0x001a:
        r0 = 0;
        r3 = r8.mDatabase;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r4 = "SELECT * FROM HwQoEWeChatAPRecordTable where (SSID like ?) and (authType = ?) and (appType = ?)";	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r5 = 3;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r5 = new java.lang.String[r5];	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r6 = 0;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r5[r6] = r9;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r6 = java.lang.String.valueOf(r10);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r7 = 1;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r5[r7] = r6;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r6 = java.lang.String.valueOf(r11);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r7 = 2;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r5[r7] = r6;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r0 = r3.rawQuery(r4, r5);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r0.getCount();	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        if (r3 <= 0) goto L_0x0083;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
    L_0x003e:
        r3 = r0.moveToNext();	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        if (r3 == 0) goto L_0x0083;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
    L_0x0044:
        r2.mSsid = r9;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r2.mAuthType = r10;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r2.mAppType = r11;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = "apType";	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r0.getColumnIndex(r3);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r0.getInt(r3);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r2.mApType = r3;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = "blackCount";	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r0.getColumnIndex(r3);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r0.getInt(r3);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r2.mBlackCount = r3;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        goto L_0x003e;
    L_0x0065:
        r1 = move-exception;
        r3 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3.<init>();	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r4 = "queryAPUseType error:";	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r3.append(r4);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r3.append(r1);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        r3 = r3.toString();	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        com.android.server.wifi.HwQoE.HwQoEUtils.logE(r3);	 Catch:{ SQLException -> 0x0065, all -> 0x0089 }
        if (r0 == 0) goto L_0x0082;
    L_0x007f:
        r0.close();
    L_0x0082:
        return r2;
    L_0x0083:
        if (r0 == 0) goto L_0x0088;
    L_0x0085:
        r0.close();
    L_0x0088:
        return r2;
    L_0x0089:
        r3 = move-exception;
        if (r0 == 0) goto L_0x008f;
    L_0x008c:
        r0.close();
    L_0x008f:
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HwQoE.HwQoEQualityManager.queryAPUseType(java.lang.String, int, int):com.android.server.wifi.HwQoE.HiDataTracfficInfo$HiDataApInfo");
    }

    public java.util.List<com.android.server.wifi.HwQoE.HiDataTracfficInfo> queryTracfficData(java.lang.String r12, int r13, long r14, long r16) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x00ac in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r11 = this;
        r2 = 0;
        r3 = new java.util.ArrayList;
        r3.<init>();
        r5 = r11.mDatabase;
        if (r5 == 0) goto L_0x0014;
    L_0x000a:
        r5 = r11.mDatabase;
        r5 = r5.isOpen();
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x001b;
    L_0x0014:
        r5 = "queryTracfficData database error.";
        com.android.server.wifi.HwQoE.HwQoEUtils.logE(r5);
        return r3;
    L_0x001b:
        r0 = 0;
        r5 = r11.mDatabase;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r6 = "SELECT * FROM HwQoEWeChatRecordTable where (IMSI = ?) and (APPType = ?)";	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r7 = 2;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r7 = new java.lang.String[r7];	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r8 = 0;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r7[r8] = r12;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r8 = java.lang.String.valueOf(r13);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r9 = 1;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r7[r9] = r8;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r0 = r5.rawQuery(r6, r7);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = r0.getCount();	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 <= 0) goto L_0x00cb;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x0038:
        r5 = r0.moveToNext();	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 == 0) goto L_0x00d1;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x003e:
        r2 = 0;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r4 = new com.android.server.wifi.HwQoE.HiDataTracfficInfo;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r4.<init>();	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r4.mIMSI = r12;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r4.mAPPType = r13;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = "Thoughtput";	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = r0.getColumnIndex(r5);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r6 = r0.getLong(r5);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r4.mThoughtput = r6;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = "Duration";	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = r0.getColumnIndex(r5);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r6 = r0.getLong(r5);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r4.mDuration = r6;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = "Timestamp";	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = r0.getColumnIndex(r5);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r6 = r0.getLong(r5);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r4.mTimestamp = r6;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r6 = 0;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = (r14 > r6 ? 1 : (r14 == r6 ? 0 : -1));	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 == 0) goto L_0x00ad;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x0075:
        r6 = 0;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = (r16 > r6 ? 1 : (r16 == r6 ? 0 : -1));	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 == 0) goto L_0x00ad;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x007b:
        r6 = r4.mTimestamp;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = (r6 > r14 ? 1 : (r6 == r14 ? 0 : -1));	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 < 0) goto L_0x0088;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x0081:
        r6 = r4.mTimestamp;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = (r6 > r16 ? 1 : (r6 == r16 ? 0 : -1));	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 > 0) goto L_0x0088;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x0087:
        r2 = 1;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x0088:
        if (r2 == 0) goto L_0x0038;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x008a:
        r3.add(r4);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        goto L_0x0038;
    L_0x008e:
        r1 = move-exception;
        r5 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5.<init>();	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r6 = "queryTracfficData error:";	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = r5.append(r6);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = r5.append(r1);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = r5.toString();	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        com.android.server.wifi.HwQoE.HwQoEUtils.logE(r5);	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = 0;
        if (r0 == 0) goto L_0x00ac;
    L_0x00a9:
        r0.close();
    L_0x00ac:
        return r5;
    L_0x00ad:
        r6 = 0;
        r5 = (r14 > r6 ? 1 : (r14 == r6 ? 0 : -1));
        if (r5 == 0) goto L_0x00bb;
    L_0x00b3:
        r6 = r4.mTimestamp;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = (r6 > r14 ? 1 : (r6 == r14 ? 0 : -1));	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 < 0) goto L_0x0088;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x00b9:
        r2 = 1;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        goto L_0x0088;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x00bb:
        r6 = 0;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = (r16 > r6 ? 1 : (r16 == r6 ? 0 : -1));	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        if (r5 == 0) goto L_0x00c9;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
    L_0x00c1:
        r6 = r4.mTimestamp;	 Catch:{ SQLException -> 0x008e, all -> 0x00d7 }
        r5 = (r6 > r16 ? 1 : (r6 == r16 ? 0 : -1));
        if (r5 > 0) goto L_0x0088;
    L_0x00c7:
        r2 = 1;
        goto L_0x0088;
    L_0x00c9:
        r2 = 1;
        goto L_0x0088;
    L_0x00cb:
        if (r0 == 0) goto L_0x00d0;
    L_0x00cd:
        r0.close();
    L_0x00d0:
        return r3;
    L_0x00d1:
        if (r0 == 0) goto L_0x00d6;
    L_0x00d3:
        r0.close();
    L_0x00d6:
        return r3;
    L_0x00d7:
        r5 = move-exception;
        if (r0 == 0) goto L_0x00dd;
    L_0x00da:
        r0.close();
    L_0x00dd:
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HwQoE.HwQoEQualityManager.queryTracfficData(java.lang.String, int, long, long):java.util.List<com.android.server.wifi.HwQoE.HiDataTracfficInfo>");
    }

    private HwQoEQualityManager(Context context) {
        this.mHelper = new HwQoEQualityDataBase(context);
        this.mDatabase = this.mHelper.getWritableDatabase();
    }

    public static HwQoEQualityManager getInstance(Context context) {
        if (mHwQoEQualityManager == null) {
            mHwQoEQualityManager = new HwQoEQualityManager(context);
        }
        return mHwQoEQualityManager;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeDB() {
        synchronized (this.mSQLLock) {
            if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
            } else {
                this.mDatabase.close();
            }
        }
    }

    private boolean updateAppQualityRcd(HwQoEQualityInfo dbr) {
        ContentValues values = new ContentValues();
        values.put(HwWifiStateMachine.BSSID_KEY, dbr.mBSSID);
        values.put("RSSI", Integer.valueOf(dbr.mRSSI));
        values.put("APPType", Integer.valueOf(dbr.mAPPType));
        values.put("Thoughtput", Long.valueOf(dbr.mThoughtput));
        try {
            if (this.mDatabase.update(HwQoEQualityDataBase.HW_QOE_QUALITY_NAME, values, "(BSSID like ?) and (RSSI = ?) and (APPType = ?)", new String[]{dbr.mBSSID, String.valueOf(dbr.mRSSI), String.valueOf(dbr.mAPPType)}) != 0) {
                return true;
            }
            HwQoEUtils.logE("updateAppQualityRcd update failed.");
            return false;
        } catch (SQLException e) {
            HwQoEUtils.logE("updateAppQualityRcd error:" + e);
            return false;
        }
    }

    private boolean insertAppQualityRcd(HwQoEQualityInfo dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO HwQoEQualityRecordTable VALUES(null,  ?, ?, ?, ?)", new Object[]{dbr.mBSSID, Integer.valueOf(dbr.mRSSI), Integer.valueOf(dbr.mAPPType), Long.valueOf(dbr.mThoughtput)});
            return true;
        } catch (SQLException e) {
            HwQoEUtils.logE("insertAppQualityRcd error:" + e);
            return false;
        }
    }

    public boolean addOrUpdateAppQualityRcd(HwQoEQualityInfo dbr) {
        synchronized (this.mSQLLock) {
            if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0 || dbr == null) {
                HwQoEUtils.logE("addOrUpdateAppQualityRcd error.");
                return false;
            } else if (dbr.mBSSID == null) {
                HwQoEUtils.logE("addOrUpdateAppQualityRcd null error.");
                return false;
            } else if (checkHistoryRecordExist(dbr)) {
                r0 = updateAppQualityRcd(dbr);
                return r0;
            } else {
                r0 = insertAppQualityRcd(dbr);
                return r0;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwQoEQualityInfo queryAppQualityRcd(String apBssid, int apRssi, int appType) {
        HwQoEUtils.logD("queryAppQualityRcd enter.");
        synchronized (this.mSQLLock) {
            if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
                HwQoEUtils.logE("queryAppQualityRcd database error.");
                return null;
            } else if (apBssid == null) {
                HwQoEUtils.logE("queryAppQualityRcd null error.");
                return null;
            } else {
                Cursor cursor = null;
                try {
                    cursor = this.mDatabase.rawQuery("SELECT * FROM HwQoEQualityRecordTable where (BSSID like ?) and (RSSI = ?) and (APPType = ?)", new String[]{apBssid, String.valueOf(apRssi), String.valueOf(appType)});
                    if (cursor.getCount() > 0) {
                        if (cursor.moveToNext()) {
                            HwQoEQualityInfo record = new HwQoEQualityInfo();
                            record.mBSSID = apBssid;
                            record.mAPPType = appType;
                            record.mRSSI = apRssi;
                            record.mThoughtput = cursor.getLong(cursor.getColumnIndex("Thoughtput"));
                            if (cursor != null) {
                                cursor.close();
                            }
                        } else if (cursor != null) {
                            cursor.close();
                        }
                    } else if (cursor != null) {
                        cursor.close();
                    }
                } catch (SQLException e) {
                    HwQoEUtils.logE("queryAppQualityRcd error:" + e);
                    if (cursor != null) {
                        cursor.close();
                    }
                    return null;
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<HwQoEQualityInfo> getAppQualityAllRcd(String apBssid, int appType) {
        List<HwQoEQualityInfo> mRecordList = new ArrayList();
        synchronized (this.mSQLLock) {
            if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
                HwQoEUtils.logE("getAppQualityAllRcd database error.");
                return mRecordList;
            } else if (apBssid == null) {
                HwQoEUtils.logE("getAppQualityAllRcd null error.");
                return mRecordList;
            } else {
                Cursor cursor = null;
                try {
                    cursor = this.mDatabase.rawQuery("SELECT * FROM HwQoEQualityRecordTable where (BSSID like ?) and (APPType = ?)", new String[]{apBssid, String.valueOf(appType)});
                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            HwQoEQualityInfo record = new HwQoEQualityInfo();
                            record.mBSSID = apBssid;
                            record.mAPPType = appType;
                            record.mRSSI = cursor.getInt(cursor.getColumnIndex("RSSI"));
                            record.mThoughtput = cursor.getLong(cursor.getColumnIndex("Thoughtput"));
                            mRecordList.add(record);
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    } else if (cursor != null) {
                        cursor.close();
                    }
                } catch (SQLException e) {
                    HwQoEUtils.logE("getAppQualityAllRcd error:" + e);
                    if (cursor != null) {
                        cursor.close();
                    }
                    return null;
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    public boolean deleteAppQualityRcd(String apBssid) {
        synchronized (this.mSQLLock) {
            if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
                HwQoEUtils.logE("deleteAppQualityRcd database error.");
                return false;
            } else if (apBssid == null) {
                HwQoEUtils.logE("deleteAppQualityRcd null error.");
                return false;
            } else {
                try {
                    this.mDatabase.delete(HwQoEQualityDataBase.HW_QOE_QUALITY_NAME, "BSSID like ?", new String[]{apBssid});
                    return true;
                } catch (SQLException e) {
                    HwQoEUtils.logE("deleteAppQualityRcd error:" + e);
                    return false;
                }
            }
        }
    }

    public boolean addAPPTracfficData(HiDataTracfficInfo data) {
        if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
            HwQoEUtils.logE("addAPPTracfficData database error.");
            return false;
        }
        try {
            this.mDatabase.execSQL("INSERT INTO HwQoEWeChatRecordTable VALUES(null,  ?, ?, ?, ?, ?)", new Object[]{data.mIMSI, String.valueOf(data.mAPPType), String.valueOf(data.mThoughtput), String.valueOf(data.mDuration), String.valueOf(data.mTimestamp)});
            return true;
        } catch (SQLException e) {
            HwQoEUtils.logE("addAPPTracfficData error:" + e);
            return false;
        }
    }

    public boolean deleteAPPTracfficData(HiDataTracfficInfo data) {
        if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
            HwQoEUtils.logE("addAPPTracfficData database error.");
            return false;
        }
        try {
            this.mDatabase.delete(HwQoEQualityDataBase.HW_QOE_WECHAT_NAME, "(IMSI = ?) and (APPType = ?) and (Timestamp = ?)", new String[]{String.valueOf(data.mAPPType), String.valueOf(data.mTimestamp)});
            return true;
        } catch (SQLException e) {
            HwQoEUtils.logE("deleteAPPTracfficData error:" + e);
            return false;
        }
    }

    private boolean updateAPRcd(String ssid, int authType, int apType, int appType, int blackCount) {
        ContentValues values = new ContentValues();
        values.put("SSID", ssid);
        values.put("authType", Integer.valueOf(authType));
        values.put("apType", Integer.valueOf(apType));
        values.put("appType", Integer.valueOf(appType));
        values.put("blackCount", Integer.valueOf(blackCount));
        try {
            if (this.mDatabase.update(HwQoEQualityDataBase.HW_QOE_WECHAT_AP_NAME, values, "(SSID like ?) and (authType = ?) and (appType = ?)", new String[]{ssid, String.valueOf(authType), String.valueOf(appType)}) != 0) {
                return true;
            }
            HwQoEUtils.logE("updateAPRcd update failed.");
            return false;
        } catch (SQLException e) {
            HwQoEUtils.logE("updateAPRcd error:" + e);
            return false;
        }
    }

    private boolean insertAPRcd(String ssid, int authType, int apType, int appType, int blackCount) {
        try {
            this.mDatabase.execSQL("INSERT INTO HwQoEWeChatAPRecordTable VALUES(null,  ?, ?, ?, ?, ?)", new Object[]{ssid, Integer.valueOf(authType), Integer.valueOf(apType), Integer.valueOf(appType), Integer.valueOf(blackCount)});
            return true;
        } catch (SQLException e) {
            HwQoEUtils.logE("insertAPRcd error:" + e);
            return false;
        }
    }

    public boolean addOrUpdateAPRcd(HiDataApInfo info) {
        if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
            HwQoEUtils.logE("addOrUpdateAPRcd error.");
            return false;
        } else if (info.mSsid == null) {
            HwQoEUtils.logE("addOrUpdateAPRcd null error.");
            return false;
        } else if (checkAPHistoryRecordExist(info.mSsid, info.mAuthType, info.mAppType)) {
            return updateAPRcd(info.mSsid, info.mAuthType, info.mApType, info.mAppType, info.mBlackCount);
        } else {
            return insertAPRcd(info.mSsid, info.mAuthType, info.mApType, info.mAppType, info.mBlackCount);
        }
    }

    public HiDataCHRStatisticsInfo getWeChatStatistics(int appType) {
        String queryId;
        SQLException e;
        Throwable th;
        HiDataCHRStatisticsInfo hiDataCHRStatisticsInfo = null;
        if (appType == 1) {
            queryId = "1";
        } else {
            queryId = "2";
        }
        if (this.mDatabase == null || (this.mDatabase.isOpen() ^ 1) != 0) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = this.mDatabase.rawQuery("SELECT * FROM HwQoEWeChatStatisticsTable where _id like ?", new String[]{queryId});
            if (cursor.moveToNext()) {
                HiDataCHRStatisticsInfo statistics = new HiDataCHRStatisticsInfo(appType);
                try {
                    statistics.mAPPType = cursor.getInt(cursor.getColumnIndex("APPType"));
                    statistics.mCallTotalCnt = cursor.getInt(cursor.getColumnIndex("CallTotalCnt"));
                    statistics.mStartInWiFiCnt = cursor.getInt(cursor.getColumnIndex("StartInWiFiCnt"));
                    statistics.mStartInCellularCnt = cursor.getInt(cursor.getColumnIndex("StartInCellularCnt"));
                    statistics.mCallInCellularDur = cursor.getInt(cursor.getColumnIndex("CallInCellularDur"));
                    statistics.mCallInWiFiDur = cursor.getInt(cursor.getColumnIndex("CallInWiFiDur"));
                    statistics.mCellLv1Cnt = cursor.getInt(cursor.getColumnIndex("CellLv1Cnt"));
                    statistics.mCellLv2Cnt = cursor.getInt(cursor.getColumnIndex("CellLv2Cnt"));
                    statistics.mCellLv2Cnt = cursor.getInt(cursor.getColumnIndex("CellLv3Cnt"));
                    statistics.mWiFiLv1Cnt = cursor.getInt(cursor.getColumnIndex("WiFiLv1Cnt"));
                    statistics.mWiFiLv2Cnt = cursor.getInt(cursor.getColumnIndex("WiFiLv2Cnt"));
                    statistics.mWiFiLv3Cnt = cursor.getInt(cursor.getColumnIndex("WiFiLv3Cnt"));
                    statistics.mTrfficCell = cursor.getInt(cursor.getColumnIndex("TrfficCell"));
                    statistics.mVipSwitchCnt = cursor.getInt(cursor.getColumnIndex("VipSwitchCnt"));
                    statistics.mStallSwitchCnt = cursor.getInt(cursor.getColumnIndex("StallSwitchCnt"));
                    statistics.mStallSwitch0Cnt = cursor.getInt(cursor.getColumnIndex("StallSwitch0Cnt"));
                    statistics.mStallSwitch1Cnt = cursor.getInt(cursor.getColumnIndex("StallSwitch1Cnt"));
                    statistics.mStallSwitchAbove1Cnt = cursor.getInt(cursor.getColumnIndex("StallSwitchAbove1Cnt"));
                    statistics.mSwitch2CellCnt = cursor.getInt(cursor.getColumnIndex("Switch2CellCnt"));
                    statistics.mSwitch2WifiCnt = cursor.getInt(cursor.getColumnIndex("Switch2WifiCnt"));
                    statistics.mLastUploadTime = cursor.getLong(cursor.getColumnIndex("LastUploadTime"));
                    hiDataCHRStatisticsInfo = statistics;
                } catch (SQLException e2) {
                    e = e2;
                    hiDataCHRStatisticsInfo = statistics;
                    try {
                        HwQoEUtils.logE("getWeChatStatistics: " + e);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            return hiDataCHRStatisticsInfo;
        } catch (SQLException e3) {
            e = e3;
            HwQoEUtils.logE("getWeChatStatistics: " + e);
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
    }

    public void initWeChatStatistics(HiDataCHRStatisticsInfo data) {
        if (this.mDatabase != null && (this.mDatabase.isOpen() ^ 1) == 0) {
            this.mDatabase.execSQL("INSERT INTO HwQoEWeChatStatisticsTable VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{Integer.valueOf(data.mAPPType), Integer.valueOf(data.mCallTotalCnt), Integer.valueOf(data.mStartInWiFiCnt), Integer.valueOf(data.mStartInCellularCnt), Integer.valueOf(data.mCallInCellularDur), Integer.valueOf(data.mCallInWiFiDur), Integer.valueOf(data.mCellLv1Cnt), Integer.valueOf(data.mCellLv2Cnt), Integer.valueOf(data.mCellLv3Cnt), Integer.valueOf(data.mWiFiLv1Cnt), Integer.valueOf(data.mWiFiLv2Cnt), Integer.valueOf(data.mWiFiLv3Cnt), Integer.valueOf(data.mTrfficCell), Integer.valueOf(data.mVipSwitchCnt), Integer.valueOf(data.mStallSwitchCnt), Integer.valueOf(data.mStallSwitch0Cnt), Integer.valueOf(data.mStallSwitch1Cnt), Integer.valueOf(data.mStallSwitchAbove1Cnt), Integer.valueOf(data.mSwitch2CellCnt), Integer.valueOf(data.mSwitch2WifiCnt), Long.valueOf(data.mLastUploadTime), Integer.valueOf(0)});
        }
    }

    public void updateWeChatStatistics(HiDataCHRStatisticsInfo data) {
        if (this.mDatabase != null && (this.mDatabase.isOpen() ^ 1) == 0) {
            HwQoEUtils.logD("updateWeChatStatistics ");
            ContentValues values = new ContentValues();
            values.put("APPType", Integer.valueOf(data.mAPPType));
            values.put("CallTotalCnt", Integer.valueOf(data.mCallTotalCnt));
            values.put("StartInWiFiCnt", Integer.valueOf(data.mStartInWiFiCnt));
            values.put("StartInCellularCnt", Integer.valueOf(data.mStartInCellularCnt));
            values.put("CallInCellularDur", Integer.valueOf(data.mCallInCellularDur));
            values.put("CallInWiFiDur", Integer.valueOf(data.mCallInWiFiDur));
            values.put("CellLv1Cnt", Integer.valueOf(data.mCellLv1Cnt));
            values.put("CellLv2Cnt", Integer.valueOf(data.mCellLv2Cnt));
            values.put("CellLv3Cnt", Integer.valueOf(data.mCellLv3Cnt));
            values.put("WiFiLv1Cnt", Integer.valueOf(data.mWiFiLv1Cnt));
            values.put("WiFiLv2Cnt", Integer.valueOf(data.mWiFiLv2Cnt));
            values.put("WiFiLv3Cnt", Integer.valueOf(data.mWiFiLv3Cnt));
            values.put("TrfficCell", Integer.valueOf(data.mTrfficCell));
            values.put("VipSwitchCnt", Integer.valueOf(data.mVipSwitchCnt));
            values.put("StallSwitchCnt", Integer.valueOf(data.mStallSwitchCnt));
            values.put("StallSwitch0Cnt", Integer.valueOf(data.mStallSwitch0Cnt));
            values.put("StallSwitch1Cnt", Integer.valueOf(data.mStallSwitch1Cnt));
            values.put("StallSwitchAbove1Cnt", Integer.valueOf(data.mStallSwitchAbove1Cnt));
            values.put("Switch2CellCnt", Integer.valueOf(data.mSwitch2CellCnt));
            values.put("Switch2WifiCnt", Integer.valueOf(data.mSwitch2WifiCnt));
            values.put("LastUploadTime", Long.valueOf(data.mLastUploadTime));
            values.put("Reserved", Integer.valueOf(0));
            if (data.mAPPType == 1) {
                this.mDatabase.update(HwQoEQualityDataBase.HW_QOE_WECHAT_STATISTICS, values, "_id like ?", new String[]{"1"});
            } else {
                this.mDatabase.update(HwQoEQualityDataBase.HW_QOE_WECHAT_STATISTICS, values, "_id like ?", new String[]{"2"});
            }
        }
    }
}
