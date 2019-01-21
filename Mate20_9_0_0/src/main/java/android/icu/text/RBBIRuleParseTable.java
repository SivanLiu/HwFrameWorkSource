package android.icu.text;

class RBBIRuleParseTable {
    static final short doCheckVarDef = (short) 1;
    static final short doDotAny = (short) 2;
    static final short doEndAssign = (short) 3;
    static final short doEndOfRule = (short) 4;
    static final short doEndVariableName = (short) 5;
    static final short doExit = (short) 6;
    static final short doExprCatOperator = (short) 7;
    static final short doExprFinished = (short) 8;
    static final short doExprOrOperator = (short) 9;
    static final short doExprRParen = (short) 10;
    static final short doExprStart = (short) 11;
    static final short doLParen = (short) 12;
    static final short doNOP = (short) 13;
    static final short doNoChain = (short) 14;
    static final short doOptionEnd = (short) 15;
    static final short doOptionStart = (short) 16;
    static final short doReverseDir = (short) 17;
    static final short doRuleChar = (short) 18;
    static final short doRuleError = (short) 19;
    static final short doRuleErrorAssignExpr = (short) 20;
    static final short doScanUnicodeSet = (short) 21;
    static final short doSlash = (short) 22;
    static final short doStartAssign = (short) 23;
    static final short doStartTagValue = (short) 24;
    static final short doStartVariableName = (short) 25;
    static final short doTagDigit = (short) 26;
    static final short doTagExpectedError = (short) 27;
    static final short doTagValue = (short) 28;
    static final short doUnaryOpPlus = (short) 29;
    static final short doUnaryOpQuestion = (short) 30;
    static final short doUnaryOpStar = (short) 31;
    static final short doVariableNameExpectedErr = (short) 32;
    static RBBIRuleTableElement[] gRuleParseStateTable = new RBBIRuleTableElement[]{new RBBIRuleTableElement((short) 13, 0, 0, 0, true, null), new RBBIRuleTableElement((short) 11, 254, 29, 9, false, "start"), new RBBIRuleTableElement((short) 13, 132, 1, 0, true, null), new RBBIRuleTableElement((short) 14, 94, 12, 9, true, null), new RBBIRuleTableElement((short) 11, 36, 88, 98, false, null), new RBBIRuleTableElement((short) 13, 33, 19, 0, true, null), new RBBIRuleTableElement((short) 13, 59, 1, 0, true, null), new RBBIRuleTableElement((short) 13, 252, 0, 0, false, null), new RBBIRuleTableElement((short) 11, 255, 29, 9, false, null), new RBBIRuleTableElement((short) 4, 59, 1, 0, true, "break-rule-end"), new RBBIRuleTableElement((short) 13, 132, 9, 0, true, null), new RBBIRuleTableElement(doRuleError, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 11, 254, 29, 0, false, "start-after-caret"), new RBBIRuleTableElement((short) 13, 132, 12, 0, true, null), new RBBIRuleTableElement(doRuleError, 94, 103, 0, false, null), new RBBIRuleTableElement((short) 11, 36, 88, 37, false, null), new RBBIRuleTableElement(doRuleError, 59, 103, 0, false, null), new RBBIRuleTableElement(doRuleError, 252, 103, 0, false, null), new RBBIRuleTableElement((short) 11, 255, 29, 0, false, null), new RBBIRuleTableElement((short) 13, 33, 21, 0, true, "rev-option"), new RBBIRuleTableElement((short) 17, 255, 28, 9, false, null), new RBBIRuleTableElement((short) 16, 130, 23, 0, true, "option-scan1"), new RBBIRuleTableElement(doRuleError, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 13, 129, 23, 0, true, "option-scan2"), new RBBIRuleTableElement((short) 15, 255, 25, 0, false, null), new RBBIRuleTableElement((short) 13, 59, 1, 0, true, "option-scan3"), new RBBIRuleTableElement((short) 13, 132, 25, 0, true, null), new RBBIRuleTableElement(doRuleError, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 11, 255, 29, 9, false, "reverse-rule"), new RBBIRuleTableElement(doRuleChar, 254, 38, 0, true, "term"), new RBBIRuleTableElement((short) 13, 132, 29, 0, true, null), new RBBIRuleTableElement(doRuleChar, 131, 38, 0, true, null), new RBBIRuleTableElement((short) 13, 91, 94, 38, false, null), new RBBIRuleTableElement((short) 12, 40, 29, 38, true, null), new RBBIRuleTableElement((short) 13, 36, 88, 37, false, null), new RBBIRuleTableElement((short) 2, 46, 38, 0, true, null), new RBBIRuleTableElement(doRuleError, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 1, 255, 38, 0, false, "term-var-ref"), new RBBIRuleTableElement((short) 13, 132, 38, 0, true, "expr-mod"), new RBBIRuleTableElement(doUnaryOpStar, 42, 43, 0, true, null), new RBBIRuleTableElement(doUnaryOpPlus, 43, 43, 0, true, null), new RBBIRuleTableElement(doUnaryOpQuestion, 63, 43, 0, true, null), new RBBIRuleTableElement((short) 13, 255, 43, 0, false, null), new RBBIRuleTableElement((short) 7, 254, 29, 0, false, "expr-cont"), new RBBIRuleTableElement((short) 13, 132, 43, 0, true, null), new RBBIRuleTableElement((short) 7, 131, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 91, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 40, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 36, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 46, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 47, 55, 0, false, null), new RBBIRuleTableElement((short) 7, 123, 67, 0, true, null), new RBBIRuleTableElement((short) 9, 124, 29, 0, true, null), new RBBIRuleTableElement((short) 10, 41, 255, 0, true, null), new RBBIRuleTableElement((short) 8, 255, 255, 0, false, null), new RBBIRuleTableElement(doSlash, 47, 57, 0, true, "look-ahead"), new RBBIRuleTableElement((short) 13, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 7, 254, 29, 0, false, "expr-cont-no-slash"), new RBBIRuleTableElement((short) 13, 132, 43, 0, true, null), new RBBIRuleTableElement((short) 7, 131, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 91, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 40, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 36, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 46, 29, 0, false, null), new RBBIRuleTableElement((short) 9, 124, 29, 0, true, null), new RBBIRuleTableElement((short) 10, 41, 255, 0, true, null), new RBBIRuleTableElement((short) 8, 255, 255, 0, false, null), new RBBIRuleTableElement((short) 13, 132, 67, 0, true, "tag-open"), new RBBIRuleTableElement(doStartTagValue, 128, 70, 0, false, null), new RBBIRuleTableElement(doTagExpectedError, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 13, 132, 74, 0, true, "tag-value"), new RBBIRuleTableElement((short) 13, 125, 74, 0, false, null), new RBBIRuleTableElement(doTagDigit, 128, 70, 0, true, null), new RBBIRuleTableElement(doTagExpectedError, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 13, 132, 74, 0, true, "tag-close"), new RBBIRuleTableElement(doTagValue, 125, 77, 0, true, null), new RBBIRuleTableElement(doTagExpectedError, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 7, 254, 29, 0, false, "expr-cont-no-tag"), new RBBIRuleTableElement((short) 13, 132, 77, 0, true, null), new RBBIRuleTableElement((short) 7, 131, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 91, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 40, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 36, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 46, 29, 0, false, null), new RBBIRuleTableElement((short) 7, 47, 55, 0, false, null), new RBBIRuleTableElement((short) 9, 124, 29, 0, true, null), new RBBIRuleTableElement((short) 10, 41, 255, 0, true, null), new RBBIRuleTableElement((short) 8, 255, 255, 0, false, null), new RBBIRuleTableElement(doStartVariableName, 36, 90, 0, true, "scan-var-name"), new RBBIRuleTableElement((short) 13, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 13, 130, 92, 0, true, "scan-var-start"), new RBBIRuleTableElement((short) 32, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 13, 129, 92, 0, true, "scan-var-body"), new RBBIRuleTableElement((short) 5, 255, 255, 0, false, null), new RBBIRuleTableElement(doScanUnicodeSet, 91, 255, 0, true, "scan-unicode-set"), new RBBIRuleTableElement(doScanUnicodeSet, 112, 255, 0, true, null), new RBBIRuleTableElement(doScanUnicodeSet, 80, 255, 0, true, null), new RBBIRuleTableElement((short) 13, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 13, 132, 98, 0, true, "assign-or-rule"), new RBBIRuleTableElement(doStartAssign, 61, 29, 101, true, null), new RBBIRuleTableElement((short) 13, 255, 37, 9, false, null), new RBBIRuleTableElement((short) 3, 59, 1, 0, true, "assign-end"), new RBBIRuleTableElement(doRuleErrorAssignExpr, 255, 103, 0, false, null), new RBBIRuleTableElement((short) 6, 255, 103, 0, true, "errorDeath")};
    static final short kRuleSet_default = (short) 255;
    static final short kRuleSet_digit_char = (short) 128;
    static final short kRuleSet_eof = (short) 252;
    static final short kRuleSet_escaped = (short) 254;
    static final short kRuleSet_name_char = (short) 129;
    static final short kRuleSet_name_start_char = (short) 130;
    static final short kRuleSet_rule_char = (short) 131;
    static final short kRuleSet_white_space = (short) 132;

    static class RBBIRuleTableElement {
        short fAction;
        short fCharClass;
        boolean fNextChar;
        short fNextState;
        short fPushState;
        String fStateName;

        RBBIRuleTableElement(short a, int cc, int ns, int ps, boolean nc, String sn) {
            this.fAction = a;
            this.fCharClass = (short) cc;
            this.fNextState = (short) ns;
            this.fPushState = (short) ps;
            this.fNextChar = nc;
            this.fStateName = sn;
        }
    }

    RBBIRuleParseTable() {
    }
}
