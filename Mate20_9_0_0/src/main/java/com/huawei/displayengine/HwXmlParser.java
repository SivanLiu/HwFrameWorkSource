package com.huawei.displayengine;

import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class HwXmlParser {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String TAG = "HwXmlParser";
    private boolean mIsParseFinished;
    private HwXmlElement mRootElement;
    private final String mXmlPath;

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public HwXmlParser(String xmlPath) {
        this.mXmlPath = xmlPath;
        if (this.mXmlPath == null) {
            Slog.e(TAG, "HwXmlParser() error! input xmlPath is null!");
        }
    }

    private FileInputStream getXmlFile() {
        if (HWDEBUG) {
            Slog.d(TAG, "getXmlFile()");
        }
        if (this.mXmlPath == null) {
            Slog.e(TAG, "getXmlFile() error! mXmlPath is null!");
            return null;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(this.mXmlPath);
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "getXmlFile() failed! FileNotFoundException");
        }
        return inputStream;
    }

    public HwXmlElement registerRootElement(HwXmlElement element) {
        if (HWDEBUG) {
            Slog.d(TAG, "registerRootElement()");
        }
        this.mRootElement = element;
        return element;
    }

    public boolean parse() {
        StringBuilder stringBuilder;
        if (HWFLOW) {
            Slog.i(TAG, "parse()");
        }
        if (this.mRootElement == null) {
            Slog.e(TAG, "parse() error! hasn't registerRootElement");
            return false;
        }
        FileInputStream inputStream = getXmlFile();
        if (inputStream == null) {
            return false;
        }
        String rootTagName;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, StandardCharsets.UTF_8.name());
            rootTagName = this.mRootElement.getName();
            int specifiedDepth = parser.getDepth();
            int type = parser.next();
            for (int currentDepth = parser.getDepth(); inSpecifiedDepth(specifiedDepth, type, currentDepth); currentDepth = parser.getDepth()) {
                if (type == 2) {
                    String tagName = parser.getName();
                    if (HWDEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("parse() rootTagName=");
                        stringBuilder2.append(rootTagName);
                        stringBuilder2.append(",tagName=");
                        stringBuilder2.append(tagName);
                        Slog.d(str, stringBuilder2.toString());
                    }
                    if (tagName.equals(rootTagName)) {
                        this.mRootElement.parse(parser);
                    }
                }
                type = parser.next();
            }
            this.mIsParseFinished = true;
            if (HWFLOW) {
                Slog.i(TAG, "parse() done");
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                Slog.e(TAG, "parse() error! close FileInputStream failed");
            }
            return true;
        } catch (XmlPullParserException e2) {
            rootTagName = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("parse() error! ");
            stringBuilder.append(e2);
            Slog.e(rootTagName, stringBuilder.toString());
            inputStream.close();
            return false;
        } catch (IOException e3) {
            rootTagName = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("parse() error! ");
            stringBuilder.append(e3);
            Slog.e(rootTagName, stringBuilder.toString());
            try {
                inputStream.close();
            } catch (IOException e4) {
                Slog.e(TAG, "parse() error! close FileInputStream failed");
            }
            return false;
        } catch (Throwable th) {
            try {
                inputStream.close();
            } catch (IOException e5) {
                Slog.e(TAG, "parse() error! close FileInputStream failed");
            }
            throw th;
        }
    }

    private boolean inSpecifiedDepth(int specifiedDepth, int type, int currentDepth) {
        if (type == 1) {
            return false;
        }
        return type != 3 || currentDepth > specifiedDepth;
    }

    public boolean check() {
        if (HWFLOW) {
            Slog.i(TAG, "check()");
        }
        if (this.mIsParseFinished && this.mRootElement.check()) {
            return true;
        }
        return false;
    }
}
