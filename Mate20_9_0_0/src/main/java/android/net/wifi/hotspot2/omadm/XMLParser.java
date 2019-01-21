package android.net.wifi.hotspot2.omadm;

import android.text.TextUtils;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLParser extends DefaultHandler {
    private XMLNode mCurrent = null;
    private XMLNode mRoot = null;

    public XMLNode parse(String text) throws IOException, SAXException {
        if (TextUtils.isEmpty(text)) {
            throw new IOException("XML string not provided");
        }
        this.mRoot = null;
        this.mCurrent = null;
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(new StringReader(text)), this);
            return this.mRoot;
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        XMLNode parent = this.mCurrent;
        this.mCurrent = new XMLNode(parent, qName);
        if (this.mRoot == null) {
            this.mRoot = this.mCurrent;
        } else if (parent != null) {
            parent.addChild(this.mCurrent);
        } else {
            throw new SAXException("More than one root nodes");
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals(this.mCurrent.getTag())) {
            this.mCurrent.close();
            this.mCurrent = this.mCurrent.getParent();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("End tag '");
        stringBuilder.append(qName);
        stringBuilder.append("' doesn't match current node: ");
        stringBuilder.append(this.mCurrent);
        throw new SAXException(stringBuilder.toString());
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        this.mCurrent.addText(new String(ch, start, length));
    }
}
