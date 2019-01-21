package org.apache.xml.dtm.ref.dom2dtm;

import java.util.Vector;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.dom.DOMSource;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.DTMDefaultBaseIterators;
import org.apache.xml.dtm.ref.DTMManagerDefault;
import org.apache.xml.dtm.ref.ExpandedNameTable;
import org.apache.xml.dtm.ref.IncrementalSAXSource;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.StringBufferPool;
import org.apache.xml.utils.SuballocatedIntVector;
import org.apache.xml.utils.TreeWalker;
import org.apache.xml.utils.XMLCharacterRecognizer;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public class DOM2DTM extends DTMDefaultBaseIterators {
    static final boolean JJK_DEBUG = false;
    static final boolean JJK_NEWCODE = true;
    static final String NAMESPACE_DECL_NS = "http://www.w3.org/XML/1998/namespace";
    private int m_last_kid = -1;
    private int m_last_parent = 0;
    protected Vector m_nodes = new Vector();
    private transient boolean m_nodesAreProcessed;
    private transient Node m_pos;
    boolean m_processedFirstElement = false;
    private transient Node m_root;
    TreeWalker m_walker = new TreeWalker(null);

    public interface CharacterNodeHandler {
        void characters(Node node) throws SAXException;
    }

    public DOM2DTM(DTMManager mgr, DOMSource domSource, int dtmIdentity, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory, boolean doIndexing) {
        super(mgr, domSource, dtmIdentity, whiteSpaceFilter, xstringfactory, doIndexing);
        Node node = domSource.getNode();
        this.m_root = node;
        this.m_pos = node;
        this.m_last_kid = -1;
        this.m_last_parent = -1;
        this.m_last_kid = addNode(this.m_root, this.m_last_parent, this.m_last_kid, -1);
        if ((short) 1 == this.m_root.getNodeType()) {
            NamedNodeMap attrs = this.m_root.getAttributes();
            int attrsize = attrs == null ? 0 : attrs.getLength();
            if (attrsize > 0) {
                int attrIndex = -1;
                for (int i = 0; i < attrsize; i++) {
                    attrIndex = addNode(attrs.item(i), 0, attrIndex, -1);
                    this.m_firstch.setElementAt(-1, attrIndex);
                }
                this.m_nextsib.setElementAt(-1, attrIndex);
            }
        }
        this.m_nodesAreProcessed = false;
    }

    protected int addNode(Node node, int parentIndex, int previousSibling, int forceNodeType) {
        int id;
        int type;
        String localName;
        int nodeIndex = this.m_nodes.size();
        if (this.m_dtmIdent.size() == (nodeIndex >>> 16)) {
            try {
                if (this.m_mgr != null) {
                    DTMManagerDefault mgrD = this.m_mgr;
                    id = mgrD.getFirstFreeDTMID();
                    mgrD.addDTM(this, id, nodeIndex);
                    this.m_dtmIdent.addElement(id << 16);
                } else {
                    throw new ClassCastException();
                }
            } catch (ClassCastException e) {
                error(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
            }
        }
        this.m_size++;
        if (-1 == forceNodeType) {
            type = node.getNodeType();
        } else {
            type = forceNodeType;
        }
        if (2 == type) {
            String name = node.getNodeName();
            if (name.startsWith(Constants.ATTRNAME_XMLNS) || name.equals("xmlns")) {
                type = 13;
            }
        }
        this.m_nodes.addElement(node);
        this.m_firstch.setElementAt(-2, nodeIndex);
        this.m_nextsib.setElementAt(-2, nodeIndex);
        this.m_prevsib.setElementAt(previousSibling, nodeIndex);
        this.m_parent.setElementAt(parentIndex, nodeIndex);
        if (!(-1 == parentIndex || type == 2 || type == 13 || -2 != this.m_firstch.elementAt(parentIndex))) {
            this.m_firstch.setElementAt(nodeIndex, parentIndex);
        }
        String nsURI = node.getNamespaceURI();
        if (type == 7) {
            localName = node.getNodeName();
        } else {
            localName = node.getLocalName();
        }
        if ((type == 1 || type == 2) && localName == null) {
            localName = node.getNodeName();
        }
        ExpandedNameTable exnt = this.m_expandedNameTable;
        if (!(node.getLocalName() == null && type == 1)) {
        }
        if (localName != null) {
            id = exnt.getExpandedTypeID(nsURI, localName, type);
        } else {
            id = exnt.getExpandedTypeID(type);
        }
        this.m_exptype.setElementAt(id, nodeIndex);
        indexNode(id, nodeIndex);
        if (-1 != previousSibling) {
            this.m_nextsib.setElementAt(nodeIndex, previousSibling);
        }
        if (type == 13) {
            declareNamespaceInContext(parentIndex, nodeIndex);
        }
        return nodeIndex;
    }

    public int getNumberOfNodes() {
        return this.m_nodes.size();
    }

    protected boolean nextNode() {
        int i = 0;
        if (this.m_nodesAreProcessed) {
            return false;
        }
        int i2;
        int i3 = -1;
        Node next = null;
        Node pos = this.m_pos;
        int nexttype = -1;
        do {
            if (pos.hasChildNodes()) {
                next = pos.getFirstChild();
                if (next != null && (short) 10 == next.getNodeType()) {
                    next = next.getNextSibling();
                }
                if ((short) 5 != pos.getNodeType()) {
                    this.m_last_parent = this.m_last_kid;
                    this.m_last_kid = -1;
                    if (this.m_wsfilter != null) {
                        short wsv = this.m_wsfilter.getShouldStripSpace(makeNodeHandle(this.m_last_parent), this);
                        boolean shouldStrip = (short) 3 == wsv ? getShouldStripWhitespace() : (short) 2 == wsv;
                        pushShouldStripWhitespace(shouldStrip);
                    }
                }
            } else {
                if (this.m_last_kid != -1 && this.m_firstch.elementAt(this.m_last_kid) == -2) {
                    this.m_firstch.setElementAt(-1, this.m_last_kid);
                }
                while (this.m_last_parent != -1) {
                    next = pos.getNextSibling();
                    if (next != null && (short) 10 == next.getNodeType()) {
                        next = next.getNextSibling();
                    }
                    if (next != null) {
                        break;
                    }
                    pos = pos.getParentNode();
                    if (pos == null || (short) 5 != pos.getNodeType()) {
                        popShouldStripWhitespace();
                        if (this.m_last_kid == -1) {
                            this.m_firstch.setElementAt(-1, this.m_last_parent);
                        } else {
                            this.m_nextsib.setElementAt(-1, this.m_last_kid);
                        }
                        SuballocatedIntVector suballocatedIntVector = this.m_parent;
                        i2 = this.m_last_parent;
                        this.m_last_kid = i2;
                        this.m_last_parent = suballocatedIntVector.elementAt(i2);
                    }
                }
                if (this.m_last_parent == -1) {
                    next = null;
                }
            }
            if (next != null) {
                nexttype = next.getNodeType();
            }
            if (5 == nexttype) {
                pos = next;
                continue;
            }
        } while (5 == nexttype);
        if (next == null) {
            this.m_nextsib.setElementAt(-1, 0);
            this.m_nodesAreProcessed = true;
            this.m_pos = null;
            return false;
        }
        boolean suppressNode = false;
        Node lastTextNode = null;
        nexttype = next.getNodeType();
        if (3 == nexttype || 4 == nexttype) {
            boolean suppressNode2 = this.m_wsfilter != null && getShouldStripWhitespace();
            int nexttype2 = nexttype;
            for (Node n = next; n != null; n = logicalNextDOMTextNode(n)) {
                lastTextNode = n;
                if ((short) 3 == n.getNodeType()) {
                    nexttype2 = 3;
                }
                suppressNode2 &= XMLCharacterRecognizer.isWhiteSpace(n.getNodeValue());
            }
            nexttype = nexttype2;
            suppressNode = suppressNode2;
        } else if (7 == nexttype) {
            suppressNode = pos.getNodeName().toLowerCase().equals("xml");
        }
        if (!suppressNode) {
            i2 = addNode(next, this.m_last_parent, this.m_last_kid, nexttype);
            this.m_last_kid = i2;
            if (1 == nexttype) {
                int attrIndex = -1;
                NamedNodeMap attrs = next.getAttributes();
                int attrsize = attrs == null ? 0 : attrs.getLength();
                if (attrsize > 0) {
                    while (i < attrsize) {
                        attrIndex = addNode(attrs.item(i), i2, attrIndex, -1);
                        this.m_firstch.setElementAt(-1, attrIndex);
                        if (!this.m_processedFirstElement && "xmlns:xml".equals(attrs.item(i).getNodeName())) {
                            this.m_processedFirstElement = true;
                        }
                        i++;
                    }
                }
                if (!this.m_processedFirstElement) {
                    i3 = -1;
                    attrIndex = addNode(new DOM2DTMdefaultNamespaceDeclarationNode((Element) next, "xml", "http://www.w3.org/XML/1998/namespace", makeNodeHandle((attrIndex == -1 ? i2 : attrIndex) + 1)), i2, attrIndex, -1);
                    this.m_firstch.setElementAt(-1, attrIndex);
                    this.m_processedFirstElement = true;
                }
                if (attrIndex != i3) {
                    this.m_nextsib.setElementAt(i3, attrIndex);
                }
            }
        }
        if (3 == nexttype || 4 == nexttype) {
            next = lastTextNode;
        }
        this.m_pos = next;
        return true;
    }

    public Node getNode(int nodeHandle) {
        return (Node) this.m_nodes.elementAt(makeNodeIdentity(nodeHandle));
    }

    protected Node lookupNode(int nodeIdentity) {
        return (Node) this.m_nodes.elementAt(nodeIdentity);
    }

    protected int getNextNodeIdentity(int identity) {
        identity++;
        if (identity < this.m_nodes.size() || nextNode()) {
            return identity;
        }
        return -1;
    }

    private int getHandleFromNode(Node node) {
        if (node != null) {
            int len = this.m_nodes.size();
            int i = 0;
            while (true) {
                if (i >= len) {
                    boolean isMore = nextNode();
                    len = this.m_nodes.size();
                    if (!isMore && i >= len) {
                        break;
                    }
                } else if (this.m_nodes.elementAt(i) == node) {
                    return makeNodeHandle(i);
                } else {
                    i++;
                }
            }
        }
        return -1;
    }

    public int getHandleOfNode(Node node) {
        if (node != null && (this.m_root == node || ((this.m_root.getNodeType() == (short) 9 && this.m_root == node.getOwnerDocument()) || (this.m_root.getNodeType() != (short) 9 && this.m_root.getOwnerDocument() == node.getOwnerDocument())))) {
            Node cursor = node;
            while (cursor != null) {
                if (cursor == this.m_root) {
                    return getHandleFromNode(node);
                }
                Node parentNode;
                if (cursor.getNodeType() != (short) 2) {
                    parentNode = cursor.getParentNode();
                } else {
                    parentNode = ((Attr) cursor).getOwnerElement();
                }
                cursor = parentNode;
            }
        }
        return -1;
    }

    public int getAttributeNode(int nodeHandle, String namespaceURI, String name) {
        if (namespaceURI == null) {
            namespaceURI = "";
        }
        if (1 == getNodeType(nodeHandle)) {
            int identity = makeNodeIdentity(nodeHandle);
            while (true) {
                int nextNodeIdentity = getNextNodeIdentity(identity);
                identity = nextNodeIdentity;
                if (-1 == nextNodeIdentity) {
                    break;
                }
                int type = _type(identity);
                if (type != 2 && type != 13) {
                    break;
                }
                Node node = lookupNode(identity);
                String nodeuri = node.getNamespaceURI();
                if (nodeuri == null) {
                    nodeuri = "";
                }
                String nodelocalname = node.getLocalName();
                if (nodeuri.equals(namespaceURI) && name.equals(nodelocalname)) {
                    return makeNodeHandle(identity);
                }
            }
        }
        return -1;
    }

    public XMLString getStringValue(int nodeHandle) {
        int type = getNodeType(nodeHandle);
        Node node = getNode(nodeHandle);
        FastStringBuffer buf;
        String s;
        if (1 == type || 9 == type || 11 == type) {
            buf = StringBufferPool.get();
            try {
                getNodeData(node, buf);
                s = buf.length() > 0 ? buf.toString() : "";
                StringBufferPool.free(buf);
                return this.m_xstrf.newstr(s);
            } catch (Throwable th) {
                StringBufferPool.free(buf);
            }
        } else if (3 != type && 4 != type) {
            return this.m_xstrf.newstr(node.getNodeValue());
        } else {
            buf = StringBufferPool.get();
            while (node != null) {
                buf.append(node.getNodeValue());
                node = logicalNextDOMTextNode(node);
            }
            s = buf.length() > 0 ? buf.toString() : "";
            StringBufferPool.free(buf);
            return this.m_xstrf.newstr(s);
        }
    }

    public boolean isWhitespace(int nodeHandle) {
        int type = getNodeType(nodeHandle);
        Node node = getNode(nodeHandle);
        if (3 != type && 4 != type) {
            return false;
        }
        FastStringBuffer buf = StringBufferPool.get();
        while (node != null) {
            buf.append(node.getNodeValue());
            node = logicalNextDOMTextNode(node);
        }
        boolean b = buf.isWhitespace(0, buf.length());
        StringBufferPool.free(buf);
        return b;
    }

    protected static void getNodeData(Node node, FastStringBuffer buf) {
        short nodeType = node.getNodeType();
        if (nodeType != (short) 7) {
            if (!(nodeType == (short) 9 || nodeType == (short) 11)) {
                switch (nodeType) {
                    case (short) 1:
                        break;
                    case (short) 2:
                    case (short) 3:
                    case (short) 4:
                        buf.append(node.getNodeValue());
                        return;
                    default:
                        return;
                }
            }
            for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                getNodeData(child, buf);
            }
        }
    }

    public String getNodeName(int nodeHandle) {
        return getNode(nodeHandle).getNodeName();
    }

    public String getNodeNameX(int nodeHandle) {
        short type = getNodeType(nodeHandle);
        if (!(type == (short) 5 || type == (short) 7)) {
            if (type != (short) 13) {
                switch (type) {
                    case (short) 1:
                    case (short) 2:
                        break;
                    default:
                        return "";
                }
            }
            String name = getNode(nodeHandle).getNodeName();
            if (name.startsWith(Constants.ATTRNAME_XMLNS)) {
                name = QName.getLocalPart(name);
            } else if (name.equals("xmlns")) {
                return "";
            }
            return name;
        }
        return getNode(nodeHandle).getNodeName();
    }

    public String getLocalName(int nodeHandle) {
        int id = makeNodeIdentity(nodeHandle);
        if (-1 == id) {
            return null;
        }
        Node newnode = (Node) this.m_nodes.elementAt(id);
        String newname = newnode.getLocalName();
        if (newname == null) {
            String qname = newnode.getNodeName();
            if ('#' == qname.charAt(0)) {
                newname = "";
            } else {
                int index = qname.indexOf(58);
                newname = index < 0 ? qname : qname.substring(index + 1);
            }
        }
        return newname;
    }

    public String getPrefix(int nodeHandle) {
        short type = getNodeType(nodeHandle);
        String qname;
        int index;
        if (type != (short) 13) {
            switch (type) {
                case (short) 1:
                case (short) 2:
                    qname = getNode(nodeHandle).getNodeName();
                    index = qname.indexOf(58);
                    return index < 0 ? "" : qname.substring(0, index);
                default:
                    return "";
            }
        }
        qname = getNode(nodeHandle).getNodeName();
        index = qname.indexOf(58);
        return index < 0 ? "" : qname.substring(index + 1);
    }

    public String getNamespaceURI(int nodeHandle) {
        int id = makeNodeIdentity(nodeHandle);
        if (id == -1) {
            return null;
        }
        return ((Node) this.m_nodes.elementAt(id)).getNamespaceURI();
    }

    private Node logicalNextDOMTextNode(Node n) {
        Node p = n.getNextSibling();
        if (p == null) {
            n = n.getParentNode();
            while (n != null && (short) 5 == n.getNodeType()) {
                p = n.getNextSibling();
                if (p != null) {
                    break;
                }
                n = n.getParentNode();
            }
        }
        n = p;
        while (n != null && (short) 5 == n.getNodeType()) {
            if (n.hasChildNodes()) {
                n = n.getFirstChild();
            } else {
                n = n.getNextSibling();
            }
        }
        if (n == null) {
            return n;
        }
        int ntype = n.getNodeType();
        if (3 == ntype || 4 == ntype) {
            return n;
        }
        return null;
    }

    public String getNodeValue(int nodeHandle) {
        short s = (short) -1;
        if (-1 != _exptype(makeNodeIdentity(nodeHandle))) {
            s = getNodeType(nodeHandle);
        }
        short type = s;
        if ((short) 3 != type && (short) 4 != type) {
            return getNode(nodeHandle).getNodeValue();
        }
        Node node = getNode(nodeHandle);
        Node n = logicalNextDOMTextNode(node);
        if (n == null) {
            return node.getNodeValue();
        }
        FastStringBuffer buf = StringBufferPool.get();
        buf.append(node.getNodeValue());
        while (n != null) {
            buf.append(n.getNodeValue());
            n = logicalNextDOMTextNode(n);
        }
        String s2 = buf.length() > 0 ? buf.toString() : "";
        StringBufferPool.free(buf);
        return s2;
    }

    public String getDocumentTypeDeclarationSystemIdentifier() {
        Document doc;
        if (this.m_root.getNodeType() == (short) 9) {
            doc = this.m_root;
        } else {
            doc = this.m_root.getOwnerDocument();
        }
        if (doc != null) {
            DocumentType dtd = doc.getDoctype();
            if (dtd != null) {
                return dtd.getSystemId();
            }
        }
        return null;
    }

    public String getDocumentTypeDeclarationPublicIdentifier() {
        Document doc;
        if (this.m_root.getNodeType() == (short) 9) {
            doc = this.m_root;
        } else {
            doc = this.m_root.getOwnerDocument();
        }
        if (doc != null) {
            DocumentType dtd = doc.getDoctype();
            if (dtd != null) {
                return dtd.getPublicId();
            }
        }
        return null;
    }

    public int getElementById(String elementId) {
        Document doc = this.m_root.getNodeType() == (short) 9 ? (Document) this.m_root : this.m_root.getOwnerDocument();
        if (doc != null) {
            Node elem = doc.getElementById(elementId);
            if (elem != null) {
                int elemHandle = getHandleFromNode(elem);
                if (-1 == elemHandle) {
                    int identity = this.m_nodes.size() - 1;
                    while (true) {
                        int nextNodeIdentity = getNextNodeIdentity(identity);
                        identity = nextNodeIdentity;
                        if (-1 == nextNodeIdentity) {
                            break;
                        } else if (getNode(identity) == elem) {
                            elemHandle = getHandleFromNode(elem);
                            break;
                        }
                    }
                }
                return elemHandle;
            }
        }
        return -1;
    }

    public String getUnparsedEntityURI(String name) {
        String url = "";
        Document doc = this.m_root.getNodeType() == (short) 9 ? (Document) this.m_root : this.m_root.getOwnerDocument();
        if (doc != null) {
            DocumentType doctype = doc.getDoctype();
            if (doctype != null) {
                NamedNodeMap entities = doctype.getEntities();
                if (entities == null) {
                    return url;
                }
                Entity entity = (Entity) entities.getNamedItem(name);
                if (entity == null) {
                    return url;
                }
                if (entity.getNotationName() != null) {
                    url = entity.getSystemId();
                    if (url == null) {
                        url = entity.getPublicId();
                    }
                }
            }
        }
        return url;
    }

    public boolean isAttributeSpecified(int attributeHandle) {
        if (2 == getNodeType(attributeHandle)) {
            return ((Attr) getNode(attributeHandle)).getSpecified();
        }
        return false;
    }

    public void setIncrementalSAXSource(IncrementalSAXSource source) {
    }

    public ContentHandler getContentHandler() {
        return null;
    }

    public LexicalHandler getLexicalHandler() {
        return null;
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
        return false;
    }

    private static boolean isSpace(char ch) {
        return XMLCharacterRecognizer.isWhiteSpace(ch);
    }

    public void dispatchCharactersEvents(int nodeHandle, ContentHandler ch, boolean normalize) throws SAXException {
        if (normalize) {
            getStringValue(nodeHandle).fixWhiteSpace(true, true, false).dispatchCharactersEvents(ch);
            return;
        }
        int type = getNodeType(nodeHandle);
        Node node = getNode(nodeHandle);
        dispatchNodeData(node, ch, 0);
        if (3 == type || 4 == type) {
            while (true) {
                Node logicalNextDOMTextNode = logicalNextDOMTextNode(node);
                node = logicalNextDOMTextNode;
                if (logicalNextDOMTextNode != null) {
                    dispatchNodeData(node, ch, 0);
                } else {
                    return;
                }
            }
        }
    }

    protected static void dispatchNodeData(Node node, ContentHandler ch, int depth) throws SAXException {
        switch (node.getNodeType()) {
            case (short) 1:
            case (short) 9:
            case (short) 11:
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    dispatchNodeData(child, ch, depth + 1);
                }
                return;
            case (short) 2:
            case (short) 3:
            case (short) 4:
                break;
            case (short) 7:
            case (short) 8:
                if (depth != 0) {
                    return;
                }
                break;
            default:
                return;
        }
        String str = node.getNodeValue();
        if (ch instanceof CharacterNodeHandler) {
            ((CharacterNodeHandler) ch).characters(node);
        } else {
            ch.characters(str.toCharArray(), 0, str.length());
        }
    }

    public void dispatchToEvents(int nodeHandle, ContentHandler ch) throws SAXException {
        TreeWalker treeWalker = this.m_walker;
        if (treeWalker.getContentHandler() != null) {
            treeWalker = new TreeWalker(null);
        }
        treeWalker.setContentHandler(ch);
        try {
            treeWalker.traverseFragment(getNode(nodeHandle));
        } finally {
            treeWalker.setContentHandler(null);
        }
    }

    public void setProperty(String property, Object value) {
    }

    public SourceLocator getSourceLocatorFor(int node) {
        return null;
    }
}
