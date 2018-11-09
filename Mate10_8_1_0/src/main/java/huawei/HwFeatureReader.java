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
        InputStreamReader inputStreamReader;
        RuntimeException e;
        XmlPullParserException e2;
        IOException e3;
        Log.d(TAG, "getAllFeatures begin");
        if (!(hasReadFileGp || hasReadFileHap)) {
            File confFileGp = new File(Environment.getDataDirectory(), XMLPATHGP);
            inputStreamReader = null;
            try {
                InputStreamReader confreader = new InputStreamReader(new FileInputStream(new File(Environment.getDataDirectory(), XMLPATHHAP)), Charset.defaultCharset());
                try {
                    String feature;
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
                            Log.d(TAG, "getAllFeatures  from confFileHap feature:" + feature + "AttributeValue:" + confparser.getAttributeValue(null, "value"));
                        }
                    }
                    hasReadFileHap = true;
                    inputStreamReader = new InputStreamReader(new FileInputStream(confFileGp), Charset.defaultCharset());
                    confparser = Xml.newPullParser();
                    confparser.setInput(inputStreamReader);
                    XmlUtils.beginDocument(confparser, "features");
                    while (true) {
                        XmlUtils.nextElement(confparser);
                        String prof_type = confparser.getName();
                        Log.d(TAG, "getAllFeatures prof_type" + prof_type);
                        if (!"feature".equals(prof_type)) {
                            break;
                        } else if (confparser.getAttributeValue(null, "value") != null) {
                            feature = confparser.getAttributeValue(null, "name");
                            map.put(feature, confparser.getAttributeValue(null, "value"));
                            Log.d(TAG, "getAllFeatures feature from confFileGp:" + feature + "AttributeValue:" + confparser.getAttributeValue(null, "value"));
                        }
                    }
                    hasReadFileGp = true;
                    if (inputStreamReader != null) {
                        try {
                            inputStreamReader.close();
                        } catch (Exception e4) {
                            e4.printStackTrace();
                        }
                    }
                    return;
                } catch (FileNotFoundException e5) {
                    inputStreamReader = confreader;
                } catch (RuntimeException e6) {
                    e = e6;
                    inputStreamReader = confreader;
                } catch (XmlPullParserException e7) {
                    e2 = e7;
                    inputStreamReader = confreader;
                } catch (IOException e8) {
                    e3 = e8;
                    inputStreamReader = confreader;
                } catch (Throwable th) {
                    Throwable th2 = th;
                    inputStreamReader = confreader;
                }
            } catch (FileNotFoundException e9) {
            } catch (RuntimeException e10) {
                e = e10;
            } catch (XmlPullParserException e11) {
                e2 = e11;
            } catch (IOException e12) {
                e3 = e12;
            }
        }
        try {
            Log.e(TAG, "File not found");
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (Exception e42) {
                    e42.printStackTrace();
                }
            }
        } catch (Throwable th3) {
            th2 = th3;
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (Exception e422) {
                    e422.printStackTrace();
                }
            }
            throw th2;
        }
        Log.e(TAG, "Exception while parsing '" + e);
        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
            } catch (Exception e4222) {
                e4222.printStackTrace();
            }
        }
        Log.e(TAG, "Exception while parsing '" + e3);
        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
            } catch (Exception e42222) {
                e42222.printStackTrace();
            }
        }
        Log.e(TAG, "Exception while parsing '" + e2);
        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
            } catch (Exception e422222) {
                e422222.printStackTrace();
            }
        }
    }
}
