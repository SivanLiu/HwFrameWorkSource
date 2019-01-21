package org.apache.xpath.axes;

import java.io.PrintStream;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.Expression;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpCodes;
import org.apache.xpath.compiler.OpMap;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.patterns.ContextMatchStepPattern;
import org.apache.xpath.patterns.FunctionPattern;
import org.apache.xpath.patterns.StepPattern;
import org.apache.xpath.res.XPATHMessages;

public class WalkerFactory {
    public static final int BITMASK_TRAVERSES_OUTSIDE_SUBTREE = 234381312;
    public static final int BITS_COUNT = 255;
    public static final int BITS_RESERVED = 3840;
    public static final int BIT_ANCESTOR = 8192;
    public static final int BIT_ANCESTOR_OR_SELF = 16384;
    public static final int BIT_ANY_DESCENDANT_FROM_ROOT = 536870912;
    public static final int BIT_ATTRIBUTE = 32768;
    public static final int BIT_BACKWARDS_SELF = 268435456;
    public static final int BIT_CHILD = 65536;
    public static final int BIT_DESCENDANT = 131072;
    public static final int BIT_DESCENDANT_OR_SELF = 262144;
    public static final int BIT_FILTER = 67108864;
    public static final int BIT_FOLLOWING = 524288;
    public static final int BIT_FOLLOWING_SIBLING = 1048576;
    public static final int BIT_MATCH_PATTERN = Integer.MIN_VALUE;
    public static final int BIT_NAMESPACE = 2097152;
    public static final int BIT_NODETEST_ANY = 1073741824;
    public static final int BIT_PARENT = 4194304;
    public static final int BIT_PRECEDING = 8388608;
    public static final int BIT_PRECEDING_SIBLING = 16777216;
    public static final int BIT_PREDICATE = 4096;
    public static final int BIT_ROOT = 134217728;
    public static final int BIT_SELF = 33554432;
    static final boolean DEBUG_ITERATOR_CREATION = false;
    static final boolean DEBUG_PATTERN_CREATION = false;
    static final boolean DEBUG_WALKER_CREATION = false;

    static AxesWalker loadOneWalker(WalkingIterator lpi, Compiler compiler, int stepOpCodePos) throws TransformerException {
        int stepType = compiler.getOp(stepOpCodePos);
        if (stepType == -1) {
            return null;
        }
        AxesWalker firstWalker = createDefaultWalker(compiler, stepType, lpi, 0);
        firstWalker.init(compiler, stepOpCodePos, stepType);
        return firstWalker;
    }

    static AxesWalker loadWalkers(WalkingIterator lpi, Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        AxesWalker firstWalker = null;
        AxesWalker prevWalker = null;
        int analysis = analyze(compiler, stepOpCodePos, stepIndex);
        do {
            int op = compiler.getOp(stepOpCodePos);
            int stepType = op;
            if (-1 == op) {
                break;
            }
            AxesWalker walker = createDefaultWalker(compiler, stepOpCodePos, lpi, analysis);
            walker.init(compiler, stepOpCodePos, stepType);
            walker.exprSetParent(lpi);
            if (firstWalker == null) {
                firstWalker = walker;
            } else {
                prevWalker.setNextWalker(walker);
                walker.setPrevWalker(prevWalker);
            }
            prevWalker = walker;
            stepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
        } while (stepOpCodePos >= 0);
        return firstWalker;
    }

    public static boolean isSet(int analysis, int bits) {
        return (analysis & bits) != 0;
    }

    public static void diagnoseIterator(String name, int analysis, Compiler compiler) {
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(compiler.toString());
        stringBuilder.append(", ");
        stringBuilder.append(name);
        stringBuilder.append(", ");
        stringBuilder.append(Integer.toBinaryString(analysis));
        stringBuilder.append(", ");
        stringBuilder.append(getAnalysisString(analysis));
        printStream.println(stringBuilder.toString());
    }

