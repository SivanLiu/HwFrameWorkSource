package org.apache.xalan.templates;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.Expression;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.SourceTreeManager;
import org.apache.xpath.XPathContext;
import org.apache.xpath.functions.Function2Args;
import org.apache.xpath.functions.WrongNumberArgsException;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;

public class FuncDocument extends Function2Args {
    static final long serialVersionUID = 2483304325971281424L;

    public XObject execute(XPathContext xctxt) throws TransformerException {
        FuncDocument funcDocument = this;
        XPathContext xPathContext = xctxt;
        int context = xctxt.getCurrentNode();
        int docContext = xPathContext.getDTM(context).getDocumentRoot(context);
        XObject arg = getArg0().execute(xPathContext);
        String base = "";
        Expression arg1Expr = getArg1();
        Object[] objArr = null;
        int i = -1;
        if (arg1Expr != null) {
            XObject arg2 = arg1Expr.execute(xPathContext);
            if (4 == arg2.getType()) {
                int baseNode = arg2.iter().nextNode();
                if (baseNode == -1) {
                    funcDocument.warn(xPathContext, XSLTErrorResources.WG_EMPTY_SECOND_ARG, null);
                    return new XNodeSet(xctxt.getDTMManager());
                }
                base = xPathContext.getDTM(baseNode).getDocumentBaseURI();
            } else {
                arg2.iter();
            }
        } else {
            funcDocument.assertion(xctxt.getNamespaceContext() != null, "Namespace context can not be null!");
            base = xctxt.getNamespaceContext().getBaseIdentifier();
        }
        XNodeSet nodes = new XNodeSet(xctxt.getDTMManager());
        NodeSetDTM mnl = nodes.mutableNodeset();
        DTMIterator iterator = 4 == arg.getType() ? arg.iter() : null;
        String base2 = base;
        int pos = -1;
        while (true) {
            if (iterator != null) {
                int nextNode = iterator.nextNode();
                pos = nextNode;
                if (i == nextNode) {
                    break;
                }
            }
            XMLString ref = iterator != null ? xPathContext.getDTM(pos).getStringValue(pos) : arg.xstr();
            if (arg1Expr == null && i != pos) {
                base2 = xPathContext.getDTM(pos).getDocumentBaseURI();
            }
            if (ref != null) {
                if (i == docContext) {
                    funcDocument.error(xPathContext, XSLTErrorResources.ER_NO_CONTEXT_OWNERDOC, objArr);
                }
                int indexOfColon = ref.indexOf(58);
                int indexOfSlash = ref.indexOf(47);
                if (!(indexOfColon == i || indexOfSlash == i || indexOfColon >= indexOfSlash)) {
                    base2 = null;
                }
                i = funcDocument.getDoc(xPathContext, context, ref.toString(), base2);
                if (!(-1 == i || mnl.contains(i))) {
                    mnl.addElement(i);
                }
                if (iterator == null || i == -1) {
                    break;
                }
                i = -1;
                funcDocument = this;
                objArr = null;
            }
        }
        return nodes;
    }

