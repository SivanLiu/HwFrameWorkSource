package com.android.internal.telephony;

import android.os.Build;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.LogException;
import android.util.SparseIntArray;
import com.android.internal.telephony.cdma.sms.UserData;

public class Sms7BitEncodingTranslator {
    private static final boolean DBG = Build.IS_DEBUGGABLE;
    private static final String TAG = "Sms7BitEncodingTranslator";
    private static final String XML_CHARACTOR_TAG = "Character";
    private static final String XML_FROM_TAG = "from";
    private static final String XML_START_TAG = "SmsEnforce7BitTranslationTable";
    private static final String XML_TO_TAG = "to";
    private static final String XML_TRANSLATION_TYPE_TAG = "TranslationType";
    private static boolean mIs7BitTranslationTableLoaded = false;
    private static SparseIntArray mTranslationTable = null;
    private static SparseIntArray mTranslationTableCDMA = null;
    private static SparseIntArray mTranslationTableCommon = null;
    private static SparseIntArray mTranslationTableGSM = null;

    private static void load7BitTranslationTableFromXml() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0093 in list []
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
        r10 = -1;
        r2 = 0;
        r3 = android.content.res.Resources.getSystem();
        r7 = DBG;
        if (r7 == 0) goto L_0x0013;
    L_0x000a:
        r7 = "Sms7BitEncodingTranslator";
        r8 = "load7BitTranslationTableFromXml: open normal file";
        android.telephony.Rlog.d(r7, r8);
    L_0x0013:
        r7 = 18284562; // 0x1170012 float:2.773437E-38 double:9.033774E-317;
        r2 = r3.getXml(r7);
        r7 = "SmsEnforce7BitTranslationTable";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        com.android.internal.util.XmlUtils.beginDocument(r2, r7);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x0020:
        com.android.internal.util.XmlUtils.nextElement(r2);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r4 = r2.getName();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = DBG;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x0045;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x002b:
        r7 = "Sms7BitEncodingTranslator";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8.<init>();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = "tag: ";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r4);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        android.telephony.Rlog.d(r7, r8);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x0045:
        r7 = "TranslationType";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = r7.equals(r4);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x00d7;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x004e:
        r7 = "Type";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = 0;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r6 = r2.getAttributeValue(r8, r7);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = DBG;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x0074;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x005a:
        r7 = "Sms7BitEncodingTranslator";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8.<init>();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = "type: ";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r6);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        android.telephony.Rlog.d(r7, r8);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x0074:
        r7 = "common";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = r6.equals(r7);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x0094;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x007d:
        r7 = mTranslationTableCommon;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        mTranslationTable = r7;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        goto L_0x0020;
    L_0x0082:
        r0 = move-exception;
        r7 = "Sms7BitEncodingTranslator";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = "Got exception while loading 7BitTranslationTable file.";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        android.telephony.Rlog.e(r7, r8, r0);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = r2 instanceof android.content.res.XmlResourceParser;
        if (r7 == 0) goto L_0x0093;
    L_0x0090:
        r2.close();
    L_0x0093:
        return;
    L_0x0094:
        r7 = "gsm";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = r6.equals(r7);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x00ac;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x009d:
        r7 = mTranslationTableGSM;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        mTranslationTable = r7;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        goto L_0x0020;
    L_0x00a3:
        r7 = move-exception;
        r8 = r2 instanceof android.content.res.XmlResourceParser;
        if (r8 == 0) goto L_0x00ab;
    L_0x00a8:
        r2.close();
    L_0x00ab:
        throw r7;
    L_0x00ac:
        r7 = "cdma";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = r6.equals(r7);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x00bb;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00b5:
        r7 = mTranslationTableCDMA;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        mTranslationTable = r7;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        goto L_0x0020;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00bb:
        r7 = "Sms7BitEncodingTranslator";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8.<init>();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = "Error Parsing 7BitTranslationTable: found incorrect type";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r6);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        android.telephony.Rlog.e(r7, r8);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        goto L_0x0020;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00d7:
        r7 = "Character";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = r7.equals(r4);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x0145;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00e0:
        r7 = mTranslationTable;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x0145;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00e4:
        r7 = "from";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = 0;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = -1;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r1 = r2.getAttributeUnsignedIntValue(r8, r7, r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7 = "to";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = 0;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = -1;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r5 = r2.getAttributeUnsignedIntValue(r8, r7, r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r1 == r10) goto L_0x013a;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00f8:
        if (r5 == r10) goto L_0x013a;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00fa:
        r7 = DBG;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x0133;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x00fe:
        r7 = "Sms7BitEncodingTranslator";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8.<init>();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = "Loading mapping ";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = java.lang.Integer.toHexString(r1);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = r9.toUpperCase();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = " -> ";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = java.lang.Integer.toHexString(r5);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r9 = r9.toUpperCase();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.append(r9);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        android.telephony.Rlog.d(r7, r8);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x0133:
        r7 = mTranslationTable;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r7.put(r1, r5);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        goto L_0x0020;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x013a:
        r7 = "Sms7BitEncodingTranslator";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = "Invalid translation table file format";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        android.telephony.Rlog.d(r7, r8);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        goto L_0x0020;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x0145:
        r7 = DBG;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        if (r7 == 0) goto L_0x0152;	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x0149:
        r7 = "Sms7BitEncodingTranslator";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        r8 = "load7BitTranslationTableFromXml: parsing successful, file loaded";	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
        android.telephony.Rlog.d(r7, r8);	 Catch:{ Exception -> 0x0082, all -> 0x00a3 }
    L_0x0152:
        r7 = r2 instanceof android.content.res.XmlResourceParser;
        if (r7 == 0) goto L_0x0093;
    L_0x0156:
        r2.close();
        goto L_0x0093;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.Sms7BitEncodingTranslator.load7BitTranslationTableFromXml():void");
    }

    public static String translate(CharSequence message) {
        if (message == null) {
            Rlog.w(TAG, "Null message can not be translated");
            return null;
        }
        int size = message.length();
        if (size <= 0) {
            return LogException.NO_VALUE;
        }
        if (!mIs7BitTranslationTableLoaded) {
            mTranslationTableCommon = new SparseIntArray();
            mTranslationTableGSM = new SparseIntArray();
            mTranslationTableCDMA = new SparseIntArray();
            load7BitTranslationTableFromXml();
            mIs7BitTranslationTableLoaded = true;
        }
        if ((mTranslationTableCommon == null || mTranslationTableCommon.size() <= 0) && ((mTranslationTableGSM == null || mTranslationTableGSM.size() <= 0) && (mTranslationTableCDMA == null || mTranslationTableCDMA.size() <= 0))) {
            return null;
        }
        char[] output = new char[size];
        boolean isCdmaFormat = useCdmaFormatForMoSms();
        for (int i = 0; i < size; i++) {
            output[i] = translateIfNeeded(message.charAt(i), isCdmaFormat);
        }
        return String.valueOf(output);
    }

    private static char translateIfNeeded(char c, boolean isCdmaFormat) {
        if (noTranslationNeeded(c, isCdmaFormat)) {
            if (DBG) {
                Rlog.v(TAG, "No translation needed for " + Integer.toHexString(c));
            }
            return c;
        }
        int translation = -1;
        if (mTranslationTableCommon != null) {
            translation = mTranslationTableCommon.get(c, -1);
        }
        if (translation == -1) {
            if (isCdmaFormat) {
                if (mTranslationTableCDMA != null) {
                    translation = mTranslationTableCDMA.get(c, -1);
                }
            } else if (mTranslationTableGSM != null) {
                translation = mTranslationTableGSM.get(c, -1);
            }
        }
        if (translation != -1) {
            if (DBG) {
                Rlog.v(TAG, Integer.toHexString(c) + " (" + c + ")" + " translated to " + Integer.toHexString(translation) + " (" + ((char) translation) + ")");
            }
            return (char) translation;
        }
        if (DBG) {
            Rlog.w(TAG, "No translation found for " + Integer.toHexString(c) + "! Replacing for empty space");
        }
        return ' ';
    }

    private static boolean noTranslationNeeded(char c, boolean isCdmaFormat) {
        boolean z = false;
        if (!isCdmaFormat) {
            return GsmAlphabet.isGsmSeptets(c);
        }
        if (GsmAlphabet.isGsmSeptets(c) && UserData.charToAscii.get(c, -1) != -1) {
            z = true;
        }
        return z;
    }

    private static boolean useCdmaFormatForMoSms() {
        if (SmsManager.getDefault().isImsSmsSupported()) {
            return "3gpp2".equals(SmsManager.getDefault().getImsSmsFormat());
        }
        return TelephonyManager.getDefault().getCurrentPhoneType() == 2;
    }
}
