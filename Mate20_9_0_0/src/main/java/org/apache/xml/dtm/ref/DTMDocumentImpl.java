package org.apache.xml.dtm.ref;

import java.io.PrintStream;
import javax.xml.transform.SourceLocator;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.compiler.PsuedoNames;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public class DTMDocumentImpl implements DTM, ContentHandler, LexicalHandler {
    protected static final int DOCHANDLE_MASK = -8388608;
    protected static final byte DOCHANDLE_SHIFT = (byte) 22;
    protected static final int NODEHANDLE_MASK = 8388607;
    private static final String[] fixednames = new String[]{null, null, null, PsuedoNames.PSEUDONAME_TEXT, "#cdata_section", null, null, null, PsuedoNames.PSEUDONAME_COMMENT, "#document", null, "#document-fragment", null};
    private final boolean DEBUG = false;
    int currentParent = 0;
    private boolean done = false;
    int[] gotslot = new int[4];
    private FastStringBuffer m_char = new FastStringBuffer();
    private int m_char_current_start = 0;
    protected int m_currentNode = -1;
    int m_docElement = -1;
    int m_docHandle = -1;
    protected String m_documentBaseURI;
    private ExpandedNameTable m_expandedNames = new ExpandedNameTable();
    private IncrementalSAXSource m_incrSAXSource = null;
    boolean m_isError = false;
    private DTMStringPool m_localNames = new DTMStringPool();
    private DTMStringPool m_nsNames = new DTMStringPool();
    private DTMStringPool m_prefixNames = new DTMStringPool();
    private XMLStringFactory m_xsf;
    ChunkedIntArray nodes = new ChunkedIntArray(4);
    int previousSibling = 0;
    private boolean previousSiblingWasParent = false;

    public DTMDocumentImpl(DTMManager mgr, int documentNumber, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory) {
        initDocument(documentNumber);
        this.m_xsf = xstringfactory;
    }

    public void setIncrementalSAXSource(IncrementalSAXSource source) {
        this.m_incrSAXSource = source;
        source.setContentHandler(this);
        source.setLexicalHandler(this);
    }

    private final int appendNode(int w0, int w1, int w2, int w3) {
        int slotnumber = this.nodes.appendSlot(w0, w1, w2, w3);
        if (this.previousSiblingWasParent) {
            this.nodes.writeEntry(this.previousSibling, 2, slotnumber);
        }
        this.previousSiblingWasParent = false;
        return slotnumber;
    }

    public void setFeature(String featureId, boolean state) {
    }

    public void setLocalNameTable(DTMStringPool poolRef) {
        this.m_localNames = poolRef;
    }

    public DTMStringPool getLocalNameTable() {
        return this.m_localNames;
    }

    public void setNsNameTable(DTMStringPool poolRef) {
        this.m_nsNames = poolRef;
    }

    public DTMStringPool getNsNameTable() {
        return this.m_nsNames;
    }

    public void setPrefixNameTable(DTMStringPool poolRef) {
        this.m_prefixNames = poolRef;
    }

    public DTMStringPool getPrefixNameTable() {
        return this.m_prefixNames;
    }

    void setContentBuffer(FastStringBuffer buffer) {
        this.m_char = buffer;
    }

    FastStringBuffer getContentBuffer() {
        return this.m_char;
    }

    public ContentHandler getContentHandler() {
        if (this.m_incrSAXSource instanceof IncrementalSAXSource_Filter) {
            return (ContentHandler) this.m_incrSAXSource;
        }
        return this;
    }

    public LexicalHandler getLexicalHandler() {
        if (this.m_incrSAXSource instanceof IncrementalSAXSource_Filter) {
            return (LexicalHandler) this.m_incrSAXSource;
        }
        return this;
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public DTDHandler getDTDHandler() {
        return null;
    }

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public DeclHandler getDeclHandler() {
        return null;
    }

    public boolean needsTwoThreads() {
        return this.m_incrSAXSource != null;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        this.m_char.append(ch, start, length);
    }

    private void processAccumulatedText() {
        int len = this.m_char.length();
        if (len != this.m_char_current_start) {
            appendTextChild(this.m_char_current_start, len - this.m_char_current_start);
            this.m_char_current_start = len;
        }
    }

    public void endDocument() throws SAXException {
        appendEndDocument();
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        processAccumulatedText();
        appendEndElement();
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
        processAccumulatedText();
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void skippedEntity(String name) throws SAXException {
        processAccumulatedText();
    }

    public void startDocument() throws SAXException {
        appendStartDocument();
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        int i;
        String qName2 = qName;
        Attributes attributes = atts;
        processAccumulatedText();
        String prefix = null;
        int i2 = 58;
        int colon = qName2.indexOf(58);
        int i3 = 0;
        if (colon > 0) {
            prefix = qName2.substring(0, colon);
        }
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Prefix=");
        stringBuilder.append(prefix);
        stringBuilder.append(" index=");
        stringBuilder.append(this.m_prefixNames.stringToIndex(prefix));
        printStream.println(stringBuilder.toString());
        appendStartElement(this.m_nsNames.stringToIndex(namespaceURI), this.m_localNames.stringToIndex(localName), this.m_prefixNames.stringToIndex(prefix));
        int nAtts = attributes == null ? 0 : atts.getLength();
        for (i = nAtts - 1; i >= 0; i--) {
            qName2 = attributes.getQName(i);
            if (qName2.startsWith(Constants.ATTRNAME_XMLNS) || "xmlns".equals(qName2)) {
                colon = qName2.indexOf(58);
                if (colon > 0) {
                    prefix = qName2.substring(0, colon);
                } else {
                    prefix = null;
                }
                appendNSDeclaration(this.m_prefixNames.stringToIndex(prefix), this.m_nsNames.stringToIndex(attributes.getValue(i)), attributes.getType(i).equalsIgnoreCase("ID"));
            }
        }
        i = nAtts - 1;
        while (true) {
            int i4 = i;
            if (i4 >= 0) {
                String qName3 = attributes.getQName(i4);
                if (!(qName3.startsWith(Constants.ATTRNAME_XMLNS) || "xmlns".equals(qName3))) {
                    int contentEnd;
                    String localName2;
                    int colon2 = qName3.indexOf(i2);
                    if (colon2 > 0) {
                        qName2 = qName3.substring(i3, colon2);
                        prefix = qName3.substring(colon2 + 1);
                    } else {
                        qName2 = "";
                        prefix = qName3;
                    }
                    String prefix2 = qName2;
                    String localName3 = prefix;
                    this.m_char.append(attributes.getValue(i4));
                    int contentEnd2 = this.m_char.length();
                    String prefix3;
                    if ("xmlns".equals(prefix2) || "xmlns".equals(qName3)) {
                        contentEnd = contentEnd2;
                        localName2 = localName3;
                        prefix3 = prefix2;
                    } else {
                        int stringToIndex = this.m_nsNames.stringToIndex(attributes.getURI(i4));
                        colon = this.m_localNames.stringToIndex(localName3);
                        i = this.m_prefixNames.stringToIndex(prefix2);
                        boolean equalsIgnoreCase = attributes.getType(i4).equalsIgnoreCase("ID");
                        i3 = contentEnd2 - this.m_char_current_start;
                        contentEnd = contentEnd2;
                        boolean z = equalsIgnoreCase;
                        localName2 = localName3;
                        prefix3 = prefix2;
                        appendAttribute(stringToIndex, colon, i, z, this.m_char_current_start, i3);
                    }
                    this.m_char_current_start = contentEnd;
                    localName3 = localName2;
                    colon = colon2;
                }
                i = i4 - 1;
                attributes = atts;
                i2 = 58;
                i3 = 0;
            } else {
                return;
            }
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        processAccumulatedText();
        this.m_char.append(ch, start, length);
        appendComment(this.m_char_current_start, length);
        this.m_char_current_start += length;
    }

    public void endCDATA() throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    public void endEntity(String name) throws SAXException {
    }

    public void startCDATA() throws SAXException {
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }

    public void startEntity(String name) throws SAXException {
    }

    final void initDocument(int documentNumber) {
        this.m_docHandle = documentNumber << 22;
        this.nodes.writeSlot(0, 9, -1, -1, 0);
        this.done = false;
    }

    public boolean hasChildNodes(int nodeHandle) {
        return getFirstChild(nodeHandle) != -1;
    }

    public int getFirstChild(int nodeHandle) {
        nodeHandle &= NODEHANDLE_MASK;
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & (short) -1);
        if (type == (short) 1 || type == (short) 9 || type == (short) 5) {
            int kid = nodeHandle + 1;
            this.nodes.readSlot(kid, this.gotslot);
            while (2 == (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT)) {
                kid = this.gotslot[2];
                if (kid == -1) {
                    return -1;
                }
                this.nodes.readSlot(kid, this.gotslot);
            }
            if (this.gotslot[1] == nodeHandle) {
                return this.m_docHandle | kid;
            }
        }
        return -1;
    }

    public int getLastChild(int nodeHandle) {
        int lastChild = -1;
        int nextkid = getFirstChild(nodeHandle & NODEHANDLE_MASK);
        while (nextkid != -1) {
            lastChild = nextkid;
            nextkid = getNextSibling(nextkid);
        }
        return this.m_docHandle | lastChild;
    }

    public int getAttributeNode(int nodeHandle, String namespaceURI, String name) {
        int nsIndex = this.m_nsNames.stringToIndex(namespaceURI);
        int nameIndex = this.m_localNames.stringToIndex(name);
        nodeHandle &= NODEHANDLE_MASK;
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & (short) -1);
        if (type == (short) 1) {
            nodeHandle++;
        }
        while (type == (short) 2) {
            if (nsIndex == (this.gotslot[0] << 16) && this.gotslot[3] == nameIndex) {
                return this.m_docHandle | nodeHandle;
            }
            nodeHandle = this.gotslot[2];
            this.nodes.readSlot(nodeHandle, this.gotslot);
        }
        return -1;
    }

    public int getFirstAttribute(int nodeHandle) {
        nodeHandle &= NODEHANDLE_MASK;
        int i = -1;
        if (1 != (this.nodes.readEntry(nodeHandle, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
            return -1;
        }
        nodeHandle++;
        if (2 == (this.nodes.readEntry(nodeHandle, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
            i = nodeHandle | this.m_docHandle;
        }
        return i;
    }

    public int getFirstNamespaceNode(int nodeHandle, boolean inScope) {
        return -1;
    }

    public int getNextSibling(int nodeHandle) {
        nodeHandle &= NODEHANDLE_MASK;
        if (nodeHandle == 0) {
            return -1;
        }
        int nextSib;
        short type = (short) (this.nodes.readEntry(nodeHandle, 0) & (short) -1);
        if (type == (short) 1 || type == (short) 2 || type == (short) 5) {
            nextSib = this.nodes.readEntry(nodeHandle, 2);
            if (nextSib == -1) {
                return -1;
            }
            if (nextSib != 0) {
                return this.m_docHandle | nextSib;
            }
        }
        nextSib = this.nodes.readEntry(nodeHandle, 1);
        nodeHandle++;
        if (this.nodes.readEntry(nodeHandle, 1) == nextSib) {
            return this.m_docHandle | nodeHandle;
        }
        return -1;
    }

    public int getPreviousSibling(int nodeHandle) {
        nodeHandle &= NODEHANDLE_MASK;
        if (nodeHandle == 0) {
            return -1;
        }
        int kid = -1;
        int nextkid = getFirstChild(this.nodes.readEntry(nodeHandle, 1));
        while (nextkid != nodeHandle) {
            kid = nextkid;
            nextkid = getNextSibling(nextkid);
        }
        return this.m_docHandle | kid;
    }

    public int getNextAttribute(int nodeHandle) {
        nodeHandle &= NODEHANDLE_MASK;
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & (short) -1);
        if (type == (short) 1) {
            return getFirstAttribute(nodeHandle);
        }
        if (type != (short) 2 || this.gotslot[2] == -1) {
            return -1;
        }
        return this.m_docHandle | this.gotslot[2];
    }

    public int getNextNamespaceNode(int baseHandle, int namespaceHandle, boolean inScope) {
        return -1;
    }

    public int getNextDescendant(int subtreeRootHandle, int nodeHandle) {
        subtreeRootHandle &= NODEHANDLE_MASK;
        nodeHandle &= NODEHANDLE_MASK;
        if (nodeHandle == 0) {
            return -1;
        }
        while (!this.m_isError && (!this.done || nodeHandle <= this.nodes.slotsUsed())) {
            if (nodeHandle > subtreeRootHandle) {
                this.nodes.readSlot(nodeHandle + 1, this.gotslot);
                if (this.gotslot[2] == 0) {
                    if (this.done) {
                        break;
                    }
                } else if (((short) (this.gotslot[0] & (short) -1)) == (short) 2) {
                    nodeHandle += 2;
                } else if (this.gotslot[1] >= subtreeRootHandle) {
                    return this.m_docHandle | (nodeHandle + 1);
                }
            } else {
                nodeHandle++;
            }
        }
        return -1;
    }

    public int getNextFollowing(int axisContextHandle, int nodeHandle) {
        return -1;
    }

    public int getNextPreceding(int axisContextHandle, int nodeHandle) {
        nodeHandle &= NODEHANDLE_MASK;
        while (nodeHandle > 1) {
            nodeHandle--;
            if (2 != (this.nodes.readEntry(nodeHandle, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
                return this.m_docHandle | this.nodes.specialFind(axisContextHandle, nodeHandle);
            }
        }
        return -1;
    }

    public int getParent(int nodeHandle) {
        return this.m_docHandle | this.nodes.readEntry(nodeHandle, 1);
    }

    public int getDocumentRoot() {
        return this.m_docHandle | this.m_docElement;
    }

    public int getDocument() {
        return this.m_docHandle;
    }

    public int getOwnerDocument(int nodeHandle) {
        if ((NODEHANDLE_MASK & nodeHandle) == 0) {
            return -1;
        }
        return DOCHANDLE_MASK & nodeHandle;
    }

    public int getDocumentRoot(int nodeHandle) {
        if ((NODEHANDLE_MASK & nodeHandle) == 0) {
            return -1;
        }
        return DOCHANDLE_MASK & nodeHandle;
    }

    public XMLString getStringValue(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        int nodetype = this.gotslot[0] & WalkerFactory.BITS_COUNT;
        String value = null;
        if (nodetype != 8) {
            switch (nodetype) {
                case 3:
                case 4:
                    break;
            }
        }
        value = this.m_char.getString(this.gotslot[2], this.gotslot[3]);
        return this.m_xsf.newstr(value);
    }

    public int getStringValueChunkCount(int nodeHandle) {
        return 0;
    }

    public char[] getStringValueChunk(int nodeHandle, int chunkIndex, int[] startAndLen) {
        return new char[0];
    }

    public int getExpandedTypeID(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        String qName = this.m_localNames.indexToString(this.gotslot[3]);
        String localName = qName.substring(qName.indexOf(":") + 1);
        String namespace = this.m_nsNames.indexToString(this.gotslot[0] << 16);
        String expandedName = new StringBuilder();
        expandedName.append(namespace);
        expandedName.append(":");
        expandedName.append(localName);
        return this.m_nsNames.stringToIndex(expandedName.toString());
    }

    public int getExpandedTypeID(String namespace, String localName, int type) {
        String expandedName = new StringBuilder();
        expandedName.append(namespace);
        expandedName.append(":");
        expandedName.append(localName);
        return this.m_nsNames.stringToIndex(expandedName.toString());
    }

    public String getLocalNameFromExpandedNameID(int ExpandedNameID) {
        String expandedName = this.m_localNames.indexToString(ExpandedNameID);
        return expandedName.substring(expandedName.indexOf(":") + 1);
    }

    public String getNamespaceFromExpandedNameID(int ExpandedNameID) {
        String expandedName = this.m_localNames.indexToString(ExpandedNameID);
        return expandedName.substring(null, expandedName.indexOf(":"));
    }

    public String getNodeName(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        String name = fixednames[(short) (this.gotslot[0] & (short) -1)];
        if (name != null) {
            return name;
        }
        int i = this.gotslot[3];
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("got i=");
        stringBuilder.append(i);
        stringBuilder.append(" ");
        stringBuilder.append(i >> 16);
        stringBuilder.append(PsuedoNames.PSEUDONAME_ROOT);
        stringBuilder.append(i & DTMManager.IDENT_NODE_DEFAULT);
        printStream.println(stringBuilder.toString());
        name = this.m_localNames.indexToString(DTMManager.IDENT_NODE_DEFAULT & i);
        String prefix = this.m_prefixNames.indexToString(i >> 16);
        if (prefix == null || prefix.length() <= 0) {
            return name;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append(":");
        stringBuilder2.append(name);
        return stringBuilder2.toString();
    }

    public String getNodeNameX(int nodeHandle) {
        return null;
    }

    public String getLocalName(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & (short) -1);
        String name = "";
        if (type != (short) 1 && type != (short) 2) {
            return name;
        }
        name = this.m_localNames.indexToString(DTMManager.IDENT_NODE_DEFAULT & this.gotslot[3]);
        if (name == null) {
            return "";
        }
        return name;
    }

    public String getPrefix(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & (short) -1);
        String name = "";
        if (type != (short) 1 && type != (short) 2) {
            return name;
        }
        name = this.m_prefixNames.indexToString(this.gotslot[3] >> 16);
        if (name == null) {
            return "";
        }
        return name;
    }

    public String getNamespaceURI(int nodeHandle) {
        return null;
    }

    public String getNodeValue(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        int nodetype = this.gotslot[0] & WalkerFactory.BITS_COUNT;
        if (nodetype != 8) {
            switch (nodetype) {
                case 2:
                    this.nodes.readSlot(nodeHandle + 1, this.gotslot);
                    break;
                case 3:
                case 4:
                    break;
                default:
                    return null;
            }
        }
        return this.m_char.getString(this.gotslot[2], this.gotslot[3]);
    }

    public short getNodeType(int nodeHandle) {
        return (short) (this.nodes.readEntry(nodeHandle, 0) & DTMManager.IDENT_NODE_DEFAULT);
    }

    public short getLevel(int nodeHandle) {
        short count = (short) 0;
        while (nodeHandle != 0) {
            count = (short) (count + 1);
            nodeHandle = this.nodes.readEntry(nodeHandle, 1);
        }
        return count;
    }

    public boolean isSupported(String feature, String version) {
        return false;
    }

    public String getDocumentBaseURI() {
        return this.m_documentBaseURI;
    }

    public void setDocumentBaseURI(String baseURI) {
        this.m_documentBaseURI = baseURI;
    }

    public String getDocumentSystemIdentifier(int nodeHandle) {
        return null;
    }

    public String getDocumentEncoding(int nodeHandle) {
        return null;
    }

    public String getDocumentStandalone(int nodeHandle) {
        return null;
    }

    public String getDocumentVersion(int documentHandle) {
        return null;
    }

    public boolean getDocumentAllDeclarationsProcessed() {
        return false;
    }

    public String getDocumentTypeDeclarationSystemIdentifier() {
        return null;
    }

    public String getDocumentTypeDeclarationPublicIdentifier() {
        return null;
    }

    public int getElementById(String elementId) {
        return 0;
    }

    public String getUnparsedEntityURI(String name) {
        return null;
    }

    public boolean supportsPreStripping() {
        return false;
    }

    public boolean isNodeAfter(int nodeHandle1, int nodeHandle2) {
        return false;
    }

    public boolean isCharacterElementContentWhitespace(int nodeHandle) {
        return false;
    }

    public boolean isDocumentAllDeclarationsProcessed(int documentHandle) {
        return false;
    }

    public boolean isAttributeSpecified(int attributeHandle) {
        return false;
    }

    public void dispatchCharactersEvents(int nodeHandle, ContentHandler ch, boolean normalize) throws SAXException {
    }

    public void dispatchToEvents(int nodeHandle, ContentHandler ch) throws SAXException {
    }

    public Node getNode(int nodeHandle) {
        return null;
    }

    public void appendChild(int newChild, boolean clone, boolean cloneDepth) {
        if ((DOCHANDLE_MASK & newChild) != this.m_docHandle) {
        }
    }

    public void appendTextChild(String str) {
    }

    void appendTextChild(int m_char_current_start, int contentLength) {
        this.previousSibling = appendNode(3, this.currentParent, m_char_current_start, contentLength);
    }

    void appendComment(int m_char_current_start, int contentLength) {
        this.previousSibling = appendNode(8, this.currentParent, m_char_current_start, contentLength);
    }

    void appendStartElement(int namespaceIndex, int localNameIndex, int prefixIndex) {
        int w0 = (namespaceIndex << 16) | 1;
        int w1 = this.currentParent;
        int w3 = (prefixIndex << 16) | localNameIndex;
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set w3=");
        stringBuilder.append(w3);
        stringBuilder.append(" ");
        stringBuilder.append(w3 >> 16);
        stringBuilder.append(PsuedoNames.PSEUDONAME_ROOT);
        stringBuilder.append(DTMManager.IDENT_NODE_DEFAULT & w3);
        printStream.println(stringBuilder.toString());
        int ourslot = appendNode(w0, w1, 0, w3);
        this.currentParent = ourslot;
        this.previousSibling = 0;
        if (this.m_docElement == -1) {
            this.m_docElement = ourslot;
        }
    }

    void appendNSDeclaration(int prefixIndex, int namespaceIndex, boolean isID) {
        int namespaceForNamespaces = this.m_nsNames.stringToIndex(SerializerConstants.XMLNS_URI);
        this.previousSibling = appendNode((this.m_nsNames.stringToIndex(SerializerConstants.XMLNS_URI) << 16) | 13, this.currentParent, 0, namespaceIndex);
        this.previousSiblingWasParent = false;
    }

    void appendAttribute(int namespaceIndex, int localNameIndex, int prefixIndex, boolean isID, int m_char_current_start, int contentLength) {
        int w0 = (namespaceIndex << 16) | 2;
        int w1 = this.currentParent;
        int w3 = (prefixIndex << 16) | localNameIndex;
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set w3=");
        stringBuilder.append(w3);
        stringBuilder.append(" ");
        stringBuilder.append(w3 >> 16);
        stringBuilder.append(PsuedoNames.PSEUDONAME_ROOT);
        stringBuilder.append(DTMManager.IDENT_NODE_DEFAULT & w3);
        printStream.println(stringBuilder.toString());
        int ourslot = appendNode(w0, w1, 0, w3);
        this.previousSibling = ourslot;
        appendNode(3, ourslot, m_char_current_start, contentLength);
        this.previousSiblingWasParent = true;
    }

    public DTMAxisTraverser getAxisTraverser(int axis) {
        return null;
    }

    public DTMAxisIterator getAxisIterator(int axis) {
        return null;
    }

    public DTMAxisIterator getTypedAxisIterator(int axis, int type) {
        return null;
    }

    void appendEndElement() {
        if (this.previousSiblingWasParent) {
            this.nodes.writeEntry(this.previousSibling, 2, -1);
        }
        this.previousSibling = this.currentParent;
        this.nodes.readSlot(this.currentParent, this.gotslot);
        this.currentParent = this.gotslot[1] & DTMManager.IDENT_NODE_DEFAULT;
        this.previousSiblingWasParent = true;
    }

    void appendStartDocument() {
        this.m_docElement = -1;
        initDocument(0);
    }

    void appendEndDocument() {
        this.done = true;
    }

    public void setProperty(String property, Object value) {
    }

    public SourceLocator getSourceLocatorFor(int node) {
        return null;
    }

    public void documentRegistration() {
    }

    public void documentRelease() {
    }

    public void migrateTo(DTMManager manager) {
    }
}
