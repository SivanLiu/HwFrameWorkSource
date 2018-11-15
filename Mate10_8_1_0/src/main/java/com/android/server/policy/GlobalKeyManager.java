package com.android.server.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.SparseArray;
import android.view.KeyEvent;
import java.io.PrintWriter;

final class GlobalKeyManager {
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_KEY_CODE = "keyCode";
    private static final String ATTR_VERSION = "version";
    private static final int GLOBAL_KEY_FILE_VERSION = 1;
    private static final String TAG = "GlobalKeyManager";
    private static final String TAG_GLOBAL_KEYS = "global_keys";
    private static final String TAG_KEY = "key";
    private SparseArray<ComponentName> mKeyMapping = new SparseArray();

    private void loadGlobalKeys(android.content.Context r13) {
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
        r12 = this;
        r7 = 0;
        r9 = r13.getResources();	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = 18284551; // 0x1170007 float:2.773434E-38 double:9.0337685E-317;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r7 = r9.getXml(r10);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r9 = "global_keys";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        com.android.internal.util.XmlUtils.beginDocument(r7, r9);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r9 = "version";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = 0;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r11 = 0;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r8 = r7.getAttributeIntValue(r10, r9, r11);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r9 = 1;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        if (r9 != r8) goto L_0x0027;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
    L_0x001e:
        com.android.internal.util.XmlUtils.nextElement(r7);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r4 = r7.getName();	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        if (r4 != 0) goto L_0x002d;
    L_0x0027:
        if (r7 == 0) goto L_0x002c;
    L_0x0029:
        r7.close();
    L_0x002c:
        return;
    L_0x002d:
        r9 = "key";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r9 = r9.equals(r4);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        if (r9 == 0) goto L_0x001e;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
    L_0x0036:
        r9 = "keyCode";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = 0;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r6 = r7.getAttributeValue(r10, r9);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r9 = "component";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = 0;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r0 = r7.getAttributeValue(r10, r9);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r5 = android.view.KeyEvent.keyCodeFromString(r6);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        if (r5 == 0) goto L_0x001e;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
    L_0x004c:
        r9 = r12.mKeyMapping;	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = android.content.ComponentName.unflattenFromString(r0);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r9.put(r5, r10);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        goto L_0x001e;
    L_0x0056:
        r1 = move-exception;
        r9 = "GlobalKeyManager";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = "global keys file not found";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        android.util.Log.w(r9, r10, r1);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        if (r7 == 0) goto L_0x002c;
    L_0x0062:
        r7.close();
        goto L_0x002c;
    L_0x0066:
        r2 = move-exception;
        r9 = "GlobalKeyManager";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = "I/O exception reading global keys file";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        android.util.Log.w(r9, r10, r2);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        if (r7 == 0) goto L_0x002c;
    L_0x0072:
        r7.close();
        goto L_0x002c;
    L_0x0076:
        r3 = move-exception;
        r9 = "GlobalKeyManager";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        r10 = "XML parser exception reading global keys file";	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        android.util.Log.w(r9, r10, r3);	 Catch:{ NotFoundException -> 0x0056, XmlPullParserException -> 0x0076, IOException -> 0x0066, all -> 0x0086 }
        if (r7 == 0) goto L_0x002c;
    L_0x0082:
        r7.close();
        goto L_0x002c;
    L_0x0086:
        r9 = move-exception;
        if (r7 == 0) goto L_0x008c;
    L_0x0089:
        r7.close();
    L_0x008c:
        throw r9;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.GlobalKeyManager.loadGlobalKeys(android.content.Context):void");
    }

    public GlobalKeyManager(Context context) {
        loadGlobalKeys(context);
    }

    boolean handleGlobalKey(Context context, int keyCode, KeyEvent event) {
        if (this.mKeyMapping.size() > 0) {
            ComponentName component = (ComponentName) this.mKeyMapping.get(keyCode);
            if (component != null) {
                context.sendBroadcastAsUser(new Intent("android.intent.action.GLOBAL_BUTTON").setComponent(component).setFlags(268435456).putExtra("android.intent.extra.KEY_EVENT", event), UserHandle.CURRENT, null);
                return true;
            }
        }
        return false;
    }

    boolean shouldHandleGlobalKey(int keyCode, KeyEvent event) {
        return this.mKeyMapping.get(keyCode) != null;
    }

    public void dump(String prefix, PrintWriter pw) {
        int numKeys = this.mKeyMapping.size();
        if (numKeys == 0) {
            pw.print(prefix);
            pw.println("mKeyMapping.size=0");
            return;
        }
        pw.print(prefix);
        pw.println("mKeyMapping={");
        for (int i = 0; i < numKeys; i++) {
            pw.print("  ");
            pw.print(prefix);
            pw.print(KeyEvent.keyCodeToString(this.mKeyMapping.keyAt(i)));
            pw.print("=");
            pw.println(((ComponentName) this.mKeyMapping.valueAt(i)).flattenToString());
        }
        pw.print(prefix);
        pw.println("}");
    }
}
