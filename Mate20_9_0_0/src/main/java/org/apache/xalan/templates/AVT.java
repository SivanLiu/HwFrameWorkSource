package org.apache.xalan.templates;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.processor.StylesheetHandler;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class AVT implements Serializable, XSLTVisitable {
    private static final int INIT_BUFFER_CHUNK_BITS = 8;
    private static final boolean USE_OBJECT_POOL = false;
    static final long serialVersionUID = 5167607155517042691L;
    private String m_name;
    private Vector m_parts = null;
    private String m_rawName;
    private String m_simpleString = null;
    private String m_uri;

    public String getRawName() {
        return this.m_rawName;
    }

    public void setRawName(String rawName) {
        this.m_rawName = rawName;
    }

    public String getName() {
        return this.m_name;
    }

    public void setName(String name) {
        this.m_name = name;
    }

    public String getURI() {
        return this.m_uri;
    }

    public void setURI(String uri) {
        this.m_uri = uri;
    }

    /* JADX WARNING: Removed duplicated region for block: B:93:0x01ad A:{LOOP_END, LOOP:0: B:6:0x004c->B:93:0x01ad, Catch:{ SAXException -> 0x01a6, all -> 0x01de }} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x019a A:{SYNTHETIC, Splitter:B:87:0x019a, EDGE_INSN: B:87:0x019a->B:88:? ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x019a A:{SYNTHETIC, Splitter:B:87:0x019a, EDGE_INSN: B:87:0x019a->B:88:? ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x01ad A:{LOOP_END, LOOP:0: B:6:0x004c->B:93:0x01ad, Catch:{ SAXException -> 0x01a6, all -> 0x01de }} */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x01ad A:{LOOP_END, LOOP:0: B:6:0x004c->B:93:0x01ad, Catch:{ SAXException -> 0x01a6, all -> 0x01de }} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x019a A:{SYNTHETIC, Splitter:B:87:0x019a, EDGE_INSN: B:87:0x019a->B:88:? ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x019a A:{SYNTHETIC, Splitter:B:87:0x019a, EDGE_INSN: B:87:0x019a->B:88:? ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x01ad A:{LOOP_END, LOOP:0: B:6:0x004c->B:93:0x01ad, Catch:{ SAXException -> 0x01a6, all -> 0x01de }} */
    /* JADX WARNING: Missing block: B:88:?, code skipped:
            r2.warn(org.apache.xalan.res.XSLTErrorResources.WG_ATTR_TEMPLATE, new java.lang.Object[]{r16});
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AVT(StylesheetHandler handler, String uri, String name, String rawName, String stringedValue, ElemTemplateElement owner) throws TransformerException {
        NoSuchElementException ex;
        Throwable th;
        StylesheetHandler stylesheetHandler = handler;
        String str = name;
        String str2 = stringedValue;
        Object[] objArr = null;
        this.m_uri = uri;
        this.m_name = str;
        this.m_rawName = rawName;
        boolean z = true;
        StringTokenizer tokenizer = new StringTokenizer(str2, "{}\"'", true);
        int nTokens = tokenizer.countTokens();
        ElemTemplateElement elemTemplateElement;
        if (nTokens < 2) {
            this.m_simpleString = str2;
            elemTemplateElement = owner;
        } else {
            FastStringBuffer buffer = new FastStringBuffer(6);
            FastStringBuffer exprBuffer = new FastStringBuffer(6);
            try {
                this.m_parts = new Vector(nTokens + 1);
                String lookahead = null;
                String t = null;
                String error = null;
                while (true) {
                    String error2 = error;
                    if (!tokenizer.hasMoreTokens()) {
                        elemTemplateElement = owner;
                        break;
                    }
                    int i;
                    if (lookahead != null) {
                        error = lookahead;
                        lookahead = null;
                    } else {
                        error = tokenizer.nextToken();
                    }
                    t = error;
                    if (t.length() == z) {
                        char charAt = t.charAt(0);
                        if (charAt == '\"' || charAt == '\'') {
                            elemTemplateElement = owner;
                            i = 2;
                            buffer.append(t);
                        } else {
                            char c = '{';
                            if (charAt != '{') {
                                if (charAt != '}') {
                                    buffer.append(t);
                                    elemTemplateElement = owner;
                                } else {
                                    String lookahead2 = tokenizer.nextToken();
                                    if (lookahead2.equals("}")) {
                                        buffer.append(lookahead2);
                                        error = null;
                                    } else {
                                        stylesheetHandler.warn(XSLTErrorResources.WG_FOUND_CURLYBRACE, objArr);
                                        buffer.append("}");
                                        elemTemplateElement = owner;
                                        lookahead = lookahead2;
                                    }
                                }
                                i = 2;
                            } else {
                                try {
                                    lookahead = tokenizer.nextToken();
                                    if (lookahead.equals("{")) {
                                        buffer.append(lookahead);
                                        error = null;
                                    } else {
                                        if (buffer.length() > 0) {
                                            try {
                                                this.m_parts.addElement(new AVTPartSimple(buffer.toString()));
                                                buffer.setLength(0);
                                            } catch (NoSuchElementException e) {
                                                ex = e;
                                                elemTemplateElement = owner;
                                                try {
                                                    i = 2;
                                                    error2 = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str, str2});
                                                    if (error2 != null) {
                                                    }
                                                } catch (SAXException se) {
                                                    throw new TransformerException(se);
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    throw th;
                                                }
                                            }
                                        }
                                        int i2 = 0;
                                        exprBuffer.setLength(0);
                                        while (lookahead != null) {
                                            if (lookahead.length() == 1) {
                                                charAt = lookahead.charAt(i2);
                                                if (charAt == '\"' || charAt == '\'') {
                                                    elemTemplateElement = owner;
                                                    exprBuffer.append(lookahead);
                                                    error = lookahead;
                                                    lookahead = tokenizer.nextToken();
                                                    while (!lookahead.equals(error)) {
                                                        exprBuffer.append(lookahead);
                                                        lookahead = tokenizer.nextToken();
                                                    }
                                                    exprBuffer.append(lookahead);
                                                    lookahead = tokenizer.nextToken();
                                                } else if (charAt == c) {
                                                    elemTemplateElement = owner;
                                                    try {
                                                        error2 = XSLMessages.createMessage(XSLTErrorResources.ER_NO_CURLYBRACE, null);
                                                        lookahead = null;
                                                    } catch (NoSuchElementException e2) {
                                                        ex = e2;
                                                        i = 2;
                                                        error2 = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str, str2});
                                                        if (error2 != null) {
                                                        }
                                                    }
                                                } else if (charAt != '}') {
                                                    exprBuffer.append(lookahead);
                                                    lookahead = tokenizer.nextToken();
                                                    i2 = 0;
                                                } else {
                                                    buffer.setLength(0);
                                                    try {
                                                        this.m_parts.addElement(new AVTPartXPath(stylesheetHandler.createXPath(exprBuffer.toString(), owner)));
                                                        lookahead = null;
                                                    } catch (NoSuchElementException e3) {
                                                        ex = e3;
                                                        i = 2;
                                                        error2 = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str, str2});
                                                        if (error2 != null) {
                                                        }
                                                    }
                                                }
                                            } else {
                                                elemTemplateElement = owner;
                                                exprBuffer.append(lookahead);
                                                lookahead = tokenizer.nextToken();
                                            }
                                            i2 = 0;
                                            c = '{';
                                        }
                                        elemTemplateElement = owner;
                                        if (error2 != null) {
                                        }
                                        i = 2;
                                    }
                                } catch (NoSuchElementException e4) {
                                    ex = e4;
                                    Object[] objArr2 = objArr;
                                    elemTemplateElement = owner;
                                    i = 2;
                                    error2 = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str, str2});
                                    if (error2 != null) {
                                    }
                                }
                            }
                            elemTemplateElement = owner;
                            lookahead = error;
                            i = 2;
                        }
                    } else {
                        elemTemplateElement = owner;
                        i = 2;
                        buffer.append(t);
                    }
                    if (error2 != null) {
                        break;
                    }
                    int i3 = i;
                    error = error2;
                    objArr = null;
                    z = true;
                }
                if (buffer.length() > 0) {
                    this.m_parts.addElement(new AVTPartSimple(buffer.toString()));
                    buffer.setLength(0);
                }
            } catch (SAXException se2) {
                throw new TransformerException(se2);
            } catch (Throwable th3) {
                th = th3;
                elemTemplateElement = owner;
                throw th;
            }
        }
        if (this.m_parts == null && this.m_simpleString == null) {
            this.m_simpleString = "";
        }
    }

    public String getSimpleString() {
        if (this.m_simpleString != null) {
            return this.m_simpleString;
        }
        if (this.m_parts == null) {
            return "";
        }
        FastStringBuffer buf = getBuffer();
        int n = this.m_parts.size();
        int i = 0;
        while (i < n) {
            try {
                buf.append(((AVTPart) this.m_parts.elementAt(i)).getSimpleString());
                i++;
            } catch (Throwable th) {
                buf.setLength(0);
            }
        }
        String out = buf.toString();
        buf.setLength(0);
        return out;
    }

    public String evaluate(XPathContext xctxt, int context, PrefixResolver nsNode) throws TransformerException {
        if (this.m_simpleString != null) {
            return this.m_simpleString;
        }
        if (this.m_parts == null) {
            return "";
        }
        FastStringBuffer buf = getBuffer();
        int n = this.m_parts.size();
        int i = 0;
        while (i < n) {
            try {
                ((AVTPart) this.m_parts.elementAt(i)).evaluate(xctxt, buf, context, nsNode);
                i++;
            } catch (Throwable th) {
                buf.setLength(0);
            }
        }
        String out = buf.toString();
        buf.setLength(0);
        return out;
    }

    public boolean isContextInsensitive() {
        return this.m_simpleString != null;
    }

    public boolean canTraverseOutsideSubtree() {
        if (this.m_parts != null) {
            int n = this.m_parts.size();
            for (int i = 0; i < n; i++) {
                if (((AVTPart) this.m_parts.elementAt(i)).canTraverseOutsideSubtree()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fixupVariables(Vector vars, int globalsSize) {
        if (this.m_parts != null) {
            int n = this.m_parts.size();
            for (int i = 0; i < n; i++) {
                ((AVTPart) this.m_parts.elementAt(i)).fixupVariables(vars, globalsSize);
            }
        }
    }

    public void callVisitors(XSLTVisitor visitor) {
        if (visitor.visitAVT(this) && this.m_parts != null) {
            int n = this.m_parts.size();
            for (int i = 0; i < n; i++) {
                ((AVTPart) this.m_parts.elementAt(i)).callVisitors(visitor);
            }
        }
    }

    public boolean isSimple() {
        return this.m_simpleString != null;
    }

    private final FastStringBuffer getBuffer() {
        return new FastStringBuffer(8);
    }
}
