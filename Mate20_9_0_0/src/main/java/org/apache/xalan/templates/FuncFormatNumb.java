package org.apache.xalan.templates;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xpath.Expression;
import org.apache.xpath.XPathContext;
import org.apache.xpath.functions.Function3Args;
import org.apache.xpath.functions.WrongNumberArgsException;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncFormatNumb extends Function3Args {
    static final long serialVersionUID = -8869935264870858636L;

    public XObject execute(XPathContext xctxt) throws TransformerException {
        ElemTemplateElement templElem = (ElemTemplateElement) xctxt.getNamespaceContext();
        StylesheetRoot ss = templElem.getStylesheetRoot();
        DecimalFormat formatter = null;
        double num = getArg0().execute(xctxt).num();
        String patternStr = getArg1().execute(xctxt).str();
        if (patternStr.indexOf(164) > 0) {
            ss.error(XSLTErrorResources.ER_CURRENCY_SIGN_ILLEGAL);
        }
        try {
            DecimalFormatSymbols dfs;
            Expression arg2Expr = getArg2();
            if (arg2Expr != null) {
                dfs = ss.getDecimalFormatComposed(new QName(arg2Expr.execute(xctxt).str(), xctxt.getNamespaceContext()));
                if (dfs == null) {
                    warn(xctxt, XSLTErrorResources.WG_NO_DECIMALFORMAT_DECLARATION, new Object[]{dfName});
                } else {
                    formatter = new DecimalFormat();
                    formatter.setDecimalFormatSymbols(dfs);
                    formatter.applyLocalizedPattern(patternStr);
                }
            }
            if (formatter == null) {
                dfs = ss.getDecimalFormatComposed(new QName(""));
                if (dfs != null) {
                    formatter = new DecimalFormat();
                    formatter.setDecimalFormatSymbols(dfs);
                    formatter.applyLocalizedPattern(patternStr);
                } else {
                    dfs = new DecimalFormatSymbols(Locale.US);
                    dfs.setInfinity(Constants.ATTRVAL_INFINITY);
                    dfs.setNaN("NaN");
                    formatter = new DecimalFormat();
                    formatter.setDecimalFormatSymbols(dfs);
                    if (patternStr != null) {
                        formatter.applyLocalizedPattern(patternStr);
                    }
                }
            }
            return new XString(formatter.format(num));
        } catch (Exception e) {
            templElem.error(XSLTErrorResources.ER_MALFORMED_FORMAT_STRING, new Object[]{patternStr});
            return XString.EMPTYSTRING;
        }
    }

    public void warn(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        xctxt.getErrorListener().warning(new TransformerException(XSLMessages.createWarning(msg, args), (SAXSourceLocator) xctxt.getSAXLocator()));
    }

    public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
        if (argNum > 3 || argNum < 2) {
            reportWrongNumberArgs();
        }
    }

    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createMessage("ER_TWO_OR_THREE", null));
    }
}
