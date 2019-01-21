package org.apache.xml.dtm.ref;

import java.io.PrintStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMException;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.dom2dtm.DOM2DTM;
import org.apache.xml.dtm.ref.dom2dtm.DOM2DTMdefaultNamespaceDeclarationNode;
import org.apache.xml.dtm.ref.sax2dtm.SAX2DTM;
import org.apache.xml.dtm.ref.sax2dtm.SAX2RTFDTM;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.SuballocatedIntVector;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLReaderManager;
import org.apache.xml.utils.XMLStringFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class DTMManagerDefault extends DTMManager {
    private static final boolean DEBUG = false;
    private static final boolean DUMPTREE = false;
    protected DefaultHandler m_defaultHandler = new DefaultHandler();
    int[] m_dtm_offsets = new int[DTMFilter.SHOW_DOCUMENT];
    protected DTM[] m_dtms = new DTM[DTMFilter.SHOW_DOCUMENT];
    private ExpandedNameTable m_expandedNameTable = new ExpandedNameTable();
    protected XMLReaderManager m_readerManager = null;

    public synchronized void addDTM(DTM dtm, int id) {
        addDTM(dtm, id, 0);
    }

    public synchronized void addDTM(DTM dtm, int id, int offset) {
        if (id < 65536) {
            int oldlen = this.m_dtms.length;
            if (oldlen <= id) {
                int newlen = Math.min(id + DTMFilter.SHOW_DOCUMENT, 65536);
                DTM[] new_m_dtms = new DTM[newlen];
                System.arraycopy(this.m_dtms, 0, new_m_dtms, 0, oldlen);
                this.m_dtms = new_m_dtms;
                int[] new_m_dtm_offsets = new int[newlen];
                System.arraycopy(this.m_dtm_offsets, 0, new_m_dtm_offsets, 0, oldlen);
                this.m_dtm_offsets = new_m_dtm_offsets;
            }
            this.m_dtms[id] = dtm;
            this.m_dtm_offsets[id] = offset;
            dtm.documentRegistration();
        } else {
            throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
        }
    }

    public synchronized int getFirstFreeDTMID() {
        int n = this.m_dtms.length;
        for (int i = 1; i < n; i++) {
            if (this.m_dtms[i] == null) {
                return i;
            }
        }
        return n;
    }

    /* JADX WARNING: Removed duplicated region for block: B:158:0x01e0 A:{SYNTHETIC, Splitter:B:158:0x01e0} */
    /* JADX WARNING: Removed duplicated region for block: B:158:0x01e0 A:{SYNTHETIC, Splitter:B:158:0x01e0} */
    /* JADX WARNING: Removed duplicated region for block: B:158:0x01e0 A:{SYNTHETIC, Splitter:B:158:0x01e0} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized DTM getDTM(Source source, boolean unique, DTMWSFilter whiteSpaceFilter, boolean incremental, boolean doIndexing) {
        Exception exception;
        boolean incremental2;
        Throwable th;
        Throwable th2;
        Source source2 = source;
        synchronized (this) {
            XMLStringFactory xstringFactory = this.m_xsf;
            int dtmPos = getFirstFreeDTMID();
            int documentID = dtmPos << 16;
            if (source2 == null || !(source2 instanceof DOMSource)) {
                InputSource xmlSource;
                int i;
                InputSource xmlSource2;
                SAX2DTM sax2dtm;
                boolean isSAXSource = source2 != null ? source2 instanceof SAXSource : true;
                boolean isStreamSource = source2 != null ? source2 instanceof StreamSource : false;
                if (!isSAXSource) {
                    if (!isStreamSource) {
                        throw new DTMException(XMLMessages.createXMLMessage("ER_NOT_SUPPORTED", new Object[]{source2}));
                    }
                }
                XMLReader reader = null;
                if (source2 == null) {
                    xmlSource = null;
                } else {
                    try {
                        reader = getXMLReader(source);
                        InputSource xmlSource3 = SAXSource.sourceToInputSource(source);
                        String urlOfSource = xmlSource3.getSystemId();
                        if (urlOfSource != null) {
                            try {
                                urlOfSource = SystemIDResolver.getAbsoluteURI(urlOfSource);
                            } catch (Exception e) {
                                exception = e;
                                PrintStream printStream = System.err;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Can not absolutize URL: ");
                                stringBuilder.append(urlOfSource);
                                printStream.println(stringBuilder.toString());
                            } catch (Throwable th22) {
                                incremental2 = incremental;
                                th = th22;
                                i = dtmPos;
                                if (reader != null) {
                                    if (!(this.m_incremental && incremental2)) {
                                        reader.setContentHandler(this.m_defaultHandler);
                                        reader.setDTDHandler(this.m_defaultHandler);
                                        reader.setErrorHandler(this.m_defaultHandler);
                                        try {
                                            reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                                        } catch (Exception e2) {
                                        }
                                    }
                                }
                                releaseXMLReader(reader);
                                throw th;
                            }
                            xmlSource3.setSystemId(urlOfSource);
                        }
                        xmlSource = xmlSource3;
                    } catch (Throwable th222) {
                        i = dtmPos;
                        incremental2 = incremental;
                        th = th222;
                        if (reader != null) {
                        }
                        releaseXMLReader(reader);
                        throw th;
                    }
                }
                if (source2 != null || !unique || incremental || doIndexing) {
                    xmlSource2 = xmlSource;
                    i = dtmPos;
                    sax2dtm = new SAX2DTM(this, source2, documentID, whiteSpaceFilter, xstringFactory, doIndexing);
                } else {
                    sax2dtm = sax2dtm;
                    xmlSource2 = xmlSource;
                    i = 0;
                    i = dtmPos;
                    try {
                        sax2dtm = new SAX2RTFDTM(this, source2, documentID, whiteSpaceFilter, xstringFactory, doIndexing);
                    } catch (Throwable th3) {
                        th222 = th3;
                        incremental2 = incremental;
                        th = th222;
                        if (reader != null) {
                        }
                        releaseXMLReader(reader);
                        throw th;
                    }
                }
                sax2dtm = dtm;
                boolean haveXercesParser = false;
                addDTM(sax2dtm, i, 0);
                if (reader != null && reader.getClass().getName().equals("org.apache.xerces.parsers.SAXParser")) {
                    haveXercesParser = true;
                }
                incremental2 = haveXercesParser ? true : incremental;
                IncrementalSAXSource coParser;
                try {
                    if (this.m_incremental && incremental2) {
                        coParser = null;
                        if (haveXercesParser) {
                            coParser = (IncrementalSAXSource) Class.forName("org.apache.xml.dtm.ref.IncrementalSAXSource_Xerces").newInstance();
                        }
                        if (coParser == null) {
                            if (reader == null) {
                                coParser = new IncrementalSAXSource_Filter();
                            } else {
                                IncrementalSAXSource_Filter filter = new IncrementalSAXSource_Filter();
                                filter.setXMLReader(reader);
                                coParser = filter;
                            }
                        }
                        sax2dtm.setIncrementalSAXSource(coParser);
                        if (xmlSource2 == null) {
                            if (reader != null) {
                                if (!(this.m_incremental && incremental2)) {
                                    reader.setContentHandler(this.m_defaultHandler);
                                    reader.setDTDHandler(this.m_defaultHandler);
                                    reader.setErrorHandler(this.m_defaultHandler);
                                    try {
                                        reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                                    } catch (Exception e3) {
                                    }
                                }
                            }
                            releaseXMLReader(reader);
                            return sax2dtm;
                        }
                        if (reader.getErrorHandler() == null) {
                            reader.setErrorHandler(sax2dtm);
                        }
                        reader.setDTDHandler(sax2dtm);
                        coParser.startParse(xmlSource2);
                    } else if (reader == null) {
                        if (reader != null) {
                            if (!(this.m_incremental && incremental2)) {
                                reader.setContentHandler(this.m_defaultHandler);
                                reader.setDTDHandler(this.m_defaultHandler);
                                reader.setErrorHandler(this.m_defaultHandler);
                                try {
                                    reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                                } catch (Exception e4) {
                                }
                            }
                        }
                        releaseXMLReader(reader);
                        return sax2dtm;
                    } else {
                        reader.setContentHandler(sax2dtm);
                        reader.setDTDHandler(sax2dtm);
                        if (reader.getErrorHandler() == null) {
                            reader.setErrorHandler(sax2dtm);
                        }
                        try {
                            reader.setProperty("http://xml.org/sax/properties/lexical-handler", sax2dtm);
                        } catch (SAXNotRecognizedException | SAXNotSupportedException e5) {
                        }
                        reader.parse(xmlSource2);
                    }
                } catch (RuntimeException re) {
                    RuntimeException runtimeException = re;
                    sax2dtm.clearCoRoutine();
                    throw re;
                } catch (Exception e6) {
                    exception = e6;
                    sax2dtm.clearCoRoutine();
                    throw new WrappedRuntimeException(e6);
                } catch (RuntimeException re2) {
                    RuntimeException runtimeException2 = re2;
                    sax2dtm.clearCoRoutine();
                    throw re2;
                } catch (Exception e62) {
                    Exception exception2 = e62;
                    sax2dtm.clearCoRoutine();
                    throw new WrappedRuntimeException(e62);
                } catch (Exception e622) {
                    e622.printStackTrace();
                    coParser = null;
                } catch (Throwable th4) {
                    th222 = th4;
                    th = th222;
                    if (reader != null) {
                    }
                    releaseXMLReader(reader);
                    throw th;
                }
                if (reader != null) {
                    if (!(this.m_incremental && incremental2)) {
                        reader.setContentHandler(this.m_defaultHandler);
                        reader.setDTDHandler(this.m_defaultHandler);
                        reader.setErrorHandler(this.m_defaultHandler);
                        try {
                            reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                        } catch (Exception e7) {
                        }
                    }
                }
                releaseXMLReader(reader);
                return sax2dtm;
            }
            DOM2DTM dom2dtm = new DOM2DTM(this, (DOMSource) source2, documentID, whiteSpaceFilter, xstringFactory, doIndexing);
            addDTM(dom2dtm, dtmPos, 0);
            return dom2dtm;
        }
    }

    public synchronized int getDTMHandleFromNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NODE_NON_NULL, null));
        } else if (node instanceof DTMNodeProxy) {
            return ((DTMNodeProxy) node).getDTMNodeNumber();
        } else {
            int handle;
            for (DTM thisDTM : this.m_dtms) {
                if (thisDTM != null && (thisDTM instanceof DOM2DTM)) {
                    int handle2 = ((DOM2DTM) thisDTM).getHandleOfNode(node);
                    if (handle2 != -1) {
                        return handle2;
                    }
                }
            }
            Node root = node;
            Node p = root.getNodeType() == (short) 2 ? ((Attr) root).getOwnerElement() : root.getParentNode();
            while (p != null) {
                root = p;
                p = p.getParentNode();
            }
            DOM2DTM dtm = (DOM2DTM) getDTM(new DOMSource(root), false, null, true, true);
            if (node instanceof DOM2DTMdefaultNamespaceDeclarationNode) {
                handle = dtm.getAttributeNode(dtm.getHandleOfNode(((Attr) node).getOwnerElement()), node.getNamespaceURI(), node.getLocalName());
            } else {
                handle = dtm.getHandleOfNode(node);
            }
            if (-1 != handle) {
                return handle;
            }
            throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COULD_NOT_RESOLVE_NODE, null));
        }
    }

    public synchronized XMLReader getXMLReader(Source inputSource) {
        XMLReader reader;
        try {
            reader = inputSource instanceof SAXSource ? ((SAXSource) inputSource).getXMLReader() : null;
            if (reader == null) {
                if (this.m_readerManager == null) {
                    this.m_readerManager = XMLReaderManager.getInstance();
                }
                reader = this.m_readerManager.getXMLReader();
            }
        } catch (SAXException se) {
            throw new DTMException(se.getMessage(), se);
        }
        return reader;
    }

    public synchronized void releaseXMLReader(XMLReader reader) {
        if (this.m_readerManager != null) {
            this.m_readerManager.releaseXMLReader(reader);
        }
    }

    public synchronized DTM getDTM(int nodeHandle) {
        try {
        } catch (ArrayIndexOutOfBoundsException e) {
            if (nodeHandle == -1) {
                return null;
            }
            throw e;
        }
        return this.m_dtms[nodeHandle >>> 16];
    }

    public synchronized int getDTMIdentity(DTM dtm) {
        int i = 0;
        if (dtm instanceof DTMDefaultBase) {
            DTMDefaultBase dtmdb = (DTMDefaultBase) dtm;
            if (dtmdb.getManager() != this) {
                return -1;
            }
            return dtmdb.getDTMIDs().elementAt(0);
        }
        int n = this.m_dtms.length;
        while (i < n) {
            if (this.m_dtms[i] == dtm && this.m_dtm_offsets[i] == 0) {
                return i << 16;
            }
            i++;
        }
        return -1;
    }

    public synchronized boolean release(DTM dtm, boolean shouldHardDelete) {
        if (dtm instanceof SAX2DTM) {
            ((SAX2DTM) dtm).clearCoRoutine();
        }
        if (dtm instanceof DTMDefaultBase) {
            SuballocatedIntVector ids = ((DTMDefaultBase) dtm).getDTMIDs();
            for (int i = ids.size() - 1; i >= 0; i--) {
                this.m_dtms[ids.elementAt(i) >>> 16] = null;
            }
        } else {
            int i2 = getDTMIdentity(dtm);
            if (i2 >= 0) {
                this.m_dtms[i2 >>> 16] = null;
            }
        }
        dtm.documentRelease();
        return true;
    }

    public synchronized DTM createDocumentFragment() {
        DocumentBuilderFactory dbf;
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
        } catch (Exception e) {
            throw new DTMException(e);
        }
        return getDTM(new DOMSource(dbf.newDocumentBuilder().newDocument().createDocumentFragment()), true, null, false, false);
    }

    public synchronized DTMIterator createDTMIterator(int whatToShow, DTMFilter filter, boolean entityReferenceExpansion) {
        return null;
    }

    public synchronized DTMIterator createDTMIterator(String xpathString, PrefixResolver presolver) {
        return null;
    }

    public synchronized DTMIterator createDTMIterator(int node) {
        return null;
    }

    public synchronized DTMIterator createDTMIterator(Object xpathCompiler, int pos) {
        return null;
    }

    public ExpandedNameTable getExpandedNameTable(DTM dtm) {
        return this.m_expandedNameTable;
    }
}
