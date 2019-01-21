package org.apache.xpath;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Vector;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.DefaultErrorHandler;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.FunctionTable;
import org.apache.xpath.compiler.XPathParser;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;
import org.apache.xpath.res.XPATHMessages;
import org.w3c.dom.Node;

public class XPath implements Serializable, ExpressionOwner {
    private static final boolean DEBUG_MATCHES = false;
    public static final int MATCH = 1;
    public static final double MATCH_SCORE_NODETEST = -0.5d;
    public static final double MATCH_SCORE_NONE = Double.NEGATIVE_INFINITY;
    public static final double MATCH_SCORE_NSWILD = -0.25d;
    public static final double MATCH_SCORE_OTHER = 0.5d;
    public static final double MATCH_SCORE_QNAME = 0.0d;
    public static final int SELECT = 0;
    static final long serialVersionUID = 3976493477939110553L;
    private transient FunctionTable m_funcTable;
    private Expression m_mainExp;
    String m_patternString;

    private void initFunctionTable() {
        this.m_funcTable = new FunctionTable();
    }

    public Expression getExpression() {
        return this.m_mainExp;
    }

    public void fixupVariables(Vector vars, int globalsSize) {
        this.m_mainExp.fixupVariables(vars, globalsSize);
    }

    public void setExpression(Expression exp) {
        if (this.m_mainExp != null) {
            exp.exprSetParent(this.m_mainExp.exprGetParent());
        }
        this.m_mainExp = exp;
    }

    public SourceLocator getLocator() {
        return this.m_mainExp;
    }

    public String getPatternString() {
        return this.m_patternString;
    }

    public XPath(String exprString, SourceLocator locator, PrefixResolver prefixResolver, int type, ErrorListener errorListener) throws TransformerException {
        this.m_funcTable = null;
        initFunctionTable();
        if (errorListener == null) {
            errorListener = new DefaultErrorHandler();
        }
        this.m_patternString = exprString;
        XPathParser parser = new XPathParser(errorListener, locator);
        Compiler compiler = new Compiler(errorListener, locator, this.m_funcTable);
        if (type == 0) {
            parser.initXPath(compiler, exprString, prefixResolver);
        } else if (1 == type) {
            parser.initMatchPattern(compiler, exprString, prefixResolver);
        } else {
            throw new RuntimeException(XPATHMessages.createXPATHMessage(XPATHErrorResources.ER_CANNOT_DEAL_XPATH_TYPE, new Object[]{Integer.toString(type)}));
        }
        Expression expr = compiler.compile(0);
        setExpression(expr);
        if (locator != null && (locator instanceof ExpressionNode)) {
            expr.exprSetParent((ExpressionNode) locator);
        }
    }

    public XPath(String exprString, SourceLocator locator, PrefixResolver prefixResolver, int type, ErrorListener errorListener, FunctionTable aTable) throws TransformerException {
        this.m_funcTable = null;
        this.m_funcTable = aTable;
        if (errorListener == null) {
            errorListener = new DefaultErrorHandler();
        }
        this.m_patternString = exprString;
        XPathParser parser = new XPathParser(errorListener, locator);
        Compiler compiler = new Compiler(errorListener, locator, this.m_funcTable);
        if (type == 0) {
            parser.initXPath(compiler, exprString, prefixResolver);
        } else if (1 == type) {
            parser.initMatchPattern(compiler, exprString, prefixResolver);
        } else {
            throw new RuntimeException(XPATHMessages.createXPATHMessage(XPATHErrorResources.ER_CANNOT_DEAL_XPATH_TYPE, new Object[]{Integer.toString(type)}));
        }
        Expression expr = compiler.compile(0);
        setExpression(expr);
        if (locator != null && (locator instanceof ExpressionNode)) {
            expr.exprSetParent((ExpressionNode) locator);
        }
    }

    public XPath(String exprString, SourceLocator locator, PrefixResolver prefixResolver, int type) throws TransformerException {
        this(exprString, locator, prefixResolver, type, null);
    }

    public XPath(Expression expr) {
        this.m_funcTable = null;
        setExpression(expr);
        initFunctionTable();
    }

    public XObject execute(XPathContext xctxt, Node contextNode, PrefixResolver namespaceContext) throws TransformerException {
        return execute(xctxt, xctxt.getDTMHandleFromNode(contextNode), namespaceContext);
    }

