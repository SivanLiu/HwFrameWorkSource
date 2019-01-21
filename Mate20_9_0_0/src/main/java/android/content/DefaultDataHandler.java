package android.content;

import android.net.Uri;
import android.util.Xml;
import android.util.Xml.Encoding;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class DefaultDataHandler implements ContentInsertHandler {
    private static final String ARG = "arg";
    private static final String COL = "col";
    private static final String DEL = "del";
    private static final String POSTFIX = "postfix";
    private static final String ROW = "row";
    private static final String SELECT = "select";
    private static final String URI_STR = "uri";
    private ContentResolver mContentResolver;
    private Stack<Uri> mUris = new Stack();
    private ContentValues mValues;

    public void insert(ContentResolver contentResolver, InputStream in) throws IOException, SAXException {
        this.mContentResolver = contentResolver;
        Xml.parse(in, Encoding.UTF_8, this);
    }

    public void insert(ContentResolver contentResolver, String in) throws SAXException {
        this.mContentResolver = contentResolver;
        Xml.parse(in, this);
    }

    private void parseRow(Attributes atts) throws SAXException {
        Uri uri;
        String uriStr = atts.getValue("uri");
        if (uriStr != null) {
            uri = Uri.parse(uriStr);
            if (uri == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("attribute ");
                stringBuilder.append(atts.getValue("uri"));
                stringBuilder.append(" parsing failure");
                throw new SAXException(stringBuilder.toString());
            }
        } else if (this.mUris.size() > 0) {
            Uri uri2;
            String postfix = atts.getValue(POSTFIX);
            if (postfix != null) {
                uri2 = Uri.withAppendedPath((Uri) this.mUris.lastElement(), postfix);
            } else {
                uri2 = (Uri) this.mUris.lastElement();
            }
            uri = uri2;
        } else {
            throw new SAXException("attribute parsing failure");
        }
        this.mUris.push(uri);
    }

    private Uri insertRow() {
        Uri u = this.mContentResolver.insert((Uri) this.mUris.lastElement(), this.mValues);
        this.mValues = null;
        return u;
    }

    public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
        StringBuilder stringBuilder;
        Uri u;
        if (!ROW.equals(localName)) {
            int i = 0;
            if (COL.equals(localName)) {
                int attrLen = atts.getLength();
                if (attrLen == 2) {
                    String key = atts.getValue(0);
                    String value = atts.getValue(1);
                    if (key == null || key.length() <= 0 || value == null || value.length() <= 0) {
                        throw new SAXException("illegal attributes value");
                    }
                    if (this.mValues == null) {
                        this.mValues = new ContentValues();
                    }
                    this.mValues.put(key, value);
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("illegal attributes number ");
                stringBuilder.append(attrLen);
                throw new SAXException(stringBuilder.toString());
            } else if (DEL.equals(localName)) {
                u = Uri.parse(atts.getValue("uri"));
                if (u != null) {
                    int attrLen2 = atts.getLength() - 2;
                    if (attrLen2 > 0) {
                        String[] selectionArgs = new String[attrLen2];
                        while (i < attrLen2) {
                            selectionArgs[i] = atts.getValue(i + 2);
                            i++;
                        }
                        this.mContentResolver.delete(u, atts.getValue(1), selectionArgs);
                        return;
                    } else if (attrLen2 == 0) {
                        this.mContentResolver.delete(u, atts.getValue(1), null);
                        return;
                    } else {
                        this.mContentResolver.delete(u, null, null);
                        return;
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("attribute ");
                stringBuilder.append(atts.getValue("uri"));
                stringBuilder.append(" parsing failure");
                throw new SAXException(stringBuilder.toString());
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unknown element: ");
                stringBuilder2.append(localName);
                throw new SAXException(stringBuilder2.toString());
            }
        } else if (this.mValues != null) {
            if (this.mUris.empty()) {
                throw new SAXException("uri is empty");
            }
            u = insertRow();
            if (u != null) {
                this.mUris.pop();
                this.mUris.push(u);
                parseRow(atts);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("insert to uri ");
            stringBuilder.append(((Uri) this.mUris.lastElement()).toString());
            stringBuilder.append(" failure");
            throw new SAXException(stringBuilder.toString());
        } else if (atts.getLength() == 0) {
            this.mUris.push((Uri) this.mUris.lastElement());
        } else {
            parseRow(atts);
        }
    }

    public void endElement(String uri, String localName, String name) throws SAXException {
        if (!ROW.equals(localName)) {
            return;
        }
        if (this.mUris.empty()) {
            throw new SAXException("uri mismatch");
        }
        if (this.mValues != null) {
            insertRow();
        }
        this.mUris.pop();
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    public void startDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }
}