    /* JADX WARNING: Removed duplicated region for block: B:66:0x0172  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int getDoc(XPathContext xctxt, int context, String uri, String base) throws TransformerException {
        IOException ioe;
        SourceTreeManager sourceTreeManager;
        Throwable throwable;
        TransformerException te;
        XPathContext xPathContext = xctxt;
        String str = base;
        SourceTreeManager treeMgr = xctxt.getSourceTreeManager();
        String uri2;
        try {
            uri2 = uri;
            try {
                Source source = treeMgr.resolveURI(str, uri2, xctxt.getSAXLocator());
                int newDoc = treeMgr.getNode(source);
                if (-1 != newDoc) {
                    return newDoc;
                }
                if (uri.length() == 0) {
                    uri2 = xctxt.getNamespaceContext().getBaseIdentifier();
                    try {
                        source = treeMgr.resolveURI(str, uri2, xctxt.getSAXLocator());
                    } catch (IOException ioe2) {
                        throw new TransformerException(ioe2.getMessage(), xctxt.getSAXLocator(), ioe2);
                    }
                }
                String diagnosticsString = null;
                if (uri2 != null) {
                    StringBuilder stringBuilder;
                    try {
                        if (uri2.length() > 0) {
                            newDoc = treeMgr.getSourceTree(source, xctxt.getSAXLocator(), xPathContext);
                            sourceTreeManager = treeMgr;
                            if (-1 == newDoc) {
                                if (diagnosticsString != null) {
                                    warn(xPathContext, XSLTErrorResources.WG_CANNOT_LOAD_REQUESTED_DOC, new Object[]{diagnosticsString});
                                } else {
                                    String stringBuilder2;
                                    String str2 = XSLTErrorResources.WG_CANNOT_LOAD_REQUESTED_DOC;
                                    Object[] objArr = new Object[1];
                                    if (uri2 == null) {
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append(str == null ? "" : str);
                                        stringBuilder.append(uri2);
                                        stringBuilder2 = stringBuilder.toString();
                                    } else {
                                        stringBuilder2 = uri2.toString();
                                    }
                                    objArr[0] = stringBuilder2;
                                    warn(xPathContext, str2, objArr);
                                }
                            }
                            return newDoc;
                        }
                    } catch (Throwable th) {
                        throwable = th;
                        newDoc = -1;
                        while (throwable instanceof WrappedRuntimeException) {
                            throwable = ((WrappedRuntimeException) throwable).getException();
                        }
                        if ((throwable instanceof NullPointerException) || (throwable instanceof ClassCastException)) {
                            WrappedRuntimeException wrappedRuntimeException = new WrappedRuntimeException((Exception) throwable);
                        } else {
                            PrintWriter diagnosticsWriter = new PrintWriter(new StringWriter());
                            if (throwable instanceof TransformerException) {
                                Throwable e = (TransformerException) throwable;
                                while (e != null) {
                                    if (e.getMessage() != null) {
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append(" (");
                                        stringBuilder3.append(e.getClass().getName());
                                        stringBuilder3.append("): ");
                                        stringBuilder3.append(e.getMessage());
                                        diagnosticsWriter.println(stringBuilder3.toString());
                                    }
                                    if (e instanceof TransformerException) {
                                        TransformerException spe2 = (TransformerException) e;
                                        SourceLocator locator = spe2.getLocator();
                                        if (locator == null || locator.getSystemId() == null) {
                                            sourceTreeManager = treeMgr;
                                        } else {
                                            stringBuilder = new StringBuilder();
                                            sourceTreeManager = treeMgr;
                                            stringBuilder.append("   ID: ");
                                            stringBuilder.append(locator.getSystemId());
                                            stringBuilder.append(" Line #");
                                            stringBuilder.append(locator.getLineNumber());
                                            stringBuilder.append(" Column #");
                                            stringBuilder.append(locator.getColumnNumber());
                                            diagnosticsWriter.println(stringBuilder.toString());
                                        }
                                        treeMgr = spe2.getException();
                                        if (treeMgr instanceof WrappedRuntimeException) {
                                            treeMgr = ((WrappedRuntimeException) treeMgr).getException();
                                        }
                                        e = treeMgr;
                                        treeMgr = sourceTreeManager;
                                    } else {
                                        e = null;
                                    }
                                }
                                sourceTreeManager = treeMgr;
                            } else {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append(" (");
                                stringBuilder4.append(throwable.getClass().getName());
                                stringBuilder4.append("): ");
                                stringBuilder4.append(throwable.getMessage());
                                diagnosticsWriter.println(stringBuilder4.toString());
                            }
                            diagnosticsString = throwable.getMessage();
                        }
                    }
                }
                throwable = "WG_CANNOT_MAKE_URL_FROM";
                Object[] objArr2 = new Object[1];
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append(str == null ? "" : str);
                stringBuilder5.append(uri2);
                objArr2[0] = stringBuilder5.toString();
                warn(xPathContext, throwable, objArr2);
                sourceTreeManager = treeMgr;
                if (-1 == newDoc) {
                }
                return newDoc;
            } catch (IOException e2) {
                ioe2 = e2;
                sourceTreeManager = treeMgr;
                throw new TransformerException(ioe2.getMessage(), xctxt.getSAXLocator(), ioe2);
            } catch (TransformerException e3) {
                te = e3;
                sourceTreeManager = treeMgr;
                throw new TransformerException(te);
            }
        } catch (IOException e4) {
            ioe2 = e4;
            uri2 = uri;
            sourceTreeManager = treeMgr;
            throw new TransformerException(ioe2.getMessage(), xctxt.getSAXLocator(), ioe2);
        } catch (TransformerException e5) {
            te = e5;
            uri2 = uri;
            sourceTreeManager = treeMgr;
            throw new TransformerException(te);
        }
    }

    public void error(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        String formattedMsg = XSLMessages.createMessage(msg, args);
        ErrorListener errHandler = xctxt.getErrorListener();
        TransformerException spe = new TransformerException(formattedMsg, xctxt.getSAXLocator());
        if (errHandler != null) {
            errHandler.error(spe);
        } else {
            System.out.println(formattedMsg);
        }
    }

    public void warn(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        String formattedMsg = XSLMessages.createWarning(msg, args);
        ErrorListener errHandler = xctxt.getErrorListener();
        TransformerException spe = new TransformerException(formattedMsg, xctxt.getSAXLocator());
        if (errHandler != null) {
            errHandler.warning(spe);
        } else {
            System.out.println(formattedMsg);
        }
    }

    public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
        if (argNum < 1 || argNum > 2) {
            reportWrongNumberArgs();
        }
    }

    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createMessage(XSLTErrorResources.ER_ONE_OR_TWO, null));
    }

    public boolean isNodesetExpr() {
        return true;
    }
}
