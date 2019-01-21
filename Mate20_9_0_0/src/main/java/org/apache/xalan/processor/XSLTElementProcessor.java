package org.apache.xalan.processor;

import java.util.ArrayList;
import java.util.List;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xml.utils.IntStack;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class XSLTElementProcessor extends ElemTemplateElement {
    static final long serialVersionUID = 5597421564955304421L;
    private XSLTElementDef m_elemDef;
    private IntStack m_savedLastOrder;

    XSLTElementProcessor() {
    }

    XSLTElementDef getElemDef() {
        return this.m_elemDef;
    }

    void setElemDef(XSLTElementDef def) {
        this.m_elemDef = def;
    }

    public InputSource resolveEntity(StylesheetHandler handler, String publicId, String systemId) throws SAXException {
        return null;
    }

    public void notationDecl(StylesheetHandler handler, String name, String publicId, String systemId) {
    }

    public void unparsedEntityDecl(StylesheetHandler handler, String name, String publicId, String systemId, String notationName) {
    }

    public void startNonText(StylesheetHandler handler) throws SAXException {
    }

    public void startElement(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        if (this.m_savedLastOrder == null) {
            this.m_savedLastOrder = new IntStack();
        }
        this.m_savedLastOrder.push(getElemDef().getLastOrder());
        getElemDef().setLastOrder(-1);
    }

    public void endElement(StylesheetHandler handler, String uri, String localName, String rawName) throws SAXException {
        if (!(this.m_savedLastOrder == null || this.m_savedLastOrder.empty())) {
            getElemDef().setLastOrder(this.m_savedLastOrder.pop());
        }
        if (!getElemDef().getRequiredFound()) {
            handler.error(XSLTErrorResources.ER_REQUIRED_ELEM_NOT_FOUND, new Object[]{getElemDef().getRequiredElem()}, null);
        }
    }

    public void characters(StylesheetHandler handler, char[] ch, int start, int length) throws SAXException {
        handler.error(XSLTErrorResources.ER_CHARS_NOT_ALLOWED, null, null);
    }

    public void ignorableWhitespace(StylesheetHandler handler, char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(StylesheetHandler handler, String target, String data) throws SAXException {
    }

    public void skippedEntity(StylesheetHandler handler, String name) throws SAXException {
    }

    void setPropertiesFromAttributes(StylesheetHandler handler, String rawName, Attributes attributes, ElemTemplateElement target) throws SAXException {
        setPropertiesFromAttributes(handler, rawName, attributes, target, true);
    }

    Attributes setPropertiesFromAttributes(StylesheetHandler handler, String rawName, Attributes attributes, ElemTemplateElement target, boolean throwError) throws SAXException {
        int nAttrs;
        List errorDefs;
        boolean isCompatibleMode;
        List processedDefs;
        ElemTemplateElement elemTemplateElement;
        StylesheetHandler stylesheetHandler = handler;
        Attributes processedDefs2 = attributes;
        XSLTElementDef def = getElemDef();
        AttributesImpl undefines = null;
        boolean z = (handler.getStylesheet() != null && handler.getStylesheet().getCompatibleMode()) || !throwError;
        boolean isCompatibleMode2 = z;
        if (isCompatibleMode2) {
            undefines = new AttributesImpl();
        }
        AttributesImpl undefines2 = undefines;
        List processedDefs3 = new ArrayList();
        List errorDefs2 = new ArrayList();
        int nAttrs2 = attributes.getLength();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= nAttrs2) {
                break;
            }
            String attrUri;
            List processedDefs4;
            int i3;
            String attrUri2 = processedDefs2.getURI(i2);
            if (attrUri2 != null && attrUri2.length() == 0 && (processedDefs2.getQName(i2).startsWith(Constants.ATTRNAME_XMLNS) || processedDefs2.getQName(i2).equals("xmlns"))) {
                attrUri = "http://www.w3.org/XML/1998/namespace";
            } else {
                attrUri = attrUri2;
            }
            attrUri2 = processedDefs2.getLocalName(i2);
            XSLTAttributeDef attrDef = def.getAttributeDef(attrUri, attrUri2);
            if (attrDef != null) {
                processedDefs4 = processedDefs3;
                XSLTAttributeDef attrDef2 = attrDef;
                String attrLocalName = attrUri2;
                nAttrs = nAttrs2;
                errorDefs = errorDefs2;
                isCompatibleMode = isCompatibleMode2;
                i3 = i2;
                if (handler.getStylesheetProcessor() == null) {
                    System.out.println("stylesheet processor null");
                }
                if (attrDef2.getName().compareTo("*") != null || handler.getStylesheetProcessor().isSecureProcessing() == null) {
                    String qName = processedDefs2.getQName(i3);
                    String value = processedDefs2.getValue(i3);
                    processedDefs = processedDefs4;
                    if (attrDef2.setAttrValue(stylesheetHandler, attrUri, attrLocalName, qName, value, target) != null) {
                        processedDefs.add(attrDef2);
                    } else {
                        errorDefs.add(attrDef2);
                    }
                    i = i3 + 1;
                    processedDefs3 = processedDefs;
                    errorDefs2 = errorDefs;
                    isCompatibleMode2 = isCompatibleMode;
                    nAttrs2 = nAttrs;
                    processedDefs2 = attributes;
                } else {
                    stylesheetHandler.error(XSLTErrorResources.ER_ATTR_NOT_ALLOWED, new Object[]{processedDefs2.getQName(i3), rawName}, null);
                }
            } else if (isCompatibleMode2) {
                processedDefs4 = processedDefs3;
                isCompatibleMode = isCompatibleMode2;
                i3 = i2;
                nAttrs = nAttrs2;
                errorDefs = errorDefs2;
                undefines2.addAttribute(attrUri, attrUri2, processedDefs2.getQName(i2), processedDefs2.getType(i2), processedDefs2.getValue(i2));
            } else {
                processedDefs4 = processedDefs3;
                stylesheetHandler.error(XSLTErrorResources.ER_ATTR_NOT_ALLOWED, new Object[]{processedDefs2.getQName(i2), rawName}, null);
                nAttrs = nAttrs2;
                errorDefs = errorDefs2;
                isCompatibleMode = isCompatibleMode2;
                processedDefs = processedDefs4;
                i3 = i2;
                i = i3 + 1;
                processedDefs3 = processedDefs;
                errorDefs2 = errorDefs;
                isCompatibleMode2 = isCompatibleMode;
                nAttrs2 = nAttrs;
                processedDefs2 = attributes;
            }
            processedDefs = processedDefs4;
            i = i3 + 1;
            processedDefs3 = processedDefs;
            errorDefs2 = errorDefs;
            isCompatibleMode2 = isCompatibleMode;
            nAttrs2 = nAttrs;
            processedDefs2 = attributes;
        }
        processedDefs = processedDefs3;
        nAttrs = nAttrs2;
        errorDefs = errorDefs2;
        isCompatibleMode = isCompatibleMode2;
        for (XSLTAttributeDef attrDef3 : def.getAttributes()) {
            if (attrDef3.getDefault() == null || processedDefs.contains(attrDef3)) {
                elemTemplateElement = target;
            } else {
                attrDef3.setDefAttrValue(stylesheetHandler, target);
            }
            if (attrDef3.getRequired() && !processedDefs.contains(attrDef3) && !errorDefs.contains(attrDef3)) {
                stylesheetHandler.error(XSLMessages.createMessage(XSLTErrorResources.ER_REQUIRES_ATTRIB, new Object[]{rawName, attrDef3.getName()}), null);
            }
        }
        elemTemplateElement = target;
        return undefines2;
    }
}
