package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.utils.IntStack;
import org.apache.xml.utils.QName;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class ElemApplyTemplates extends ElemCallTemplate {
    static final long serialVersionUID = 2903125371542621004L;
    private boolean m_isDefaultTemplate = false;
    private QName m_mode = null;

    public void setMode(QName mode) {
        this.m_mode = mode;
    }

    public QName getMode() {
        return this.m_mode;
    }

    public void setIsDefaultTemplate(boolean b) {
        this.m_isDefaultTemplate = b;
    }

    public int getXSLToken() {
        return 50;
    }

    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
    }

    public String getNodeName() {
        return Constants.ELEMNAME_APPLY_TEMPLATES_STRING;
    }

    public void execute(TransformerImpl transformer) throws TransformerException {
        boolean pushMode = false;
        transformer.pushCurrentTemplateRuleIsNull(false);
        try {
            QName mode = transformer.getMode();
            if (!this.m_isDefaultTemplate && ((mode == null && this.m_mode != null) || !(mode == null || mode.equals(this.m_mode)))) {
                pushMode = true;
                transformer.pushMode(this.m_mode);
            }
            transformSelectedNodes(transformer);
        } finally {
            if (pushMode) {
                transformer.popMode();
            }
            transformer.popCurrentTemplateRuleIsNull();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:86:0x01e2 A:{Catch:{ all -> 0x022e, SAXException -> 0x0260, all -> 0x0259 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x01dd A:{Catch:{ all -> 0x022e, SAXException -> 0x0260, all -> 0x0259 }} */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x02df  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x02df  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x02df  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x02df  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x02df  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x02df  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x02df  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02f7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x02ff  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void transformSelectedNodes(TransformerImpl transformer) throws TransformerException {
        SAXException se;
        int thisframe;
        boolean z;
        Throwable dtm;
        ElemApplyTemplates elemApplyTemplates = this;
        TransformerImpl transformerImpl = transformer;
        XPathContext xctxt = transformer.getXPathContext();
        int sourceNode = xctxt.getCurrentNode();
        DTMIterator sourceNodes = elemApplyTemplates.m_selectExpression.asIterator(xctxt, sourceNode);
        VariableStack vars = xctxt.getVarStack();
        int nParams = getParamElemCount();
        int thisframe2 = vars.getStackFrame();
        boolean pushContextNodeListFlag = false;
        int sourceNode2;
        try {
            xctxt.pushCurrentNode(-1);
            xctxt.pushCurrentExpressionNode(-1);
            xctxt.pushSAXLocatorNull();
            transformerImpl.pushElemTemplateElement(null);
            Vector keys = elemApplyTemplates.m_sortElems == null ? null : transformerImpl.processSortKeys(elemApplyTemplates, sourceNode);
            if (keys != null) {
                try {
                    sourceNodes = elemApplyTemplates.sortNodes(xctxt, keys, sourceNodes);
                } catch (SAXException e) {
                    se = e;
                    thisframe = thisframe2;
                    try {
                        transformer.getErrorListener().fatalError(new TransformerException(se));
                        if (nParams > 0) {
                            vars.unlink(thisframe);
                        }
                        xctxt.popSAXLocator();
                        if (pushContextNodeListFlag) {
                            xctxt.popContextNodeList();
                        }
                        transformer.popElemTemplateElement();
                        xctxt.popCurrentExpressionNode();
                        xctxt.popCurrentNode();
                        sourceNodes.detach();
                        z = pushContextNodeListFlag;
                    } catch (Throwable th) {
                        dtm = th;
                        z = pushContextNodeListFlag;
                        if (nParams > 0) {
                        }
                        xctxt.popSAXLocator();
                        if (z) {
                        }
                        transformer.popElemTemplateElement();
                        xctxt.popCurrentExpressionNode();
                        xctxt.popCurrentNode();
                        sourceNodes.detach();
                        throw dtm;
                    }
                } catch (Throwable th2) {
                    dtm = th2;
                    z = pushContextNodeListFlag;
                    thisframe = thisframe2;
                    if (nParams > 0) {
                        vars.unlink(thisframe);
                    }
                    xctxt.popSAXLocator();
                    if (z) {
                        xctxt.popContextNodeList();
                    }
                    transformer.popElemTemplateElement();
                    xctxt.popCurrentExpressionNode();
                    xctxt.popCurrentNode();
                    sourceNodes.detach();
                    throw dtm;
                }
            }
            DTMIterator sourceNodes2 = sourceNodes;
            try {
                int argsFrame;
                SerializationHandler rth = transformer.getSerializationHandler();
                StylesheetRoot sroot = transformer.getStylesheet();
                TemplateList tl = sroot.getTemplateListComposed();
                StylesheetRoot sroot2 = sroot;
                boolean quiet = transformer.getQuietConflictWarnings();
                DTM dtm2 = xctxt.getDTM(sourceNode);
                if (nParams > 0) {
                    try {
                        int argsFrame2 = vars.link(nParams);
                        vars.setStackFrame(thisframe2);
                        int argsFrame3 = 0;
                        while (true) {
                            int i = argsFrame3;
                            if (i >= nParams) {
                                break;
                            }
                            ElemWithParam ewp = elemApplyTemplates.m_paramElems[i];
                            ElemWithParam elemWithParam = ewp;
                            argsFrame = argsFrame2;
                            vars.setLocalVariable(i, ewp.getValue(transformerImpl, sourceNode), argsFrame);
                            argsFrame3 = i + 1;
                            argsFrame2 = argsFrame;
                        }
                        argsFrame = argsFrame2;
                        vars.setStackFrame(argsFrame);
                    } catch (SAXException e2) {
                        se = e2;
                        sourceNodes = sourceNodes2;
                        thisframe = thisframe2;
                        transformer.getErrorListener().fatalError(new TransformerException(se));
                        if (nParams > 0) {
                        }
                        xctxt.popSAXLocator();
                        if (pushContextNodeListFlag) {
                        }
                        transformer.popElemTemplateElement();
                        xctxt.popCurrentExpressionNode();
                        xctxt.popCurrentNode();
                        sourceNodes.detach();
                        z = pushContextNodeListFlag;
                    } catch (Throwable th3) {
                        dtm = th3;
                        z = pushContextNodeListFlag;
                        sourceNodes = sourceNodes2;
                        thisframe = thisframe2;
                        if (nParams > 0) {
                        }
                        xctxt.popSAXLocator();
                        if (z) {
                        }
                        transformer.popElemTemplateElement();
                        xctxt.popCurrentExpressionNode();
                        xctxt.popCurrentNode();
                        sourceNodes.detach();
                        throw dtm;
                    }
                }
                argsFrame = -1;
                xctxt.pushContextNodeList(sourceNodes2);
                z = true;
                try {
                    IntStack currentNodes = xctxt.getCurrentNodeStack();
                    IntStack currentExpressionNodes = xctxt.getCurrentExpressionNodeStack();
                    DTM dtm3 = dtm2;
                    while (true) {
                        IntStack currentExpressionNodes2 = currentExpressionNodes;
                        int nextNode = sourceNodes2.nextNode();
                        int child = nextNode;
                        int argsFrame4 = argsFrame;
                        DTMIterator sourceNodes3;
                        int thisframe3;
                        if (-1 != nextNode) {
                            SerializationHandler rth2;
                            int exNodeType;
                            short nodeType;
                            QName mode;
                            DTM dtm4;
                            Vector keys2;
                            IntStack currentExpressionNodes3;
                            IntStack currentNodes2;
                            sourceNode2 = sourceNode;
                            sourceNode = child;
                            try {
                                currentNodes.setTop(sourceNode);
                                IntStack currentNodes3 = currentNodes;
                                currentNodes = currentExpressionNodes2;
                                currentNodes.setTop(sourceNode);
                                if (xctxt.getDTM(sourceNode) != dtm3) {
                                    try {
                                        dtm3 = xctxt.getDTM(sourceNode);
                                    } catch (SAXException e3) {
                                        se = e3;
                                        sourceNodes = sourceNodes2;
                                        thisframe = thisframe2;
                                        pushContextNodeListFlag = true;
                                        transformer.getErrorListener().fatalError(new TransformerException(se));
                                        if (nParams > 0) {
                                        }
                                        xctxt.popSAXLocator();
                                        if (pushContextNodeListFlag) {
                                        }
                                        transformer.popElemTemplateElement();
                                        xctxt.popCurrentExpressionNode();
                                        xctxt.popCurrentNode();
                                        sourceNodes.detach();
                                        z = pushContextNodeListFlag;
                                    } catch (Throwable th4) {
                                        dtm = th4;
                                        sourceNodes = sourceNodes2;
                                        thisframe = thisframe2;
                                        if (nParams > 0) {
                                        }
                                        xctxt.popSAXLocator();
                                        if (z) {
                                        }
                                        transformer.popElemTemplateElement();
                                        xctxt.popCurrentExpressionNode();
                                        xctxt.popCurrentNode();
                                        sourceNodes.detach();
                                        throw dtm;
                                    }
                                }
                                rth2 = rth;
                                exNodeType = dtm3.getExpandedTypeID(sourceNode);
                                nodeType = dtm3.getNodeType(sourceNode);
                                sourceNodes3 = sourceNodes2;
                                mode = transformer.getMode();
                                dtm4 = dtm3;
                                keys2 = keys;
                                currentExpressionNodes3 = currentNodes;
                                thisframe3 = thisframe2;
                                currentNodes2 = currentNodes3;
                                nextNode = -1;
                                thisframe2 = argsFrame4;
                            } catch (SAXException e4) {
                                se = e4;
                                thisframe = thisframe2;
                                pushContextNodeListFlag = true;
                                sourceNodes = sourceNodes2;
                                transformer.getErrorListener().fatalError(new TransformerException(se));
                                if (nParams > 0) {
                                }
                                xctxt.popSAXLocator();
                                if (pushContextNodeListFlag) {
                                }
                                transformer.popElemTemplateElement();
                                xctxt.popCurrentExpressionNode();
                                xctxt.popCurrentNode();
                                sourceNodes.detach();
                                z = pushContextNodeListFlag;
                            } catch (Throwable th5) {
                                dtm = th5;
                                thisframe = thisframe2;
                                sourceNodes = sourceNodes2;
                                if (nParams > 0) {
                                }
                                xctxt.popSAXLocator();
                                if (z) {
                                }
                                transformer.popElemTemplateElement();
                                xctxt.popCurrentExpressionNode();
                                xctxt.popCurrentNode();
                                sourceNodes.detach();
                                throw dtm;
                            }
                            try {
                                StylesheetRoot sroot3;
                                TemplateList tl2;
                                SerializationHandler rth3;
                                DTM tl3;
                                DTM dtm5;
                                StylesheetRoot sroot4;
                                int currentFrameBottom;
                                ElemTemplate template = tl.getTemplateFast(xctxt, sourceNode, exNodeType, mode, -1, quiet, dtm4);
                                short nodeType2;
                                if (template == null) {
                                    nodeType2 = nodeType;
                                    if (nodeType2 != (short) 9) {
                                        if (nodeType2 != (short) 11) {
                                            switch (nodeType2) {
                                                case (short) 1:
                                                    break;
                                                case (short) 2:
                                                case (short) 3:
                                                case (short) 4:
                                                    sroot3 = sroot2;
                                                    transformerImpl.pushPairCurrentMatched(sroot3.getDefaultTextRule(), sourceNode);
                                                    transformerImpl.setCurrentElement(sroot3.getDefaultTextRule());
                                                    tl2 = tl;
                                                    rth3 = rth2;
                                                    tl3 = dtm4;
                                                    tl3.dispatchCharactersEvents(sourceNode, rth3, false);
                                                    transformer.popCurrentMatched();
                                                    dtm5 = tl3;
                                                    sroot4 = sroot3;
                                                    break;
                                                default:
                                                    tl2 = tl;
                                                    sroot4 = sroot2;
                                                    rth3 = rth2;
                                                    dtm5 = dtm4;
                                                    break;
                                            }
                                        }
                                        tl2 = tl;
                                        sroot3 = sroot2;
                                        rth3 = rth2;
                                        tl3 = dtm4;
                                        template = sroot3.getDefaultRule();
                                    } else {
                                        tl2 = tl;
                                        sroot3 = sroot2;
                                        rth3 = rth2;
                                        tl3 = dtm4;
                                        template = sroot3.getDefaultRootRule();
                                    }
                                } else {
                                    tl2 = tl;
                                    sroot3 = sroot2;
                                    rth3 = rth2;
                                    nodeType2 = nodeType;
                                    tl3 = dtm4;
                                    transformerImpl.setCurrentElement(template);
                                }
                                transformerImpl.pushPairCurrentMatched(template, sourceNode);
                                int exNodeType2;
                                QName mode2;
                                if (template.m_frameSize > 0) {
                                    xctxt.pushRTFContext();
                                    currentFrameBottom = vars.getStackFrame();
                                    vars.link(template.m_frameSize);
                                    if (template.m_inArgsSize > 0) {
                                        nextNode = 0;
                                        ElemTemplateElement elem = template.getFirstChildElem();
                                        while (true) {
                                            dtm5 = tl3;
                                            ElemTemplateElement dtm6 = elem;
                                            if (dtm6 != null) {
                                                sroot4 = sroot3;
                                                exNodeType2 = exNodeType;
                                                if (41 == dtm6.getXSLToken()) {
                                                    ElemParam sroot5 = (ElemParam) dtm6;
                                                    exNodeType = 0;
                                                    while (exNodeType < nParams) {
                                                        mode2 = mode;
                                                        mode = elemApplyTemplates.m_paramElems[exNodeType];
                                                        QName ewp2 = mode;
                                                        if (mode.m_qnameID == sroot5.m_qnameID) {
                                                            vars.setLocalVariable(nextNode, vars.getLocalVariable(exNodeType, thisframe2));
                                                            if (exNodeType != nParams) {
                                                                vars.setLocalVariable(nextNode, null);
                                                            }
                                                            nextNode++;
                                                            elem = dtm6.getNextSiblingElem();
                                                            tl3 = dtm5;
                                                            sroot3 = sroot4;
                                                            exNodeType = exNodeType2;
                                                            mode = mode2;
                                                            elemApplyTemplates = this;
                                                        } else {
                                                            exNodeType++;
                                                            mode = mode2;
                                                            elemApplyTemplates = this;
                                                        }
                                                    }
                                                    mode2 = mode;
                                                    if (exNodeType != nParams) {
                                                    }
                                                    nextNode++;
                                                    elem = dtm6.getNextSiblingElem();
                                                    tl3 = dtm5;
                                                    sroot3 = sroot4;
                                                    exNodeType = exNodeType2;
                                                    mode = mode2;
                                                    elemApplyTemplates = this;
                                                }
                                            } else {
                                                sroot4 = sroot3;
                                                exNodeType2 = exNodeType;
                                                mode2 = mode;
                                            }
                                        }
                                    } else {
                                        dtm5 = tl3;
                                        sroot4 = sroot3;
                                        exNodeType2 = exNodeType;
                                        mode2 = mode;
                                    }
                                } else {
                                    dtm5 = tl3;
                                    sroot4 = sroot3;
                                    exNodeType2 = exNodeType;
                                    mode2 = mode;
                                    currentFrameBottom = 0;
                                }
                                tl = currentFrameBottom;
                                ElemTemplateElement t = template.m_firstChild;
                                while (true) {
                                    ElemTemplateElement t2 = t;
                                    if (t2 != null) {
                                        xctxt.setSAXLocator(t2);
                                        transformerImpl.pushElemTemplateElement(t2);
                                        t2.execute(transformerImpl);
                                        transformer.popElemTemplateElement();
                                        t = t2.m_nextSibling;
                                    } else {
                                        if (template.m_frameSize > 0) {
                                            vars.unlink(tl);
                                            xctxt.popRTFContext();
                                        }
                                        transformer.popCurrentMatched();
                                        rth = rth3;
                                        argsFrame = thisframe2;
                                        keys = keys2;
                                        currentNodes = currentNodes2;
                                        currentExpressionNodes = currentExpressionNodes3;
                                        sourceNode = sourceNode2;
                                        sourceNodes2 = sourceNodes3;
                                        thisframe2 = thisframe3;
                                        tl = tl2;
                                        dtm3 = dtm5;
                                        sroot2 = sroot4;
                                        elemApplyTemplates = this;
                                    }
                                }
                            } catch (SAXException e5) {
                                se = e5;
                                pushContextNodeListFlag = true;
                                sourceNodes = sourceNodes3;
                                thisframe = thisframe3;
                                transformer.getErrorListener().fatalError(new TransformerException(se));
                                if (nParams > 0) {
                                }
                                xctxt.popSAXLocator();
                                if (pushContextNodeListFlag) {
                                }
                                transformer.popElemTemplateElement();
                                xctxt.popCurrentExpressionNode();
                                xctxt.popCurrentNode();
                                sourceNodes.detach();
                                z = pushContextNodeListFlag;
                            } catch (Throwable th6) {
                                dtm = th6;
                                sourceNodes = sourceNodes3;
                                thisframe = thisframe3;
                                if (nParams > 0) {
                                }
                                xctxt.popSAXLocator();
                                if (z) {
                                }
                                transformer.popElemTemplateElement();
                                xctxt.popCurrentExpressionNode();
                                xctxt.popCurrentNode();
                                sourceNodes.detach();
                                throw dtm;
                            }
                        }
                        sourceNodes3 = sourceNodes2;
                        sourceNode2 = sourceNode;
                        thisframe3 = thisframe2;
                        if (nParams > 0) {
                            vars.unlink(thisframe3);
                        }
                        xctxt.popSAXLocator();
                        if (1 != null) {
                            xctxt.popContextNodeList();
                        }
                        transformer.popElemTemplateElement();
                        xctxt.popCurrentExpressionNode();
                        xctxt.popCurrentNode();
                        sourceNodes3.detach();
                        return;
                    }
                } catch (SAXException e6) {
                    se = e6;
                    sourceNodes = sourceNodes2;
                    sourceNode2 = sourceNode;
                    thisframe = thisframe2;
                    pushContextNodeListFlag = true;
                    transformer.getErrorListener().fatalError(new TransformerException(se));
                    if (nParams > 0) {
                    }
                    xctxt.popSAXLocator();
                    if (pushContextNodeListFlag) {
                    }
                    transformer.popElemTemplateElement();
                    xctxt.popCurrentExpressionNode();
                    xctxt.popCurrentNode();
                    sourceNodes.detach();
                    z = pushContextNodeListFlag;
                } catch (Throwable th7) {
                    dtm = th7;
                    sourceNodes = sourceNodes2;
                    sourceNode2 = sourceNode;
                    thisframe = thisframe2;
                    if (nParams > 0) {
                    }
                    xctxt.popSAXLocator();
                    if (z) {
                    }
                    transformer.popElemTemplateElement();
                    xctxt.popCurrentExpressionNode();
                    xctxt.popCurrentNode();
                    sourceNodes.detach();
                    throw dtm;
                }
            } catch (SAXException e7) {
                se = e7;
                sourceNodes = sourceNodes2;
                sourceNode2 = sourceNode;
                thisframe = thisframe2;
                transformer.getErrorListener().fatalError(new TransformerException(se));
                if (nParams > 0) {
                }
                xctxt.popSAXLocator();
                if (pushContextNodeListFlag) {
                }
                transformer.popElemTemplateElement();
                xctxt.popCurrentExpressionNode();
                xctxt.popCurrentNode();
                sourceNodes.detach();
                z = pushContextNodeListFlag;
            } catch (Throwable th8) {
                dtm = th8;
                sourceNodes = sourceNodes2;
                sourceNode2 = sourceNode;
                thisframe = thisframe2;
                z = pushContextNodeListFlag;
                if (nParams > 0) {
                }
                xctxt.popSAXLocator();
                if (z) {
                }
                transformer.popElemTemplateElement();
                xctxt.popCurrentExpressionNode();
                xctxt.popCurrentNode();
                sourceNodes.detach();
                throw dtm;
            }
        } catch (SAXException e8) {
            se = e8;
            sourceNode2 = sourceNode;
            thisframe = thisframe2;
            transformer.getErrorListener().fatalError(new TransformerException(se));
            if (nParams > 0) {
            }
            xctxt.popSAXLocator();
            if (pushContextNodeListFlag) {
            }
            transformer.popElemTemplateElement();
            xctxt.popCurrentExpressionNode();
            xctxt.popCurrentNode();
            sourceNodes.detach();
            z = pushContextNodeListFlag;
        } catch (Throwable th9) {
            dtm = th9;
            sourceNode2 = sourceNode;
            thisframe = thisframe2;
            z = pushContextNodeListFlag;
            if (nParams > 0) {
            }
            xctxt.popSAXLocator();
            if (z) {
            }
            transformer.popElemTemplateElement();
            xctxt.popCurrentExpressionNode();
            xctxt.popCurrentNode();
            sourceNodes.detach();
            throw dtm;
        }
    }
}
