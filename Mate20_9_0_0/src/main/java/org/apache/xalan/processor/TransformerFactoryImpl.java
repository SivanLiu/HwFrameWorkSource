package org.apache.xalan.processor;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TrAXFilter;
import org.apache.xalan.transformer.TransformerIdentityImpl;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.DOM2Helper;
import org.apache.xml.utils.DefaultErrorHandler;
import org.apache.xml.utils.StylesheetPIHandler;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.TreeWalker;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class TransformerFactoryImpl extends SAXTransformerFactory {
    public static final String FEATURE_INCREMENTAL = "http://xml.apache.org/xalan/features/incremental";
    public static final String FEATURE_OPTIMIZE = "http://xml.apache.org/xalan/features/optimize";
    public static final String FEATURE_SOURCE_LOCATION = "http://xml.apache.org/xalan/properties/source-location";
    public static final String XSLT_PROPERTIES = "org/apache/xalan/res/XSLTInfo.properties";
    private String m_DOMsystemID = null;
    private ErrorListener m_errorListener = new DefaultErrorHandler(false);
    private boolean m_incremental = false;
    private boolean m_isSecureProcessing = false;
    private boolean m_optimize = true;
    private boolean m_source_location = false;
    URIResolver m_uriResolver;

    public Templates processFromNode(Node node) throws TransformerConfigurationException {
        try {
            TemplatesHandler builder = newTemplatesHandler();
            new TreeWalker(builder, new DOM2Helper(), builder.getSystemId()).traverse(node);
            return builder.getTemplates();
        } catch (SAXException se) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(se));
                    return null;
                } catch (TransformerConfigurationException ex) {
                    throw ex;
                } catch (TransformerException ex2) {
                    throw new TransformerConfigurationException(ex2);
                }
            }
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSFROMNODE_FAILED, null), se);
        } catch (TransformerConfigurationException ex3) {
            throw ex3;
        } catch (Exception e) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(e));
                    return null;
                } catch (TransformerConfigurationException ex32) {
                    throw ex32;
                } catch (TransformerException ex22) {
                    throw new TransformerConfigurationException(ex22);
                }
            }
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSFROMNODE_FAILED, null), e);
        }
    }

    String getDOMsystemID() {
        return this.m_DOMsystemID;
    }

    Templates processFromNode(Node node, String systemID) throws TransformerConfigurationException {
        this.m_DOMsystemID = systemID;
        return processFromNode(node);
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x003d A:{ExcHandler: StopParseException (e org.apache.xml.utils.StopParseException), Splitter:B:8:0x002b} */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0039 A:{ExcHandler: IOException (r5_3 'se' org.xml.sax.SAXException), Splitter:B:8:0x002b} */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:8:0x002b, B:16:0x004b] */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:8:0x002b, B:13:0x003f] */
    /* JADX WARNING: Missing block: B:10:0x0039, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:11:0x003b, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:30:0x0078, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:32:0x0082, code skipped:
            throw new org.xml.sax.SAXException(r5.toString());
     */
    /* JADX WARNING: Missing block: B:33:0x0083, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:35:0x0089, code skipped:
            throw new org.xml.sax.SAXException(r5);
     */
    /* JADX WARNING: Missing block: B:37:0x0092, code skipped:
            throw new javax.xml.transform.TransformerConfigurationException("getAssociatedStylesheets failed", r5);
     */
    /* JADX WARNING: Missing block: B:39:0x009b, code skipped:
            throw new javax.xml.transform.TransformerConfigurationException("getAssociatedStylesheets failed", r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Source getAssociatedStylesheet(Source source, String media, String title, String charset) throws TransformerConfigurationException {
        String baseID;
        InputSource isource = null;
        Node node = null;
        XMLReader reader = null;
        if (source instanceof DOMSource) {
            DOMSource baseID2 = (DOMSource) source;
            node = baseID2.getNode();
            baseID = baseID2.getSystemId();
        } else {
            isource = SAXSource.sourceToInputSource(source);
            baseID = isource.getSystemId();
        }
        StylesheetPIHandler handler = new StylesheetPIHandler(baseID, media, title, charset);
        if (this.m_uriResolver != null) {
            handler.setURIResolver(this.m_uriResolver);
        }
        if (node != null) {
            new TreeWalker(handler, new DOM2Helper(), baseID).traverse(node);
        } else {
            SAXException se = SAXParserFactory.newInstance();
            se.setNamespaceAware(true);
            if (this.m_isSecureProcessing) {
                se.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            }
            reader = se.newSAXParser().getXMLReader();
            if (reader == null) {
                reader = XMLReaderFactory.createXMLReader();
            }
            if (this.m_isSecureProcessing != null) {
                reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            }
            reader.setContentHandler(handler);
            reader.parse(isource);
        }
        return handler.getAssociatedStylesheet();
    }

    public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
        return new StylesheetHandler(this);
    }

    public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        if (name == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_SET_FEATURE_NULL_NAME, null));
        } else if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            this.m_isSecureProcessing = value;
        } else {
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_UNSUPPORTED_FEATURE, new Object[]{name}));
        }
    }

    /* JADX WARNING: Missing block: B:40:0x0073, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getFeature(String name) {
        if (name == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_GET_FEATURE_NULL_NAME, null));
        } else if ("http://javax.xml.transform.dom.DOMResult/feature" == name || "http://javax.xml.transform.dom.DOMSource/feature" == name || "http://javax.xml.transform.sax.SAXResult/feature" == name || "http://javax.xml.transform.sax.SAXSource/feature" == name || "http://javax.xml.transform.stream.StreamResult/feature" == name || "http://javax.xml.transform.stream.StreamSource/feature" == name || "http://javax.xml.transform.sax.SAXTransformerFactory/feature" == name || "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter" == name || "http://javax.xml.transform.dom.DOMResult/feature".equals(name) || "http://javax.xml.transform.dom.DOMSource/feature".equals(name) || "http://javax.xml.transform.sax.SAXResult/feature".equals(name) || "http://javax.xml.transform.sax.SAXSource/feature".equals(name) || "http://javax.xml.transform.stream.StreamResult/feature".equals(name) || "http://javax.xml.transform.stream.StreamSource/feature".equals(name) || "http://javax.xml.transform.sax.SAXTransformerFactory/feature".equals(name) || "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter".equals(name)) {
            return true;
        } else {
            if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
                return this.m_isSecureProcessing;
            }
            return false;
        }
    }

    public void setAttribute(String name, Object value) throws IllegalArgumentException {
        if (name.equals(FEATURE_INCREMENTAL)) {
            if (value instanceof Boolean) {
                this.m_incremental = ((Boolean) value).booleanValue();
            } else if (value instanceof String) {
                this.m_incremental = new Boolean((String) value).booleanValue();
            } else {
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{name, value}));
            }
        } else if (name.equals(FEATURE_OPTIMIZE)) {
            if (value instanceof Boolean) {
                this.m_optimize = ((Boolean) value).booleanValue();
            } else if (value instanceof String) {
                this.m_optimize = new Boolean((String) value).booleanValue();
            } else {
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{name, value}));
            }
        } else if (!name.equals("http://xml.apache.org/xalan/properties/source-location")) {
            throw new IllegalArgumentException(XSLMessages.createMessage("ER_NOT_SUPPORTED", new Object[]{name}));
        } else if (value instanceof Boolean) {
            this.m_source_location = ((Boolean) value).booleanValue();
        } else if (value instanceof String) {
            this.m_source_location = new Boolean((String) value).booleanValue();
        } else {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{name, value}));
        }
    }

    public Object getAttribute(String name) throws IllegalArgumentException {
        if (name.equals(FEATURE_INCREMENTAL)) {
            return new Boolean(this.m_incremental);
        }
        if (name.equals(FEATURE_OPTIMIZE)) {
            return new Boolean(this.m_optimize);
        }
        if (name.equals("http://xml.apache.org/xalan/properties/source-location")) {
            return new Boolean(this.m_source_location);
        }
        throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ATTRIB_VALUE_NOT_RECOGNIZED, new Object[]{name}));
    }

    public XMLFilter newXMLFilter(Source src) throws TransformerConfigurationException {
        Templates templates = newTemplates(src);
        if (templates == null) {
            return null;
        }
        return newXMLFilter(templates);
    }

    public XMLFilter newXMLFilter(Templates templates) throws TransformerConfigurationException {
        try {
            return new TrAXFilter(templates);
        } catch (TransformerConfigurationException ex) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(ex);
                    return null;
                } catch (TransformerConfigurationException ex1) {
                    throw ex1;
                } catch (TransformerException ex12) {
                    throw new TransformerConfigurationException(ex12);
                }
            }
            throw ex;
        }
    }

    public TransformerHandler newTransformerHandler(Source src) throws TransformerConfigurationException {
        Templates templates = newTemplates(src);
        if (templates == null) {
            return null;
        }
        return newTransformerHandler(templates);
    }

    public TransformerHandler newTransformerHandler(Templates templates) throws TransformerConfigurationException {
        try {
            TransformerImpl transformer = (TransformerImpl) templates.newTransformer();
            transformer.setURIResolver(this.m_uriResolver);
            return (TransformerHandler) transformer.getInputContentHandler(true);
        } catch (TransformerConfigurationException ex) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(ex);
                    return null;
                } catch (TransformerConfigurationException ex1) {
                    throw ex1;
                } catch (TransformerException ex12) {
                    throw new TransformerConfigurationException(ex12);
                }
            }
            throw ex;
        }
    }

    public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
        return new TransformerIdentityImpl(this.m_isSecureProcessing);
    }

    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        try {
            Templates tmpl = newTemplates(source);
            if (tmpl == null) {
                return null;
            }
            Transformer transformer = tmpl.newTransformer();
            transformer.setURIResolver(this.m_uriResolver);
            return transformer;
        } catch (TransformerConfigurationException ex) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(ex);
                    return null;
                } catch (TransformerConfigurationException ex1) {
                    throw ex1;
                } catch (TransformerException ex12) {
                    throw new TransformerConfigurationException(ex12);
                }
            }
            throw ex;
        }
    }

    public Transformer newTransformer() throws TransformerConfigurationException {
        return new TransformerIdentityImpl(this.m_isSecureProcessing);
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0087 A:{ExcHandler: Exception (r3_2 'e' java.lang.Exception), Splitter:B:12:0x0030} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:28:0x0067, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:0x0071, code skipped:
            throw new org.xml.sax.SAXException(r5.toString());
     */
    /* JADX WARNING: Missing block: B:32:0x0072, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:34:0x0078, code skipped:
            throw new org.xml.sax.SAXException(r5);
     */
    /* JADX WARNING: Missing block: B:38:0x0087, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:40:0x008a, code skipped:
            if (r8.m_errorListener != null) goto L_0x008c;
     */
    /* JADX WARNING: Missing block: B:42:?, code skipped:
            r8.m_errorListener.fatalError(new javax.xml.transform.TransformerException(r3));
     */
    /* JADX WARNING: Missing block: B:43:0x0096, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:44:0x0097, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:46:0x009d, code skipped:
            throw new javax.xml.transform.TransformerConfigurationException(r2);
     */
    /* JADX WARNING: Missing block: B:47:0x009e, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:48:0x009f, code skipped:
            throw r2;
     */
    /* JADX WARNING: Missing block: B:50:0x00a9, code skipped:
            throw new javax.xml.transform.TransformerConfigurationException(r3.getMessage(), r3);
     */
    /* JADX WARNING: Missing block: B:51:0x00aa, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:53:0x00ad, code skipped:
            if (r8.m_errorListener != null) goto L_0x00af;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            r8.m_errorListener.fatalError(new javax.xml.transform.TransformerException(r2));
     */
    /* JADX WARNING: Missing block: B:58:0x00c0, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:60:0x00c6, code skipped:
            throw new javax.xml.transform.TransformerConfigurationException(r3);
     */
    /* JADX WARNING: Missing block: B:61:0x00c7, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:62:0x00c8, code skipped:
            throw r3;
     */
    /* JADX WARNING: Missing block: B:64:0x00d2, code skipped:
            throw new javax.xml.transform.TransformerConfigurationException(r2.getMessage(), r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        String baseID = source.getSystemId();
        if (baseID != null) {
            baseID = SystemIDResolver.getAbsoluteURI(baseID);
        }
        if (source instanceof DOMSource) {
            Node node = ((DOMSource) source).getNode();
            if (node != null) {
                return processFromNode(node, baseID);
            }
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_DOMSOURCE_INPUT, null));
        }
        InputSource isource;
        XMLReader reader;
        TemplatesHandler builder = newTemplatesHandler();
        builder.setSystemId(baseID);
        try {
            isource = SAXSource.sourceToInputSource(source);
            isource.setSystemId(baseID);
            reader = null;
            if (source instanceof SAXSource) {
                reader = ((SAXSource) source).getXMLReader();
            }
            if (reader == null) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                if (this.m_isSecureProcessing) {
                    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                }
                reader = factory.newSAXParser().getXMLReader();
            }
        } catch (SAXException e) {
        } catch (Exception e2) {
        }
        if (reader == null) {
            reader = XMLReaderFactory.createXMLReader();
        }
        reader.setContentHandler(builder);
        reader.parse(isource);
        return builder.getTemplates();
    }

    public void setURIResolver(URIResolver resolver) {
        this.m_uriResolver = resolver;
    }

    public URIResolver getURIResolver() {
        return this.m_uriResolver;
    }

    public ErrorListener getErrorListener() {
        return this.m_errorListener;
    }

    public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
        if (listener != null) {
            this.m_errorListener = listener;
            return;
        }
        throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ERRORLISTENER, null));
    }

    public boolean isSecureProcessing() {
        return this.m_isSecureProcessing;
    }
}
