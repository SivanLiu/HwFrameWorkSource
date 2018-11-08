package com.android.contacts.hap.numbermark.hww3.api;

import android.content.Context;
import com.android.contacts.util.HwLog;

public final class W3ApiManager {
    private static final String CONNECTION_TIMEOUT = "connect overtime";
    private static final String LOG_W3 = "w3";
    private static final String LOG_WELINK = "welink";
    private static final String NUMBER_MARK_INFO_NO_ATTRIBUTE = "";
    private static final int RETURN_CODE_BUSINESS_EXCEPTION = 103;
    private static final int RETURN_CODE_MUTI_RESULT = 101;
    private static final int RETURN_CODE_PARAM_ERROR = 105;
    private static final int RETURN_CODE_SUCCESS = 100;
    private static final int RETURN_CODE_TIME_OUT = 102;
    private static final int RETURN_CODE_W3_LOG_OUT = 104;
    private static final String TAG = "W3ApiManager";
    private static final boolean W3_DEFAULT_CLOUD_MARK = true;
    private static final String W3_DEFAULT_MARK_CLASSIFY = "w3";
    private static final int W3_DEFAULT_MARK_COUNT = -1;
    private static final String W3_QUERY_URI = "content://huawei.w3.contact/query/";
    private static final String W3_TIMEOUT_LIMIT = "&2";
    private static final String WELINK_QUERY_URI = "content://com.huawei.works.contact/query/";
    private static volatile W3ApiManager mInfoManager;
    private Context mContext;

    public com.android.contacts.hap.service.NumberMarkInfo getMarkInfoFromW3Server(java.lang.String r22) {
        /* JADX: method processing error */
/*
Error: java.util.NoSuchElementException
	at java.util.HashMap$HashIterator.nextNode(HashMap.java:1439)
	at java.util.HashMap$KeyIterator.next(HashMap.java:1461)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.applyRemove(BlockFinallyExtract.java:537)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.extractFinally(BlockFinallyExtract.java:176)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.processExceptionHandler(BlockFinallyExtract.java:81)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:52)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r21 = this;
        r1 = android.text.TextUtils.isEmpty(r22);
        if (r1 == 0) goto L_0x0008;
    L_0x0006:
        r3 = 0;
    L_0x0007:
        return r3;
    L_0x0008:
        r15 = com.android.contacts.hap.welink.WeLinkManager.isSuppotWeLink();
        if (r15 == 0) goto L_0x005c;
    L_0x000e:
        r16 = "welink";
    L_0x0010:
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r0 = r22;
        r1 = r1.append(r0);
        r3 = "&2";
        r1 = r1.append(r3);
        r18 = r1.toString();
        if (r15 == 0) goto L_0x005f;
    L_0x0027:
        r19 = "content://com.huawei.works.contact/query/";
    L_0x0029:
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r0 = r19;
        r1 = r1.append(r0);
        r0 = r18;
        r1 = r1.append(r0);
        r1 = r1.toString();
        r2 = android.net.Uri.parse(r1);
        r13 = 0;
        r0 = r21;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r0.mContext;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.getContentResolver();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3 = 0;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r4 = 0;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r5 = 0;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r6 = 0;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r13 = r1.query(r2, r3, r4, r5, r6);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        if (r13 != 0) goto L_0x0062;
    L_0x0055:
        r3 = 0;
        if (r13 == 0) goto L_0x0007;
    L_0x0058:
        r13.close();
        goto L_0x0007;
    L_0x005c:
        r16 = "w3";
        goto L_0x0010;
    L_0x005f:
        r19 = "content://huawei.w3.contact/query/";
        goto L_0x0029;
    L_0x0062:
        r1 = r13.moveToFirst();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        if (r1 == 0) goto L_0x0188;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
    L_0x0068:
        r1 = "code";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r13.getColumnIndex(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r20 = r13.getInt(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        switch(r20) {
            case 100: goto L_0x007c;
            case 101: goto L_0x0105;
            case 102: goto L_0x00dd;
            case 103: goto L_0x012d;
            case 104: goto L_0x0150;
            case 105: goto L_0x016c;
            default: goto L_0x0075;
        };
    L_0x0075:
        r3 = 0;
        if (r13 == 0) goto L_0x0007;
    L_0x0078:
        r13.close();
        goto L_0x0007;
    L_0x007c:
        r1 = "name";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r13.getColumnIndex(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r17 = r13.getString(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = "account";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r13.getColumnIndex(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r12 = r13.getString(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = "department";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r13.getColumnIndex(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r11 = r13.getString(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = android.text.TextUtils.isEmpty(r17);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        if (r1 != 0) goto L_0x00a6;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
    L_0x00a0:
        r1 = android.text.TextUtils.isEmpty(r12);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        if (r1 == 0) goto L_0x00ae;
    L_0x00a6:
        r3 = 0;
        if (r13 == 0) goto L_0x0007;
    L_0x00a9:
        r13.close();
        goto L_0x0007;
    L_0x00ae:
        r3 = new com.android.contacts.hap.service.NumberMarkInfo;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r5 = "";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1.<init>();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r17;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r0);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r4 = " ";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r4);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r12);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r6 = r1.toString();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r7 = "w3";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r8 = 1;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r9 = -1;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r10 = "w3";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r4 = r22;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3.<init>(r4, r5, r6, r7, r8, r9, r10, r11);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        if (r13 == 0) goto L_0x0007;
    L_0x00d8:
        r13.close();
        goto L_0x0007;
    L_0x00dd:
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1.<init>();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r16;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r0);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3 = " time out error";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r3);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r21;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0.w3log(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3 = new com.android.contacts.hap.service.NumberMarkInfo;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = "connect overtime";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3.<init>(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        if (r13 == 0) goto L_0x0007;
    L_0x0100:
        r13.close();
        goto L_0x0007;
    L_0x0105:
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1.<init>();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r16;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r0);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3 = " muti result error";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r3);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r21;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0.w3log(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        goto L_0x0075;
    L_0x0121:
        r14 = move-exception;
        r14.printStackTrace();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        if (r13 == 0) goto L_0x012a;
    L_0x0127:
        r13.close();
    L_0x012a:
        r3 = 0;
        goto L_0x0007;
    L_0x012d:
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1.<init>();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r16;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r0);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3 = " business error";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r3);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r21;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0.w3log(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        goto L_0x0075;
    L_0x0149:
        r1 = move-exception;
        if (r13 == 0) goto L_0x014f;
    L_0x014c:
        r13.close();
    L_0x014f:
        throw r1;
    L_0x0150:
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1.<init>();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r16;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r0);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3 = " log out error";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r3);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r21;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0.w3log(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        goto L_0x0075;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
    L_0x016c:
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1.<init>();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r16;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r0);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r3 = " param error";	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.append(r3);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0 = r21;	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        r0.w3log(r1);	 Catch:{ Exception -> 0x0121, all -> 0x0149 }
        goto L_0x0075;
    L_0x0188:
        if (r13 == 0) goto L_0x012a;
    L_0x018a:
        r13.close();
        goto L_0x012a;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.contacts.hap.numbermark.hww3.api.W3ApiManager.getMarkInfoFromW3Server(java.lang.String):com.android.contacts.hap.service.NumberMarkInfo");
    }

    private W3ApiManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static W3ApiManager getInstance(Context context) {
        if (mInfoManager == null) {
            mInfoManager = new W3ApiManager(context);
        }
        return mInfoManager;
    }

    private void w3log(String msg) {
        HwLog.i(TAG, msg);
    }
}
