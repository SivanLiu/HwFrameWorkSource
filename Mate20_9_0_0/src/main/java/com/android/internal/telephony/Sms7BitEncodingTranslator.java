package com.android.internal.telephony;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.SparseIntArray;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.util.XmlUtils;

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

    public static String translate(CharSequence message) {
        if (message == null) {
            Rlog.w(TAG, "Null message can not be translated");
            return null;
        }
        int size = message.length();
        if (size <= 0) {
            return "";
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No translation needed for ");
                stringBuilder.append(Integer.toHexString(c));
                Rlog.v(str, stringBuilder.toString());
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
        String str2;
        StringBuilder stringBuilder2;
        if (translation != -1) {
            if (DBG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(Integer.toHexString(c));
                stringBuilder2.append(" (");
                stringBuilder2.append(c);
                stringBuilder2.append(") translated to ");
                stringBuilder2.append(Integer.toHexString(translation));
                stringBuilder2.append(" (");
                stringBuilder2.append((char) translation);
                stringBuilder2.append(")");
                Rlog.v(str2, stringBuilder2.toString());
            }
            return (char) translation;
        }
        if (DBG) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No translation found for ");
            stringBuilder2.append(Integer.toHexString(c));
            stringBuilder2.append("! Replacing for empty space");
            Rlog.w(str2, stringBuilder2.toString());
        }
        return ' ';
    }

    private static boolean noTranslationNeeded(char c, boolean isCdmaFormat) {
        if (!isCdmaFormat) {
            return GsmAlphabet.isGsmSeptets(c);
        }
        boolean z = GsmAlphabet.isGsmSeptets(c) && UserData.charToAscii.get(c, -1) != -1;
        return z;
    }

    private static boolean useCdmaFormatForMoSms() {
        if (SmsManager.getDefault().isImsSmsSupported()) {
            return SmsConstants.FORMAT_3GPP2.equals(SmsManager.getDefault().getImsSmsFormat());
        }
        return TelephonyManager.getDefault().getCurrentPhoneType() == 2;
    }

    private static void load7BitTranslationTableFromXml() {
        XmlResourceParser parser = null;
        Resources r = Resources.getSystem();
        if (null == null) {
            if (DBG) {
                Rlog.d(TAG, "load7BitTranslationTableFromXml: open normal file");
            }
            parser = r.getXml(18284565);
        }
        try {
            XmlUtils.beginDocument(parser, XML_START_TAG);
            while (true) {
                String str;
                XmlUtils.nextElement(parser);
                String tag = parser.getName();
                if (DBG) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("tag: ");
                    stringBuilder.append(tag);
                    Rlog.d(str, stringBuilder.toString());
                }
                if (XML_TRANSLATION_TYPE_TAG.equals(tag)) {
                    String str2;
                    StringBuilder stringBuilder2;
                    str = parser.getAttributeValue(null, "Type");
                    if (DBG) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("type: ");
                        stringBuilder2.append(str);
                        Rlog.d(str2, stringBuilder2.toString());
                    }
                    if (str.equals("common")) {
                        mTranslationTable = mTranslationTableCommon;
                    } else if (str.equals("gsm")) {
                        mTranslationTable = mTranslationTableGSM;
                    } else if (str.equals("cdma")) {
                        mTranslationTable = mTranslationTableCDMA;
                    } else {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error Parsing 7BitTranslationTable: found incorrect type");
                        stringBuilder2.append(str);
                        Rlog.e(str2, stringBuilder2.toString());
                    }
                } else if (XML_CHARACTOR_TAG.equals(tag) && mTranslationTable != null) {
                    int from = parser.getAttributeUnsignedIntValue(null, XML_FROM_TAG, -1);
                    int to = parser.getAttributeUnsignedIntValue(null, XML_TO_TAG, -1);
                    if (from == -1 || to == -1) {
                        Rlog.d(TAG, "Invalid translation table file format");
                    } else {
                        if (DBG) {
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Loading mapping ");
                            stringBuilder3.append(Integer.toHexString(from).toUpperCase());
                            stringBuilder3.append(" -> ");
                            stringBuilder3.append(Integer.toHexString(to).toUpperCase());
                            Rlog.d(str3, stringBuilder3.toString());
                        }
                        mTranslationTable.put(from, to);
                    }
                }
            }
            if (DBG) {
                Rlog.d(TAG, "load7BitTranslationTableFromXml: parsing successful, file loaded");
            }
            if (!(parser instanceof XmlResourceParser)) {
                return;
            }
        } catch (Exception e) {
            Rlog.e(TAG, "Got exception while loading 7BitTranslationTable file.", e);
            if (!(parser instanceof XmlResourceParser)) {
                return;
            }
        } catch (Throwable th) {
            if (parser instanceof XmlResourceParser) {
                parser.close();
            }
            throw th;
        }
        parser.close();
    }
}
