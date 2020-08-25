package com.huawei.opcollect.config;

import android.util.Xml;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.StringUtils;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class XmpParserGenerator {
    private static final String TAG = "XmpParserGenerator";
    private XmlItem xmlItem;
    private String xmlPath;

    public XmpParserGenerator(String xmlPath2) {
        this.xmlPath = xmlPath2;
        loadXml();
    }

    public XmlItem getXmlItem() {
        return this.xmlItem;
    }

    public XmlItem getChildItem(String tag) {
        if (this.xmlItem == null || this.xmlItem.getChildItemList() == null) {
            return null;
        }
        for (XmlItem item : this.xmlItem.getChildItemList()) {
            if (tag != null && tag.equals(item.getTagName())) {
                return item;
            }
        }
        return null;
    }

    public void closeQuitely(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                OPCollectLog.e(TAG, "exception occurred when close the inputStream");
            }
        }
    }

    private void loadXml() {
        if (StringUtils.isEmpty(this.xmlPath)) {
            OPCollectLog.e(TAG, "xml file path is null or empty");
            return;
        }
        InputStream inputStream = null;
        InputStream inputStream2 = null;
        XmlPullParser parser = Xml.newPullParser();
        try {
            InputStream inputStream3 = new FileInputStream(this.xmlPath);
            try {
                parser.setInput(inputStream3, "UTF-8");
                this.xmlItem = parserXml(parser);
                closeQuitely(inputStream3);
            } catch (XmlPullParserException e) {
                e = e;
                inputStream2 = inputStream3;
                try {
                    OPCollectLog.e(TAG, "failed to load and parse the xml");
                    closeQuitely(inputStream2);
                } catch (Throwable th) {
                    th = th;
                    closeQuitely(inputStream2);
                    throw th;
                }
            } catch (IOException e2) {
                e = e2;
                inputStream = inputStream3;
                OPCollectLog.e(TAG, "failed to load and parse the xml");
                closeQuitely(inputStream2);
            } catch (Throwable th2) {
                th = th2;
                inputStream2 = inputStream3;
                closeQuitely(inputStream2);
                throw th;
            }
        } catch (XmlPullParserException e3) {
            e = e3;
            OPCollectLog.e(TAG, "failed to load and parse the xml");
            closeQuitely(inputStream2);
        } catch (IOException e4) {
            e = e4;
            OPCollectLog.e(TAG, "failed to load and parse the xml");
            closeQuitely(inputStream2);
        }
    }

    private XmlItem parserXmlTagStart(XmlPullParser parser, XmlItem tempXmlItem) throws XmlPullParserException, IOException {
        XmlItem currentXmlItem = tempXmlItem;
        if (tempXmlItem == null) {
            currentXmlItem = new XmlItem();
            currentXmlItem.setTagName(parser.getName());
            int attributeCount = parser.getAttributeCount();
            if (attributeCount > 0) {
                for (int i = 0; i < attributeCount; i++) {
                    currentXmlItem.setProp(parser.getAttributeName(i), parser.getAttributeValue(i));
                }
            }
        } else {
            currentXmlItem.setChildItem(parserXml(parser));
        }
        return currentXmlItem;
    }

    private XmlItem parserXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        XmlItem tempXmlItem = null;
        if (parser == null) {
            OPCollectLog.e(TAG, "praser is null");
            return null;
        }
        int eventType = parser.getEventType();
        while (eventType != 1) {
            switch (eventType) {
                case 2:
                    tempXmlItem = parserXmlTagStart(parser, tempXmlItem);
                    break;
                case 3:
                    if (tempXmlItem != null && tempXmlItem.getTagName().equals(parser.getName())) {
                        return tempXmlItem;
                    }
                case 4:
                    if (tempXmlItem == null) {
                        break;
                    } else {
                        tempXmlItem.setText(parser.getText());
                        break;
                    }
            }
            eventType = parser.next();
        }
        return null;
    }
}
