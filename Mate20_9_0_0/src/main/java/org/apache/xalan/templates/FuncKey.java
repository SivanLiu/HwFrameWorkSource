package org.apache.xalan.templates;

import java.util.Hashtable;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.KeyManager;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.UnionPathIterator;
import org.apache.xpath.functions.Function2Args;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;

public class FuncKey extends Function2Args {
    private static Boolean ISTRUE = new Boolean(true);
    static final long serialVersionUID = 9089293100115347340L;

    public XObject execute(XPathContext xctxt) throws TransformerException {
        XNodeSet ns;
        XPathContext xPathContext = xctxt;
        TransformerImpl transformer = (TransformerImpl) xctxt.getOwnerObject();
        int context = xctxt.getCurrentNode();
        int docContext = xPathContext.getDTM(context).getDocumentRoot(context);
        QName keyname = new QName(getArg0().execute(xPathContext).str(), xctxt.getNamespaceContext());
        XObject arg = getArg1().execute(xPathContext);
        boolean argIsNodeSetDTM = 4 == arg.getType();
        KeyManager kmgr = transformer.getKeyManager();
        if (argIsNodeSetDTM) {
            ns = (XNodeSet) arg;
            ns.setShouldCacheNodes(true);
            if (ns.getLength() <= 1) {
                argIsNodeSetDTM = false;
            }
        }
        if (argIsNodeSetDTM) {
            Hashtable usedrefs = null;
            DTMIterator ni = arg.iter();
            DTMIterator upi = new UnionPathIterator();
            upi.exprSetParent(this);
            while (true) {
                int nextNode = ni.nextNode();
                int pos = nextNode;
                if (-1 != nextNode) {
                    DTM dtm = xPathContext.getDTM(pos);
                    XMLString ref = dtm.getStringValue(pos);
                    if (ref == null) {
                        DTM dtm2 = dtm;
                    } else {
                        Hashtable usedrefs2;
                        if (usedrefs == null) {
                            usedrefs = new Hashtable();
                        }
                        DTM dtm3;
                        if (usedrefs.get(ref) != null) {
                            usedrefs2 = usedrefs;
                            dtm3 = dtm;
                        } else {
                            usedrefs.put(ref, ISTRUE);
                            usedrefs2 = usedrefs;
                            dtm3 = dtm;
                            XNodeSet nl = kmgr.getNodeSetDTMByKey(xPathContext, docContext, keyname, ref, xctxt.getNamespaceContext());
                            nl.setRoot(xctxt.getCurrentNode(), xPathContext);
                            upi.addIterator(nl);
                        }
                        usedrefs = usedrefs2;
                    }
                } else {
                    int i = pos;
                    upi.setRoot(xctxt.getCurrentNode(), xPathContext);
                    XObject xObject = arg;
                    return new XNodeSet(upi);
                }
            }
        }
        ns = kmgr.getNodeSetDTMByKey(xPathContext, docContext, keyname, arg.xstr(), xctxt.getNamespaceContext());
        ns.setRoot(xctxt.getCurrentNode(), xPathContext);
        return ns;
    }
}