    public XObject execute(XPathContext xctxt, int contextNode, PrefixResolver namespaceContext) throws TransformerException {
        TransformerException te;
        xctxt.pushNamespaceContext(namespaceContext);
        xctxt.pushCurrentNodeAndExpression(contextNode, contextNode);
        XObject xobj = null;
        try {
            xobj = this.m_mainExp.execute(xctxt);
        } catch (TransformerException te2) {
            te2.setLocator(getLocator());
            ErrorListener el = xctxt.getErrorListener();
            if (el != null) {
                el.error(te2);
            } else {
                throw te2;
            }
        } catch (Exception e) {
            Exception e2 = e;
            while (e2 instanceof WrappedRuntimeException) {
                e2 = ((WrappedRuntimeException) e2).getException();
            }
            String msg = e2.getMessage();
            if (msg == null || msg.length() == 0) {
                msg = XPATHMessages.createXPATHMessage(XPATHErrorResources.ER_XPATH_ERROR, null);
            }
            te2 = new TransformerException(msg, getLocator(), e2);
            ErrorListener el2 = xctxt.getErrorListener();
            if (el2 != null) {
                el2.fatalError(te2);
            } else {
                throw te2;
            }
        } catch (Throwable th) {
            xctxt.popNamespaceContext();
            xctxt.popCurrentNodeAndExpression();
        }
        xctxt.popNamespaceContext();
        xctxt.popCurrentNodeAndExpression();
        return xobj;
    }

    /* JADX WARNING: Missing block: B:24:0x005c, code skipped:
            r5.popNamespaceContext();
            r5.popCurrentNodeAndExpression();
     */
    /* JADX WARNING: Missing block: B:25:0x0064, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean bool(XPathContext xctxt, int contextNode, PrefixResolver namespaceContext) throws TransformerException {
        xctxt.pushNamespaceContext(namespaceContext);
        xctxt.pushCurrentNodeAndExpression(contextNode, contextNode);
        try {
            boolean bool = this.m_mainExp.bool(xctxt);
            xctxt.popNamespaceContext();
            xctxt.popCurrentNodeAndExpression();
            return bool;
        } catch (TransformerException te) {
            te.setLocator(getLocator());
            ErrorListener el = xctxt.getErrorListener();
            if (el != null) {
                el.error(te);
            } else {
                throw te;
            }
        } catch (Exception e) {
            Exception e2 = e;
            while (e2 instanceof WrappedRuntimeException) {
                e2 = ((WrappedRuntimeException) e2).getException();
            }
            String msg = e2.getMessage();
            if (msg == null || msg.length() == 0) {
                msg = XPATHMessages.createXPATHMessage(XPATHErrorResources.ER_XPATH_ERROR, null);
            }
            TransformerException te2 = new TransformerException(msg, getLocator(), e2);
            ErrorListener el2 = xctxt.getErrorListener();
            if (el2 != null) {
                el2.fatalError(te2);
            } else {
                throw te2;
            }
        } catch (Throwable th) {
            xctxt.popNamespaceContext();
            xctxt.popCurrentNodeAndExpression();
        }
    }

    public double getMatchScore(XPathContext xctxt, int context) throws TransformerException {
        xctxt.pushCurrentNode(context);
        xctxt.pushCurrentExpressionNode(context);
        try {
            double num = this.m_mainExp.execute(xctxt).num();
            return num;
        } finally {
            xctxt.popCurrentNode();
            xctxt.popCurrentExpressionNode();
        }
    }

    public void warn(XPathContext xctxt, int sourceNode, String msg, Object[] args) throws TransformerException {
        String fmsg = XPATHMessages.createXPATHWarning(msg, args);
        ErrorListener ehandler = xctxt.getErrorListener();
        if (ehandler != null) {
            ehandler.warning(new TransformerException(fmsg, (SAXSourceLocator) xctxt.getSAXLocator()));
        }
    }

    public void assertion(boolean b, String msg) {
        if (!b) {
            throw new RuntimeException(XPATHMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{msg}));
        }
    }

    public void error(XPathContext xctxt, int sourceNode, String msg, Object[] args) throws TransformerException {
        String fmsg = XPATHMessages.createXPATHMessage(msg, args);
        ErrorListener ehandler = xctxt.getErrorListener();
        if (ehandler != null) {
            ehandler.fatalError(new TransformerException(fmsg, (SAXSourceLocator) xctxt.getSAXLocator()));
            return;
        }
        SourceLocator slocator = xctxt.getSAXLocator();
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(fmsg);
        stringBuilder.append("; file ");
        stringBuilder.append(slocator.getSystemId());
        stringBuilder.append("; line ");
        stringBuilder.append(slocator.getLineNumber());
        stringBuilder.append("; column ");
        stringBuilder.append(slocator.getColumnNumber());
        printStream.println(stringBuilder.toString());
    }

    public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
        this.m_mainExp.callVisitors(this, visitor);
    }
}
