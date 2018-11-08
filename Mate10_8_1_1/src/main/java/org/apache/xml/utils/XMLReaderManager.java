package org.apache.xml.utils;

import java.util.Hashtable;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XMLReaderManager {
    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    private static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";
    private static SAXParserFactory m_parserFactory;
    private static final XMLReaderManager m_singletonManager = new XMLReaderManager();
    private Hashtable m_inUse;
    private ThreadLocal m_readers;

    private XMLReaderManager() {
    }

    public static XMLReaderManager getInstance() {
        return m_singletonManager;
    }

    public synchronized XMLReader getXMLReader() throws SAXException {
        XMLReader reader;
        if (this.m_readers == null) {
            this.m_readers = new ThreadLocal();
        }
        if (this.m_inUse == null) {
            this.m_inUse = new Hashtable();
        }
        reader = (XMLReader) this.m_readers.get();
        boolean threadHasReader = reader != null;
        if (!threadHasReader || this.m_inUse.get(reader) == Boolean.TRUE) {
            try {
                reader = XMLReaderFactory.createXMLReader();
            } catch (Exception e) {
                try {
                    if (m_parserFactory == null) {
                        m_parserFactory = SAXParserFactory.newInstance();
                        m_parserFactory.setNamespaceAware(true);
                    }
                    reader = m_parserFactory.newSAXParser().getXMLReader();
                } catch (ParserConfigurationException pce) {
                    throw pce;
                } catch (FactoryConfigurationError ex1) {
                    throw new SAXException(ex1.toString());
                } catch (NoSuchMethodError e2) {
                } catch (AbstractMethodError e3) {
                } catch (ParserConfigurationException ex) {
                    throw new SAXException(ex);
                }
            }
            try {
                reader.setFeature(NAMESPACES_FEATURE, true);
                reader.setFeature(NAMESPACE_PREFIXES_FEATURE, false);
            } catch (SAXException e4) {
            }
            if (!threadHasReader) {
                this.m_readers.set(reader);
                this.m_inUse.put(reader, Boolean.TRUE);
            }
        } else {
            this.m_inUse.put(reader, Boolean.TRUE);
        }
        return reader;
    }

    public synchronized void releaseXMLReader(XMLReader reader) {
        if (this.m_readers.get() == reader && reader != null) {
            this.m_inUse.remove(reader);
        }
    }
}
