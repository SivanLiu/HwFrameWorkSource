package android.util;

import android.os.Bundle;
import com.huawei.displayengine.DisplayEngineDBManager.BrightnessCurveKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PersonalizedCurveXmlManager {
    private static boolean HWDEBUG = false;
    private static boolean HWFLOW = false;
    private static final String TAG = "PersonalizedCurveXmlManager";
    private static final String XML_FILE = "DisplayEnginePersonalizedCurve.xml";
    private static final String XML_PATH = "/data/system";

    protected static class CurveText {
        public String mTextAL = null;
        public String mTextBL = null;

        protected CurveText() {
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    private static File getXmlFile() {
        String str;
        StringBuilder stringBuilder;
        File file = null;
        try {
            file = new File(XML_PATH);
            if (file.exists() || file.mkdirs()) {
                try {
                    file = new File(XML_PATH, XML_FILE);
                    if (file.exists()) {
                        if (HWFLOW) {
                            Slog.i(TAG, "/data/system/DisplayEnginePersonalizedCurve.xml already exist!");
                        }
                        if (!file.delete()) {
                            Slog.e(TAG, "Failed to delete file:/data/system/DisplayEnginePersonalizedCurve.xml");
                            return null;
                        }
                    }
                    if (file.createNewFile()) {
                        return file;
                    }
                    Slog.e(TAG, "Create /data/system/DisplayEnginePersonalizedCurve.xml failed!");
                    return null;
                } catch (SecurityException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("writeCurveToXmlFile() error:");
                    stringBuilder.append(e.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    return null;
                } catch (IOException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("writeCurveToXmlFile() error:");
                    stringBuilder.append(e2.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    return null;
                }
            }
            Slog.e(TAG, "Failed to mkdir /data/system");
            return null;
        } catch (SecurityException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("writeCurveToXmlFile() error:");
            stringBuilder.append(e3.getMessage());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static boolean writeCurveToXmlFile(String name, ArrayList<Bundle> curve) {
        FileOutputStream fos;
        IOException e;
        String str;
        StringBuilder stringBuilder;
        String str2;
        if (name == null || curve == null) {
            Slog.e(TAG, "Invalid input: name=null or curve=null!");
            return false;
        } else if (curve.size() <= 0) {
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid input: curve size=");
            stringBuilder2.append(curve.size());
            Slog.e(str3, stringBuilder2.toString());
            return false;
        } else {
            StringBuilder stringBuilder3;
            StringBuffer textAL = new StringBuffer();
            StringBuffer textBL = new StringBuffer();
            for (int i = 0; i < curve.size(); i++) {
                Bundle data = (Bundle) curve.get(i);
                float al = data.getFloat("AmbientLight");
                float bl = data.getFloat(BrightnessCurveKey.BL);
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(al);
                stringBuilder3.append(",");
                textAL.append(stringBuilder3.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(bl);
                stringBuilder3.append(",");
                textBL.append(stringBuilder3.toString());
            }
            File file = getXmlFile();
            if (file == null) {
                return false;
            }
            fos = null;
            try {
                FileOutputStream fos2 = new FileOutputStream(file);
                fos = null;
                XmlSerializer serializer = Xml.newSerializer();
                try {
                    serializer.setOutput(fos2, "UTF-8");
                    serializer.startDocument("UTF-8", Boolean.valueOf(true));
                    serializer.startTag(null, "curves");
                    serializer.startTag(null, "curve");
                    serializer.attribute(null, "name", name);
                    serializer.startTag(null, "AmbientLight");
                    serializer.text(textAL.toString());
                    serializer.endTag(null, "AmbientLight");
                    serializer.startTag(null, BrightnessCurveKey.BL);
                    serializer.text(textBL.toString());
                    serializer.endTag(null, BrightnessCurveKey.BL);
                    serializer.endTag(null, "curve");
                    serializer.endTag(null, "curves");
                    serializer.endDocument();
                    serializer.flush();
                    fos2.flush();
                    fos = true;
                    try {
                        fos2.close();
                    } catch (IOException e2) {
                        e = e2;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } catch (IOException e3) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Write /data/system/DisplayEnginePersonalizedCurve.xml error:");
                    stringBuilder.append(e3.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    try {
                        fos2.close();
                    } catch (IOException e4) {
                        e3 = e4;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } catch (IllegalArgumentException e5) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Write /data/system/DisplayEnginePersonalizedCurve.xml error:");
                    stringBuilder.append(e5.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    try {
                        fos2.close();
                    } catch (IOException e6) {
                        e3 = e6;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } catch (IllegalStateException e7) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Write /data/system/DisplayEnginePersonalizedCurve.xml error:");
                    stringBuilder.append(e7.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    try {
                        fos2.close();
                    } catch (IOException e8) {
                        e3 = e8;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } catch (Throwable th) {
                    try {
                        fos2.close();
                    } catch (IOException e9) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Write /data/system/DisplayEnginePersonalizedCurve.xml error:");
                        stringBuilder.append(e9.getMessage());
                        Slog.w(TAG, stringBuilder.toString());
                    }
                    throw th;
                }
                return fos;
            } catch (FileNotFoundException e10) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("writeCurveToXmlFile() error:");
                stringBuilder3.append(e10.getMessage());
                Slog.e(str2, stringBuilder3.toString());
                return false;
            } catch (SecurityException e11) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("writeCurveToXmlFile() error:");
                stringBuilder3.append(e11.getMessage());
                Slog.e(str2, stringBuilder3.toString());
                return false;
            }
        }
        stringBuilder.append("Write /data/system/DisplayEnginePersonalizedCurve.xml error:");
        stringBuilder.append(e3.getMessage());
        Slog.w(str, stringBuilder.toString());
        return fos;
    }

    private static CurveText getCurveTextFromXmlFile(String name) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        XmlPullParser parser = Xml.newPullParser();
        File file = null;
        String textAL;
        try {
            CurveText ct;
            file = new File(XML_PATH, XML_FILE);
            if (file.exists()) {
                ct = null;
                textAL = null;
                String textBL = null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    parser.setInput(fis, "UTF-8");
                    boolean match = false;
                    for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                        if (eventType == 2) {
                            String tagName = parser.getName();
                            if ("curve".equals(tagName)) {
                                match = name.equals(parser.getAttributeValue(null, "name"));
                            } else if ("AmbientLight".equals(tagName)) {
                                if (match && textAL == null) {
                                    textAL = parser.nextText();
                                }
                            } else if (BrightnessCurveKey.BL.equals(tagName) && match && textBL == null) {
                                textBL = parser.nextText();
                            }
                        }
                    }
                    ct = new CurveText();
                    ct.mTextAL = textAL;
                    ct.mTextBL = textBL;
                    try {
                        fis.close();
                    } catch (IOException e2) {
                        e = e2;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } catch (FileNotFoundException e3) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("file not found exception! error:");
                    stringBuilder.append(e3.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e4) {
                            e = e4;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    }
                } catch (XmlPullParserException e5) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("XmlPullParserException! error:");
                    stringBuilder.append(e5.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e6) {
                            e = e6;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    }
                } catch (IOException e7) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IOException! error:");
                    stringBuilder.append(e7.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e8) {
                            e7 = e8;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    }
                } catch (SecurityException e9) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SecurityException! error:");
                    stringBuilder.append(e9.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e10) {
                            e7 = e10;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    }
                } catch (Throwable th) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e11) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("IOException! error:");
                            stringBuilder.append(e11.getMessage());
                            Slog.e(TAG, stringBuilder.toString());
                        }
                    }
                }
                return ct;
            }
            Slog.w(TAG, "/data/system/DisplayEnginePersonalizedCurve.xml is not exist!");
            return null;
            stringBuilder.append("IOException! error:");
            stringBuilder.append(e7.getMessage());
            Slog.e(str, stringBuilder.toString());
            return ct;
        } catch (SecurityException e12) {
            textAL = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCurveTextFromXmlFile() error:");
            stringBuilder2.append(e12.getMessage());
            Slog.e(textAL, stringBuilder2.toString());
            return null;
        }
    }

    public static ArrayList<Bundle> readCurveFromXmlFile(String name) {
        CurveText ct = getCurveTextFromXmlFile(name);
        if (ct == null) {
            Slog.e(TAG, "Failed to parse /data/system/DisplayEnginePersonalizedCurve.xml");
            return null;
        }
        String textAL = ct.mTextAL;
        String textBL = ct.mTextBL;
        ArrayList<Bundle> result = null;
        if (!(textAL == null || textBL == null)) {
            String[] textALs = textAL.split(",");
            String[] textBLs = textBL.split(",");
            if (textALs.length <= 0 || textALs.length != textBLs.length) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid curve information from xml: al length=");
                stringBuilder.append(textALs.length);
                stringBuilder.append(",bl length=");
                stringBuilder.append(textBLs.length);
                Slog.e(str, stringBuilder.toString());
            } else {
                result = new ArrayList();
                for (int i = 0; i < textALs.length; i++) {
                    Bundle point = new Bundle();
                    point.putFloat("AmbientLight", Float.parseFloat(textALs[i]));
                    point.putFloat(BrightnessCurveKey.BL, Float.parseFloat(textBLs[i]));
                    result.add(point);
                }
            }
        }
        return result;
    }
}