    public static DTMIterator newDTMIterator(Compiler compiler, int opPos, boolean isTopLevel) throws TransformerException {
        int firstStepPos = OpMap.getFirstChildPos(opPos);
        int analysis = analyze(compiler, firstStepPos, 0);
        boolean isOneStep = isOneStep(analysis);
        DTMIterator iter = (isOneStep && walksSelfOnly(analysis) && isWild(analysis) && !hasPredicate(analysis)) ? new SelfIteratorNoPredicate(compiler, opPos, analysis) : (walksChildrenOnly(analysis) && isOneStep) ? (!isWild(analysis) || hasPredicate(analysis)) ? new ChildTestIterator(compiler, opPos, analysis) : new ChildIterator(compiler, opPos, analysis) : (isOneStep && walksAttributes(analysis)) ? new AttributeIterator(compiler, opPos, analysis) : (!isOneStep || walksFilteredList(analysis)) ? isOptimizableForDescendantIterator(compiler, firstStepPos, 0) ? new DescendantIterator(compiler, opPos, analysis) : isNaturalDocOrder(compiler, firstStepPos, 0, analysis) ? new WalkingIterator(compiler, opPos, analysis, true) : new WalkingIteratorSorted(compiler, opPos, analysis, true) : (walksNamespaces(analysis) || !(walksInDocOrder(analysis) || isSet(analysis, BIT_PARENT))) ? new OneStepIterator(compiler, opPos, analysis) : new OneStepIteratorForward(compiler, opPos, analysis);
        if (iter instanceof LocPathIterator) {
            ((LocPathIterator) iter).setIsTopLevel(isTopLevel);
        }
        return iter;
    }

    public static int getAxisFromStep(Compiler compiler, int stepOpCodePos) throws TransformerException {
        int stepType = compiler.getOp(stepOpCodePos);
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                return 20;
            default:
                switch (stepType) {
                    case 37:
                        return 0;
                    case 38:
                        return 1;
                    case 39:
                        return 2;
                    case 40:
                        return 3;
                    case 41:
                        return 4;
                    case 42:
                        return 5;
                    case 43:
                        return 6;
                    case 44:
                        return 7;
                    case 45:
                        return 10;
                    case 46:
                        return 11;
                    case 47:
                        return 12;
                    case 48:
                        return 13;
                    case 49:
                        return 9;
                    case 50:
                        return 19;
                    default:
                        throw new RuntimeException(XPATHMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                }
        }
    }

    public static int getAnalysisBitFromAxes(int axis) {
        switch (axis) {
            case 0:
                return BIT_ANCESTOR;
            case 1:
                return BIT_ANCESTOR_OR_SELF;
            case 2:
                return BIT_ATTRIBUTE;
            case 3:
                return 65536;
            case 4:
                return BIT_DESCENDANT;
            case 5:
                return BIT_DESCENDANT_OR_SELF;
            case 6:
                return BIT_FOLLOWING;
            case 7:
                return BIT_FOLLOWING_SIBLING;
            case 8:
            case 9:
                return BIT_NAMESPACE;
            case 10:
                return BIT_PARENT;
            case 11:
                return BIT_PRECEDING;
            case 12:
                return BIT_PRECEDING_SIBLING;
            case 13:
                return BIT_SELF;
            case 14:
                return BIT_DESCENDANT_OR_SELF;
            case 16:
            case 17:
            case 18:
                return BIT_ANY_DESCENDANT_FROM_ROOT;
            case 19:
                return BIT_ROOT;
            case 20:
                return BIT_FILTER;
            default:
                return BIT_FILTER;
        }
    }

    static boolean functionProximateOrContainsProximate(Compiler compiler, int opPos) {
        int endFunc = (compiler.getOp(opPos + 1) + opPos) - 1;
        opPos = OpMap.getFirstChildPos(opPos);
        switch (compiler.getOp(opPos)) {
            case 1:
            case 2:
                return true;
            default:
                int i = 0;
                int p = opPos + 1;
                while (p < endFunc) {
                    int innerExprOpPos = p + 2;
                    int argOp = compiler.getOp(innerExprOpPos);
                    if (isProximateInnerExpr(compiler, innerExprOpPos)) {
                        return true;
                    }
                    p = compiler.getNextOpPos(p);
                    i++;
                }
                return false;
        }
    }

