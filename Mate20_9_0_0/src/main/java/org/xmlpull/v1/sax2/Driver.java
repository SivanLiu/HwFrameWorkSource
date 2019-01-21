package org.xmlpull.v1.sax2;

import android.icu.text.PluralRules;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class Driver implements Locator, XMLReader, Attributes {
    protected static final String APACHE_DYNAMIC_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/dynamic";
    protected static final String APACHE_SCHEMA_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/schema";
    protected static final String DECLARATION_HANDLER_PROPERTY = "http://xml.org/sax/properties/declaration-handler";
    protected static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";
    protected static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    protected static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";
    protected static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";
    protected ContentHandler contentHandler = new DefaultHandler();
    protected ErrorHandler errorHandler = new DefaultHandler();
    protected XmlPullParser pp;
    protected String systemId;

    public Driver() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        this.pp = factory.newPullParser();
    }

    public Driver(XmlPullParser pp) throws XmlPullParserException {
        this.pp = pp;
    }

    public int getLength() {
        return this.pp.getAttributeCount();
    }

    public String getURI(int index) {
        return this.pp.getAttributeNamespace(index);
    }

    public String getLocalName(int index) {
        return this.pp.getAttributeName(index);
    }

    public String getQName(int index) {
        String prefix = this.pp.getAttributePrefix(index);
        if (prefix == null) {
            return this.pp.getAttributeName(index);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(':');
        stringBuilder.append(this.pp.getAttributeName(index));
        return stringBuilder.toString();
    }

    public String getType(int index) {
        return this.pp.getAttributeType(index);
    }

    public String getValue(int index) {
        return this.pp.getAttributeValue(index);
    }

    public int getIndex(String uri, String localName) {
        int i = 0;
        while (i < this.pp.getAttributeCount()) {
            if (this.pp.getAttributeNamespace(i).equals(uri) && this.pp.getAttributeName(i).equals(localName)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int getIndex(String qName) {
        for (int i = 0; i < this.pp.getAttributeCount(); i++) {
            if (this.pp.getAttributeName(i).equals(qName)) {
                return i;
            }
        }
        return -1;
    }

    public String getType(String uri, String localName) {
        int i = 0;
        while (i < this.pp.getAttributeCount()) {
            if (this.pp.getAttributeNamespace(i).equals(uri) && this.pp.getAttributeName(i).equals(localName)) {
                return this.pp.getAttributeType(i);
            }
            i++;
        }
        return null;
    }

    public String getType(String qName) {
        for (int i = 0; i < this.pp.getAttributeCount(); i++) {
            if (this.pp.getAttributeName(i).equals(qName)) {
                return this.pp.getAttributeType(i);
            }
        }
        return null;
    }

    public String getValue(String uri, String localName) {
        return this.pp.getAttributeValue(uri, localName);
    }

    public String getValue(String qName) {
        return this.pp.getAttributeValue(null, qName);
    }

    public String getPublicId() {
        return null;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public int getLineNumber() {
        return this.pp.getLineNumber();
    }

    public int getColumnNumber() {
        return this.pp.getColumnNumber();
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (NAMESPACES_FEATURE.equals(name)) {
            return this.pp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
        }
        if (NAMESPACE_PREFIXES_FEATURE.equals(name)) {
            return this.pp.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES);
        }
        if (VALIDATION_FEATURE.equals(name)) {
            return this.pp.getFeature(XmlPullParser.FEATURE_VALIDATION);
        }
        return this.pp.getFeature(name);
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            if (NAMESPACES_FEATURE.equals(name)) {
                this.pp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, value);
            } else if (NAMESPACE_PREFIXES_FEATURE.equals(name)) {
                if (this.pp.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES) != value) {
                    this.pp.setFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES, value);
                }
            } else if (VALIDATION_FEATURE.equals(name)) {
                this.pp.setFeature(XmlPullParser.FEATURE_VALIDATION, value);
            } else {
                this.pp.setFeature(name, value);
            }
        } catch (XmlPullParserException e) {
        }
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (DECLARATION_HANDLER_PROPERTY.equals(name) || LEXICAL_HANDLER_PROPERTY.equals(name)) {
            return null;
        }
        return this.pp.getProperty(name);
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        StringBuilder stringBuilder;
        if (DECLARATION_HANDLER_PROPERTY.equals(name)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("not supported setting property ");
            stringBuilder.append(name);
            throw new SAXNotSupportedException(stringBuilder.toString());
        } else if (LEXICAL_HANDLER_PROPERTY.equals(name)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("not supported setting property ");
            stringBuilder.append(name);
            throw new SAXNotSupportedException(stringBuilder.toString());
        } else {
            try {
                this.pp.setProperty(name, value);
            } catch (XmlPullParserException ex) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("not supported set property ");
                stringBuilder2.append(name);
                stringBuilder2.append(PluralRules.KEYWORD_RULE_SEPARATOR);
                stringBuilder2.append(ex);
                throw new SAXNotSupportedException(stringBuilder2.toString());
            }
        }
    }

    public void setEntityResolver(EntityResolver resolver) {
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public void setDTDHandler(DTDHandler handler) {
    }

    public DTDHandler getDTDHandler() {
        return null;
    }

    public void setContentHandler(ContentHandler handler) {
        this.contentHandler = handler;
    }

    public ContentHandler getContentHandler() {
        return this.contentHandler;
    }

    public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
    }

    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    public void parse(InputSource source) throws SAXException, IOException {
        StringBuilder stringBuilder;
        this.systemId = source.getSystemId();
        this.contentHandler.setDocumentLocator(this);
        Reader reader = source.getCharacterStream();
        if (reader == null) {
            try {
                InputStream stream = source.getByteStream();
                String encoding = source.getEncoding();
                if (stream == null) {
                    this.systemId = source.getSystemId();
                    if (this.systemId == null) {
                        this.errorHandler.fatalError(new SAXParseException("null source systemId", this));
                        return;
                    }
                    try {
                        stream = new URL(this.systemId).openStream();
                    } catch (MalformedURLException e) {
                        try {
                            stream = new FileInputStream(this.systemId);
                        } catch (FileNotFoundException fnfe) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("could not open file with systemId ");
                            stringBuilder2.append(this.systemId);
                            this.errorHandler.fatalError(new SAXParseException(stringBuilder2.toString(), this, fnfe));
                            return;
                        }
                    }
                }
                this.pp.setInput(stream, encoding);
            } catch (XmlPullParserException ex) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("parsing initialization error: ");
                stringBuilder.append(ex);
                this.errorHandler.fatalError(new SAXParseException(stringBuilder.toString(), this, ex));
                return;
            }
        }
        this.pp.setInput(reader);
        try {
            this.contentHandler.startDocument();
            this.pp.next();
            if (this.pp.getEventType() != 2) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("expected start tag not");
                stringBuilder3.append(this.pp.getPositionDescription());
                this.errorHandler.fatalError(new SAXParseException(stringBuilder3.toString(), this));
                return;
            }
            parseSubTree(this.pp);
            this.contentHandler.endDocument();
        } catch (XmlPullParserException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("parsing initialization error: ");
            stringBuilder.append(ex2);
            this.errorHandler.fatalError(new SAXParseException(stringBuilder.toString(), this, ex2));
        }
    }

    public void parse(String systemId) throws SAXException, IOException {
        parse(new InputSource(systemId));
    }

    public void parseSubTree(XmlPullParser pp) throws SAXException, IOException {
        XmlPullParser xmlPullParser = pp;
        this.pp = xmlPullParser;
        boolean namespaceAware = xmlPullParser.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
        StringBuilder rawName;
        try {
            int type = 2;
            if (pp.getEventType() == 2) {
                int[] holderForStartAndLength = new int[2];
                rawName = new StringBuilder(16);
                int i = 1;
                int level = pp.getDepth() - 1;
                while (true) {
                    int countPrev = 0;
                    int countPrev2;
                    String name;
                    String prefix;
                    switch (type) {
                        case 1:
                            break;
                        case 2:
                            if (!namespaceAware) {
                                startElement(pp.getNamespace(), pp.getName(), pp.getName());
                                break;
                            }
                            String str;
                            int depth = pp.getDepth() - i;
                            countPrev2 = level > depth ? xmlPullParser.getNamespaceCount(depth) : 0;
                            int count = xmlPullParser.getNamespaceCount(depth + 1);
                            for (int i2 = countPrev2; i2 < count; i2++) {
                                this.contentHandler.startPrefixMapping(xmlPullParser.getNamespacePrefix(i2), xmlPullParser.getNamespaceUri(i2));
                            }
                            name = pp.getName();
                            prefix = pp.getPrefix();
                            if (prefix != null) {
                                rawName.setLength(0);
                                rawName.append(prefix);
                                rawName.append(':');
                                rawName.append(name);
                            }
                            String namespace = pp.getNamespace();
                            if (prefix == null) {
                                str = name;
                            } else {
                                str = rawName.toString();
                            }
                            startElement(namespace, name, str);
                            break;
                        case 3:
                            if (!namespaceAware) {
                                this.contentHandler.endElement(pp.getNamespace(), pp.getName(), pp.getName());
                                break;
                            }
                            String str2;
                            name = pp.getName();
                            prefix = pp.getPrefix();
                            if (prefix != null) {
                                rawName.setLength(0);
                                rawName.append(prefix);
                                rawName.append(':');
                                rawName.append(name);
                            }
                            ContentHandler contentHandler = this.contentHandler;
                            String namespace2 = pp.getNamespace();
                            if (prefix != null) {
                                str2 = name;
                            } else {
                                str2 = rawName.toString();
                            }
                            contentHandler.endElement(namespace2, name, str2);
                            if (level > pp.getDepth()) {
                                countPrev = xmlPullParser.getNamespaceCount(pp.getDepth());
                            }
                            for (countPrev2 = xmlPullParser.getNamespaceCount(pp.getDepth() - i) - 1; countPrev2 >= countPrev; countPrev2--) {
                                this.contentHandler.endPrefixMapping(xmlPullParser.getNamespacePrefix(countPrev2));
                            }
                            break;
                        case 4:
                            this.contentHandler.characters(xmlPullParser.getTextCharacters(holderForStartAndLength), holderForStartAndLength[0], holderForStartAndLength[i]);
                            break;
                        default:
                            break;
                    }
                    type = pp.next();
                    if (pp.getDepth() > level) {
                        i = 1;
                    }
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start tag must be read before skiping subtree");
            stringBuilder.append(pp.getPositionDescription());
            throw new SAXException(stringBuilder.toString());
        } catch (XmlPullParserException ex) {
            rawName = new StringBuilder();
            rawName.append("parsing error: ");
            rawName.append(ex);
            SAXParseException saxException = new SAXParseException(rawName.toString(), this, ex);
            ex.printStackTrace();
            this.errorHandler.fatalError(saxException);
        }
    }

    protected void startElement(String namespace, String localName, String qName) throws SAXException {
        this.contentHandler.startElement(namespace, localName, qName, this);
    }
}
