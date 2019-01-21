package org.xml.sax.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import javax.xml.XMLConstants;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class ParserAdapter implements XMLReader, DocumentHandler {
    private static final String FEATURES = "http://xml.org/sax/features/";
    private static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    private static final String NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
    private static final String XMLNS_URIs = "http://xml.org/sax/features/xmlns-uris";
    private AttributeListAdapter attAdapter;
    private AttributesImpl atts;
    ContentHandler contentHandler;
    DTDHandler dtdHandler;
    EntityResolver entityResolver;
    ErrorHandler errorHandler;
    Locator locator;
    private String[] nameParts;
    private boolean namespaces;
    private NamespaceSupport nsSupport;
    private Parser parser;
    private boolean parsing;
    private boolean prefixes;
    private boolean uris;

    final class AttributeListAdapter implements Attributes {
        private AttributeList qAtts;

        AttributeListAdapter() {
        }

        void setAttributeList(AttributeList qAtts) {
            this.qAtts = qAtts;
        }

        public int getLength() {
            return this.qAtts.getLength();
        }

        public String getURI(int i) {
            return "";
        }

        public String getLocalName(int i) {
            return "";
        }

        public String getQName(int i) {
            return this.qAtts.getName(i).intern();
        }

        public String getType(int i) {
            return this.qAtts.getType(i).intern();
        }

        public String getValue(int i) {
            return this.qAtts.getValue(i);
        }

        public int getIndex(String uri, String localName) {
            return -1;
        }

        public int getIndex(String qName) {
            int max = ParserAdapter.this.atts.getLength();
            for (int i = 0; i < max; i++) {
                if (this.qAtts.getName(i).equals(qName)) {
                    return i;
                }
            }
            return -1;
        }

        public String getType(String uri, String localName) {
            return null;
        }

        public String getType(String qName) {
            return this.qAtts.getType(qName).intern();
        }

        public String getValue(String uri, String localName) {
            return null;
        }

        public String getValue(String qName) {
            return this.qAtts.getValue(qName);
        }
    }

    public ParserAdapter() throws SAXException {
        StringBuilder stringBuilder;
        this.parsing = false;
        this.nameParts = new String[3];
        this.parser = null;
        this.atts = null;
        this.namespaces = true;
        this.prefixes = false;
        this.uris = false;
        this.entityResolver = null;
        this.dtdHandler = null;
        this.contentHandler = null;
        this.errorHandler = null;
        String driver = System.getProperty("org.xml.sax.parser");
        try {
            setup(ParserFactory.makeParser());
        } catch (ClassNotFoundException e1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot find SAX1 driver class ");
            stringBuilder.append(driver);
            throw new SAXException(stringBuilder.toString(), e1);
        } catch (IllegalAccessException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("SAX1 driver class ");
            stringBuilder.append(driver);
            stringBuilder.append(" found but cannot be loaded");
            throw new SAXException(stringBuilder.toString(), e2);
        } catch (InstantiationException e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("SAX1 driver class ");
            stringBuilder.append(driver);
            stringBuilder.append(" loaded but cannot be instantiated");
            throw new SAXException(stringBuilder.toString(), e3);
        } catch (ClassCastException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("SAX1 driver class ");
            stringBuilder.append(driver);
            stringBuilder.append(" does not implement org.xml.sax.Parser");
            throw new SAXException(stringBuilder.toString());
        } catch (NullPointerException e4) {
            throw new SAXException("System property org.xml.sax.parser not specified");
        }
    }

    public ParserAdapter(Parser parser) {
        this.parsing = false;
        this.nameParts = new String[3];
        this.parser = null;
        this.atts = null;
        this.namespaces = true;
        this.prefixes = false;
        this.uris = false;
        this.entityResolver = null;
        this.dtdHandler = null;
        this.contentHandler = null;
        this.errorHandler = null;
        setup(parser);
    }

    private void setup(Parser parser) {
        if (parser != null) {
            this.parser = parser;
            this.atts = new AttributesImpl();
            this.nsSupport = new NamespaceSupport();
            this.attAdapter = new AttributeListAdapter();
            return;
        }
        throw new NullPointerException("Parser argument must not be null");
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            checkNotParsing("feature", name);
            this.namespaces = value;
            if (!this.namespaces && !this.prefixes) {
                this.prefixes = true;
            }
        } else if (name.equals(NAMESPACE_PREFIXES)) {
            checkNotParsing("feature", name);
            this.prefixes = value;
            if (!this.prefixes && !this.namespaces) {
                this.namespaces = true;
            }
        } else if (name.equals(XMLNS_URIs)) {
            checkNotParsing("feature", name);
            this.uris = value;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Feature: ");
            stringBuilder.append(name);
            throw new SAXNotRecognizedException(stringBuilder.toString());
        }
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            return this.namespaces;
        }
        if (name.equals(NAMESPACE_PREFIXES)) {
            return this.prefixes;
        }
        if (name.equals(XMLNS_URIs)) {
            return this.uris;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Feature: ");
        stringBuilder.append(name);
        throw new SAXNotRecognizedException(stringBuilder.toString());
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Property: ");
        stringBuilder.append(name);
        throw new SAXNotRecognizedException(stringBuilder.toString());
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Property: ");
        stringBuilder.append(name);
        throw new SAXNotRecognizedException(stringBuilder.toString());
    }

    public void setEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    public EntityResolver getEntityResolver() {
        return this.entityResolver;
    }

    public void setDTDHandler(DTDHandler handler) {
        this.dtdHandler = handler;
    }

    public DTDHandler getDTDHandler() {
        return this.dtdHandler;
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

    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    public void parse(InputSource input) throws IOException, SAXException {
        if (this.parsing) {
            throw new SAXException("Parser is already in use");
        }
        setupParser();
        this.parsing = true;
        try {
            this.parser.parse(input);
            this.parsing = false;
        } finally {
            this.parsing = false;
        }
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        if (this.contentHandler != null) {
            this.contentHandler.setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.endDocument();
        }
    }

    public void startElement(String qName, AttributeList qAtts) throws SAXException {
        AttributeList attributeList = qAtts;
        if (this.namespaces) {
            String prefix;
            this.nsSupport.pushContext();
            int length = qAtts.getLength();
            for (int i = 0; i < length; i++) {
                String attQName = attributeList.getName(i);
                if (attQName.startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                    int n = attQName.indexOf(58);
                    if (n == -1 && attQName.length() == 5) {
                        prefix = "";
                    } else if (n == 5) {
                        prefix = attQName.substring(n + 1);
                    }
                    String value = attributeList.getValue(i);
                    if (!this.nsSupport.declarePrefix(prefix, value)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Illegal Namespace prefix: ");
                        stringBuilder.append(prefix);
                        reportError(stringBuilder.toString());
                    } else if (this.contentHandler != null) {
                        this.contentHandler.startPrefixMapping(prefix, value);
                    }
                }
            }
            this.atts.clear();
            ArrayList<SAXParseException> exceptions = null;
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 >= length) {
                    break;
                }
                String attQName2 = attributeList.getName(i3);
                String type = attributeList.getType(i3);
                String value2 = attributeList.getValue(i3);
                if (attQName2.startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                    String prefix2;
                    int n2 = attQName2.indexOf(58);
                    if (n2 == -1 && attQName2.length() == 5) {
                        prefix2 = "";
                    } else if (n2 != 5) {
                        prefix2 = null;
                    } else {
                        prefix2 = attQName2.substring(6);
                    }
                    if (prefix2 != null) {
                        if (this.prefixes) {
                            if (this.uris) {
                                AttributesImpl attributesImpl = this.atts;
                                NamespaceSupport namespaceSupport = this.nsSupport;
                                attributesImpl.addAttribute("http://www.w3.org/XML/1998/namespace", prefix2, attQName2.intern(), type, value2);
                            } else {
                                this.atts.addAttribute("", "", attQName2.intern(), type, value2);
                            }
                        }
                        i2 = i3 + 1;
                    }
                }
                try {
                    String[] attName = processName(attQName2, true, true);
                    this.atts.addAttribute(attName[0], attName[1], attName[2], type, value2);
                } catch (SAXException e) {
                    if (exceptions == null) {
                        exceptions = new ArrayList();
                    }
                    exceptions.add((SAXParseException) e);
                    this.atts.addAttribute("", attQName2, attQName2, type, value2);
                }
                i2 = i3 + 1;
            }
            if (!(exceptions == null || this.errorHandler == null)) {
                Iterator it = exceptions.iterator();
                while (it.hasNext()) {
                    this.errorHandler.error((SAXParseException) it.next());
                }
            }
            if (this.contentHandler != null) {
                String[] name = processName(qName, false, false);
                this.contentHandler.startElement(name[0], name[1], name[2], this.atts);
            } else {
                prefix = qName;
            }
            return;
        }
        if (this.contentHandler != null) {
            this.attAdapter.setAttributeList(attributeList);
            this.contentHandler.startElement("", "", qName.intern(), this.attAdapter);
        }
    }

    public void endElement(String qName) throws SAXException {
        if (this.namespaces) {
            String[] names = processName(qName, false, false);
            if (this.contentHandler != null) {
                this.contentHandler.endElement(names[0], names[1], names[2]);
                Enumeration prefixes = this.nsSupport.getDeclaredPrefixes();
                while (prefixes.hasMoreElements()) {
                    this.contentHandler.endPrefixMapping((String) prefixes.nextElement());
                }
            }
            this.nsSupport.popContext();
            return;
        }
        if (this.contentHandler != null) {
            this.contentHandler.endElement("", "", qName.intern());
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.ignorableWhitespace(ch, start, length);
        }
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.processingInstruction(target, data);
        }
    }

    private void setupParser() {
        if (this.prefixes || this.namespaces) {
            this.nsSupport.reset();
            if (this.uris) {
                this.nsSupport.setNamespaceDeclUris(true);
            }
            if (this.entityResolver != null) {
                this.parser.setEntityResolver(this.entityResolver);
            }
            if (this.dtdHandler != null) {
                this.parser.setDTDHandler(this.dtdHandler);
            }
            if (this.errorHandler != null) {
                this.parser.setErrorHandler(this.errorHandler);
            }
            this.parser.setDocumentHandler(this);
            this.locator = null;
            return;
        }
        throw new IllegalStateException();
    }

    private String[] processName(String qName, boolean isAttribute, boolean useException) throws SAXException {
        String[] parts = this.nsSupport.processName(qName, this.nameParts, isAttribute);
        if (parts != null) {
            return parts;
        }
        StringBuilder stringBuilder;
        if (useException) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Undeclared prefix: ");
            stringBuilder.append(qName);
            throw makeException(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Undeclared prefix: ");
        stringBuilder.append(qName);
        reportError(stringBuilder.toString());
        parts = new String[3];
        String str = "";
        parts[1] = str;
        parts[0] = str;
        parts[2] = qName.intern();
        return parts;
    }

    void reportError(String message) throws SAXException {
        if (this.errorHandler != null) {
            this.errorHandler.error(makeException(message));
        }
    }

    private SAXParseException makeException(String message) {
        if (this.locator != null) {
            return new SAXParseException(message, this.locator);
        }
        return new SAXParseException(message, null, null, -1, -1);
    }

    private void checkNotParsing(String type, String name) throws SAXNotSupportedException {
        if (this.parsing) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot change ");
            stringBuilder.append(type);
            stringBuilder.append(' ');
            stringBuilder.append(name);
            stringBuilder.append(" while parsing");
            throw new SAXNotSupportedException(stringBuilder.toString());
        }
    }
}
