package org.apache.xml.utils;

import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import org.apache.xalan.templates.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class StylesheetPIHandler extends DefaultHandler {
    String m_baseID;
    String m_charset;
    String m_media;
    Vector m_stylesheets = new Vector();
    String m_title;
    URIResolver m_uriResolver;

    public void setURIResolver(URIResolver resolver) {
        this.m_uriResolver = resolver;
    }

    public URIResolver getURIResolver() {
        return this.m_uriResolver;
    }

    public StylesheetPIHandler(String baseID, String media, String title, String charset) {
        this.m_baseID = baseID;
        this.m_media = media;
        this.m_title = title;
        this.m_charset = charset;
    }

    public Source getAssociatedStylesheet() {
        int sz = this.m_stylesheets.size();
        if (sz > 0) {
            return (Source) this.m_stylesheets.elementAt(sz - 1);
        }
        return null;
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (target.equals("xml-stylesheet")) {
            String href = null;
            String type = null;
            String title = null;
            String media = null;
            String charset = null;
            int i = 1;
            StringTokenizer tokenizer = new StringTokenizer(data, " \t=\n", true);
            boolean lookedAhead = false;
            Source source = null;
            String token = "";
            while (tokenizer.hasMoreTokens()) {
                if (lookedAhead) {
                    lookedAhead = false;
                } else {
                    token = tokenizer.nextToken();
                }
                if (tokenizer.hasMoreTokens()) {
                    if (!(token.equals(" ") || token.equals("\t"))) {
                        if (token.equals("=")) {
                        }
                    }
                }
                String name = token;
                String href2;
                if (name.equals("type")) {
                    token = tokenizer.nextToken();
                    while (tokenizer.hasMoreTokens() && (token.equals(" ") || token.equals("\t") || token.equals("="))) {
                        token = tokenizer.nextToken();
                    }
                    type = token.substring(i, token.length() - i);
                } else if (name.equals(Constants.ATTRNAME_HREF)) {
                    token = tokenizer.nextToken();
                    while (tokenizer.hasMoreTokens() && (token.equals(" ") || token.equals("\t") || token.equals("="))) {
                        token = tokenizer.nextToken();
                    }
                    href = token;
                    if (tokenizer.hasMoreTokens()) {
                        token = tokenizer.nextToken();
                        while (token.equals("=") && tokenizer.hasMoreTokens()) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(href);
                            stringBuilder.append(token);
                            stringBuilder.append(tokenizer.nextToken());
                            href = stringBuilder.toString();
                            if (!tokenizer.hasMoreTokens()) {
                                break;
                            }
                            token = tokenizer.nextToken();
                            lookedAhead = true;
                        }
                    }
                    href2 = href.substring(1, href.length() - 1);
                    try {
                        Source source2;
                        if (this.m_uriResolver != null) {
                            source2 = this.m_uriResolver.resolve(href2, this.m_baseID);
                        } else {
                            href2 = SystemIDResolver.getAbsoluteURI(href2, this.m_baseID);
                            source2 = new SAXSource(new InputSource(href2));
                        }
                        source = source2;
                        href = href2;
                    } catch (TransformerException href3) {
                        throw new SAXException(href3);
                    }
                } else {
                    if (name.equals("title")) {
                        href2 = tokenizer.nextToken();
                        while (tokenizer.hasMoreTokens() && (href2.equals(" ") || href2.equals("\t") || href2.equals("="))) {
                            href2 = tokenizer.nextToken();
                        }
                        title = href2.substring(1, href2.length() - 1);
                    } else if (name.equals("media")) {
                        href2 = tokenizer.nextToken();
                        while (tokenizer.hasMoreTokens() && (href2.equals(" ") || href2.equals("\t") || href2.equals("="))) {
                            href2 = tokenizer.nextToken();
                        }
                        media = href2.substring(1, href2.length() - 1);
                    } else if (name.equals("charset")) {
                        href2 = tokenizer.nextToken();
                        while (tokenizer.hasMoreTokens() && (href2.equals(" ") || href2.equals("\t") || href2.equals("="))) {
                            href2 = tokenizer.nextToken();
                        }
                        charset = href2.substring(1, href2.length() - 1);
                    } else if (name.equals("alternate")) {
                        href2 = tokenizer.nextToken();
                        while (tokenizer.hasMoreTokens() && (href2.equals(" ") || href2.equals("\t") || href2.equals("="))) {
                            href2 = tokenizer.nextToken();
                        }
                        boolean alternate = href2.substring(1, href2.length() - 1).equals("yes");
                    }
                    token = href2;
                }
                i = 1;
            }
            if (type != null && ((type.equals("text/xsl") || type.equals("text/xml") || type.equals("application/xml+xslt")) && href3 != null && (this.m_media == null || (media != null && media.equals(this.m_media))))) {
                if (this.m_charset != null && (charset == null || !charset.equals(this.m_charset))) {
                    return;
                }
                if (this.m_title == null || (title != null && title.equals(this.m_title))) {
                    this.m_stylesheets.addElement(source);
                } else {
                    return;
                }
            }
            return;
        }
        String str = data;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        throw new StopParseException();
    }

    public void setBaseId(String baseId) {
        this.m_baseID = baseId;
    }

    public String getBaseId() {
        return this.m_baseID;
    }
}
