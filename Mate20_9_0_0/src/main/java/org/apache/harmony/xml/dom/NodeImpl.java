package org.apache.harmony.xml.dom;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

public abstract class NodeImpl implements Node {
    private static final NodeList EMPTY_LIST = new NodeListImpl();
    static final TypeInfo NULL_TYPE_INFO = new TypeInfo() {
        public String getTypeName() {
            return null;
        }

        public String getTypeNamespace() {
            return null;
        }

        public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
            return false;
        }
    };
    DocumentImpl document;

    static class UserData {
        final UserDataHandler handler;
        final Object value;

        UserData(Object value, UserDataHandler handler) {
            this.value = value;
            this.handler = handler;
        }
    }

    public abstract short getNodeType();

    NodeImpl(DocumentImpl document) {
        this.document = document;
    }

    public Node appendChild(Node newChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    public final Node cloneNode(boolean deep) {
        return this.document.cloneOrImportNode((short) 1, this, deep);
    }

    public NamedNodeMap getAttributes() {
        return null;
    }

    public NodeList getChildNodes() {
        return EMPTY_LIST;
    }

    public Node getFirstChild() {
        return null;
    }

    public Node getLastChild() {
        return null;
    }

    public String getLocalName() {
        return null;
    }

    public String getNamespaceURI() {
        return null;
    }

    public Node getNextSibling() {
        return null;
    }

    public String getNodeName() {
        return null;
    }

    public String getNodeValue() throws DOMException {
        return null;
    }

    public final Document getOwnerDocument() {
        return this.document == this ? null : this.document;
    }

    public Node getParentNode() {
        return null;
    }

    public String getPrefix() {
        return null;
    }

    public Node getPreviousSibling() {
        return null;
    }

    public boolean hasAttributes() {
        return false;
    }

    public boolean hasChildNodes() {
        return false;
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    public boolean isSupported(String feature, String version) {
        return DOMImplementationImpl.getInstance().hasFeature(feature, version);
    }

    public void normalize() {
    }

    public Node removeChild(Node oldChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    public final void setNodeValue(String nodeValue) throws DOMException {
        switch (getNodeType()) {
            case (short) 1:
            case (short) 5:
            case (short) 6:
            case (short) 9:
            case (short) 10:
            case (short) 11:
            case (short) 12:
                return;
            case (short) 2:
                ((Attr) this).setValue(nodeValue);
                return;
            case (short) 3:
            case (short) 4:
            case (short) 8:
                ((CharacterData) this).setData(nodeValue);
                return;
            case (short) 7:
                ((ProcessingInstruction) this).setData(nodeValue);
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported node type ");
                stringBuilder.append(getNodeType());
                throw new DOMException((short) 9, stringBuilder.toString());
        }
    }

    public void setPrefix(String prefix) throws DOMException {
    }

    static String validatePrefix(String prefix, boolean namespaceAware, String namespaceURI) {
        if (!namespaceAware) {
            throw new DOMException((short) 14, prefix);
        } else if (prefix == null || (namespaceURI != null && DocumentImpl.isXMLIdentifier(prefix) && ((!XMLConstants.XML_NS_PREFIX.equals(prefix) || "http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) && (!XMLConstants.XMLNS_ATTRIBUTE.equals(prefix) || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI))))) {
            return prefix;
        } else {
            throw new DOMException((short) 14, prefix);
        }
    }

    static void setNameNS(NodeImpl node, String namespaceURI, String qualifiedName) {
        if (qualifiedName != null) {
            String prefix = null;
            int p = qualifiedName.lastIndexOf(":");
            if (p != -1) {
                prefix = validatePrefix(qualifiedName.substring(0, p), true, namespaceURI);
                qualifiedName = qualifiedName.substring(p + 1);
            }
            if (DocumentImpl.isXMLIdentifier(qualifiedName)) {
                switch (node.getNodeType()) {
                    case (short) 1:
                        ElementImpl element = (ElementImpl) node;
                        element.namespaceAware = true;
                        element.namespaceURI = namespaceURI;
                        element.prefix = prefix;
                        element.localName = qualifiedName;
                        return;
                    case (short) 2:
                        if (!XMLConstants.XMLNS_ATTRIBUTE.equals(qualifiedName) || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
                            AttrImpl attr = (AttrImpl) node;
                            attr.namespaceAware = true;
                            attr.namespaceURI = namespaceURI;
                            attr.prefix = prefix;
                            attr.localName = qualifiedName;
                            return;
                        }
                        throw new DOMException((short) 14, qualifiedName);
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot rename nodes of type ");
                        stringBuilder.append(node.getNodeType());
                        throw new DOMException((short) 9, stringBuilder.toString());
                }
            }
            throw new DOMException((short) 5, qualifiedName);
        }
        throw new DOMException((short) 14, qualifiedName);
    }

    static void setName(NodeImpl node, String name) {
        int prefixSeparator = name.lastIndexOf(":");
        if (prefixSeparator != -1) {
            String prefix = name.substring(0, prefixSeparator);
            String localName = name.substring(prefixSeparator + 1);
            if (!(DocumentImpl.isXMLIdentifier(prefix) && DocumentImpl.isXMLIdentifier(localName))) {
                throw new DOMException((short) 5, name);
            }
        } else if (!DocumentImpl.isXMLIdentifier(name)) {
            throw new DOMException((short) 5, name);
        }
        switch (node.getNodeType()) {
            case (short) 1:
                ElementImpl element = (ElementImpl) node;
                element.namespaceAware = false;
                element.localName = name;
                return;
            case (short) 2:
                AttrImpl attr = (AttrImpl) node;
                attr.namespaceAware = false;
                attr.localName = name;
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot rename nodes of type ");
                stringBuilder.append(node.getNodeType());
                throw new DOMException((short) 9, stringBuilder.toString());
        }
    }

    public final String getBaseURI() {
        switch (getNodeType()) {
            case (short) 1:
                String uri = ((Element) this).getAttributeNS("http://www.w3.org/XML/1998/namespace", "base");
                if (uri != null) {
                    try {
                        if (!uri.isEmpty()) {
                            if (new URI(uri).isAbsolute()) {
                                return uri;
                            }
                            String parentUri = getParentBaseUri();
                            if (parentUri == null) {
                                return null;
                            }
                            return new URI(parentUri).resolve(uri).toString();
                        }
                    } catch (URISyntaxException e) {
                        return null;
                    }
                }
                return getParentBaseUri();
            case (short) 2:
            case (short) 3:
            case (short) 4:
            case (short) 8:
            case (short) 10:
            case (short) 11:
                return null;
            case (short) 5:
                return null;
            case (short) 6:
            case (short) 12:
                return null;
            case (short) 7:
                return getParentBaseUri();
            case (short) 9:
                return sanitizeUri(((Document) this).getDocumentURI());
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported node type ");
                stringBuilder.append(getNodeType());
                throw new DOMException((short) 9, stringBuilder.toString());
        }
    }

    private String getParentBaseUri() {
        Node parentNode = getParentNode();
        return parentNode != null ? parentNode.getBaseURI() : null;
    }

    private String sanitizeUri(String uri) {
        if (uri == null || uri.length() == 0) {
            return null;
        }
        try {
            return new URI(uri).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public short compareDocumentPosition(Node other) throws DOMException {
        throw new UnsupportedOperationException();
    }

    public String getTextContent() throws DOMException {
        return getNodeValue();
    }

    void getTextContent(StringBuilder buf) throws DOMException {
        String content = getNodeValue();
        if (content != null) {
            buf.append(content);
        }
    }

    public final void setTextContent(String textContent) throws DOMException {
        switch (getNodeType()) {
            case (short) 1:
            case (short) 5:
            case (short) 6:
            case (short) 11:
                break;
            case (short) 2:
            case (short) 3:
            case (short) 4:
            case (short) 7:
            case (short) 8:
            case (short) 12:
                setNodeValue(textContent);
                return;
            case (short) 9:
            case (short) 10:
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported node type ");
                stringBuilder.append(getNodeType());
                throw new DOMException((short) 9, stringBuilder.toString());
        }
        while (true) {
            Node firstChild = getFirstChild();
            Node child = firstChild;
            if (firstChild != null) {
                removeChild(child);
            } else {
                if (!(textContent == null || textContent.length() == 0)) {
                    appendChild(this.document.createTextNode(textContent));
                }
                return;
            }
        }
    }

    public boolean isSameNode(Node other) {
        return this == other;
    }

    private NodeImpl getNamespacingElement() {
        switch (getNodeType()) {
            case (short) 1:
                return this;
            case (short) 2:
                return (NodeImpl) ((Attr) this).getOwnerElement();
            case (short) 3:
            case (short) 4:
            case (short) 5:
            case (short) 7:
            case (short) 8:
                return getContainingElement();
            case (short) 6:
            case (short) 10:
            case (short) 11:
            case (short) 12:
                return null;
            case (short) 9:
                return (NodeImpl) ((Document) this).getDocumentElement();
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported node type ");
                stringBuilder.append(getNodeType());
                throw new DOMException((short) 9, stringBuilder.toString());
        }
    }

    private NodeImpl getContainingElement() {
        for (Node p = getParentNode(); p != null; p = p.getParentNode()) {
            if (p.getNodeType() == (short) 1) {
                return (NodeImpl) p;
            }
        }
        return null;
    }

    public final String lookupPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            return null;
        }
        NodeImpl target = getNamespacingElement();
        NodeImpl node = target;
        while (node != null) {
            if (namespaceURI.equals(node.getNamespaceURI()) && target.isPrefixMappedToUri(node.getPrefix(), namespaceURI)) {
                return node.getPrefix();
            }
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    Node attr = attributes.item(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()) && XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getPrefix()) && namespaceURI.equals(attr.getNodeValue()) && target.isPrefixMappedToUri(attr.getLocalName(), namespaceURI)) {
                        return attr.getLocalName();
                    }
                }
                continue;
            }
            node = node.getContainingElement();
        }
        return null;
    }

    boolean isPrefixMappedToUri(String prefix, String uri) {
        if (prefix == null) {
            return false;
        }
        return uri.equals(lookupNamespaceURI(prefix));
    }

    public final boolean isDefaultNamespace(String namespaceURI) {
        String actual = lookupNamespaceURI(null);
        if (namespaceURI == null) {
            return actual == null;
        } else {
            return namespaceURI.equals(actual);
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0021, code skipped:
            return r1.getNamespaceURI();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final String lookupNamespaceURI(String prefix) {
        String str;
        Node attr;
        NodeImpl node = getNamespacingElement();
        loop0:
        while (true) {
            str = null;
            if (node == null) {
                return null;
            }
            String nodePrefix = node.getPrefix();
            if (node.getNamespaceURI() != null) {
                if (prefix != null) {
                    if (prefix.equals(nodePrefix)) {
                        break;
                    }
                } else if (nodePrefix == null) {
                    break;
                }
            }
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    attr = attributes.item(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) {
                        if (prefix != null) {
                            if (XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getPrefix()) && prefix.equals(attr.getLocalName())) {
                                break loop0;
                            }
                        } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getNodeName())) {
                            break loop0;
                        }
                    }
                }
                continue;
            }
            node = node.getContainingElement();
        }
        String value = attr.getNodeValue();
        if (value.length() > 0) {
            str = value;
        }
        return str;
    }

    private static List<Object> createEqualityKey(Node node) {
        List<Object> values = new ArrayList();
        values.add(Short.valueOf(node.getNodeType()));
        values.add(node.getNodeName());
        values.add(node.getLocalName());
        values.add(node.getNamespaceURI());
        values.add(node.getPrefix());
        values.add(node.getNodeValue());
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            values.add(child);
        }
        short nodeType = node.getNodeType();
        if (nodeType == (short) 1) {
            values.add(((Element) node).getAttributes());
        } else if (nodeType == (short) 10) {
            DocumentTypeImpl doctype = (DocumentTypeImpl) node;
            values.add(doctype.getPublicId());
            values.add(doctype.getSystemId());
            values.add(doctype.getInternalSubset());
            values.add(doctype.getEntities());
            values.add(doctype.getNotations());
        }
        return values;
    }

    public final boolean isEqualNode(Node arg) {
        if (arg == this) {
            return true;
        }
        List<Object> listA = createEqualityKey(this);
        List<Object> listB = createEqualityKey(arg);
        if (listA.size() != listB.size()) {
            return false;
        }
        for (int i = 0; i < listA.size(); i++) {
            Object a = listA.get(i);
            Object b = listB.get(i);
            if (a != b) {
                if (a == null || b == null) {
                    return false;
                }
                if ((a instanceof String) || (a instanceof Short)) {
                    if (!a.equals(b)) {
                        return false;
                    }
                } else if (a instanceof NamedNodeMap) {
                    if (!(b instanceof NamedNodeMap) || !namedNodeMapsEqual((NamedNodeMap) a, (NamedNodeMap) b)) {
                        return false;
                    }
                } else if (!(a instanceof Node)) {
                    throw new AssertionError();
                } else if (!(b instanceof Node) || !((Node) a).isEqualNode((Node) b)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean namedNodeMapsEqual(NamedNodeMap a, NamedNodeMap b) {
        if (a.getLength() != b.getLength()) {
            return false;
        }
        for (int i = 0; i < a.getLength(); i++) {
            Node bNode;
            Node aNode = a.item(i);
            if (aNode.getLocalName() == null) {
                bNode = b.getNamedItem(aNode.getNodeName());
            } else {
                bNode = b.getNamedItemNS(aNode.getNamespaceURI(), aNode.getLocalName());
            }
            if (bNode == null || !aNode.isEqualNode(bNode)) {
                return false;
            }
        }
        return true;
    }

    public final Object getFeature(String feature, String version) {
        return isSupported(feature, version) ? this : null;
    }

    public final Object setUserData(String key, Object data, UserDataHandler handler) {
        if (key != null) {
            UserData previous;
            Map<String, UserData> map = this.document.getUserDataMap(this);
            if (data == null) {
                previous = (UserData) map.remove(key);
            } else {
                previous = (UserData) map.put(key, new UserData(data, handler));
            }
            return previous != null ? previous.value : null;
        } else {
            throw new NullPointerException("key == null");
        }
    }

    public final Object getUserData(String key) {
        if (key != null) {
            UserData userData = (UserData) this.document.getUserDataMapForRead(this).get(key);
            return userData != null ? userData.value : null;
        } else {
            throw new NullPointerException("key == null");
        }
    }
}
