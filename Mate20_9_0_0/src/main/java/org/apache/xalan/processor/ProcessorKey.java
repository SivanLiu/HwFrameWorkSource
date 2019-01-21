package org.apache.xalan.processor;

import java.util.ArrayList;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.KeyDeclaration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorKey extends XSLTElementProcessor {
    static final long serialVersionUID = 4285205417566822979L;

    ProcessorKey() {
    }

    public void startElement(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        KeyDeclaration kd = new KeyDeclaration(handler.getStylesheet(), handler.nextUid());
        kd.setDOMBackPointer(handler.getOriginatingNode());
        kd.setLocaterInfo(handler.getLocator());
        setPropertiesFromAttributes(handler, rawName, attributes, kd);
        handler.getStylesheet().setKey(kd);
    }

    void setPropertiesFromAttributes(StylesheetHandler handler, String rawName, Attributes attributes, ElemTemplateElement target) throws SAXException {
        ElemTemplateElement elemTemplateElement;
        StylesheetHandler stylesheetHandler = handler;
        String str = rawName;
        Attributes attributes2 = attributes;
        XSLTElementDef def = getElemDef();
        ArrayList processedDefs = new ArrayList();
        int nAttrs = attributes.getLength();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= nAttrs) {
                break;
            }
            String attrUri = attributes2.getURI(i2);
            String attrLocalName = attributes2.getLocalName(i2);
            XSLTAttributeDef attrDef = def.getAttributeDef(attrUri, attrLocalName);
            if (attrDef == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(attributes2.getQName(i2));
                stringBuilder.append("attribute is not allowed on the ");
                stringBuilder.append(str);
                stringBuilder.append(" element!");
                stylesheetHandler.error(stringBuilder.toString(), null);
            } else {
                String valueString = attributes2.getValue(i2);
                if (valueString.indexOf("key(") >= 0) {
                    stylesheetHandler.error(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_KEY_CALL, null), null);
                }
                processedDefs.add(attrDef);
                attrDef.setAttrValue(stylesheetHandler, attrUri, attrLocalName, attributes2.getQName(i2), attributes2.getValue(i2), target);
            }
            i = i2 + 1;
        }
        for (XSLTAttributeDef attrDef2 : def.getAttributes()) {
            if (attrDef2.getDefault() == null || processedDefs.contains(attrDef2)) {
                elemTemplateElement = target;
            } else {
                attrDef2.setDefAttrValue(stylesheetHandler, target);
            }
            if (attrDef2.getRequired() && !processedDefs.contains(attrDef2)) {
                stylesheetHandler.error(XSLMessages.createMessage(XSLTErrorResources.ER_REQUIRES_ATTRIB, new Object[]{str, attrDef2.getName()}), null);
            }
        }
        elemTemplateElement = target;
    }
}
