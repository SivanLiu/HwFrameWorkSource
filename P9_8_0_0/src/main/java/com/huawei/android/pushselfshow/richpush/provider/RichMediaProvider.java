package com.huawei.android.pushselfshow.richpush.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.a.b;

public class RichMediaProvider extends ContentProvider {
    private static final UriMatcher b = new UriMatcher(-1);
    b a = null;

    public static class a {
        public static final Uri a = Uri.parse("content://com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider/support_porvider");
        public static final Uri b = Uri.parse("content://com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider/insert_bmp");
        public static final Uri c = Uri.parse("content://com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider/update_bmp");
        public static final Uri d = Uri.parse("content://com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider/query_bmp");
        public static final Uri e = Uri.parse("content://com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider/insert_msg");
        public static final Uri f = Uri.parse("content://com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider/query_msg");
        public static final Uri g = Uri.parse("content://com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider/delete_msg");
    }

    static {
        b.addURI("com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider", "support_porvider", 1);
        b.addURI("com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider", "insert_bmp", 2);
        b.addURI("com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider", "update_bmp", 3);
        b.addURI("com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider", "query_bmp", 4);
        b.addURI("com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider", "insert_msg", 5);
        b.addURI("com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider", "query_msg", 6);
        b.addURI("com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider", "delete_msg", 7);
    }

