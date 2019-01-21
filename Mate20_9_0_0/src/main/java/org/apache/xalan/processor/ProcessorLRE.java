package org.apache.xalan.processor;

import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.ElemExtensionCall;
import org.apache.xalan.templates.ElemLiteralResult;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.templates.XMLNSDecl;
import org.apache.xml.utils.Constants;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xpath.XPath;
import org.apache.xpath.compiler.PsuedoNames;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ProcessorLRE extends ProcessorTemplateElem {
    static final long serialVersionUID = -1490218021772101404L;

    /* JADX WARNING: Removed duplicated region for block: B:85:0x01e3 A:{ExcHandler: IllegalAccessException (r0_40 'iae' java.lang.IllegalAccessException A:{Catch:{ InstantiationException -> 0x01e5, IllegalAccessException -> 0x01e3 }}), Splitter:B:82:0x01dc, Catch:{ InstantiationException -> 0x01e5, IllegalAccessException -> 0x01e3 }} */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:82:0x01dc, B:93:0x01fb] */
    /* JADX WARNING: Missing block: B:85:0x01e3, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:102:?, code skipped:
            r42 = r6;
            r2.error(org.apache.xalan.res.XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, false, r0);
     */
    /* JADX WARNING: Missing block: B:103:0x0241, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:104:0x0242, code skipped:
            r42 = r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startElement(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        Attributes attributes2;
        boolean isLREAsStyleSheet;
        Exception e;
        Locator locator;
        SAXSourceLocator sAXSourceLocator;
        boolean z;
        TransformerException te;
        StylesheetHandler stylesheetHandler = handler;
        String str = uri;
        String str2 = localName;
        String str3 = rawName;
        Attributes attributes3 = attributes;
        ElemTemplateElement p;
        ElemTemplateElement p2;
        boolean excludeXSLDecl;
        try {
            ElemTemplateElement p3;
            p = handler.getElemTemplateElement();
            boolean excludeXSLDecl2 = false;
            if (p == null) {
                int n;
                boolean isLREAsStyleSheet2;
                int n2;
                XSLTElementProcessor lreProcessor = handler.popProcessor();
                XSLTElementProcessor stylesheetProcessor = stylesheetHandler.getProcessorFor(Constants.S_XSLNAMESPACEURL, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, "xsl:stylesheet");
                stylesheetHandler.pushProcessor(lreProcessor);
                Stylesheet stylesheet = getStylesheetRoot(handler);
                SAXSourceLocator slocator = new SAXSourceLocator();
                Locator locator2 = handler.getLocator();
                if (locator2 != null) {
                    slocator.setLineNumber(locator2.getLineNumber());
                    slocator.setColumnNumber(locator2.getColumnNumber());
                    slocator.setPublicId(locator2.getPublicId());
                    slocator.setSystemId(locator2.getSystemId());
                }
                stylesheet.setLocaterInfo(slocator);
                stylesheet.setPrefixes(handler.getNamespaceSupport());
                stylesheetHandler.pushStylesheet(stylesheet);
                boolean isLREAsStyleSheet3 = true;
                AttributesImpl stylesheetAttrs = new AttributesImpl();
                AttributesImpl lreAttrs = new AttributesImpl();
                int n3 = attributes.getLength();
                int i = 0;
                while (true) {
                    n = n3;
                    if (i >= n) {
                        break;
                    }
                    String attrLocalName = attributes3.getLocalName(i);
                    String attrUri = attributes3.getURI(i);
                    String value = attributes3.getValue(i);
                    p2 = p;
                    String p4 = attrUri;
                    if (p4 != null) {
                        excludeXSLDecl = excludeXSLDecl2;
                        if (p4.equals(Constants.S_XSLNAMESPACEURL)) {
                            stylesheetAttrs.addAttribute(null, attrLocalName, attrLocalName, attributes3.getType(i), attributes3.getValue(i));
                            isLREAsStyleSheet2 = isLREAsStyleSheet3;
                            n2 = n;
                            i++;
                            p = p2;
                            excludeXSLDecl2 = excludeXSLDecl;
                            isLREAsStyleSheet3 = isLREAsStyleSheet2;
                            n3 = n2;
                        }
                    } else {
                        excludeXSLDecl = excludeXSLDecl2;
                    }
                    isLREAsStyleSheet2 = isLREAsStyleSheet3;
                    String attrLocalName2 = attrLocalName;
                    if (!attrLocalName2.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS)) {
                        if (!attrLocalName2.equals("xmlns")) {
                            n2 = n;
                            n = value;
                            lreAttrs.addAttribute(p4, attrLocalName2, attributes3.getQName(i), attributes3.getType(i), attributes3.getValue(i));
                            i++;
                            p = p2;
                            excludeXSLDecl2 = excludeXSLDecl;
                            isLREAsStyleSheet3 = isLREAsStyleSheet2;
                            n3 = n2;
                        }
                    }
                    n2 = n;
                    if (value.equals(Constants.S_XSLNAMESPACEURL)) {
                        i++;
                        p = p2;
                        excludeXSLDecl2 = excludeXSLDecl;
                        isLREAsStyleSheet3 = isLREAsStyleSheet2;
                        n3 = n2;
                    }
                    lreAttrs.addAttribute(p4, attrLocalName2, attributes3.getQName(i), attributes3.getType(i), attributes3.getValue(i));
                    i++;
                    p = p2;
                    excludeXSLDecl2 = excludeXSLDecl;
                    isLREAsStyleSheet3 = isLREAsStyleSheet2;
                    n3 = n2;
                }
                excludeXSLDecl = excludeXSLDecl2;
                isLREAsStyleSheet2 = isLREAsStyleSheet3;
                n2 = n;
                attributes3 = lreAttrs;
                try {
                    try {
                        stylesheetProcessor.setPropertiesFromAttributes(stylesheetHandler, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, stylesheetAttrs, stylesheet);
                        stylesheetHandler.pushElemTemplateElement(stylesheet);
                        ElemTemplate template = new ElemTemplate();
                        template.setLocaterInfo(slocator);
                        appendAndPush(stylesheetHandler, template);
                        template.setMatch(new XPath(PsuedoNames.PSEUDONAME_ROOT, stylesheet, stylesheet, 1, handler.getStylesheetProcessor().getErrorListener()));
                        stylesheet.setTemplate(template);
                        p3 = handler.getElemTemplateElement();
                        excludeXSLDecl2 = true;
                        attributes2 = attributes3;
                        isLREAsStyleSheet = isLREAsStyleSheet2;
                    } catch (Exception e2) {
                        e = e2;
                        locator = locator2;
                        sAXSourceLocator = slocator;
                    }
                } catch (Exception e3) {
                    e = e3;
                    locator = locator2;
                    sAXSourceLocator = slocator;
                    AttributesImpl p5 = stylesheetAttrs;
                    if (stylesheet.getDeclaredPrefixes() != null) {
                        if (declaredXSLNS(stylesheet)) {
                            throw new SAXException(e);
                        }
                    }
                    throw new SAXException(XSLMessages.createWarning(XSLTErrorResources.WG_OLD_XSLT_NS, null));
                }
            }
            p2 = p;
            excludeXSLDecl = false;
            attributes2 = attributes3;
            isLREAsStyleSheet = false;
            p3 = p2;
            try {
                boolean isUnknownTopLevel;
                ElemTemplateElement elem;
                Class classObject = getElemDef().getClassObject();
                boolean isExtension = false;
                boolean isExtension2 = false;
                boolean isUnknownTopLevel2 = false;
                while (true) {
                    isUnknownTopLevel = isUnknownTopLevel2;
                    if (p3 == null) {
                        isUnknownTopLevel2 = isUnknownTopLevel;
                        break;
                    }
                    if (p3 instanceof ElemLiteralResult) {
                        isExtension = ((ElemLiteralResult) p3).containsExtensionElementURI(str);
                    } else if (p3 instanceof Stylesheet) {
                        isExtension = ((Stylesheet) p3).containsExtensionElementURI(str);
                        if (isExtension || str == null || !(str.equals("http://xml.apache.org/xalan") || str.equals(Constants.S_BUILTIN_OLD_EXTENSIONS_URL))) {
                            isUnknownTopLevel = true;
                        } else {
                            isExtension2 = true;
                        }
                    }
                    isUnknownTopLevel2 = isUnknownTopLevel;
                    if (isExtension) {
                        break;
                    }
                    p3 = p3.getParentElem();
                }
                isUnknownTopLevel = isExtension2;
                isExtension2 = isExtension;
                ElemTemplateElement elem2 = null;
                if (isExtension2) {
                    try {
                        elem = new ElemExtensionCall();
                    } catch (InstantiationException e4) {
                        InstantiationException ie = e4;
                        z = isLREAsStyleSheet;
                        stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, ie);
                        setPropertiesFromAttributes(stylesheetHandler, str3, attributes2, elem2);
                        elem2 = new ElemExtensionCall();
                        elem2.setLocaterInfo(handler.getLocator());
                        elem2.setPrefixes(handler.getNamespaceSupport());
                        ((ElemLiteralResult) elem2).setNamespace(str);
                        ((ElemLiteralResult) elem2).setLocalName(str2);
                        ((ElemLiteralResult) elem2).setRawName(str3);
                        setPropertiesFromAttributes(stylesheetHandler, str3, attributes2, elem2);
                        appendAndPush(stylesheetHandler, elem2);
                    } catch (IllegalAccessException iae) {
                    }
                } else if (isUnknownTopLevel) {
                    elem = (ElemTemplateElement) classObject.newInstance();
                } else if (isUnknownTopLevel2) {
                    elem = (ElemTemplateElement) classObject.newInstance();
                } else {
                    elem = (ElemTemplateElement) classObject.newInstance();
                }
                elem2 = elem;
                elem2.setDOMBackPointer(handler.getOriginatingNode());
                elem2.setLocaterInfo(handler.getLocator());
                elem2.setPrefixes(handler.getNamespaceSupport(), excludeXSLDecl2);
                if (elem2 instanceof ElemLiteralResult) {
                    ((ElemLiteralResult) elem2).setNamespace(str);
                    ((ElemLiteralResult) elem2).setLocalName(str2);
                    ((ElemLiteralResult) elem2).setRawName(str3);
                    ((ElemLiteralResult) elem2).setIsLiteralResultAsStylesheet(isLREAsStyleSheet);
                }
                z = isLREAsStyleSheet;
                setPropertiesFromAttributes(stylesheetHandler, str3, attributes2, elem2);
                if (!isExtension2 && (elem2 instanceof ElemLiteralResult) && ((ElemLiteralResult) elem2).containsExtensionElementURI(str)) {
                    elem2 = new ElemExtensionCall();
                    elem2.setLocaterInfo(handler.getLocator());
                    elem2.setPrefixes(handler.getNamespaceSupport());
                    ((ElemLiteralResult) elem2).setNamespace(str);
                    ((ElemLiteralResult) elem2).setLocalName(str2);
                    ((ElemLiteralResult) elem2).setRawName(str3);
                    setPropertiesFromAttributes(stylesheetHandler, str3, attributes2, elem2);
                }
                appendAndPush(stylesheetHandler, elem2);
            } catch (TransformerException e5) {
                te = e5;
                throw new SAXException(te);
            }
        } catch (TransformerConfigurationException tfe) {
            p2 = p;
            excludeXSLDecl = false;
            TransformerConfigurationException p6 = tfe;
            throw new TransformerException(tfe);
        } catch (TransformerException e6) {
            te = e6;
            throw new SAXException(te);
        }
    }

    protected Stylesheet getStylesheetRoot(StylesheetHandler handler) throws TransformerConfigurationException {
        StylesheetRoot stylesheet = new StylesheetRoot(handler.getSchema(), handler.getStylesheetProcessor().getErrorListener());
        if (handler.getStylesheetProcessor().isSecureProcessing()) {
            stylesheet.setSecureProcessing(true);
        }
        return stylesheet;
    }

    public void endElement(StylesheetHandler handler, String uri, String localName, String rawName) throws SAXException {
        ElemTemplateElement elem = handler.getElemTemplateElement();
        if ((elem instanceof ElemLiteralResult) && ((ElemLiteralResult) elem).getIsLiteralResultAsStylesheet()) {
            handler.popStylesheet();
        }
        super.endElement(handler, uri, localName, rawName);
    }

    private boolean declaredXSLNS(Stylesheet stylesheet) {
        List declaredPrefixes = stylesheet.getDeclaredPrefixes();
        int n = declaredPrefixes.size();
        for (int i = 0; i < n; i++) {
            if (((XMLNSDecl) declaredPrefixes.get(i)).getURI().equals(Constants.S_XSLNAMESPACEURL)) {
                return true;
            }
        }
        return false;
    }
}
