package org.apache.xml.utils;

import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.ref.DTMNodeProxy;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class DOMHelper {
    protected static final NSInfo m_NSInfoNullNoAncestorXMLNS = new NSInfo(true, false, 2);
    protected static final NSInfo m_NSInfoNullWithXMLNS = new NSInfo(true, true);
    protected static final NSInfo m_NSInfoNullWithoutXMLNS = new NSInfo(true, false);
    protected static final NSInfo m_NSInfoUnProcNoAncestorXMLNS = new NSInfo(false, false, 2);
    protected static final NSInfo m_NSInfoUnProcWithXMLNS = new NSInfo(false, true);
    protected static final NSInfo m_NSInfoUnProcWithoutXMLNS = new NSInfo(false, false);
    protected Document m_DOMFactory = null;
    Hashtable m_NSInfos = new Hashtable();
    protected Vector m_candidateNoAncestorXMLNS = new Vector();

    public static Document createDocument(boolean isSecureProcessing) {
        try {
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setNamespaceAware(true);
            return dfactory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CREATEDOCUMENT_NOT_SUPPORTED, null));
        }
    }

    public static Document createDocument() {
        return createDocument(false);
    }

    public boolean shouldStripSourceNode(Node textNode) throws TransformerException {
        return false;
    }

    public String getUniqueID(Node node) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("N");
        stringBuilder.append(Integer.toHexString(node.hashCode()).toUpperCase());
        return stringBuilder.toString();
    }

    public static boolean isNodeAfter(Node node1, Node node2) {
        boolean z = true;
        if (node1 == node2 || isNodeTheSame(node1, node2)) {
            return true;
        }
        boolean isNodeAfter = true;
        Node parent1 = getParentOfNode(node1);
        Node parent2 = getParentOfNode(node2);
        if (parent1 != parent2 && !isNodeTheSame(parent1, parent2)) {
            Node startNode2;
            int nParents1 = 2;
            int nParents2 = 2;
            while (parent1 != null) {
                nParents1++;
                parent1 = getParentOfNode(parent1);
            }
            while (parent2 != null) {
                nParents2++;
                parent2 = getParentOfNode(parent2);
            }
            Node startNode1 = node1;
            Node startNode22 = node2;
            int adjust;
            if (nParents1 < nParents2) {
                adjust = nParents2 - nParents1;
                startNode2 = startNode22;
                for (int i = 0; i < adjust; i++) {
                    startNode2 = getParentOfNode(startNode2);
                }
                startNode22 = startNode2;
            } else if (nParents1 > nParents2) {
                adjust = nParents1 - nParents2;
                startNode2 = startNode1;
                for (int i2 = 0; i2 < adjust; i2++) {
                    startNode2 = getParentOfNode(startNode2);
                }
                startNode1 = startNode2;
            }
            Node prevChild1 = null;
            startNode2 = null;
            while (startNode1 != null) {
                if (startNode1 != startNode22 && !isNodeTheSame(startNode1, startNode22)) {
                    prevChild1 = startNode1;
                    startNode1 = getParentOfNode(startNode1);
                    startNode2 = startNode22;
                    startNode22 = getParentOfNode(startNode22);
                } else if (prevChild1 == null) {
                    if (nParents1 >= nParents2) {
                        z = false;
                    }
                    isNodeAfter = z;
                } else {
                    isNodeAfter = isNodeAfterSibling(startNode1, prevChild1, startNode2);
                }
            }
        } else if (parent1 != null) {
            isNodeAfter = isNodeAfterSibling(parent1, node1, node2);
        }
        return isNodeAfter;
    }

    public static boolean isNodeTheSame(Node node1, Node node2) {
        if ((node1 instanceof DTMNodeProxy) && (node2 instanceof DTMNodeProxy)) {
            return ((DTMNodeProxy) node1).equals((DTMNodeProxy) node2);
        }
        return node1 == node2;
    }

    private static boolean isNodeAfterSibling(Node parent, Node child1, Node child2) {
        short child1type = child1.getNodeType();
        short child2type = child2.getNodeType();
        if ((short) 2 != child1type && (short) 2 == child2type) {
            return false;
        }
        if ((short) 2 == child1type && (short) 2 != child2type) {
            return true;
        }
        boolean found2 = false;
        boolean nNodes;
        if ((short) 2 == child1type) {
            NamedNodeMap children = parent.getAttributes();
            nNodes = children.getLength();
            boolean found1 = false;
            boolean found22 = false;
            while (found2 < nNodes) {
                Node child = children.item(found2);
                if (child1 == child || isNodeTheSame(child1, child)) {
                    if (found22) {
                        return false;
                    }
                    found1 = true;
                } else if (child2 == child || isNodeTheSame(child2, child)) {
                    if (found1) {
                        return true;
                    }
                    found22 = true;
                }
                found2++;
            }
            return false;
        }
        Node child3 = parent.getFirstChild();
        nNodes = false;
        while (child3 != null) {
            if (child1 == child3 || isNodeTheSame(child1, child3)) {
                if (found2) {
                    return false;
                }
                nNodes = true;
            } else if (child2 == child3 || isNodeTheSame(child2, child3)) {
                if (nNodes) {
                    return true;
                }
                found2 = true;
            }
            child3 = child3.getNextSibling();
        }
        return false;
    }

    public short getLevel(Node n) {
        short level = (short) 1;
        while (true) {
            Node parentOfNode = getParentOfNode(n);
            n = parentOfNode;
            if (parentOfNode == null) {
                return level;
            }
            level = (short) (level + 1);
        }
    }

    public String getNamespaceForPrefix(String prefix, Element namespaceContext) {
        Node parent = namespaceContext;
        if (prefix.equals("xml")) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if (prefix.equals("xmlns")) {
            return SerializerConstants.XMLNS_URI;
        }
        String declname;
        if (prefix == "") {
            declname = "xmlns";
        } else {
            declname = new StringBuilder();
            declname.append(Constants.ATTRNAME_XMLNS);
            declname.append(prefix);
            declname = declname.toString();
        }
        while (parent != null && null == null) {
            short nodeType = parent.getNodeType();
            short type = nodeType;
            if (nodeType != (short) 1 && type != (short) 5) {
                return null;
            }
            if (type == (short) 1) {
                Attr attr = ((Element) parent).getAttributeNode(declname);
                if (attr != null) {
                    return attr.getNodeValue();
                }
            }
            parent = getParentOfNode(parent);
        }
        return null;
    }

    public String getNamespaceOfNode(Node n) {
        NSInfo nsInfo;
        boolean hasProcessedNS;
        String namespaceOfPrefix;
        Node node = n;
        short ntype = n.getNodeType();
        boolean z = false;
        int i = 2;
        if ((short) 2 != ntype) {
            Object nsObj = this.m_NSInfos.get(node);
            nsInfo = nsObj == null ? null : (NSInfo) nsObj;
            hasProcessedNS = nsInfo == null ? false : nsInfo.m_hasProcessedNS;
        } else {
            hasProcessedNS = false;
            nsInfo = null;
        }
        boolean z2;
        if (hasProcessedNS) {
            namespaceOfPrefix = nsInfo.m_namespace;
            z2 = hasProcessedNS;
        } else {
            String prefix;
            String namespaceOfPrefix2 = null;
            String nodeName = n.getNodeName();
            int indexOfNSSep = nodeName.indexOf(58);
            if ((short) 2 != ntype) {
                prefix = indexOfNSSep >= 0 ? nodeName.substring(0, indexOfNSSep) : "";
            } else if (indexOfNSSep <= 0) {
                return null;
            } else {
                prefix = nodeName.substring(0, indexOfNSSep);
            }
            boolean ancestorsHaveXMLNS = false;
            boolean nHasXMLNS = false;
            if (prefix.equals("xml")) {
                namespaceOfPrefix = "http://www.w3.org/XML/1998/namespace";
                z2 = hasProcessedNS;
            } else {
                String namespaceOfPrefix3;
                boolean nHasXMLNS2 = false;
                nHasXMLNS = false;
                NSInfo nsInfo2 = nsInfo;
                Node parent = node;
                while (parent != null && namespaceOfPrefix2 == null) {
                    if (nsInfo2 != null && nsInfo2.m_ancestorHasXMLNSAttrs == i) {
                        z2 = hasProcessedNS;
                        namespaceOfPrefix3 = namespaceOfPrefix2;
                        break;
                    }
                    Node parent2;
                    int parentType = parent.getNodeType();
                    if (nsInfo2 == null || nsInfo2.m_hasXMLNSAttrs) {
                        boolean ancestorsHaveXMLNS2;
                        if (parentType == 1) {
                            NamedNodeMap nnm = parent.getAttributes();
                            ancestorsHaveXMLNS2 = nHasXMLNS;
                            nHasXMLNS = false;
                            boolean elementHasXMLNS = z;
                            while (elementHasXMLNS < nnm.getLength()) {
                                Node attr = nnm.item(elementHasXMLNS);
                                NamedNodeMap nnm2 = nnm;
                                String aname = attr.getNodeName();
                                z2 = hasProcessedNS;
                                namespaceOfPrefix3 = namespaceOfPrefix2;
                                if (aname.charAt(false)) {
                                    hasProcessedNS = aname.startsWith(Constants.ATTRNAME_XMLNS);
                                    if (aname.equals("xmlns") || hasProcessedNS) {
                                        if (node == parent) {
                                            nHasXMLNS2 = true;
                                        }
                                        boolean isPrefix;
                                        if (hasProcessedNS) {
                                            isPrefix = hasProcessedNS;
                                            hasProcessedNS = aname.substring(true);
                                        } else {
                                            isPrefix = hasProcessedNS;
                                            hasProcessedNS = "";
                                        }
                                        if (hasProcessedNS.equals(prefix)) {
                                            namespaceOfPrefix3 = attr.getNodeValue();
                                            ancestorsHaveXMLNS2 = true;
                                            nHasXMLNS = true;
                                            break;
                                        }
                                        ancestorsHaveXMLNS2 = true;
                                        nHasXMLNS = true;
                                    }
                                }
                                elementHasXMLNS++;
                                nnm = nnm2;
                                hasProcessedNS = z2;
                                namespaceOfPrefix2 = namespaceOfPrefix3;
                            }
                            z2 = hasProcessedNS;
                            namespaceOfPrefix3 = namespaceOfPrefix2;
                        } else {
                            z2 = hasProcessedNS;
                            namespaceOfPrefix3 = namespaceOfPrefix2;
                            ancestorsHaveXMLNS2 = nHasXMLNS;
                            nHasXMLNS = false;
                        }
                        if (!(2 == parentType || nsInfo2 != null || node == parent)) {
                            NSInfo nsInfo3 = nHasXMLNS ? m_NSInfoUnProcWithXMLNS : m_NSInfoUnProcWithoutXMLNS;
                            this.m_NSInfos.put(parent, nsInfo3);
                            nsInfo2 = nsInfo3;
                        }
                        nHasXMLNS = ancestorsHaveXMLNS2;
                        namespaceOfPrefix2 = namespaceOfPrefix3;
                    } else {
                        z2 = hasProcessedNS;
                    }
                    if (2 == parentType) {
                        parent2 = getParentOfNode(parent);
                    } else {
                        this.m_candidateNoAncestorXMLNS.addElement(parent);
                        this.m_candidateNoAncestorXMLNS.addElement(nsInfo2);
                        parent2 = parent.getParentNode();
                    }
                    parent = parent2;
                    if (parent != null) {
                        Object nsObj2 = this.m_NSInfos.get(parent);
                        nsInfo2 = nsObj2 == null ? null : (NSInfo) nsObj2;
                    }
                    hasProcessedNS = z2;
                    z = false;
                    i = 2;
                }
                z2 = hasProcessedNS;
                namespaceOfPrefix3 = namespaceOfPrefix2;
                int nCandidates = this.m_candidateNoAncestorXMLNS.size();
                if (nCandidates > 0) {
                    if (!nHasXMLNS && parent == null) {
                        int i2 = 0;
                        while (true) {
                            int i3 = i2;
                            if (i3 >= nCandidates) {
                                break;
                            }
                            NSInfo candidateInfo = this.m_candidateNoAncestorXMLNS.elementAt(i3 + 1);
                            if (candidateInfo == m_NSInfoUnProcWithoutXMLNS) {
                                this.m_NSInfos.put(this.m_candidateNoAncestorXMLNS.elementAt(i3), m_NSInfoUnProcNoAncestorXMLNS);
                            } else if (candidateInfo == m_NSInfoNullWithoutXMLNS) {
                                this.m_NSInfos.put(this.m_candidateNoAncestorXMLNS.elementAt(i3), m_NSInfoNullNoAncestorXMLNS);
                            }
                            i2 = i3 + 2;
                        }
                    }
                    this.m_candidateNoAncestorXMLNS.removeAllElements();
                }
                ancestorsHaveXMLNS = nHasXMLNS;
                nHasXMLNS = nHasXMLNS2;
                namespaceOfPrefix = namespaceOfPrefix3;
            }
            if ((short) 2 != ntype) {
                if (namespaceOfPrefix != null) {
                    this.m_NSInfos.put(node, new NSInfo(namespaceOfPrefix, nHasXMLNS));
                } else if (!ancestorsHaveXMLNS) {
                    this.m_NSInfos.put(node, m_NSInfoNullNoAncestorXMLNS);
                } else if (nHasXMLNS) {
                    this.m_NSInfos.put(node, m_NSInfoNullWithXMLNS);
                } else {
                    this.m_NSInfos.put(node, m_NSInfoNullWithoutXMLNS);
                }
            }
        }
        return namespaceOfPrefix;
    }

    public String getLocalNameOfNode(Node n) {
        String qname = n.getNodeName();
        int index = qname.indexOf(58);
        return index < 0 ? qname : qname.substring(index + 1);
    }

    public String getExpandedElementName(Element elem) {
        String namespace = getNamespaceOfNode(elem);
        if (namespace == null) {
            return getLocalNameOfNode(elem);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(namespace);
        stringBuilder.append(":");
        stringBuilder.append(getLocalNameOfNode(elem));
        return stringBuilder.toString();
    }

    public String getExpandedAttributeName(Attr attr) {
        String namespace = getNamespaceOfNode(attr);
        if (namespace == null) {
            return getLocalNameOfNode(attr);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(namespace);
        stringBuilder.append(":");
        stringBuilder.append(getLocalNameOfNode(attr));
        return stringBuilder.toString();
    }

    public boolean isIgnorableWhitespace(Text node) {
        return false;
    }

    public Node getRoot(Node node) {
        Node root = null;
        while (node != null) {
            root = node;
            node = getParentOfNode(node);
        }
        return root;
    }

    public Node getRootNode(Node n) {
        int nt = n.getNodeType();
        if (9 == nt || 11 == nt) {
            return n;
        }
        return n.getOwnerDocument();
    }

    public boolean isNamespaceNode(Node n) {
        boolean z = false;
        if ((short) 2 != n.getNodeType()) {
            return false;
        }
        String attrName = n.getNodeName();
        if (attrName.startsWith(Constants.ATTRNAME_XMLNS) || attrName.equals("xmlns")) {
            z = true;
        }
        return z;
    }

    public static Node getParentOfNode(Node node) throws RuntimeException {
        Node parent;
        if ((short) 2 == node.getNodeType()) {
            Document doc = node.getOwnerDocument();
            DOMImplementation impl = doc.getImplementation();
            if (impl != null && impl.hasFeature("Core", "2.0")) {
                return ((Attr) node).getOwnerElement();
            }
            Element rootElem = doc.getDocumentElement();
            if (rootElem != null) {
                parent = locateAttrParent(rootElem, node);
            } else {
                throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT, null));
            }
        }
        parent = node.getParentNode();
        return parent;
    }

    public Element getElementByID(String id, Document doc) {
        return null;
    }

    public String getUnparsedEntityURI(String name, Document doc) {
        String url = "";
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
        return url;
    }

    private static Node locateAttrParent(Element elem, Node attr) {
        Node parent = null;
        if (elem.getAttributeNode(attr.getNodeName()) == attr) {
            parent = elem;
        }
        if (parent == null) {
            for (Node node = elem.getFirstChild(); node != null; node = node.getNextSibling()) {
                if ((short) 1 == node.getNodeType()) {
                    parent = locateAttrParent((Element) node, attr);
                    if (parent != null) {
                        break;
                    }
                }
            }
        }
        return parent;
    }

    public void setDOMFactory(Document domFactory) {
        this.m_DOMFactory = domFactory;
    }

    public Document getDOMFactory() {
        if (this.m_DOMFactory == null) {
            this.m_DOMFactory = createDocument();
        }
        return this.m_DOMFactory;
    }

    public static String getNodeData(Node node) {
        FastStringBuffer buf = StringBufferPool.get();
        try {
            getNodeData(node, buf);
            String s = buf.length() > 0 ? buf.toString() : "";
            StringBufferPool.free(buf);
            return s;
        } catch (Throwable th) {
            StringBufferPool.free(buf);
        }
    }

    public static void getNodeData(Node node, FastStringBuffer buf) {
        short nodeType = node.getNodeType();
        if (nodeType != (short) 7) {
            if (!(nodeType == (short) 9 || nodeType == (short) 11)) {
                switch (nodeType) {
                    case (short) 1:
                        break;
                    case (short) 2:
                        buf.append(node.getNodeValue());
                        return;
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
}