    private android.net.Uri a(android.database.sqlite.SQLiteDatabase r21, java.lang.String r22, android.content.ContentValues r23, android.net.Uri r24) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x00ba in list []
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
        r20 = this;
        r13 = 0;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "enter insertToDb, table is:";
        r4 = r4.append(r5);
        r0 = r22;
        r4 = r4.append(r0);
        r4 = r4.toString();
        r5 = "PushSelfShowLog_RichMediaProvider";
        com.huawei.android.pushagent.a.a.c.a(r5, r4);
        if (r21 == 0) goto L_0x0058;
    L_0x001f:
        r14 = 0;
        r6 = 0;
        r7 = 0;
        r8 = 0;
        r9 = 0;
        r10 = 0;
        r11 = 0;
        r4 = r21;
        r5 = r22;
        r14 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        if (r14 == 0) goto L_0x0063;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
    L_0x0030:
        r15 = r14.getCount();	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        if (r15 < r4) goto L_0x0078;
    L_0x0038:
        if (r14 != 0) goto L_0x00b6;
    L_0x003a:
        r21.close();
    L_0x003d:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "resultUri is:";
        r4 = r4.append(r5);
        r4 = r4.append(r13);
        r4 = r4.toString();
        r5 = "PushSelfShowLog_RichMediaProvider";
        com.huawei.android.pushagent.a.a.c.a(r5, r4);
        return r13;
    L_0x0058:
        r4 = "PushSelfShowLog_RichMediaProvider";
        r5 = "db is null";
        com.huawei.android.pushagent.a.a.c.d(r4, r5);
        r4 = 0;
        return r4;
    L_0x0063:
        r4 = "PushSelfShowLog_RichMediaProvider";	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r5 = "cursor is null";	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        com.huawei.android.pushagent.a.a.c.d(r4, r5);	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r16 = 0;
        if (r14 != 0) goto L_0x0074;
    L_0x0070:
        r21.close();
        return r16;
    L_0x0074:
        r14.close();
        goto L_0x0070;
    L_0x0078:
        r4 = 0;
        r0 = r21;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r1 = r22;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r2 = r23;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r18 = r0.insert(r1, r4, r2);	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r4 = 0;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r4 = (r18 > r4 ? 1 : (r18 == r4 ? 0 : -1));	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        if (r4 > 0) goto L_0x00b4;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
    L_0x0089:
        r4 = 1;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
    L_0x008a:
        if (r4 != 0) goto L_0x0038;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
    L_0x008c:
        r0 = r24;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r1 = r18;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r13 = android.content.ContentUris.withAppendedId(r0, r1);	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r4 = r20.getContext();	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r4 = r4.getContentResolver();	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r5 = 0;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r4.notifyChange(r13, r5);	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        goto L_0x0038;
    L_0x00a1:
        r16 = move-exception;
        r4 = r16.toString();	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r5 = "PushSelfShowLog_RichMediaProvider";	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        r0 = r16;	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        com.huawei.android.pushagent.a.a.c.d(r5, r4, r0);	 Catch:{ Exception -> 0x00a1, all -> 0x00be }
        if (r14 != 0) goto L_0x00ba;
    L_0x00b0:
        r21.close();
        goto L_0x003d;
    L_0x00b4:
        r4 = 0;
        goto L_0x008a;
    L_0x00b6:
        r14.close();
        goto L_0x003a;
    L_0x00ba:
        r14.close();
        goto L_0x00b0;
    L_0x00be:
        r12 = move-exception;
        if (r14 != 0) goto L_0x00c5;
    L_0x00c1:
        r21.close();
        throw r12;
    L_0x00c5:
        r14.close();
        goto L_0x00c1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider.a(android.database.sqlite.SQLiteDatabase, java.lang.String, android.content.ContentValues, android.net.Uri):android.net.Uri");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean a(String str) {
        if (str == null || str.length() == 0 || !str.contains("'")) {
            return false;
        }
        c.d("PushSelfShowLog_RichMediaProvider", str + " can be reject, should check sql");
        return true;
    }

    private boolean a(String[] -l_2_R) {
        if (-l_2_R == null || -l_2_R.length == 0) {
            return false;
        }
        for (String -l_5_R : -l_2_R) {
            if (a(-l_5_R)) {
                return true;
            }
        }
        return false;
    }

    public int delete(Uri uri, String str, String[] strArr) {
        int -l_4_I = b.match(uri);
        c.a("PushSelfShowLog_RichMediaProvider", "uri is:" + uri + ",match result: " + -l_4_I);
        if (this.a != null) {
            switch (-l_4_I) {
                case 7:
                    Object -l_5_R = this.a.getWritableDatabase();
                    if (-l_5_R != null) {
                        int i = 0;
                        try {
                            i = -l_5_R.delete("pushmsg", "_id = ?", strArr);
                            getContext().getContentResolver().notifyChange(uri, null);
                        } catch (Object -l_7_R) {
                            c.d("PushSelfShowLog_RichMediaProvider", -l_7_R.toString(), -l_7_R);
                        } finally {
                            -l_5_R.close();
                        }
                        return i;
                    }
                    c.d("PushSelfShowLog_RichMediaProvider", "db is null");
                    return 0;
                default:
                    c.d("PushSelfShowLog_RichMediaProvider", "uri not match!");
                    return 0;
            }
        }
        c.d("PushSelfShowLog_RichMediaProvider", "dbHelper is null");
        return 0;
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        int -l_3_I = b.match(uri);
        c.a("PushSelfShowLog_RichMediaProvider", "uri is:" + uri + ",match result: " + -l_3_I);
        if (this.a != null) {
            switch (-l_3_I) {
                case 2:
                    return a(this.a.getWritableDatabase(), "notify", contentValues, uri);
                case 5:
                    return a(this.a.getWritableDatabase(), "pushmsg", contentValues, uri);
                default:
                    c.d("PushSelfShowLog_RichMediaProvider", "uri not match!");
                    return null;
            }
        }
        c.d("PushSelfShowLog_RichMediaProvider", "dbHelper is null");
        return null;
    }

    public boolean onCreate() {
        c.a("PushSelfShowLog_RichMediaProvider", "onCreate");
        this.a = b.a(getContext());
        return true;
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        if (a(str) || a(strArr)) {
            c.d("PushSelfShowLog_RichMediaProvider", "in query selection:" + str + " or projection is invalied");
            return null;
        }
        int -l_6_I = b.match(uri);
        c.a("PushSelfShowLog_RichMediaProvider", "uri is:" + uri + ",match result: " + -l_6_I);
        if (this.a != null) {
            Object -l_7_R = this.a.getReadableDatabase();
            if (-l_7_R != null) {
                switch (-l_6_I) {
                    case 1:
                        Object -l_8_R = new MatrixCursor(new String[]{"isSupport"});
                        -l_8_R.addRow(new Integer[]{Integer.valueOf(1)});
                        return -l_8_R;
                    case 4:
                        try {
                            return -l_7_R.query("notify", new String[]{"bmp"}, "url = ?", strArr2, null, null, str2, null);
                        } catch (Object -l_9_R) {
                            c.d("PushSelfShowLog_RichMediaProvider", -l_9_R.toString(), -l_9_R);
                            break;
                        }
                    case 6:
                        try {
                            return -l_7_R.rawQuery("SELECT pushmsg._id,pushmsg.msg,pushmsg.token,pushmsg.url,notify.bmp  FROM pushmsg LEFT OUTER JOIN notify ON pushmsg.url = notify.url and pushmsg.url = ? order by pushmsg._id desc limit 1000;", strArr2);
                        } catch (Object -l_9_R2) {
                            c.d("PushSelfShowLog_RichMediaProvider", -l_9_R2.toString(), -l_9_R2);
                            break;
                        }
                    default:
                        c.d("PushSelfShowLog_RichMediaProvider", "uri not match!");
                        break;
                }
                return null;
            }
            c.d("PushSelfShowLog_RichMediaProvider", "db is null");
            return null;
        }
        c.d("PushSelfShowLog_RichMediaProvider", "dbHelper is null");
        return null;
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int -l_5_I = b.match(uri);
        c.a("PushSelfShowLog_RichMediaProvider", "uri is:" + uri + ",match result: " + -l_5_I);
        if (this.a != null) {
            switch (-l_5_I) {
                case 3:
                    Object -l_6_R = this.a.getWritableDatabase();
                    if (-l_6_R != null) {
                        int i = 0;
                        try {
                            i = -l_6_R.update("notify", contentValues, "url = ?", strArr);
                            getContext().getContentResolver().notifyChange(uri, null);
                        } catch (Object -l_8_R) {
                            c.d("PushSelfShowLog_RichMediaProvider", -l_8_R.toString(), -l_8_R);
                        } finally {
                            -l_6_R.close();
                        }
                        return i;
                    }
                    c.d("PushSelfShowLog_RichMediaProvider", "db is null");
                    return 0;
                default:
                    c.d("PushSelfShowLog_RichMediaProvider", "uri not match!");
                    return 0;
            }
        }
        c.d("PushSelfShowLog_RichMediaProvider", "dbHelper is null");
        return 0;
    }
}
