package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import java.io.PrintStream;
import java.text.ParsePosition;
import java.util.HashMap;

class RBBIRuleScanner {
    static final int chLS = 8232;
    static final int chNEL = 133;
    private static String gRuleSet_digit_char_pattern = "[0-9]";
    private static String gRuleSet_name_char_pattern = "[_\\p{L}\\p{N}]";
    private static String gRuleSet_name_start_char_pattern = "[_\\p{L}]";
    private static String gRuleSet_rule_char_pattern = "[^[\\p{Z}\\u0020-\\u007f]-[\\p{L}]-[\\p{N}]]";
    private static String gRuleSet_white_space_pattern = "[\\p{Pattern_White_Space}]";
    private static String kAny = "any";
    private static final int kStackSize = 100;
    RBBIRuleChar fC = new RBBIRuleChar();
    int fCharNum;
    int fLastChar;
    int fLineNum;
    boolean fLookAheadRule;
    int fNextIndex;
    boolean fNoChainInRule;
    RBBINode[] fNodeStack = new RBBINode[100];
    int fNodeStackPtr;
    int fOptionStart;
    boolean fQuoteMode;
    RBBIRuleBuilder fRB;
    boolean fReverseRule;
    int fRuleNum;
    UnicodeSet[] fRuleSets = new UnicodeSet[10];
    int fScanIndex;
    HashMap<String, RBBISetTableEl> fSetTable = new HashMap();
    short[] fStack = new short[100];
    int fStackPtr;
    RBBISymbolTable fSymbolTable;

    static class RBBIRuleChar {
        int fChar;
        boolean fEscaped;

        RBBIRuleChar() {
        }
    }

    static class RBBISetTableEl {
        String key;
        RBBINode val;

        RBBISetTableEl() {
        }
    }

    RBBIRuleScanner(RBBIRuleBuilder rb) {
        this.fRB = rb;
        this.fLineNum = 1;
        this.fRuleSets[3] = new UnicodeSet(gRuleSet_rule_char_pattern);
        this.fRuleSets[4] = new UnicodeSet(gRuleSet_white_space_pattern);
        this.fRuleSets[1] = new UnicodeSet(gRuleSet_name_char_pattern);
        this.fRuleSets[2] = new UnicodeSet(gRuleSet_name_start_char_pattern);
        this.fRuleSets[0] = new UnicodeSet(gRuleSet_digit_char_pattern);
        this.fSymbolTable = new RBBISymbolTable(this);
    }

