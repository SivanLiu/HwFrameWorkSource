package org.apache.xpath.patterns;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.objects.XObject;

public class ContextMatchStepPattern extends StepPattern {
    static final long serialVersionUID = -1888092779313211942L;

    public ContextMatchStepPattern(int axis, int paxis) {
        super(-1, axis, paxis);
    }

    public XObject execute(XPathContext xctxt) throws TransformerException {
        if (xctxt.getIteratorRoot() == xctxt.getCurrentNode()) {
            return getStaticScore();
        }
        return SCORE_NONE;
    }

    /* JADX WARNING: Missing block: B:57:0x00b4, code skipped:
            r15 = 9;
            r14 = r14 + 1;
            r0 = -1;
            r10 = 2;
            r11 = true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public XObject executeRelativePathPattern(XPathContext xctxt, StepPattern prevStep) throws TransformerException {
        XObject score;
        Throwable th;
        XPathContext xPathContext = xctxt;
        XObject score2 = NodeTest.SCORE_NONE;
        int context = xctxt.getCurrentNode();
        DTM dtm = xPathContext.getDTM(context);
        if (dtm != null) {
            int predContext = xctxt.getCurrentNode();
            int axis = this.m_axis;
            boolean needToTraverseAttrs = WalkerFactory.isDownwardAxisOfMany(axis);
            int i = 2;
            XObject score3 = true;
            boolean iterRootIsAttr = dtm.getNodeType(xctxt.getIteratorRoot()) == (short) 2;
            if (11 == axis && iterRootIsAttr) {
                axis = 15;
            }
            DTMAxisTraverser traverser = dtm.getAxisTraverser(axis);
            int relative = traverser.first(context);
            score = score2;
            while (true) {
                int i2 = -1;
                if (-1 == relative) {
                    break;
                }
                try {
                    boolean score4;
                    xPathContext.pushCurrentNode(relative);
                    score = execute(xctxt);
                    if (score != NodeTest.SCORE_NONE) {
                        if (executePredicates(xPathContext, dtm, context)) {
                            xctxt.popCurrentNode();
                            return score;
                        }
                        score = NodeTest.SCORE_NONE;
                    }
                    if (needToTraverseAttrs && iterRootIsAttr && score == dtm.getNodeType(relative)) {
                        int xaxis = 2;
                        XObject score5 = score;
                        int i3 = 0;
                        while (i3 < i) {
                            try {
                                DTMAxisTraverser atraverser = dtm.getAxisTraverser(xaxis);
                                int arelative = atraverser.first(relative);
                                while (true) {
                                    i = arelative;
                                    if (i2 == i) {
                                        break;
                                    }
                                    try {
                                        xPathContext.pushCurrentNode(i);
                                        XObject score6 = execute(xctxt);
                                        try {
                                            score3 = score6;
                                            if (score3 != NodeTest.SCORE_NONE) {
                                                try {
                                                    if (score3 != NodeTest.SCORE_NONE) {
                                                        xctxt.popCurrentNode();
                                                        xctxt.popCurrentNode();
                                                        return score3;
                                                    }
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    xctxt.popCurrentNode();
                                                    throw th;
                                                }
                                            }
                                            xctxt.popCurrentNode();
                                            arelative = atraverser.next(relative, i);
                                            score5 = score3;
                                            i2 = -1;
                                            score4 = true;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            score = score3;
                                            xctxt.popCurrentNode();
                                            throw th;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        score3 = score5;
                                        xctxt.popCurrentNode();
                                        throw th;
                                    }
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                score = score5;
                                xctxt.popCurrentNode();
                                throw th;
                            }
                        }
                        score = score5;
                    }
                    xctxt.popCurrentNode();
                    relative = traverser.next(context, relative);
                    i = 2;
                    score4 = true;
                } catch (Throwable th6) {
                    th = th6;
                    xctxt.popCurrentNode();
                    throw th;
                }
            }
        }
        score = score2;
        return score;
    }
}
