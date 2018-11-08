package com.huawei.android.pushselfshow.utils.a;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.huawei.android.pushagent.a.a.c;

public class h implements c {
    private String a;

    public h() {
        this.a = null;
        this.a = null;
    }

    protected h(String str) {
        this.a = null;
        this.a = str;
    }

    private static void a(android.content.Context r12, android.database.sqlite.SQLiteDatabase r13, java.lang.String r14, android.content.ContentValues r15) throws java.lang.Exception {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0095 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        if (r12 == 0) goto L_0x004a;
    L_0x0002:
        if (r13 == 0) goto L_0x0054;
    L_0x0004:
        r0 = android.text.TextUtils.isEmpty(r14);
        if (r0 != 0) goto L_0x005e;
    L_0x000a:
        r8 = 0;
        r2 = 0;
        r3 = 0;
        r4 = 0;
        r5 = 0;
        r6 = 0;
        r7 = 0;
        r0 = r13;
        r1 = r14;
        r8 = r0.query(r1, r2, r3, r4, r5, r6, r7);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        if (r8 == 0) goto L_0x0068;	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
    L_0x0019:
        r9 = r8.getCount();	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r0 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r0.<init>();	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r1 = "queryAndInsert, exist rowNumber:";	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r0 = r0.append(r1);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r0 = r0.append(r9);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r0 = r0.toString();	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r1 = "PushSelfShowLog";	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        com.huawei.android.pushagent.a.a.c.a(r1, r0);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r0 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        if (r9 < r0) goto L_0x007b;	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
    L_0x003b:
        r0 = "PushSelfShowLog";	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r1 = "queryAndInsert failed";	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        com.huawei.android.pushagent.a.a.c.d(r0, r1);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
    L_0x0044:
        if (r8 != 0) goto L_0x0091;
    L_0x0046:
        r13.close();
    L_0x0049:
        return;
    L_0x004a:
        r0 = "PushSelfShowLog";
        r1 = "context is null";
        com.huawei.android.pushagent.a.a.c.d(r0, r1);
        return;
    L_0x0054:
        r0 = "PushSelfShowLog";
        r1 = "db is null";
        com.huawei.android.pushagent.a.a.c.d(r0, r1);
        return;
    L_0x005e:
        r0 = "PushSelfShowLog";
        r1 = "table is null";
        com.huawei.android.pushagent.a.a.c.d(r0, r1);
        return;
    L_0x0068:
        r0 = "PushSelfShowLog";	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r1 = "cursor is null";	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        com.huawei.android.pushagent.a.a.c.d(r0, r1);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        if (r8 != 0) goto L_0x0077;
    L_0x0073:
        r13.close();
        return;
    L_0x0077:
        r8.close();
        goto L_0x0073;
    L_0x007b:
        r0 = 0;
        r13.insert(r14, r0, r15);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        goto L_0x0044;
    L_0x0080:
        r10 = move-exception;
        r0 = r10.toString();	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        r1 = "PushSelfShowLog";	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        com.huawei.android.pushagent.a.a.c.d(r1, r0, r10);	 Catch:{ Exception -> 0x0080, all -> 0x0099 }
        if (r8 != 0) goto L_0x0095;
    L_0x008d:
        r13.close();
        goto L_0x0049;
    L_0x0091:
        r8.close();
        goto L_0x0046;
    L_0x0095:
        r8.close();
        goto L_0x008d;
    L_0x0099:
        r11 = move-exception;
        if (r8 != 0) goto L_0x00a0;
    L_0x009c:
        r13.close();
        throw r11;
    L_0x00a0:
        r8.close();
        goto L_0x009c;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.android.pushselfshow.utils.a.h.a(android.content.Context, android.database.sqlite.SQLiteDatabase, java.lang.String, android.content.ContentValues):void");
    }

    public Cursor a(Context context, Uri uri, String str, String[] strArr) throws Exception {
        Object -l_5_R = a(context).getReadableDatabase();
        if (-l_5_R != null) {
            try {
                return -l_5_R.rawQuery(str, strArr);
            } catch (Object -l_6_R) {
                c.d("PushSelfShowLog", -l_6_R.toString(), -l_6_R);
            }
        }
        return null;
    }

    b a(Context context) {
        return this.a != null ? b.a(context, this.a) : b.a(context);
    }

    public void a(Context context, Uri uri, String str, ContentValues contentValues) throws Exception {
        a(context, a(context).getWritableDatabase(), str, contentValues);
    }

    public void a(Context context, i iVar) throws Exception {
        if (context == null) {
            c.d("PushSelfShowLog", "context is null");
        } else if (iVar != null) {
            Object -l_3_R = iVar.b();
            Object -l_4_R = iVar.c();
            Object -l_5_R = iVar.d();
            if (-l_3_R == null || -l_3_R.length() == 0) {
                c.d("PushSelfShowLog", "table is null");
            } else if (-l_4_R == null || -l_4_R.length() == 0) {
                c.d("PushSelfShowLog", "whereClause is null");
            } else if (-l_5_R == null || -l_5_R.length == 0) {
                c.d("PushSelfShowLog", "whereArgs is null");
            } else {
                Object -l_6_R = a(context).getWritableDatabase();
                if (-l_6_R != null) {
                    try {
                        -l_6_R.delete(-l_3_R, -l_4_R, -l_5_R);
                    } catch (Object -l_7_R) {
                        c.d("PushSelfShowLog", -l_7_R.toString(), -l_7_R);
                    } finally {
                        -l_6_R.close();
                    }
                }
            }
        } else {
            c.d("PushSelfShowLog", "sqlParam is null");
        }
    }
}
