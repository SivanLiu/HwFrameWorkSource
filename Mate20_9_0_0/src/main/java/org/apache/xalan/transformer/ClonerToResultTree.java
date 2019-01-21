package org.apache.xalan.transformer;

import javax.xml.transform.TransformerException;
import org.apache.xalan.serialize.SerializerUtils;
import org.apache.xml.dtm.DTM;
import org.apache.xml.serializer.SerializationHandler;
import org.xml.sax.SAXException;

public class ClonerToResultTree {
    public static void cloneToResultTree(int node, int nodeType, DTM dtm, SerializationHandler rth, boolean shouldCloneAttributes) throws TransformerException {
        switch (nodeType) {
            case 1:
                String ns = dtm.getNamespaceURI(node);
                if (ns == null) {
                    ns = "";
                }
                rth.startElement(ns, dtm.getLocalName(node), dtm.getNodeNameX(node));
                if (shouldCloneAttributes) {
                    SerializerUtils.addAttributes(rth, node);
                    SerializerUtils.processNSDecls(rth, node, nodeType, dtm);
                }
                break;
            case 2:
                SerializerUtils.addAttribute(rth, node);
                break;
            case 3:
                dtm.dispatchCharactersEvents(node, rth, false);
                break;
            case 4:
                rth.startCDATA();
                dtm.dispatchCharactersEvents(node, rth, false);
                rth.endCDATA();
                break;
            case 5:
                rth.entityReference(dtm.getNodeNameX(node));
                break;
            case 7:
                rth.processingInstruction(dtm.getNodeNameX(node), dtm.getNodeValue(node));
                break;
            case 8:
                dtm.getStringValue(node).dispatchAsComment(rth);
                break;
            case 9:
            case 11:
                break;
            case 13:
                SerializerUtils.processNSDecls(rth, node, 13, dtm);
                break;
            default:
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't clone node: ");
                    stringBuilder.append(dtm.getNodeName(node));
                    throw new TransformerException(stringBuilder.toString());
                } catch (SAXException se) {
                    throw new TransformerException(se);
                }
        }
    }
}
