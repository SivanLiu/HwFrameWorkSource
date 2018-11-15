package com.android.server.display;

import android.graphics.PointF;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class HwXmlElement {
    protected final boolean HWDEBUG;
    protected final boolean HWFLOW;
    protected final String TAG;
    private Map<String, HwXmlElement> mChildMap;
    protected boolean mIsParsed;

    public abstract String getName();

    protected abstract boolean parseValue(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException;

    public HwXmlElement() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwXmlElement_");
        stringBuilder.append(getName());
        this.TAG = stringBuilder.toString();
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(this.TAG, 3));
        this.HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(this.TAG, 4)))) {
            z = false;
        }
        this.HWFLOW = z;
    }

    protected List<String> getNameList() {
        return null;
    }

    protected boolean isOptional() {
        return false;
    }

    protected boolean checkValue() {
        return true;
    }

    public HwXmlElement registerChildElement(HwXmlElement element) {
        if (element == null) {
            Slog.e(this.TAG, "registerChildElement() error! input element is null!");
            return null;
        }
        if (this.HWDEBUG) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerChildElement() ");
            stringBuilder.append(element.getName());
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mChildMap == null) {
            this.mChildMap = new HashMap();
        }
        List<String> nameList = element.getNameList();
        if (nameList != null) {
            for (String name : nameList) {
                if (this.mChildMap.put(name, element) != null) {
                    String str2 = this.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("registerChildElement() warning! ");
                    stringBuilder2.append(name);
                    stringBuilder2.append(" already registered!");
                    Slog.w(str2, stringBuilder2.toString());
                }
            }
        } else if (this.mChildMap.put(element.getName(), element) != null) {
            String str3 = this.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("registerChildElement() warning! ");
            stringBuilder3.append(element.getName());
            stringBuilder3.append(" already registered!");
            Slog.w(str3, stringBuilder3.toString());
        }
        return element;
    }

    public void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        StringBuilder stringBuilder;
        try {
            if (parseValue(parser)) {
                this.mIsParsed = true;
                if (this.mChildMap != null) {
                    int specifiedDepth = parser.getDepth();
                    int type = parser.next();
                    int currentDepth = parser.getDepth();
                    while (inSpecifiedDepth(specifiedDepth, type, currentDepth)) {
                        if (type == 2) {
                            String tagName = parser.getName();
                            if (this.HWDEBUG) {
                                String str = this.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("parse() tagName = ");
                                stringBuilder2.append(tagName);
                                Slog.d(str, stringBuilder2.toString());
                            }
                            HwXmlElement element = (HwXmlElement) this.mChildMap.get(tagName);
                            if (element != null) {
                                element.parse(parser);
                            }
                        }
                        type = parser.next();
                        currentDepth = parser.getDepth();
                    }
                    return;
                }
                return;
            }
            XmlUtils.skipCurrentTag(parser);
        } catch (XmlPullParserException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Element:");
            stringBuilder.append(getName());
            stringBuilder.append(", ");
            stringBuilder.append(e);
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (IOException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Element:");
            stringBuilder.append(getName());
            stringBuilder.append(", ");
            stringBuilder.append(e2);
            throw new IOException(stringBuilder.toString());
        }
    }

    private boolean inSpecifiedDepth(int specifiedDepth, int type, int currentDepth) {
        if (type == 1) {
            return false;
        }
        return type != 3 || currentDepth > specifiedDepth;
    }

    public boolean check() {
        if (this.mIsParsed) {
            if (this.mChildMap != null) {
                for (HwXmlElement element : this.mChildMap.values()) {
                    if (!element.check()) {
                        String str = this.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("check() ");
                        stringBuilder.append(element.getName());
                        stringBuilder.append(" check() failed!");
                        Slog.e(str, stringBuilder.toString());
                        return false;
                    }
                }
            }
            if (checkValue()) {
                return true;
            }
            Slog.e(this.TAG, "check() checkValue() failed!");
            return false;
        } else if (isOptional()) {
            return true;
        } else {
            Slog.e(this.TAG, "check() required tag didn't parsed");
            return false;
        }
    }

    protected static boolean string2Boolean(String str) throws XmlPullParserException {
        if (str != null && !str.isEmpty()) {
            return Boolean.parseBoolean(str);
        }
        throw new XmlPullParserException("string2Boolean() input str is null or empty!");
    }

    protected static int string2Int(String str) throws XmlPullParserException {
        if (str == null || str.isEmpty()) {
            throw new XmlPullParserException("string2Int() input str is null or empty!");
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("string2Int() ");
            stringBuilder.append(e);
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    protected static long string2Long(String str) throws XmlPullParserException {
        if (str == null || str.isEmpty()) {
            throw new XmlPullParserException("string2Long() input str is null or empty!");
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("string2Long() ");
            stringBuilder.append(e);
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    protected static float string2Float(String str) throws XmlPullParserException {
        if (str == null || str.isEmpty()) {
            throw new XmlPullParserException("string2Float() input str is null or empty!");
        }
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("string2Float() ");
            stringBuilder.append(e);
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    protected static List<PointF> parsePointFList(XmlPullParser parser, List<PointF> list) throws XmlPullParserException, IOException {
        try {
            String s = parser.nextText();
            String[] pointSplited = s.split(",");
            if (pointSplited.length == 2) {
                float x = string2Float(pointSplited[0]);
                float y = string2Float(pointSplited[1]);
                if (list == null) {
                    list = new ArrayList();
                }
                list.add(new PointF(x, y));
                return list;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parsePointFList() split failed, text=");
            stringBuilder.append(s);
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("parsePointFList() ");
            stringBuilder2.append(e);
            throw new XmlPullParserException(stringBuilder2.toString());
        }
    }

    protected static List<HwXmlAmPoint> parseAmPointList(XmlPullParser parser, List<HwXmlAmPoint> list) throws XmlPullParserException, IOException {
        try {
            String s = parser.nextText();
            String[] pointSplited = s.split(",");
            if (pointSplited.length == 3) {
                float x = string2Float(pointSplited[0]);
                float y = string2Float(pointSplited[1]);
                float z = string2Float(pointSplited[2]);
                if (list == null) {
                    list = new ArrayList();
                }
                list.add(new HwXmlAmPoint(x, y, z));
                return list;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parsePointFList() split failed, text=");
            stringBuilder.append(s);
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("parsePointFList() ");
            stringBuilder2.append(e);
            throw new XmlPullParserException(stringBuilder2.toString());
        }
    }
}
