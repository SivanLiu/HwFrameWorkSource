package org.apache.xml.utils;

import org.apache.xalan.templates.Constants;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PrefixResolverDefault implements PrefixResolver {
    Node m_context;

    public PrefixResolverDefault(Node xpathExpressionContext) {
        this.m_context = xpathExpressionContext;
    }

    public String getNamespaceForPrefix(String prefix) {
        return getNamespaceForPrefix(prefix, this.m_context);
    }

    public String getNamespaceForPrefix(String prefix, Node namespaceContext) {
        String namespace = null;
        if (!prefix.equals("xml")) {
            for (Node parent = namespaceContext; parent != null && namespace == null; parent = parent.getParentNode()) {
                short nodeType = parent.getNodeType();
                short type = nodeType;
                if (nodeType != (short) 1 && type != (short) 5) {
                    break;
                }
                if (type == (short) 1) {
                    String nodeName = parent.getNodeName();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(prefix);
                    stringBuilder.append(":");
                    if (nodeName.indexOf(stringBuilder.toString()) == 0) {
                        return parent.getNamespaceURI();
                    }
                    NamedNodeMap nnm = parent.getAttributes();
                    for (int i = 0; i < nnm.getLength(); i++) {
                        Node attr = nnm.item(i);
                        String aname = attr.getNodeName();
                        boolean isPrefix = aname.startsWith(Constants.ATTRNAME_XMLNS);
                        if (isPrefix || aname.equals("xmlns")) {
                            if ((isPrefix ? aname.substring(aname.indexOf(58) + 1) : "").equals(prefix)) {
                                namespace = attr.getNodeValue();
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            namespace = "http://www.w3.org/XML/1998/namespace";
        }
        return namespace;
    }

    public String getBaseIdentifier() {
        return null;
    }

    public boolean handlesNullPrefixes() {
        return false;
    }
}