    boolean doParseActions(int action) {
        int i = 1;
        RBBINode n;
        RBBINode startExprNode;
        RBBINode varRefNode;
        RBBINode RHSExprNode;
        int i2;
        RBBINode[] rBBINodeArr;
        int i3;
        switch (action) {
            case 1:
                if (this.fNodeStack[this.fNodeStackPtr].fLeftChild != null) {
                    return true;
                }
                error(66058);
                return false;
            case 2:
                n = pushNewNode(0);
                findSetFor(kAny, n, null);
                n.fFirstPos = this.fScanIndex;
                n.fLastPos = this.fNextIndex;
                n.fText = this.fRB.fRules.substring(n.fFirstPos, n.fLastPos);
                return true;
            case 3:
                fixOpStack(1);
                startExprNode = this.fNodeStack[this.fNodeStackPtr - 2];
                varRefNode = this.fNodeStack[this.fNodeStackPtr - 1];
                RHSExprNode = this.fNodeStack[this.fNodeStackPtr];
                RHSExprNode.fFirstPos = startExprNode.fFirstPos;
                RHSExprNode.fLastPos = this.fScanIndex;
                RHSExprNode.fText = this.fRB.fRules.substring(RHSExprNode.fFirstPos, RHSExprNode.fLastPos);
                varRefNode.fLeftChild = RHSExprNode;
                RHSExprNode.fParent = varRefNode;
                this.fSymbolTable.addEntry(varRefNode.fText, varRefNode);
                this.fNodeStackPtr -= 3;
                return true;
            case 4:
                RBBINode endNode;
                fixOpStack(1);
                if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("rtree") >= 0) {
                    printNodeStack("end of rule");
                }
                Assert.assrt(this.fNodeStackPtr == 1);
                RHSExprNode = this.fNodeStack[this.fNodeStackPtr];
                if (this.fLookAheadRule) {
                    endNode = pushNewNode(6);
                    startExprNode = pushNewNode(8);
                    this.fNodeStackPtr -= 2;
                    startExprNode.fLeftChild = RHSExprNode;
                    startExprNode.fRightChild = endNode;
                    this.fNodeStack[this.fNodeStackPtr] = startExprNode;
                    endNode.fVal = this.fRuleNum;
                    endNode.fLookAheadEnd = true;
                    RHSExprNode = startExprNode;
                }
                RHSExprNode.fRuleRoot = true;
                if (this.fRB.fChainRules && !this.fNoChainInRule) {
                    RHSExprNode.fChainIn = true;
                }
                if (!this.fReverseRule) {
                    i = this.fRB.fDefaultTree;
                }
                int destRules = i;
                if (this.fRB.fTreeRoots[destRules] != null) {
                    RHSExprNode = this.fNodeStack[this.fNodeStackPtr];
                    endNode = this.fRB.fTreeRoots[destRules];
                    varRefNode = pushNewNode(9);
                    varRefNode.fLeftChild = endNode;
                    endNode.fParent = varRefNode;
                    varRefNode.fRightChild = RHSExprNode;
                    RHSExprNode.fParent = varRefNode;
                    this.fRB.fTreeRoots[destRules] = varRefNode;
                } else {
                    this.fRB.fTreeRoots[destRules] = this.fNodeStack[this.fNodeStackPtr];
                }
                this.fReverseRule = false;
                this.fLookAheadRule = false;
                this.fNoChainInRule = false;
                this.fNodeStackPtr = 0;
                return true;
            case 5:
                n = this.fNodeStack[this.fNodeStackPtr];
                if (n == null || n.fType != 2) {
                    error(66049);
                    return true;
                }
                n.fLastPos = this.fScanIndex;
                n.fText = this.fRB.fRules.substring(n.fFirstPos + 1, n.fLastPos);
                n.fLeftChild = this.fSymbolTable.lookupNode(n.fText);
                return true;
            case 6:
                return false;
            case 7:
                fixOpStack(4);
                varRefNode = this.fNodeStack;
                i2 = this.fNodeStackPtr;
                this.fNodeStackPtr = i2 - 1;
                varRefNode = varRefNode[i2];
                startExprNode = pushNewNode(8);
                startExprNode.fLeftChild = varRefNode;
                varRefNode.fParent = startExprNode;
                return true;
            case 8:
            case 13:
                return true;
            case 9:
                fixOpStack(4);
                startExprNode = this.fNodeStack;
                i2 = this.fNodeStackPtr;
                this.fNodeStackPtr = i2 - 1;
                startExprNode = startExprNode[i2];
                varRefNode = pushNewNode(9);
                varRefNode.fLeftChild = startExprNode;
                startExprNode.fParent = varRefNode;
                return true;
            case 10:
                fixOpStack(2);
                return true;
            case 11:
                pushNewNode(7);
                this.fRuleNum++;
                return true;
            case 12:
                pushNewNode(15);
                return true;
            case 14:
                this.fNoChainInRule = true;
                return true;
            case 15:
                String opt = this.fRB.fRules.substring(this.fOptionStart, this.fScanIndex);
                if (opt.equals("chain")) {
                    this.fRB.fChainRules = true;
                    return true;
                } else if (opt.equals("LBCMNoChain")) {
                    this.fRB.fLBCMNoChain = true;
                    return true;
                } else if (opt.equals("forward")) {
                    this.fRB.fDefaultTree = 0;
                    return true;
                } else if (opt.equals("reverse")) {
                    this.fRB.fDefaultTree = 1;
                    return true;
                } else if (opt.equals("safe_forward")) {
                    this.fRB.fDefaultTree = 2;
                    return true;
                } else if (opt.equals("safe_reverse")) {
                    this.fRB.fDefaultTree = 3;
                    return true;
                } else if (opt.equals("lookAheadHardBreak")) {
                    this.fRB.fLookAheadHardBreak = true;
                    return true;
                } else if (opt.equals("quoted_literals_only")) {
                    this.fRuleSets[3].clear();
                    return true;
                } else if (opt.equals("unquoted_literals")) {
                    this.fRuleSets[3].applyPattern(gRuleSet_rule_char_pattern);
                    return true;
                } else {
                    error(66061);
                    return true;
                }
            case 16:
                this.fOptionStart = this.fScanIndex;
                return true;
            case 17:
                this.fReverseRule = true;
                return true;
            case 18:
                n = pushNewNode(0);
                findSetFor(String.valueOf((char) this.fC.fChar), n, null);
                n.fFirstPos = this.fScanIndex;
                n.fLastPos = this.fNextIndex;
                n.fText = this.fRB.fRules.substring(n.fFirstPos, n.fLastPos);
                return true;
            case 19:
                error(66052);
                return false;
            case 20:
                error(66054);
                return false;
            case 21:
                scanSet();
                return true;
            case 22:
                n = pushNewNode(4);
                n.fVal = this.fRuleNum;
                n.fFirstPos = this.fScanIndex;
                n.fLastPos = this.fNextIndex;
                n.fText = this.fRB.fRules.substring(n.fFirstPos, n.fLastPos);
                this.fLookAheadRule = true;
                return true;
            case 23:
                this.fNodeStack[this.fNodeStackPtr - 1].fFirstPos = this.fNextIndex;
                pushNewNode(7);
                return true;
            case 24:
                n = pushNewNode(5);
                n.fVal = 0;
                n.fFirstPos = this.fScanIndex;
                n.fLastPos = this.fNextIndex;
                return true;
            case 25:
                pushNewNode(2).fFirstPos = this.fScanIndex;
                return true;
            case 26:
                n = this.fNodeStack[this.fNodeStackPtr];
                n.fVal = (n.fVal * 10) + UCharacter.digit((char) this.fC.fChar, 10);
                return true;
            case 27:
                error(66062);
                return false;
            case 28:
                n = this.fNodeStack[this.fNodeStackPtr];
                n.fLastPos = this.fNextIndex;
                n.fText = this.fRB.fRules.substring(n.fFirstPos, n.fLastPos);
                return true;
            case 29:
                rBBINodeArr = this.fNodeStack;
                i3 = this.fNodeStackPtr;
                this.fNodeStackPtr = i3 - 1;
                startExprNode = rBBINodeArr[i3];
                varRefNode = pushNewNode(11);
                varRefNode.fLeftChild = startExprNode;
                startExprNode.fParent = varRefNode;
                return true;
            case 30:
                rBBINodeArr = this.fNodeStack;
                i3 = this.fNodeStackPtr;
                this.fNodeStackPtr = i3 - 1;
                startExprNode = rBBINodeArr[i3];
                varRefNode = pushNewNode(12);
                varRefNode.fLeftChild = startExprNode;
                startExprNode.fParent = varRefNode;
                return true;
            case 31:
                startExprNode = this.fNodeStack;
                i3 = this.fNodeStackPtr;
                this.fNodeStackPtr = i3 - 1;
                startExprNode = startExprNode[i3];
                varRefNode = pushNewNode(10);
                varRefNode.fLeftChild = startExprNode;
                startExprNode.fParent = varRefNode;
                return true;
            case 32:
                error(66052);
                return true;
            default:
                error(66049);
                return false;
        }
    }

    void error(int e) {
        String s = new StringBuilder();
        s.append("Error ");
        s.append(e);
        s.append(" at line ");
        s.append(this.fLineNum);
        s.append(" column ");
        s.append(this.fCharNum);
        throw new IllegalArgumentException(s.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x003d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void fixOpStack(int p) {
        while (true) {
            RBBINode n = this.fNodeStack[this.fNodeStackPtr - 1];
            if (n.fPrecedence == 0) {
                System.out.print("RBBIRuleScanner.fixOpStack, bad operator node");
                error(66049);
                return;
            } else if (n.fPrecedence >= p && n.fPrecedence > 2) {
                n.fRightChild = this.fNodeStack[this.fNodeStackPtr];
                this.fNodeStack[this.fNodeStackPtr].fParent = n;
                this.fNodeStackPtr--;
            } else if (p <= 2) {
                if (n.fPrecedence != p) {
                    error(66056);
                }
                this.fNodeStack[this.fNodeStackPtr - 1] = this.fNodeStack[this.fNodeStackPtr];
                this.fNodeStackPtr--;
            }
        }
        if (p <= 2) {
        }
    }

    void findSetFor(String s, RBBINode node, UnicodeSet setToAdopt) {
        RBBISetTableEl el = (RBBISetTableEl) this.fSetTable.get(s);
        boolean c = false;
        if (el != null) {
            node.fLeftChild = el.val;
            if (node.fLeftChild.fType == 1) {
                c = true;
            }
            Assert.assrt(c);
            return;
        }
        if (setToAdopt == null) {
            if (s.equals(kAny)) {
                setToAdopt = new UnicodeSet(0, 1114111);
            } else {
                int c2 = UTF16.charAt(s, 0);
                setToAdopt = new UnicodeSet(c2, c2);
            }
        }
        RBBINode usetNode = new RBBINode(1);
        usetNode.fInputSet = setToAdopt;
        usetNode.fParent = node;
        node.fLeftChild = usetNode;
        usetNode.fText = s;
        this.fRB.fUSetNodes.add(usetNode);
        el = new RBBISetTableEl();
        el.key = s;
        el.val = usetNode;
        this.fSetTable.put(el.key, el);
    }

    static String stripRules(String rules) {
        StringBuilder strippedRules = new StringBuilder();
        char rulesLength = rules.length();
        char idx;
        for (char ch = 0; ch < rulesLength; ch = idx) {
            idx = ch + 1;
            ch = rules.charAt(ch);
            if (ch == '#') {
                while (idx < rulesLength && ch != 13 && ch != 10 && ch != 133) {
                    char idx2 = idx + 1;
                    ch = rules.charAt(idx);
                    idx = idx2;
                }
            }
            if (!UCharacter.isISOControl(ch)) {
                strippedRules.append(ch);
            }
        }
        return strippedRules.toString();
    }

    int nextCharLL() {
        if (this.fNextIndex >= this.fRB.fRules.length()) {
            return -1;
        }
        int ch = UTF16.charAt(this.fRB.fRules, this.fNextIndex);
        this.fNextIndex = UTF16.moveCodePointOffset(this.fRB.fRules, this.fNextIndex, 1);
        if (ch == 13 || ch == 133 || ch == chLS || (ch == 10 && this.fLastChar != 13)) {
            this.fLineNum++;
            this.fCharNum = 0;
            if (this.fQuoteMode) {
                error(66057);
                this.fQuoteMode = false;
            }
        } else if (ch != 10) {
            this.fCharNum++;
        }
        this.fLastChar = ch;
        return ch;
    }

    void nextChar(RBBIRuleChar c) {
        this.fScanIndex = this.fNextIndex;
        c.fChar = nextCharLL();
        c.fEscaped = false;
        if (c.fChar == 39) {
            if (UTF16.charAt(this.fRB.fRules, this.fNextIndex) == 39) {
                c.fChar = nextCharLL();
                c.fEscaped = true;
            } else {
                this.fQuoteMode ^= 1;
                if (this.fQuoteMode) {
                    c.fChar = 40;
                } else {
                    c.fChar = 41;
                }
                c.fEscaped = false;
                return;
            }
        }
        if (this.fQuoteMode) {
            c.fEscaped = true;
        } else {
            if (c.fChar == 35) {
                do {
                    c.fChar = nextCharLL();
                    if (c.fChar == -1 || c.fChar == 13 || c.fChar == 10 || c.fChar == 133) {
                        break;
                    }
                } while (c.fChar != chLS);
            }
            if (c.fChar != -1 && c.fChar == 92) {
                c.fEscaped = true;
                int[] unescapeIndex = new int[]{this.fNextIndex};
                c.fChar = Utility.unescapeAt(this.fRB.fRules, unescapeIndex);
                if (unescapeIndex[0] == this.fNextIndex) {
                    error(66050);
                }
                this.fCharNum += unescapeIndex[0] - this.fNextIndex;
                this.fNextIndex = unescapeIndex[0];
            }
        }
    }

    void parse() {
        int state = 1;
        nextChar(this.fC);
        while (state != 0) {
            RBBIRuleTableElement tableEl;
            RBBIRuleTableElement tableEl2 = RBBIRuleParseTable.gRuleParseStateTable[state];
            if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("char, line, col = ('");
                stringBuilder.append((char) this.fC.fChar);
                stringBuilder.append("', ");
                stringBuilder.append(this.fLineNum);
                stringBuilder.append(", ");
                stringBuilder.append(this.fCharNum);
                stringBuilder.append("    state = ");
                stringBuilder.append(tableEl2.fStateName);
                printStream.println(stringBuilder.toString());
            }
            int tableRow = state;
            while (true) {
                tableEl = RBBIRuleParseTable.gRuleParseStateTable[tableRow];
                if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                    System.out.print(".");
                }
                if ((tableEl.fCharClass >= (short) 127 || this.fC.fEscaped || tableEl.fCharClass != this.fC.fChar) && tableEl.fCharClass != (short) 255 && (!(tableEl.fCharClass == (short) 254 && this.fC.fEscaped) && (!(tableEl.fCharClass == (short) 253 && this.fC.fEscaped && (this.fC.fChar == 80 || this.fC.fChar == 112)) && (!(tableEl.fCharClass == (short) 252 && this.fC.fChar == -1) && (tableEl.fCharClass < (short) 128 || tableEl.fCharClass >= (short) 240 || this.fC.fEscaped || this.fC.fChar == -1 || !this.fRuleSets[tableEl.fCharClass - 128].contains(this.fC.fChar)))))) {
                    tableRow++;
                }
            }
            if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                System.out.println("");
            }
            if (!doParseActions(tableEl.fAction)) {
                break;
            }
            if (tableEl.fPushState != (short) 0) {
                this.fStackPtr++;
                if (this.fStackPtr >= 100) {
                    System.out.println("RBBIRuleScanner.parse() - state stack overflow.");
                    error(66049);
                }
                this.fStack[this.fStackPtr] = tableEl.fPushState;
            }
            if (tableEl.fNextChar) {
                nextChar(this.fC);
            }
            if (tableEl.fNextState != (short) 255) {
                state = tableEl.fNextState;
            } else {
                state = this.fStack[this.fStackPtr];
                this.fStackPtr--;
                if (this.fStackPtr < 0) {
                    System.out.println("RBBIRuleScanner.parse() - state stack underflow.");
                    error(66049);
                }
            }
        }
        if (this.fRB.fTreeRoots[0] == null) {
            error(66052);
        }
        if (this.fRB.fTreeRoots[1] == null) {
            this.fRB.fTreeRoots[1] = pushNewNode(10);
            RBBINode operand = pushNewNode(0);
            findSetFor(kAny, operand, null);
            this.fRB.fTreeRoots[1].fLeftChild = operand;
            operand.fParent = this.fRB.fTreeRoots[1];
            this.fNodeStackPtr -= 2;
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("symbols") >= 0) {
            this.fSymbolTable.rbbiSymtablePrint();
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("ptree") >= 0) {
            System.out.println("Completed Forward Rules Parse Tree...");
            this.fRB.fTreeRoots[0].printTree(true);
            System.out.println("\nCompleted Reverse Rules Parse Tree...");
            this.fRB.fTreeRoots[1].printTree(true);
            System.out.println("\nCompleted Safe Point Forward Rules Parse Tree...");
            if (this.fRB.fTreeRoots[2] == null) {
                System.out.println("  -- null -- ");
            } else {
                this.fRB.fTreeRoots[2].printTree(true);
            }
            System.out.println("\nCompleted Safe Point Reverse Rules Parse Tree...");
            if (this.fRB.fTreeRoots[3] == null) {
                System.out.println("  -- null -- ");
            } else {
                this.fRB.fTreeRoots[3].printTree(true);
            }
        }
    }

    void printNodeStack(String title) {
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(title);
        stringBuilder.append(".  Dumping node stack...\n");
        printStream.println(stringBuilder.toString());
        for (int i = this.fNodeStackPtr; i > 0; i--) {
            this.fNodeStack[i].printTree(true);
        }
    }

    RBBINode pushNewNode(int nodeType) {
        this.fNodeStackPtr++;
        if (this.fNodeStackPtr >= 100) {
            System.out.println("RBBIRuleScanner.pushNewNode - stack overflow.");
            error(66049);
        }
        this.fNodeStack[this.fNodeStackPtr] = new RBBINode(nodeType);
        return this.fNodeStack[this.fNodeStackPtr];
    }

    void scanSet() {
        UnicodeSet uset = null;
        ParsePosition pos = new ParsePosition(this.fScanIndex);
        int startPos = this.fScanIndex;
        try {
            uset = new UnicodeSet(this.fRB.fRules, pos, this.fSymbolTable, 1);
        } catch (Exception e) {
            error(66063);
        }
        if (uset.isEmpty()) {
            error(66060);
        }
        int i = pos.getIndex();
        while (this.fNextIndex < i) {
            nextCharLL();
        }
        RBBINode n = pushNewNode(null);
        n.fFirstPos = startPos;
        n.fLastPos = this.fNextIndex;
        n.fText = this.fRB.fRules.substring(n.fFirstPos, n.fLastPos);
        findSetFor(n.fText, n, uset);
    }
}
