package org.apache.xalan.processor;

import java.io.IOException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.DOM2Helper;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.TreeWalker;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class ProcessorInclude extends XSLTElementProcessor {
    static final long serialVersionUID = -4570078731972673481L;
    private String m_href = null;

    public String getHref() {
        return this.m_href;
    }

    public void setHref(String baseIdent) {
        this.m_href = baseIdent;
    }

    protected int getStylesheetType() {
        return 2;
    }

    protected String getStylesheetInclErr() {
        return XSLTErrorResources.ER_STYLESHEET_INCLUDES_ITSELF;
    }

    public void startElement(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        setPropertiesFromAttributes(handler, rawName, attributes, this);
        int savedStylesheetType;
        try {
            Source sourceFromURIResolver = getSourceFromUriResolver(handler);
            String hrefUrl = getBaseURIOfIncludedStylesheet(handler, sourceFromURIResolver);
            if (handler.importStackContains(hrefUrl)) {
                throw new SAXException(XSLMessages.createMessage(getStylesheetInclErr(), new Object[]{hrefUrl}));
            }
            handler.pushImportURL(hrefUrl);
            handler.pushImportSource(sourceFromURIResolver);
            savedStylesheetType = handler.getStylesheetType();
            handler.setStylesheetType(getStylesheetType());
            handler.pushNewNamespaceSupport();
            parse(handler, uri, localName, rawName, attributes);
            handler.setStylesheetType(savedStylesheetType);
            handler.popImportURL();
            handler.popImportSource();
            handler.popNamespaceSupport();
        } catch (TransformerException te) {
            handler.error(te.getMessage(), te);
        } catch (Throwable th) {
            handler.setStylesheetType(savedStylesheetType);
            handler.popImportURL();
            handler.popImportSource();
            handler.popNamespaceSupport();
        }
    }

    protected void parse(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        IOException ioe = null;
        if (handler.getStylesheetProcessor().getURIResolver() != null) {
            try {
                ioe = handler.peekSourceFromURIResolver();
                if (ioe != null && (ioe instanceof DOMSource)) {
                    Node node = ((DOMSource) ioe).getNode();
                    String systemId = handler.peekImportURL();
                    if (systemId != null) {
                        handler.pushBaseIndentifier(systemId);
                    }
                    new TreeWalker(handler, new DOM2Helper(), systemId).traverse(node);
                    if (systemId != null) {
                        handler.popBaseIndentifier();
                    }
                    return;
                }
            } catch (ParserConfigurationException ex) {
                throw new SAXException(ex);
            } catch (FactoryConfigurationError ex1) {
                throw new SAXException(ex1.toString());
            } catch (AbstractMethodError | NoSuchMethodError e) {
            } catch (SAXException se) {
                throw new TransformerException(se);
            } catch (IOException ioe2) {
                handler.error(XSLTErrorResources.ER_IOEXCEPTION, new Object[]{getHref()}, ioe2);
            } catch (TransformerException ioe22) {
                handler.error(ioe22.getMessage(), ioe22);
            } catch (Throwable th) {
                handler.popBaseIndentifier();
            }
        }
        if (ioe22 == null) {
            ioe22 = new StreamSource(SystemIDResolver.getAbsoluteURI(getHref(), handler.getBaseIdentifier()));
        }
        Source ioe3 = processSource(handler, ioe22);
        XMLReader reader = null;
        if (ioe3 instanceof SAXSource) {
            reader = ((SAXSource) ioe3).getXMLReader();
        }
        InputSource inputSource = SAXSource.sourceToInputSource(ioe3);
        if (reader == null) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            if (handler.getStylesheetProcessor().isSecureProcessing()) {
                try {
                    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                } catch (SAXException e2) {
                }
            }
            reader = factory.newSAXParser().getXMLReader();
        }
        if (reader == null) {
            reader = XMLReaderFactory.createXMLReader();
        }
        if (reader != null) {
            reader.setContentHandler(handler);
            handler.pushBaseIndentifier(inputSource.getSystemId());
            reader.parse(inputSource);
            handler.popBaseIndentifier();
        }
    }

    protected Source processSource(StylesheetHandler handler, Source source) {
        return source;
    }

    private Source getSourceFromUriResolver(StylesheetHandler handler) throws TransformerException {
        URIResolver uriresolver = handler.getStylesheetProcessor().getURIResolver();
        if (uriresolver != null) {
            return uriresolver.resolve(getHref(), handler.getBaseIdentifier());
        }
        return null;
    }

    private String getBaseURIOfIncludedStylesheet(StylesheetHandler handler, Source s) throws TransformerException {
        if (s != null) {
            String systemId = s.getSystemId();
            String idFromUriResolverSource = systemId;
            if (systemId != null) {
                return idFromUriResolverSource;
            }
        }
        return SystemIDResolver.getAbsoluteURI(getHref(), handler.getBaseIdentifier());
    }
}
