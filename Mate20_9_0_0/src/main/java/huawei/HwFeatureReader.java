package huawei;

import android.content.ContentValues;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwFeatureReader {
    private static String TAG = "HwFeatureReader";
    private static final String XMLPATHGP = "/cust/xml/HwFeatureConfig_Gp.xml";
    private static final String XMLPATHHAP = "/cust/xml/HwFeatureConfig_Hap.xml";
    private static boolean hasReadFileGp = false;
    private static boolean hasReadFileHap = false;
    private static ContentValues map = null;

    public static synchronized boolean getFeature(String feature) {
        synchronized (HwFeatureReader.class) {
            Log.d(TAG, "getFeature begin");
            if (map == null) {
                map = new ContentValues();
                getAllFeatures();
            }
            if (map.getAsBoolean(feature) != null) {
                boolean booleanValue = map.getAsBoolean(feature).booleanValue();
                return booleanValue;
            }
            Log.d(TAG, "getFeature finish");
            return false;
        }
    }

    private static void getAllFeatures() {
        String str;
        StringBuilder stringBuilder;
        Log.d(TAG, "getAllFeatures begin");
        if (!(hasReadFileGp || hasReadFileHap)) {
            File confFileGp = new File(Environment.getDataDirectory(), XMLPATHGP);
            InputStreamReader confreader = null;
            try {
                String feature;
                String str2;
                StringBuilder stringBuilder2;
                confreader = new InputStreamReader(new FileInputStream(new File(Environment.getDataDirectory(), XMLPATHHAP)), Charset.defaultCharset());
                XmlPullParser confparser = Xml.newPullParser();
                confparser.setInput(confreader);
                XmlUtils.beginDocument(confparser, "features");
                while (true) {
                    XmlUtils.nextElement(confparser);
                    if (!"feature".equals(confparser.getName())) {
                        break;
                    } else if (confparser.getAttributeValue(null, "value") != null) {
                        feature = confparser.getAttributeValue(null, "name");
                        map.put(feature, confparser.getAttributeValue(null, "value"));
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("getAllFeatures  from confFileHap feature:");
                        stringBuilder2.append(feature);
                        stringBuilder2.append("AttributeValue:");
                        stringBuilder2.append(confparser.getAttributeValue(null, "value"));
                        Log.d(str2, stringBuilder2.toString());
                    }
                }
                hasReadFileHap = true;
                confreader = new InputStreamReader(new FileInputStream(confFileGp), Charset.defaultCharset());
                confparser = Xml.newPullParser();
                confparser.setInput(confreader);
                XmlUtils.beginDocument(confparser, "features");
                while (true) {
                    XmlUtils.nextElement(confparser);
                    feature = confparser.getName();
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getAllFeatures prof_type");
                    stringBuilder2.append(feature);
                    Log.d(str2, stringBuilder2.toString());
                    if (!"feature".equals(feature)) {
                        break;
                    } else if (confparser.getAttributeValue(null, "value") != null) {
                        str2 = confparser.getAttributeValue(null, "name");
                        map.put(str2, confparser.getAttributeValue(null, "value"));
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("getAllFeatures feature from confFileGp:");
                        stringBuilder3.append(str2);
                        stringBuilder3.append("AttributeValue:");
                        stringBuilder3.append(confparser.getAttributeValue(null, "value"));
                        Log.d(str3, stringBuilder3.toString());
                    }
                }
                hasReadFileGp = true;
                try {
                    confreader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "File not found");
                if (confreader != null) {
                    confreader.close();
                }
            } catch (RuntimeException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while parsing '");
                stringBuilder.append(e3);
                Log.e(str, stringBuilder.toString());
                if (confreader != null) {
                    confreader.close();
                }
            } catch (XmlPullParserException e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while parsing '");
                stringBuilder.append(e4);
                Log.e(str, stringBuilder.toString());
                if (confreader != null) {
                    confreader.close();
                }
            } catch (IOException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while parsing '");
                stringBuilder.append(e5);
                Log.e(str, stringBuilder.toString());
                if (confreader != null) {
                    try {
                        confreader.close();
                    } catch (Exception e6) {
                        e6.printStackTrace();
                    }
                }
            } catch (Throwable th) {
                if (confreader != null) {
                    try {
                        confreader.close();
                    } catch (Exception e7) {
                        e7.printStackTrace();
                    }
                }
            }
        }
    }
}