    static boolean isProximateInnerExpr(Compiler compiler, int opPos) {
        int op = compiler.getOp(opPos);
        int innerExprOpPos = opPos + 2;
        switch (op) {
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                int leftPos = OpMap.getFirstChildPos(op);
                int rightPos = compiler.getNextOpPos(leftPos);
                if (isProximateInnerExpr(compiler, leftPos) || isProximateInnerExpr(compiler, rightPos)) {
                    return true;
                }
            default:
                switch (op) {
                    case 21:
                    case 22:
                        break;
                    default:
                        switch (op) {
                            case 25:
                                if (functionProximateOrContainsProximate(compiler, opPos)) {
                                    return true;
                                }
                                break;
                            case 26:
                                if (isProximateInnerExpr(compiler, innerExprOpPos)) {
                                    return true;
                                }
                                break;
                            case 27:
                            case 28:
                                break;
                            default:
                                return true;
                        }
                }
        }
        return false;
    }

    public static boolean mightBeProximate(Compiler compiler, int opPos, int stepType) throws TransformerException {
        int argLen;
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                argLen = compiler.getArgLength(opPos);
                break;
            default:
                argLen = compiler.getArgLengthOfStep(opPos);
                break;
        }
        int predPos = compiler.getFirstPredicateOpPos(opPos);
        int count = 0;
        while (29 == compiler.getOp(predPos)) {
            count++;
            int innerExprOpPos = predPos + 2;
            int predOp = compiler.getOp(innerExprOpPos);
            if (predOp == 19 || predOp == 22) {
                return true;
            }
            if (predOp != 25) {
                switch (predOp) {
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                        int leftPos = OpMap.getFirstChildPos(innerExprOpPos);
                        int rightPos = compiler.getNextOpPos(leftPos);
                        if (isProximateInnerExpr(compiler, leftPos) || isProximateInnerExpr(compiler, rightPos)) {
                            return true;
                        }
                        continue;
                    default:
                        switch (predOp) {
                            case 27:
                                break;
                            case 28:
                                continue;
                            default:
                                return true;
                        }
                }
                return true;
            } else if (functionProximateOrContainsProximate(compiler, innerExprOpPos)) {
                return true;
            }
            predPos = compiler.getNextOpPos(predPos);
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:24:0x004d, code skipped:
            if (3 != r1) goto L_0x0050;
     */
    /* JADX WARNING: Missing block: B:25:0x004f, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:26:0x0050, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:31:0x0059, code skipped:
            r13 = r12.getStepTestType(r0);
            r10 = r12.getNextStepPos(r0);
     */
    /* JADX WARNING: Missing block: B:32:0x0061, code skipped:
            if (r10 >= 0) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:34:0x0068, code skipped:
            if (-1 == r12.getOp(r10)) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:36:0x006e, code skipped:
            if (r12.countPredicates(r0) <= 0) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:37:0x0070, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:40:0x0074, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isOptimizableForDescendantIterator(Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        boolean foundDS = false;
        boolean foundSelf = false;
        boolean foundDorDS = false;
        int stepCount = 0;
        int stepOpCodePos2 = stepOpCodePos;
        stepOpCodePos = OpCodes.NODETYPE_NODE;
        while (true) {
            int op = compiler.getOp(stepOpCodePos2);
            int stepType = op;
            if (-1 != op) {
                if (stepOpCodePos != OpCodes.NODETYPE_NODE && stepOpCodePos != 35) {
                    return false;
                }
                stepCount++;
                if (stepCount <= 3 && !mightBeProximate(compiler, stepOpCodePos2, stepType)) {
                    switch (stepType) {
                        case 22:
                        case 23:
                        case 24:
                        case 25:
                            break;
                        default:
                            switch (stepType) {
                                case 37:
                                case 38:
                                case 39:
                                case 43:
                                case 44:
                                case 45:
                                case 46:
                                case 47:
                                case 49:
                                case 51:
                                case 52:
                                case 53:
                                    break;
                                case 40:
                                    if (!(foundDS || (foundDorDS && foundSelf))) {
                                        return false;
                                    }
                                case 41:
                                    break;
                                case 42:
                                    foundDS = true;
                                    break;
                                case 48:
                                    if (1 == stepCount) {
                                        foundSelf = true;
                                        break;
                                    }
                                    return false;
                                case 50:
                                    if (1 != stepCount) {
                                        return false;
                                    }
                                    break;
                                default:
                                    throw new RuntimeException(XPATHMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                            }
                            break;
                    }
                }
                return false;
            }
            stepOpCodePos2 = nextStepOpCodePos;
        }
        return false;
    }

    private static int analyze(Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        int stepCount = 0;
        int stepOpCodePos2 = stepOpCodePos;
        stepOpCodePos = 0;
        while (true) {
            int op = compiler.getOp(stepOpCodePos2);
            int stepType = op;
            if (-1 != op) {
                stepCount++;
                if (analyzePredicate(compiler, stepOpCodePos2, stepType)) {
                    stepOpCodePos |= 4096;
                }
                switch (stepType) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        stepOpCodePos |= BIT_FILTER;
                        break;
                    default:
                        switch (stepType) {
                            case 37:
                                stepOpCodePos |= BIT_ANCESTOR;
                                break;
                            case 38:
                                stepOpCodePos |= BIT_ANCESTOR_OR_SELF;
                                break;
                            case 39:
                                stepOpCodePos |= BIT_ATTRIBUTE;
                                break;
                            case 40:
                                stepOpCodePos |= 65536;
                                break;
                            case 41:
                                stepOpCodePos |= BIT_DESCENDANT;
                                break;
                            case 42:
                                if (2 == stepCount && BIT_ROOT == stepOpCodePos) {
                                    stepOpCodePos |= BIT_ANY_DESCENDANT_FROM_ROOT;
                                }
                                stepOpCodePos |= BIT_DESCENDANT_OR_SELF;
                                break;
                            case 43:
                                stepOpCodePos |= BIT_FOLLOWING;
                                break;
                            case 44:
                                stepOpCodePos |= BIT_FOLLOWING_SIBLING;
                                break;
                            case 45:
                                stepOpCodePos |= BIT_PARENT;
                                break;
                            case 46:
                                stepOpCodePos |= BIT_PRECEDING;
                                break;
                            case 47:
                                stepOpCodePos |= BIT_PRECEDING_SIBLING;
                                break;
                            case 48:
                                stepOpCodePos |= BIT_SELF;
                                break;
                            case 49:
                                stepOpCodePos |= BIT_NAMESPACE;
                                break;
                            case 50:
                                stepOpCodePos |= BIT_ROOT;
                                break;
                            case 51:
                                stepOpCodePos |= -2147450880;
                                break;
                            case 52:
                                stepOpCodePos |= -2147475456;
                                break;
                            case 53:
                                stepOpCodePos |= -2143289344;
                                break;
                            default:
                                throw new RuntimeException(XPATHMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                        }
                }
                if (OpCodes.NODETYPE_NODE == compiler.getOp(stepOpCodePos2 + 3)) {
                    stepOpCodePos |= BIT_NODETEST_ANY;
                }
                stepOpCodePos2 = compiler.getNextStepPos(stepOpCodePos2);
                if (stepOpCodePos2 < 0) {
                }
            }
        }
        return stepOpCodePos | (stepCount & BITS_COUNT);
    }

    public static boolean isDownwardAxisOfMany(int axis) {
        return 5 == axis || 4 == axis || 6 == axis || 11 == axis;
    }

    static StepPattern loadSteps(MatchPatternIterator mpi, Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        Compiler compiler2 = compiler;
        int analysis = analyze(compiler, stepOpCodePos, stepIndex);
        int stepOpCodePos2 = stepOpCodePos;
        StepPattern step = null;
        StepPattern firstStep = null;
        StepPattern prevStep = null;
        do {
            int op = compiler2.getOp(stepOpCodePos2);
            int stepType = op;
            if (-1 == op) {
                break;
            }
            step = createDefaultStepPattern(compiler2, stepOpCodePos2, mpi, analysis, firstStep, prevStep);
            if (firstStep == null) {
                firstStep = step;
            } else {
                step.setRelativePathPattern(prevStep);
            }
            prevStep = step;
            stepOpCodePos2 = compiler2.getNextStepPos(stepOpCodePos2);
        } while (stepOpCodePos2 >= 0);
        StepPattern tail = step;
        int axis = 13;
        StepPattern pat = step;
        while (pat != null) {
            int analysis2;
            int nextAxis = pat.getAxis();
            pat.setAxis(axis);
            int whatToShow = pat.getWhatToShow();
            int i;
            if (whatToShow == 2 || whatToShow == 4096) {
                int newAxis = whatToShow == 2 ? 2 : 9;
                if (isDownwardAxisOfMany(axis)) {
                    StepPattern attrPat = new StepPattern(whatToShow, pat.getNamespace(), pat.getLocalName(), newAxis, 0);
                    XNumber score = pat.getStaticScore();
                    pat.setNamespace(null);
                    pat.setLocalName("*");
                    attrPat.setPredicates(pat.getPredicates());
                    pat.setPredicates(null);
                    pat.setWhatToShow(1);
                    StepPattern rel = pat.getRelativePathPattern();
                    pat.setRelativePathPattern(attrPat);
                    attrPat.setRelativePathPattern(rel);
                    attrPat.setStaticScore(score);
                    analysis2 = analysis;
                    if (11 == pat.getAxis()) {
                        pat.setAxis(15);
                    } else if (4 == pat.getAxis()) {
                        pat.setAxis(5);
                    }
                    pat = attrPat;
                } else {
                    analysis2 = analysis;
                    i = whatToShow;
                    if (3 == pat.getAxis()) {
                        pat.setAxis(2);
                    }
                }
            } else {
                analysis2 = analysis;
                i = whatToShow;
            }
            axis = nextAxis;
            tail = pat;
            pat = pat.getRelativePathPattern();
            analysis = analysis2;
        }
        if (axis < 16) {
            pat = new ContextMatchStepPattern(axis, 13);
            XNumber score2 = tail.getStaticScore();
            tail.setRelativePathPattern(pat);
            tail.setStaticScore(score2);
            pat.setStaticScore(score2);
        }
        return step;
    }

    private static StepPattern createDefaultStepPattern(Compiler compiler, int opPos, MatchPatternIterator mpi, int analysis, StepPattern tail, StepPattern head) throws TransformerException {
        int axis;
        int predicateAxis;
        Compiler compiler2 = compiler;
        int stepType = compiler.getOp(opPos);
        int whatToShow = compiler.getWhatToShow(opPos);
        StepPattern ai = null;
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                Expression expr;
                switch (stepType) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        expr = compiler.compile(opPos);
                        break;
                    default:
                        expr = compiler2.compile(opPos + 2);
                        break;
                }
                ai = new FunctionPattern(expr, 20, 20);
                axis = 20;
                predicateAxis = 20;
                break;
            default:
                int axis2;
                int predicateAxis2;
                switch (stepType) {
                    case 37:
                        axis2 = 4;
                        predicateAxis2 = 0;
                        break;
                    case 38:
                        axis2 = 5;
                        predicateAxis2 = 1;
                        break;
                    case 39:
                        axis2 = 10;
                        predicateAxis2 = 2;
                        break;
                    case 40:
                        axis2 = 10;
                        predicateAxis2 = 3;
                        break;
                    case 41:
                        axis2 = 0;
                        predicateAxis2 = 4;
                        break;
                    case 42:
                        axis2 = 1;
                        predicateAxis2 = 5;
                        break;
                    case 43:
                        axis2 = 11;
                        predicateAxis2 = 6;
                        break;
                    case 44:
                        axis2 = 12;
                        predicateAxis2 = 7;
                        break;
                    case 45:
                        axis2 = 3;
                        predicateAxis2 = 10;
                        break;
                    case 46:
                        axis2 = 6;
                        predicateAxis2 = 11;
                        break;
                    case 47:
                        axis2 = 7;
                        predicateAxis2 = 12;
                        break;
                    case 48:
                        axis2 = 13;
                        predicateAxis2 = 13;
                        break;
                    case 49:
                        axis2 = 10;
                        predicateAxis2 = 9;
                        break;
                    case 50:
                        axis2 = 19;
                        predicateAxis2 = 19;
                        ai = new StepPattern(1280, 19, 19);
                        break;
                    default:
                        throw new RuntimeException(XPATHMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                }
                axis = axis2;
                predicateAxis = predicateAxis2;
                break;
        }
        if (ai == null) {
            ai = new StepPattern(compiler.getWhatToShow(opPos), compiler.getStepNS(opPos), compiler.getStepLocalName(opPos), axis, predicateAxis);
        }
        ai.setPredicates(compiler2.getCompiledPredicates(compiler.getFirstPredicateOpPos(opPos)));
        return ai;
    }

    static boolean analyzePredicate(Compiler compiler, int opPos, int stepType) throws TransformerException {
        int argLen;
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                argLen = compiler.getArgLength(opPos);
                break;
            default:
                argLen = compiler.getArgLengthOfStep(opPos);
                break;
        }
        return compiler.countPredicates(compiler.getFirstPredicateOpPos(opPos)) > 0;
    }

    private static AxesWalker createDefaultWalker(Compiler compiler, int opPos, WalkingIterator lpi, int analysis) {
        AxesWalker ai;
        int stepType = compiler.getOp(opPos);
        boolean simpleInit = false;
        int totalNumberWalkers = analysis & BITS_COUNT;
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                ai = new FilterExprWalker(lpi);
                simpleInit = true;
                break;
            default:
                switch (stepType) {
                    case 37:
                        ai = new ReverseAxesWalker(lpi, 0);
                        break;
                    case 38:
                        ai = new ReverseAxesWalker(lpi, 1);
                        break;
                    case 39:
                        ai = new AxesWalker(lpi, 2);
                        break;
                    case 40:
                        ai = new AxesWalker(lpi, 3);
                        break;
                    case 41:
                        ai = new AxesWalker(lpi, 4);
                        break;
                    case 42:
                        ai = new AxesWalker(lpi, 5);
                        break;
                    case 43:
                        ai = new AxesWalker(lpi, 6);
                        break;
                    case 44:
                        ai = new AxesWalker(lpi, 7);
                        break;
                    case 45:
                        ai = new ReverseAxesWalker(lpi, 10);
                        break;
                    case 46:
                        ai = new ReverseAxesWalker(lpi, 11);
                        break;
                    case 47:
                        ai = new ReverseAxesWalker(lpi, 12);
                        break;
                    case 48:
                        ai = new AxesWalker(lpi, 13);
                        break;
                    case 49:
                        ai = new AxesWalker(lpi, 9);
                        break;
                    case 50:
                        ai = new AxesWalker(lpi, 19);
                        break;
                    default:
                        throw new RuntimeException(XPATHMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                }
        }
        if (simpleInit) {
            ai.initNodeTest(-1);
        } else {
            int whatToShow = compiler.getWhatToShow(opPos);
            if ((whatToShow & 4163) == 0 || whatToShow == -1) {
                ai.initNodeTest(whatToShow);
            } else {
                ai.initNodeTest(whatToShow, compiler.getStepNS(opPos), compiler.getStepLocalName(opPos));
            }
        }
        return ai;
    }

    public static String getAnalysisString(int analysis) {
        StringBuffer buf = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("count: ");
        stringBuilder.append(getStepCount(analysis));
        stringBuilder.append(" ");
        buf.append(stringBuilder.toString());
        if ((BIT_NODETEST_ANY & analysis) != 0) {
            buf.append("NTANY|");
        }
        if ((analysis & 4096) != 0) {
            buf.append("PRED|");
        }
        if ((analysis & BIT_ANCESTOR) != 0) {
            buf.append("ANC|");
        }
        if ((analysis & BIT_ANCESTOR_OR_SELF) != 0) {
            buf.append("ANCOS|");
        }
        if ((BIT_ATTRIBUTE & analysis) != 0) {
            buf.append("ATTR|");
        }
        if ((65536 & analysis) != 0) {
            buf.append("CH|");
        }
        if ((BIT_DESCENDANT & analysis) != 0) {
            buf.append("DESC|");
        }
        if ((BIT_DESCENDANT_OR_SELF & analysis) != 0) {
            buf.append("DESCOS|");
        }
        if ((BIT_FOLLOWING & analysis) != 0) {
            buf.append("FOL|");
        }
        if ((BIT_FOLLOWING_SIBLING & analysis) != 0) {
            buf.append("FOLS|");
        }
        if ((BIT_NAMESPACE & analysis) != 0) {
            buf.append("NS|");
        }
        if ((BIT_PARENT & analysis) != 0) {
            buf.append("P|");
        }
        if ((BIT_PRECEDING & analysis) != 0) {
            buf.append("PREC|");
        }
        if ((BIT_PRECEDING_SIBLING & analysis) != 0) {
            buf.append("PRECS|");
        }
        if ((BIT_SELF & analysis) != 0) {
            buf.append(".|");
        }
        if ((BIT_FILTER & analysis) != 0) {
            buf.append("FLT|");
        }
        if ((BIT_ROOT & analysis) != 0) {
            buf.append("R|");
        }
        return buf.toString();
    }

    public static boolean hasPredicate(int analysis) {
        return (analysis & 4096) != 0;
    }

    public static boolean isWild(int analysis) {
        return (BIT_NODETEST_ANY & analysis) != 0;
    }

    public static boolean walksAncestors(int analysis) {
        return isSet(analysis, 24576);
    }

    public static boolean walksAttributes(int analysis) {
        return (BIT_ATTRIBUTE & analysis) != 0;
    }

    public static boolean walksNamespaces(int analysis) {
        return (BIT_NAMESPACE & analysis) != 0;
    }

    public static boolean walksChildren(int analysis) {
        return (65536 & analysis) != 0;
    }

    public static boolean walksDescendants(int analysis) {
        return isSet(analysis, 393216);
    }

    public static boolean walksSubtree(int analysis) {
        return isSet(analysis, 458752);
    }

    public static boolean walksSubtreeOnlyMaybeAbsolute(int analysis) {
        return (!walksSubtree(analysis) || walksExtraNodes(analysis) || walksUp(analysis) || walksSideways(analysis)) ? false : true;
    }

    public static boolean walksSubtreeOnly(int analysis) {
        return walksSubtreeOnlyMaybeAbsolute(analysis) && !isAbsolute(analysis);
    }

    public static boolean walksFilteredList(int analysis) {
        return isSet(analysis, BIT_FILTER);
    }

    public static boolean walksSubtreeOnlyFromRootOrContext(int analysis) {
        return (!walksSubtree(analysis) || walksExtraNodes(analysis) || walksUp(analysis) || walksSideways(analysis) || isSet(analysis, BIT_FILTER)) ? false : true;
    }

    public static boolean walksInDocOrder(int analysis) {
        return (walksSubtreeOnlyMaybeAbsolute(analysis) || walksExtraNodesOnly(analysis) || walksFollowingOnlyMaybeAbsolute(analysis)) && !isSet(analysis, BIT_FILTER);
    }

    public static boolean walksFollowingOnlyMaybeAbsolute(int analysis) {
        return (!isSet(analysis, 35127296) || walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis)) ? false : true;
    }

    public static boolean walksUp(int analysis) {
        return isSet(analysis, 4218880);
    }

    public static boolean walksSideways(int analysis) {
        return isSet(analysis, 26738688);
    }

    public static boolean walksExtraNodes(int analysis) {
        return isSet(analysis, 2129920);
    }

    public static boolean walksExtraNodesOnly(int analysis) {
        return (!walksExtraNodes(analysis) || isSet(analysis, BIT_SELF) || walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean isAbsolute(int analysis) {
        return isSet(analysis, 201326592);
    }

    public static boolean walksChildrenOnly(int analysis) {
        return (!walksChildren(analysis) || isSet(analysis, BIT_SELF) || walksExtraNodes(analysis) || walksDescendants(analysis) || walksUp(analysis) || walksSideways(analysis) || (isAbsolute(analysis) && !isSet(analysis, BIT_ROOT))) ? false : true;
    }

    public static boolean walksChildrenAndExtraAndSelfOnly(int analysis) {
        return (!walksChildren(analysis) || walksDescendants(analysis) || walksUp(analysis) || walksSideways(analysis) || (isAbsolute(analysis) && !isSet(analysis, BIT_ROOT))) ? false : true;
    }

    public static boolean walksDescendantsAndExtraAndSelfOnly(int analysis) {
        return (walksChildren(analysis) || !walksDescendants(analysis) || walksUp(analysis) || walksSideways(analysis) || (isAbsolute(analysis) && !isSet(analysis, BIT_ROOT))) ? false : true;
    }

    public static boolean walksSelfOnly(int analysis) {
        return (!isSet(analysis, BIT_SELF) || walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean walksUpOnly(int analysis) {
        return (walksSubtree(analysis) || !walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean walksDownOnly(int analysis) {
        return (!walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean walksDownExtraOnly(int analysis) {
        return (!walksSubtree(analysis) || !walksExtraNodes(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean canSkipSubtrees(int analysis) {
        return isSet(analysis, 65536) | walksSideways(analysis);
    }

    public static boolean canCrissCross(int analysis) {
        if (walksSelfOnly(analysis)) {
            return false;
        }
        if ((walksDownOnly(analysis) && !canSkipSubtrees(analysis)) || walksChildrenAndExtraAndSelfOnly(analysis) || walksDescendantsAndExtraAndSelfOnly(analysis) || walksUpOnly(analysis) || walksExtraNodesOnly(analysis) || !walksSubtree(analysis) || (!walksSideways(analysis) && !walksUp(analysis) && !canSkipSubtrees(analysis))) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isNaturalDocOrder(int analysis) {
        if (canCrissCross(analysis) || isSet(analysis, BIT_NAMESPACE) || walksFilteredList(analysis) || !walksInDocOrder(analysis)) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:24:0x005e, code skipped:
            if (r9 <= 0) goto L_0x0061;
     */
    /* JADX WARNING: Missing block: B:25:0x0060, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:26:0x0061, code skipped:
            r9 = r9 + 1;
     */
    /* JADX WARNING: Missing block: B:27:0x0063, code skipped:
            if (r3 == false) goto L_0x0066;
     */
    /* JADX WARNING: Missing block: B:28:0x0065, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:29:0x0066, code skipped:
            r4 = r8.getNextStepPos(r0);
     */
    /* JADX WARNING: Missing block: B:30:0x006a, code skipped:
            if (r4 >= 0) goto L_0x006d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isNaturalDocOrder(Compiler compiler, int stepOpCodePos, int stepIndex, int analysis) throws TransformerException {
        if (canCrissCross(analysis) || isSet(analysis, BIT_NAMESPACE)) {
            return false;
        }
        if (isSet(analysis, 1572864) && isSet(analysis, 25165824)) {
            return false;
        }
        boolean foundWildAttribute = false;
        int stepCount = 0;
        int stepOpCodePos2 = stepOpCodePos;
        stepOpCodePos = 0;
        while (true) {
            int op = compiler.getOp(stepOpCodePos2);
            int stepType = op;
            if (-1 != op) {
                stepCount++;
                switch (stepType) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        break;
                    default:
                        switch (stepType) {
                            case 37:
                            case 38:
                            case 41:
                            case 42:
                            case 43:
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                            case 49:
                            case 52:
                            case 53:
                                break;
                            case 39:
                            case 51:
                                if (!foundWildAttribute) {
                                    if (compiler.getStepLocalName(stepOpCodePos2).equals("*")) {
                                        foundWildAttribute = true;
                                        break;
                                    }
                                }
                                return false;
                                break;
                            case 40:
                            case 48:
                            case 50:
                                break;
                            default:
                                throw new RuntimeException(XPATHMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                        }
                }
            }
            stepOpCodePos2 = nextStepOpCodePos;
        }
        return true;
    }

    public static boolean isOneStep(int analysis) {
        return (analysis & BITS_COUNT) == 1;
    }

    public static int getStepCount(int analysis) {
        return analysis & BITS_COUNT;
    }
}
