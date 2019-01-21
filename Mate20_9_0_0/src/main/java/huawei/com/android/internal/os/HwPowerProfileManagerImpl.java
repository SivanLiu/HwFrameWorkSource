package huawei.com.android.internal.os;

import android.util.Log;
import android.util.Xml;
import com.android.internal.os.IHwPowerProfileManager;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwPowerProfileManagerImpl implements IHwPowerProfileManager {
    private static final String ATTR_NAME = "name";
    private static final String CUST_FILE_DIR = "/data/cust/xml";
    private static final String POWER_PROFILE_NAME = "power_profile.xml";
    private static final String SYSTEM_FILE_DIR = "/system/etc/xml";
    private static final String TAG = "PowerProfile";
    private static final String TAG_ARRAY = "array";
    private static final String TAG_ARRAYITEM = "value";
    private static final String TAG_DEVICE = "device";
    private static final String TAG_ITEM = "item";
    private static IHwPowerProfileManager mHwPowerProfileManager = null;

    public static IHwPowerProfileManager getDefault() {
        if (mHwPowerProfileManager == null) {
            mHwPowerProfileManager = new HwPowerProfileManagerImpl();
        }
        return mHwPowerProfileManager;
    }

    public boolean readHwPowerValuesFromXml(HashMap<String, Double> sPowerItemMap, HashMap<String, Double[]> sPowerArrayMap) {
        if (readHwPowerValuesFromXml(sPowerItemMap, sPowerArrayMap, CUST_FILE_DIR, POWER_PROFILE_NAME) || readHwPowerValuesFromXml(sPowerItemMap, sPowerArrayMap, SYSTEM_FILE_DIR, POWER_PROFILE_NAME)) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:82:0x017b A:{SYNTHETIC, Splitter:B:82:0x017b} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x017b A:{SYNTHETIC, Splitter:B:82:0x017b} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean readHwPowerValuesFromXml(HashMap<String, Double> sPowerItemMap, HashMap<String, Double[]> sPowerArrayMap, String fileDir, String fileName) {
        XmlPullParserException e;
        HashMap<String, Double> hashMap;
        IOException e2;
        Throwable e3;
        Throwable th;
        HashMap<String, Double[]> hashMap2 = sPowerArrayMap;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("xml/");
        stringBuilder.append(fileName);
        File powerProfile = HwCfgFilePolicy.getCfgFile(stringBuilder.toString(), 0);
        if (powerProfile == null) {
            Log.w(TAG, "power_profile.xml not found! ");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(powerProfile.getAbsolutePath());
        stringBuilder2.append(" be read ! ");
        Log.v(str, stringBuilder2.toString());
        InputStream inputStream = null;
        String str2;
        if (powerProfile.canRead()) {
            try {
                String arrayName;
                inputStream = new FileInputStream(powerProfile);
                XmlPullParser parser = Xml.newPullParser();
                String str3 = null;
                parser.setInput(inputStream, null);
                ArrayList<Double> array = new ArrayList();
                boolean parsingArray = false;
                str = null;
                while (true) {
                    arrayName = str;
                    XmlUtils.nextElement(parser);
                    String element = parser.getName();
                    if (element == null) {
                        break;
                    }
                    str2 = fileDir;
                    if (parsingArray) {
                        try {
                            if (!element.equals("value")) {
                                hashMap2.put(arrayName, (Double[]) array.toArray(new Double[array.size()]));
                                parsingArray = false;
                            }
                        } catch (XmlPullParserException e4) {
                            e = e4;
                            hashMap = sPowerItemMap;
                            throw new RuntimeException(e);
                        } catch (IOException e5) {
                            e2 = e5;
                            hashMap = sPowerItemMap;
                            throw new RuntimeException(e2);
                        } catch (Throwable th2) {
                            e3 = th2;
                            hashMap = sPowerItemMap;
                            th = e3;
                            if (inputStream != null) {
                            }
                            throw th;
                        }
                    }
                    if (element.equals(TAG_ARRAY)) {
                        array.clear();
                        String arrayName2 = parser.getAttributeValue(str3, ATTR_NAME);
                        hashMap = sPowerItemMap;
                        String str4 = arrayName2;
                        parsingArray = true;
                        str = str4;
                    } else {
                        String name;
                        if (!element.equals(TAG_ITEM)) {
                            if (element.equals("value")) {
                            }
                            hashMap = sPowerItemMap;
                            str = arrayName;
                        }
                        if (parsingArray) {
                            name = null;
                        } else {
                            name = parser.getAttributeValue(str3, ATTR_NAME);
                        }
                        if (parser.next() == 4) {
                            double value = 0.0d;
                            try {
                                value = Double.valueOf(parser.getText()).doubleValue();
                            } catch (NumberFormatException nfe) {
                                Log.e(TAG, "there is a NumberFormatException");
                            }
                            if (element.equals(TAG_ITEM)) {
                                try {
                                    sPowerItemMap.put(name, Double.valueOf(value));
                                } catch (XmlPullParserException e6) {
                                    e = e6;
                                    throw new RuntimeException(e);
                                } catch (IOException e7) {
                                    e2 = e7;
                                    throw new RuntimeException(e2);
                                }
                            }
                            hashMap = sPowerItemMap;
                            if (parsingArray) {
                                array.add(Double.valueOf(value));
                            }
                            str = arrayName;
                        }
                        hashMap = sPowerItemMap;
                        str = arrayName;
                    }
                    str3 = null;
                }
                if (parsingArray) {
                    hashMap2.put(arrayName, (Double[]) array.toArray(new Double[array.size()]));
                }
                try {
                    inputStream.close();
                } catch (IOException e22) {
                    IOException iOException = e22;
                    Log.e(TAG, "Fail to close!", e22);
                }
                str = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(fileDir);
                stringBuilder3.append("/");
                stringBuilder3.append(POWER_PROFILE_NAME);
                stringBuilder3.append(" be read ! ");
                Log.w(str, stringBuilder3.toString());
                return true;
            } catch (XmlPullParserException e8) {
                e = e8;
                hashMap = sPowerItemMap;
                str2 = fileDir;
                throw new RuntimeException(e);
            } catch (IOException e9) {
                e22 = e9;
                hashMap = sPowerItemMap;
                str2 = fileDir;
                throw new RuntimeException(e22);
            } catch (Throwable th3) {
                e3 = th3;
                th = e3;
                if (inputStream != null) {
                }
                throw th;
            }
        }
        hashMap = sPowerItemMap;
        str2 = fileDir;
        Log.w(TAG, "power_profile.xml 11111not found! power profile maybe not right!");
        return false;
    }
}
