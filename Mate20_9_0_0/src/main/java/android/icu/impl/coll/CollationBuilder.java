package android.icu.impl.coll;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Normalizer2Impl.Hangul;
import android.icu.text.CanonicalIterator;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.Normalizer2;
import android.icu.text.UnicodeSet;
import android.icu.text.UnicodeSetIterator;
import android.icu.util.ULocale;
import java.text.ParseException;

public final class CollationBuilder extends Sink {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final UnicodeSet COMPOSITES = new UnicodeSet("[:NFD_QC=N:]");
    private static final boolean DEBUG = false;
    private static final int HAS_BEFORE2 = 64;
    private static final int HAS_BEFORE3 = 32;
    private static final int IS_TAILORED = 8;
    private static final int MAX_INDEX = 1048575;
    private CollationTailoring base;
    private CollationData baseData;
    private long[] ces = new long[31];
    private int cesLength;
    private CollationDataBuilder dataBuilder;
    private boolean fastLatinEnabled;
    private Normalizer2 fcd = Norm2AllModes.getFCDNormalizer2();
    private Normalizer2Impl nfcImpl = Norm2AllModes.getNFCInstance().impl;
    private Normalizer2 nfd = Normalizer2.getNFDInstance();
    private UVector64 nodes;
    private UnicodeSet optimizeSet = new UnicodeSet();
    private CollationRootElements rootElements;
    private UVector32 rootPrimaryIndexes;
    private long variableTop;

    private static final class BundleImporter implements Importer {
        BundleImporter() {
        }

        public String getRules(String localeID, String collationType) {
            return CollationLoader.loadRules(new ULocale(localeID), collationType);
        }
    }

    private static final class CEFinalizer implements CEModifier {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private long[] finalCEs;

        static {
            Class cls = CollationBuilder.class;
        }

        CEFinalizer(long[] ces) {
            this.finalCEs = ces;
        }

        public long modifyCE32(int ce32) {
            if (CollationBuilder.isTempCE32(ce32)) {
                return this.finalCEs[CollationBuilder.indexFromTempCE32(ce32)] | ((long) ((ce32 & 192) << 8));
            }
            return Collation.NO_CE;
        }

        public long modifyCE(long ce) {
            if (CollationBuilder.isTempCE(ce)) {
                return this.finalCEs[CollationBuilder.indexFromTempCE(ce)] | (49152 & ce);
            }
            return Collation.NO_CE;
        }
    }

    static {
        COMPOSITES.remove(Hangul.HANGUL_BASE, Hangul.HANGUL_END);
    }

    public CollationBuilder(CollationTailoring b) {
        this.base = b;
        this.baseData = b.data;
        this.rootElements = new CollationRootElements(b.data.rootElements);
        this.variableTop = 0;
        this.dataBuilder = new CollationDataBuilder();
        this.fastLatinEnabled = true;
        this.cesLength = 0;
        this.rootPrimaryIndexes = new UVector32();
        this.nodes = new UVector64();
        this.nfcImpl.ensureCanonIterData();
        this.dataBuilder.initForTailoring(this.baseData);
    }

    public CollationTailoring parseAndBuild(String ruleString) throws ParseException {
        if (this.baseData.rootElements != null) {
            CollationTailoring tailoring = new CollationTailoring(this.base.settings);
            CollationRuleParser parser = new CollationRuleParser(this.baseData);
            this.variableTop = ((CollationSettings) this.base.settings.readOnly()).variableTop;
            parser.setSink(this);
            parser.setImporter(new BundleImporter());
            CollationSettings ownedSettings = (CollationSettings) tailoring.settings.copyOnWrite();
            parser.parse(ruleString, ownedSettings);
            if (this.dataBuilder.hasMappings()) {
                makeTailoredCEs();
                closeOverComposites();
                finalizeCEs();
                this.optimizeSet.add(0, 127);
                this.optimizeSet.add(192, 255);
                this.optimizeSet.remove(Hangul.HANGUL_BASE, Hangul.HANGUL_END);
                this.dataBuilder.optimize(this.optimizeSet);
                tailoring.ensureOwnedData();
                if (this.fastLatinEnabled) {
                    this.dataBuilder.enableFastLatin();
                }
                this.dataBuilder.build(tailoring.ownedData);
                this.dataBuilder = null;
            } else {
                tailoring.data = this.baseData;
            }
            ownedSettings.fastLatinOptions = CollationFastLatin.getOptions(tailoring.data, ownedSettings, ownedSettings.fastLatinPrimaries);
            tailoring.setRules(ruleString);
            tailoring.setVersion(this.base.version, 0);
            return tailoring;
        }
        throw new UnsupportedOperationException("missing root elements data, tailoring not supported");
    }

    void addReset(int strength, CharSequence str) {
        if (str.charAt(0) == 65534) {
            this.ces[0] = getSpecialResetPosition(str);
            this.cesLength = 1;
        } else {
            this.cesLength = this.dataBuilder.getCEs(this.nfd.normalize(str), this.ces, 0);
            if (this.cesLength > 31) {
                throw new IllegalArgumentException("reset position maps to too many collation elements (more than 31)");
            }
        }
        if (strength != 15) {
            int index = findOrInsertNodeForCEs(strength);
            long node = this.nodes.elementAti(index);
            while (strengthFromNode(node) > strength) {
                index = previousIndexFromNode(node);
                node = this.nodes.elementAti(index);
            }
            if (strengthFromNode(node) == strength && isTailoredNode(node)) {
                index = previousIndexFromNode(node);
            } else if (strength == 0) {
                long p = weight32FromNode(node);
                if (p == 0) {
                    throw new UnsupportedOperationException("reset primary-before ignorable not possible");
                } else if (p <= this.rootElements.getFirstPrimary()) {
                    throw new UnsupportedOperationException("reset primary-before first non-ignorable not supported");
                } else if (p != 4278321664L) {
                    index = findOrInsertNodeForPrimary(this.rootElements.getPrimaryBefore(p, this.baseData.isCompressiblePrimary(p)));
                    while (true) {
                        p = nextIndexFromNode(this.nodes.elementAti(index));
                        if (p == null) {
                            break;
                        }
                        index = p;
                    }
                } else {
                    throw new UnsupportedOperationException("reset primary-before [first trailing] not supported");
                }
            } else {
                index = findCommonNode(index, 1);
                if (strength >= 2) {
                    index = findCommonNode(index, 2);
                }
                String nfdString = this.nodes.elementAti(index);
                if (strengthFromNode(nfdString) != strength) {
                    index = findOrInsertWeakNode(index, getWeight16Before(index, nfdString, strength), strength);
                } else if (weight16FromNode(nfdString) == 0) {
                    throw new UnsupportedOperationException(strength == 1 ? "reset secondary-before secondary ignorable not possible" : "reset tertiary-before completely ignorable not possible");
                } else {
                    int previousWeight16;
                    int weight16 = getWeight16Before(index, nfdString, strength);
                    int previousIndex = previousIndexFromNode(nfdString);
                    long node2 = nfdString;
                    nfdString = previousIndex;
                    while (true) {
                        node2 = this.nodes.elementAti(nfdString);
                        int previousStrength = strengthFromNode(node2);
                        if (previousStrength >= strength) {
                            if (previousStrength == strength && !isTailoredNode(node2)) {
                                previousWeight16 = weight16FromNode(node2);
                                break;
                            }
                            nfdString = previousIndexFromNode(node2);
                        } else {
                            previousWeight16 = Collation.COMMON_WEIGHT16;
                            break;
                        }
                    }
                    if (previousWeight16 == weight16) {
                        index = previousIndex;
                    } else {
                        node2 = nodeFromWeight16(weight16) | nodeFromStrength(strength);
                        index = insertNodeBetween(previousIndex, index, node2);
                    }
                    nfdString = node2;
                }
                strength = ceStrength(this.ces[this.cesLength - 1]);
            }
            this.ces[this.cesLength - 1] = tempCEFromIndexAndStrength(index, strength);
        }
    }

    private int getWeight16Before(int index, long node, int level) {
        int strengthFromNode = strengthFromNode(node);
        int s = Collation.COMMON_WEIGHT16;
        strengthFromNode = strengthFromNode == 2 ? weight16FromNode(node) : Collation.COMMON_WEIGHT16;
        while (strengthFromNode(node) > 1) {
            node = this.nodes.elementAti(previousIndexFromNode(node));
        }
        if (isTailoredNode(node)) {
            return 256;
        }
        if (strengthFromNode(node) == 1) {
            s = weight16FromNode(node);
        }
        while (strengthFromNode(node) > 0) {
            node = this.nodes.elementAti(previousIndexFromNode(node));
        }
        if (isTailoredNode(node)) {
            return 256;
        }
        int weight16;
        long p = weight32FromNode(node);
        if (level == 1) {
            weight16 = this.rootElements.getSecondaryBefore(p, s);
        } else {
            weight16 = this.rootElements.getTertiaryBefore(p, s, strengthFromNode);
        }
        return weight16;
    }

    private long getSpecialResetPosition(CharSequence str) {
        long ce;
        int strength;
        boolean isBoundary;
        int strength2 = 0;
        boolean isBoundary2 = false;
        Position pos = CollationRuleParser.POSITION_VALUES[str.charAt(1) - 10240];
        int nextIndexFromNode;
        int index;
        long node;
        switch (pos) {
            case FIRST_TERTIARY_IGNORABLE:
                return 0;
            case LAST_TERTIARY_IGNORABLE:
                return 0;
            case FIRST_SECONDARY_IGNORABLE:
                nextIndexFromNode = nextIndexFromNode(this.nodes.elementAti(findOrInsertNodeForRootCE(0, 2)));
                index = nextIndexFromNode;
                if (nextIndexFromNode != 0) {
                    node = this.nodes.elementAti(index);
                    if (isTailoredNode(node) && strengthFromNode(node) == 2) {
                        return tempCEFromIndexAndStrength(index, 2);
                    }
                }
                return this.rootElements.getFirstTertiaryCE();
            case LAST_SECONDARY_IGNORABLE:
                ce = this.rootElements.getLastTertiaryCE();
                strength2 = 2;
                break;
            case FIRST_PRIMARY_IGNORABLE:
                node = this.nodes.elementAti(findOrInsertNodeForRootCE(0, 1));
                do {
                    nextIndexFromNode = nextIndexFromNode(node);
                    index = nextIndexFromNode;
                    if (nextIndexFromNode != 0) {
                        node = this.nodes.elementAti(index);
                        strength2 = strengthFromNode(node);
                        if (strength2 < 1) {
                        }
                    }
                    strength = 1;
                    isBoundary = false;
                    ce = this.rootElements.getFirstSecondaryCE();
                    break;
                } while (strength2 != 1);
                if (isTailoredNode(node)) {
                    if (nodeHasBefore3(node)) {
                        index = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(node)));
                    }
                    return tempCEFromIndexAndStrength(index, 1);
                }
                strength = 1;
                isBoundary = false;
                ce = this.rootElements.getFirstSecondaryCE();
            case LAST_PRIMARY_IGNORABLE:
                ce = this.rootElements.getLastSecondaryCE();
                strength2 = 1;
                break;
            case FIRST_VARIABLE:
                ce = this.rootElements.getFirstPrimaryCE();
                isBoundary2 = true;
                break;
            case LAST_VARIABLE:
                ce = this.rootElements.lastCEWithPrimaryBefore(this.variableTop + 1);
                break;
            case FIRST_REGULAR:
                ce = this.rootElements.firstCEWithPrimaryAtLeast(this.variableTop + 1);
                isBoundary2 = true;
                break;
            case LAST_REGULAR:
                ce = this.rootElements.firstCEWithPrimaryAtLeast(this.baseData.getFirstPrimaryForGroup(17));
                break;
            case FIRST_IMPLICIT:
                ce = this.baseData.getSingleCE(19968);
                break;
            case LAST_IMPLICIT:
                throw new UnsupportedOperationException("reset to [last implicit] not supported");
            case FIRST_TRAILING:
                ce = Collation.makeCE(4278321664L);
                isBoundary2 = true;
                break;
            case LAST_TRAILING:
                throw new IllegalArgumentException("LDML forbids tailoring to U+FFFF");
            default:
                return 0;
        }
        strength = strength2;
        isBoundary = isBoundary2;
        strength2 = findOrInsertNodeForRootCE(ce, strength);
        long node2 = this.nodes.elementAti(strength2);
        long p;
        if ((pos.ordinal() & 1) == 0) {
            if (!nodeHasAnyBefore(node2) && isBoundary) {
                int nextIndexFromNode2 = nextIndexFromNode(node2);
                strength2 = nextIndexFromNode2;
                if (nextIndexFromNode2 != 0) {
                    node2 = this.nodes.elementAti(strength2);
                    ce = tempCEFromIndexAndStrength(strength2, strength);
                } else {
                    p = ce >>> 32;
                    ce = Collation.makeCE(this.rootElements.getPrimaryAfter(p, this.rootElements.findPrimary(p), this.baseData.isCompressiblePrimary(p)));
                    strength2 = findOrInsertNodeForRootCE(ce, 0);
                    node2 = this.nodes.elementAti(strength2);
                }
            }
            if (nodeHasAnyBefore(node2)) {
                if (nodeHasBefore2(node2)) {
                    strength2 = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(node2)));
                    node2 = this.nodes.elementAti(strength2);
                }
                if (nodeHasBefore3(node2)) {
                    strength2 = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(node2)));
                }
                ce = tempCEFromIndexAndStrength(strength2, strength);
            }
        } else {
            while (true) {
                isBoundary2 = nextIndexFromNode(node2);
                if (isBoundary2) {
                    p = this.nodes.elementAti(isBoundary2);
                    if (strengthFromNode(p) >= strength) {
                        boolean index2 = isBoundary2;
                        node2 = p;
                    }
                }
            }
            if (isTailoredNode(node2)) {
                ce = tempCEFromIndexAndStrength(strength2, strength);
            }
        }
        return ce;
    }

    void addRelation(int strength, CharSequence prefix, CharSequence str, CharSequence extension) {
        String nfdPrefix;
        int i = strength;
        CharSequence charSequence = prefix;
        CharSequence charSequence2 = str;
        if (prefix.length() == 0) {
            nfdPrefix = "";
        } else {
            nfdPrefix = this.nfd.normalize(charSequence);
        }
        String nfdPrefix2 = nfdPrefix;
        String nfdString = this.nfd.normalize(charSequence2);
        int nfdLength = nfdString.length();
        if (nfdLength >= 2) {
            char c = nfdString.charAt(0);
            if (Hangul.isJamoL(c) || Hangul.isJamoV(c)) {
                throw new UnsupportedOperationException("contractions starting with conjoining Jamo L or V not supported");
            }
            c = nfdString.charAt(nfdLength - 1);
            if (Hangul.isJamoL(c) || (Hangul.isJamoV(c) && Hangul.isJamoL(nfdString.charAt(nfdLength - 2)))) {
                throw new UnsupportedOperationException("contractions ending with conjoining Jamo L or L+V not supported");
            }
        }
        if (i != 15) {
            int index = findOrInsertNodeForCEs(strength);
            long ce = this.ces[this.cesLength - 1];
            if (i == 0 && !isTempCE(ce) && (ce >>> 32) == 0) {
                throw new UnsupportedOperationException("tailoring primary after ignorables not supported");
            } else if (i == 3 && ce == 0) {
                throw new UnsupportedOperationException("tailoring quaternary after tertiary ignorables not supported");
            } else {
                index = insertTailoredNodeAfter(index, i);
                int tempStrength = ceStrength(ce);
                if (i < tempStrength) {
                    tempStrength = i;
                }
                this.ces[this.cesLength - 1] = tempCEFromIndexAndStrength(index, tempStrength);
            }
        }
        setCaseBits(nfdString);
        int cesLengthBeforeExtension = this.cesLength;
        if (extension.length() != 0) {
            this.cesLength = this.dataBuilder.getCEs(this.nfd.normalize(extension), this.ces, this.cesLength);
            if (this.cesLength > 31) {
                throw new IllegalArgumentException("extension string adds too many collation elements (more than 31 total)");
            }
        }
        CharSequence charSequence3 = extension;
        int ce32 = -1;
        if (!((nfdPrefix2.contentEquals(charSequence) && nfdString.contentEquals(charSequence2)) || ignorePrefix(charSequence) || ignoreString(charSequence2))) {
            ce32 = addIfDifferent(charSequence, charSequence2, this.ces, this.cesLength, -1);
        }
        addWithClosure(nfdPrefix2, nfdString, this.ces, this.cesLength, ce32);
        this.cesLength = cesLengthBeforeExtension;
    }

    private int findOrInsertNodeForCEs(int strength) {
        long ce;
        while (this.cesLength != 0) {
            ce = this.ces[this.cesLength - 1];
            if (ceStrength(ce) <= strength) {
                break;
            }
            this.cesLength--;
        }
        this.ces[0] = 0;
        ce = 0;
        this.cesLength = 1;
        if (isTempCE(ce)) {
            return indexFromTempCE(ce);
        }
        if (((int) (ce >>> 56)) != 254) {
            return findOrInsertNodeForRootCE(ce, strength);
        }
        throw new UnsupportedOperationException("tailoring relative to an unassigned code point not supported");
    }

    private int findOrInsertNodeForRootCE(long ce, int strength) {
        int index = findOrInsertNodeForPrimary(ce >>> 32);
        if (strength < 1) {
            return index;
        }
        int lower32 = (int) ce;
        index = findOrInsertWeakNode(index, lower32 >>> 16, 1);
        if (strength >= 2) {
            return findOrInsertWeakNode(index, lower32 & Collation.ONLY_TERTIARY_MASK, 2);
        }
        return index;
    }

    private static final int binarySearchForRootPrimaryNode(int[] rootPrimaryIndexes, int length, long[] nodes, long p) {
        if (length == 0) {
            return -1;
        }
        int start = 0;
        int limit = length;
        while (true) {
            int i = (int) ((((long) start) + ((long) limit)) / 2);
            long nodePrimary = nodes[rootPrimaryIndexes[i]] >>> 32;
            if (p == nodePrimary) {
                return i;
            }
            if (p < nodePrimary) {
                if (i == start) {
                    return ~start;
                }
                limit = i;
            } else if (i == start) {
                return ~(start + 1);
            } else {
                start = i;
            }
        }
    }

    private int findOrInsertNodeForPrimary(long p) {
        int rootIndex = binarySearchForRootPrimaryNode(this.rootPrimaryIndexes.getBuffer(), this.rootPrimaryIndexes.size(), this.nodes.getBuffer(), p);
        if (rootIndex >= 0) {
            return this.rootPrimaryIndexes.elementAti(rootIndex);
        }
        int index = this.nodes.size();
        this.nodes.addElement(nodeFromWeight32(p));
        this.rootPrimaryIndexes.insertElementAt(index, ~rootIndex);
        return index;
    }

    private int findOrInsertWeakNode(int index, int weight16, int level) {
        if (weight16 == Collation.COMMON_WEIGHT16) {
            return findCommonNode(index, level);
        }
        int hasThisLevelBefore;
        int nextIndex;
        int nextIndex2;
        long node = this.nodes.elementAti(index);
        if (weight16 != 0 && weight16 < Collation.COMMON_WEIGHT16) {
            hasThisLevelBefore = level == 1 ? 64 : 32;
            if ((((long) hasThisLevelBefore) & node) == 0) {
                long commonNode = nodeFromWeight16(Collation.COMMON_WEIGHT16) | nodeFromStrength(level);
                if (level == 1) {
                    commonNode |= 32 & node;
                    node &= -33;
                }
                this.nodes.setElementAt(((long) hasThisLevelBefore) | node, index);
                nextIndex = nextIndexFromNode(node);
                index = insertNodeBetween(index, nextIndex, nodeFromWeight16(weight16) | nodeFromStrength(level));
                insertNodeBetween(index, nextIndex, commonNode);
                return index;
            }
        }
        while (true) {
            nextIndex = nextIndexFromNode(node);
            nextIndex2 = nextIndex;
            if (nextIndex == 0) {
                break;
            }
            node = this.nodes.elementAti(nextIndex2);
            nextIndex = strengthFromNode(node);
            if (nextIndex <= level) {
                if (nextIndex < level) {
                    break;
                } else if (isTailoredNode(node)) {
                    continue;
                } else {
                    hasThisLevelBefore = weight16FromNode(node);
                    if (hasThisLevelBefore == weight16) {
                        return nextIndex2;
                    }
                    if (hasThisLevelBefore > weight16) {
                        break;
                    }
                }
            }
            index = nextIndex2;
        }
        return insertNodeBetween(index, nextIndex2, nodeFromWeight16(weight16) | nodeFromStrength(level));
    }

    private int insertTailoredNodeAfter(int index, int strength) {
        int nextIndex;
        if (strength >= 1) {
            index = findCommonNode(index, 1);
            if (strength >= 2) {
                index = findCommonNode(index, 2);
            }
        }
        long node = this.nodes.elementAti(index);
        while (true) {
            int nextIndexFromNode = nextIndexFromNode(node);
            nextIndex = nextIndexFromNode;
            if (nextIndexFromNode == 0) {
                break;
            }
            node = this.nodes.elementAti(nextIndex);
            if (strengthFromNode(node) <= strength) {
                break;
            }
            index = nextIndex;
        }
        return insertNodeBetween(index, nextIndex, 8 | nodeFromStrength(strength));
    }

    private int insertNodeBetween(int index, int nextIndex, long node) {
        int newIndex = this.nodes.size();
        this.nodes.addElement(node | (nodeFromPreviousIndex(index) | nodeFromNextIndex(nextIndex)));
        this.nodes.setElementAt(changeNodeNextIndex(this.nodes.elementAti(index), newIndex), index);
        if (nextIndex != 0) {
            this.nodes.setElementAt(changeNodePreviousIndex(this.nodes.elementAti(nextIndex), newIndex), nextIndex);
        }
        return newIndex;
    }

    private int findCommonNode(int index, int strength) {
        long node = this.nodes.elementAti(index);
        if (strengthFromNode(node) >= strength) {
            return index;
        }
        if (!strength != 1 ? nodeHasBefore2(node) : nodeHasBefore3(node)) {
            return index;
        }
        node = this.nodes.elementAti(nextIndexFromNode(node));
        while (true) {
            index = nextIndexFromNode(node);
            node = this.nodes.elementAti(index);
            if (!isTailoredNode(node) && strengthFromNode(node) <= strength && weight16FromNode(node) >= Collation.COMMON_WEIGHT16) {
                return index;
            }
        }
    }

    private void setCaseBits(CharSequence nfdString) {
        int i;
        int numTailoredPrimaries = 0;
        for (i = 0; i < this.cesLength; i++) {
            if (ceStrength(this.ces[i]) == 0) {
                numTailoredPrimaries++;
            }
        }
        long cases = 0;
        i = 14;
        if (numTailoredPrimaries > 0) {
            UTF16CollationIterator baseCEs = new UTF16CollationIterator(this.baseData, false, nfdString, 0);
            int baseCEsLength = baseCEs.fetchCEs() - 1;
            int lastCase = 0;
            int numBasePrimaries = 0;
            long cases2 = 0;
            int i2 = 0;
            while (i2 < baseCEsLength) {
                long ce = baseCEs.getCE(i2);
                if ((ce >>> 32) != 0) {
                    numBasePrimaries++;
                    int c = (((int) ce) >> i) & 3;
                    if (numBasePrimaries < numTailoredPrimaries) {
                        cases2 |= ((long) c) << ((numBasePrimaries - 1) * 2);
                    } else if (numBasePrimaries == numTailoredPrimaries) {
                        lastCase = c;
                    } else if (c != lastCase) {
                        lastCase = 1;
                        break;
                    }
                }
                i2++;
                i = 14;
            }
            if (numBasePrimaries >= numTailoredPrimaries) {
                cases = cases2 | (((long) lastCase) << ((numTailoredPrimaries - 1) * 2));
            } else {
                cases = cases2;
            }
        }
        int i3 = 0;
        while (true) {
            i = i3;
            if (i < this.cesLength) {
                long ce2 = this.ces[i] & -49153;
                int strength = ceStrength(ce2);
                if (strength == 0) {
                    ce2 |= (3 & cases) << 14;
                    cases >>>= 2;
                } else if (strength == 2) {
                    ce2 |= 32768;
                }
                this.ces[i] = ce2;
                i3 = i + 1;
            } else {
                return;
            }
        }
    }

    void suppressContractions(UnicodeSet set) {
        this.dataBuilder.suppressContractions(set);
    }

    void optimize(UnicodeSet set) {
        this.optimizeSet.addAll(set);
    }

    private int addWithClosure(CharSequence nfdPrefix, CharSequence nfdString, long[] newCEs, int newCEsLength, int ce32) {
        ce32 = addOnlyClosure(nfdPrefix, nfdString, newCEs, newCEsLength, addIfDifferent(nfdPrefix, nfdString, newCEs, newCEsLength, ce32));
        addTailComposites(nfdPrefix, nfdString);
        return ce32;
    }

    private int addOnlyClosure(CharSequence nfdPrefix, CharSequence nfdString, long[] newCEs, int newCEsLength, int ce32) {
        CharSequence charSequence = nfdString;
        CanonicalIterator stringIter;
        CanonicalIterator stringIter2;
        String str;
        if (nfdPrefix.length() == 0) {
            stringIter = new CanonicalIterator(nfdString.toString());
            stringIter2 = ce32;
            String prefix = "";
            while (true) {
                str = stringIter.next();
                if (str == null) {
                    return stringIter2;
                }
                if (!ignoreString(str)) {
                    if (!str.contentEquals(charSequence)) {
                        stringIter2 = addIfDifferent(prefix, str, newCEs, newCEsLength, stringIter2);
                    }
                }
            }
        } else {
            stringIter = new CanonicalIterator(nfdPrefix.toString());
            CanonicalIterator stringIter3 = new CanonicalIterator(nfdString.toString());
            int ce322 = ce32;
            while (true) {
                stringIter2 = stringIter3;
                str = stringIter.next();
                if (str == null) {
                    return ce322;
                }
                if (ignorePrefix(str)) {
                    stringIter3 = stringIter2;
                } else {
                    boolean samePrefix = str.contentEquals(nfdPrefix);
                    int ce323 = ce322;
                    while (true) {
                        boolean samePrefix2 = samePrefix;
                        String str2 = stringIter2.next();
                        if (str2 == null) {
                            break;
                        }
                        if (!(ignoreString(str2) || (samePrefix2 && str2.contentEquals(charSequence)))) {
                            ce323 = addIfDifferent(str, str2, newCEs, newCEsLength, ce323);
                        }
                        samePrefix = samePrefix2;
                    }
                    stringIter2.reset();
                    stringIter3 = stringIter2;
                    ce322 = ce323;
                }
            }
        }
    }

    private void addTailComposites(CharSequence nfdPrefix, CharSequence nfdString) {
        int indexAfterLastStarter = nfdString.length();
        while (true) {
            int indexAfterLastStarter2 = indexAfterLastStarter;
            if (indexAfterLastStarter2 != 0) {
                CharSequence charSequence = nfdString;
                int lastStarter = Character.codePointBefore(charSequence, indexAfterLastStarter2);
                if (this.nfd.getCombiningClass(lastStarter) != 0) {
                    indexAfterLastStarter = indexAfterLastStarter2 - Character.charCount(lastStarter);
                } else if (!Hangul.isJamoL(lastStarter)) {
                    UnicodeSet composites = new UnicodeSet();
                    if (this.nfcImpl.getCanonStartSet(lastStarter, composites)) {
                        CharSequence newNFDString = new StringBuilder();
                        StringBuilder newString = new StringBuilder();
                        long[] newCEs = new long[31];
                        UnicodeSetIterator iter = new UnicodeSetIterator(composites);
                        while (true) {
                            UnicodeSetIterator iter2 = iter;
                            if (iter2.next()) {
                                int composite = iter2.codepoint;
                                if (mergeCompositeIntoString(charSequence, indexAfterLastStarter2, composite, this.nfd.getDecomposition(composite), newNFDString, newString)) {
                                    CharSequence charSequence2 = nfdPrefix;
                                    int cEs = this.dataBuilder.getCEs(charSequence2, newNFDString, newCEs, 0);
                                    if (cEs <= 31) {
                                        int newCEsLength = cEs;
                                        composite = addIfDifferent(charSequence2, newString, newCEs, cEs, -1);
                                        if (composite != -1) {
                                            addOnlyClosure(nfdPrefix, newNFDString, newCEs, newCEsLength, composite);
                                        }
                                    }
                                }
                                iter = iter2;
                            } else {
                                return;
                            }
                        }
                    }
                    return;
                } else {
                    return;
                }
            }
            return;
        }
    }

    private boolean mergeCompositeIntoString(CharSequence nfdString, int indexAfterLastStarter, int composite, CharSequence decomp, StringBuilder newNFDString, StringBuilder newString) {
        CharSequence charSequence = nfdString;
        int i = indexAfterLastStarter;
        CharSequence charSequence2 = decomp;
        StringBuilder stringBuilder = newNFDString;
        StringBuilder stringBuilder2 = newString;
        boolean sourceChar = true;
        int lastStarterLength = Character.offsetByCodePoints(charSequence2, 0, 1);
        if (lastStarterLength == decomp.length() || equalSubSequences(charSequence, i, charSequence2, lastStarterLength)) {
            return false;
        }
        stringBuilder.setLength(0);
        stringBuilder.append(charSequence, 0, i);
        stringBuilder2.setLength(0);
        stringBuilder2.append(charSequence, 0, i - lastStarterLength);
        stringBuilder2.appendCodePoint(composite);
        int sourceChar2 = -1;
        int sourceCC = 0;
        int decompIndex = lastStarterLength;
        int sourceIndex = i;
        int decompCC = 0;
        while (true) {
            if (sourceChar2 < 0) {
                if (sourceIndex >= nfdString.length()) {
                    break;
                }
                sourceChar2 = Character.codePointAt(charSequence, sourceIndex);
                sourceCC = this.nfd.getCombiningClass(sourceChar2);
            }
            if (decompIndex >= decomp.length()) {
                break;
            }
            int decompChar = Character.codePointAt(charSequence2, decompIndex);
            decompCC = this.nfd.getCombiningClass(decompChar);
            if (decompCC == 0 || sourceCC < decompCC) {
                return false;
            }
            if (decompCC < sourceCC) {
                stringBuilder.appendCodePoint(decompChar);
                decompIndex += Character.charCount(decompChar);
            } else if (decompChar != sourceChar2) {
                return false;
            } else {
                stringBuilder.appendCodePoint(decompChar);
                decompIndex += Character.charCount(decompChar);
                sourceIndex += Character.charCount(decompChar);
                sourceChar2 = -1;
            }
            sourceChar = true;
        }
        if (sourceChar2 >= 0) {
            if (sourceCC < decompCC) {
                return false;
            }
            stringBuilder.append(charSequence, sourceIndex, nfdString.length());
            stringBuilder2.append(charSequence, sourceIndex, nfdString.length());
        } else if (decompIndex < decomp.length()) {
            stringBuilder.append(charSequence2, decompIndex, decomp.length());
        }
        return sourceChar;
    }

    private boolean equalSubSequences(CharSequence left, int leftStart, CharSequence right, int rightStart) {
        int leftLength = left.length();
        if (leftLength - leftStart != right.length() - rightStart) {
            return false;
        }
        while (leftStart < leftLength) {
            int leftStart2 = leftStart + 1;
            int rightStart2 = rightStart + 1;
            if (left.charAt(leftStart) != right.charAt(rightStart)) {
                return false;
            }
            leftStart = leftStart2;
            rightStart = rightStart2;
        }
        return true;
    }

    private boolean ignorePrefix(CharSequence s) {
        return isFCD(s) ^ 1;
    }

    private boolean ignoreString(CharSequence s) {
        return !isFCD(s) || Hangul.isHangul(s.charAt(0));
    }

    private boolean isFCD(CharSequence s) {
        return this.fcd.isNormalized(s);
    }

    private void closeOverComposites() {
        String prefix = "";
        UnicodeSetIterator iter = new UnicodeSetIterator(COMPOSITES);
        while (true) {
            UnicodeSetIterator iter2 = iter;
            if (iter2.next()) {
                this.cesLength = this.dataBuilder.getCEs(this.nfd.getDecomposition(iter2.codepoint), this.ces, 0);
                if (this.cesLength <= 31) {
                    addIfDifferent(prefix, iter2.getString(), this.ces, this.cesLength, -1);
                }
                iter = iter2;
            } else {
                return;
            }
        }
    }

    private int addIfDifferent(CharSequence prefix, CharSequence str, long[] newCEs, int newCEsLength, int ce32) {
        long[] oldCEs = new long[31];
        if (!sameCEs(newCEs, newCEsLength, oldCEs, this.dataBuilder.getCEs(prefix, str, oldCEs, 0))) {
            if (ce32 == -1) {
                ce32 = this.dataBuilder.encodeCEs(newCEs, newCEsLength);
            }
            this.dataBuilder.addCE32(prefix, str, ce32);
        }
        return ce32;
    }

    private static boolean sameCEs(long[] ces1, int ces1Length, long[] ces2, int ces2Length) {
        if (ces1Length != ces2Length) {
            return false;
        }
        for (int i = 0; i < ces1Length; i++) {
            if (ces1[i] != ces2[i]) {
                return false;
            }
        }
        return true;
    }

    private static final int alignWeightRight(int w) {
        if (w != 0) {
            while ((w & 255) == 0) {
                w >>>= 8;
            }
        }
        return w;
    }

    private void makeTailoredCEs() {
        CollationBuilder pIndex = this;
        CollationWeights primaries = new CollationWeights();
        CollationWeights secondaries = new CollationWeights();
        CollationWeights tertiaries = new CollationWeights();
        long[] nodesArray = pIndex.nodes.getBuffer();
        int rpi = 0;
        while (true) {
            int rpi2 = rpi;
            if (rpi2 < pIndex.rootPrimaryIndexes.size()) {
                long node = nodesArray[pIndex.rootPrimaryIndexes.elementAti(rpi2)];
                long p = weight32FromNode(node);
                int s = p == 0 ? 0 : Collation.COMMON_WEIGHT16;
                int t = s;
                boolean pIsTailored = false;
                boolean sIsTailored = false;
                boolean tIsTailored = false;
                int pIndex2 = p == 0 ? 0 : pIndex.rootElements.findPrimary(p);
                int nextIndex = nextIndexFromNode(node);
                long p2 = p;
                int t2 = t;
                int q = 0;
                while (true) {
                    int nextIndex2 = nextIndex;
                    if (nextIndex2 == 0) {
                        break;
                    }
                    int pIndex3;
                    int nextIndex3;
                    long p3;
                    int i = nextIndex2;
                    int rpi3 = rpi2;
                    long node2 = nodesArray[i];
                    rpi = nextIndexFromNode(node2);
                    nextIndex = strengthFromNode(node2);
                    if (nextIndex != 3) {
                        CollationWeights primaries2;
                        int i2;
                        int pIndex4;
                        int tCount;
                        int strength;
                        if (nextIndex == 2) {
                            if (isTailoredNode(node2)) {
                                if (tIsTailored) {
                                    primaries2 = primaries;
                                    i2 = q;
                                    primaries = nextIndex;
                                    pIndex4 = pIndex2;
                                } else {
                                    int tLimit;
                                    tCount = 1 + countTailoredNodes(nodesArray, rpi, 2);
                                    if (t2 == 0) {
                                        tLimit = ((int) pIndex.rootElements.getFirstTertiaryCE()) & Collation.ONLY_TERTIARY_MASK;
                                        t2 = pIndex.rootElements.getTertiaryBoundary() - 256;
                                    } else if (!pIsTailored && !sIsTailored) {
                                        tLimit = pIndex.rootElements.getTertiaryAfter(pIndex2, s, t2);
                                    } else if (t2 == 256) {
                                        tLimit = Collation.COMMON_WEIGHT16;
                                    } else {
                                        tLimit = pIndex.rootElements.getTertiaryBoundary();
                                    }
                                    tertiaries.initForTertiary();
                                    primaries2 = primaries;
                                    i2 = q;
                                    int t3 = t2;
                                    primaries = nextIndex;
                                    pIndex4 = pIndex2;
                                    if (tertiaries.allocWeights((long) t2, (long) tLimit, tCount)) {
                                        tIsTailored = true;
                                        t2 = t3;
                                    } else {
                                        throw new UnsupportedOperationException("tertiary tailoring gap too small");
                                    }
                                }
                                nextIndex2 = (int) tertiaries.nextWeight();
                            } else {
                                primaries2 = primaries;
                                i2 = q;
                                primaries = nextIndex;
                                pIndex4 = pIndex2;
                                nextIndex2 = weight16FromNode(node2);
                                tIsTailored = false;
                            }
                            pIndex3 = pIndex4;
                            strength = primaries;
                            nextIndex3 = rpi;
                            primaries = primaries2;
                            pIndex = this;
                        } else {
                            primaries2 = primaries;
                            i2 = q;
                            int strength2 = nextIndex;
                            pIndex4 = pIndex2;
                            int pIndex5;
                            int i3;
                            CollationWeights collationWeights;
                            if (strength2 == 1) {
                                if (isTailoredNode(node2)) {
                                    if (sIsTailored) {
                                        pIndex5 = pIndex4;
                                        strength = strength2;
                                        i3 = t2;
                                        pIndex = this;
                                        t = Collation.COMMON_WEIGHT16;
                                    } else {
                                        CollationBuilder collationBuilder;
                                        tCount = 1 + countTailoredNodes(nodesArray, rpi, 1);
                                        if (s == 0) {
                                            collationBuilder = this;
                                            s = collationBuilder.rootElements.getSecondaryBoundary() - 256;
                                            nextIndex2 = (int) (collationBuilder.rootElements.getFirstSecondaryCE() >> 16);
                                        } else {
                                            collationBuilder = this;
                                            if (!pIsTailored) {
                                                nextIndex2 = collationBuilder.rootElements.getSecondaryAfter(pIndex4, s);
                                            } else if (s == 256) {
                                                nextIndex2 = Collation.COMMON_WEIGHT16;
                                            } else {
                                                nextIndex2 = collationBuilder.rootElements.getSecondaryBoundary();
                                            }
                                        }
                                        nextIndex = nextIndex2;
                                        if (s == Collation.COMMON_WEIGHT16) {
                                            s = collationBuilder.rootElements.getLastCommonSecondary();
                                        }
                                        secondaries.initForSecondary();
                                        long j = (long) s;
                                        pIndex5 = pIndex4;
                                        strength = strength2;
                                        long j2 = (long) nextIndex;
                                        collationWeights = secondaries;
                                        t = Collation.COMMON_WEIGHT16;
                                        long j3 = j2;
                                        pIndex = collationBuilder;
                                        if (collationWeights.allocWeights(j, j3, tCount)) {
                                            sIsTailored = true;
                                        } else {
                                            throw new UnsupportedOperationException("secondary tailoring gap too small");
                                        }
                                    }
                                    s = (int) secondaries.nextWeight();
                                    nextIndex3 = rpi;
                                } else {
                                    pIndex5 = pIndex4;
                                    strength = strength2;
                                    i3 = t2;
                                    pIndex = this;
                                    t = Collation.COMMON_WEIGHT16;
                                    s = weight16FromNode(node2);
                                    nextIndex3 = rpi;
                                    sIsTailored = false;
                                }
                                primaries = primaries2;
                                pIndex3 = pIndex5;
                            } else {
                                pIndex5 = pIndex4;
                                strength = strength2;
                                i3 = t2;
                                pIndex = this;
                                t = Collation.COMMON_WEIGHT16;
                                if (pIsTailored) {
                                    nextIndex3 = rpi;
                                    long j4 = p2;
                                    primaries = primaries2;
                                    pIndex3 = pIndex5;
                                } else {
                                    pIndex2 = countTailoredNodes(nodesArray, rpi, 0) + 1;
                                    p = p2;
                                    boolean isCompressible = pIndex.baseData.isCompressiblePrimary(p);
                                    nextIndex = pIndex5;
                                    p2 = pIndex.rootElements.getPrimaryAfter(p, nextIndex, isCompressible);
                                    collationWeights = primaries2;
                                    collationWeights.initForPrimary(isCompressible);
                                    nextIndex3 = rpi;
                                    primaries = collationWeights;
                                    pIndex3 = nextIndex;
                                    if (collationWeights.allocWeights(p, p2, pIndex2)) {
                                        pIsTailored = true;
                                    } else {
                                        throw new UnsupportedOperationException("primary tailoring gap too small");
                                    }
                                }
                                p2 = primaries.nextWeight();
                                s = Collation.COMMON_WEIGHT16;
                                sIsTailored = false;
                            }
                            nextIndex2 = s == 0 ? 0 : t;
                            tIsTailored = false;
                        }
                        q = 0;
                        t2 = nextIndex2;
                        p3 = p2;
                    } else if (q != 3) {
                        q++;
                        nextIndex3 = rpi;
                        pIndex3 = pIndex2;
                        p3 = p2;
                    } else {
                        throw new UnsupportedOperationException("quaternary tailoring gap too small");
                    }
                    if (isTailoredNode(node2)) {
                        nodesArray[i] = Collation.makeCE(p3, s, t2, q);
                    }
                    p2 = p3;
                    long j5 = node2;
                    p3 = i;
                    nextIndex = nextIndex3;
                    rpi2 = rpi3;
                    pIndex2 = pIndex3;
                }
            } else {
                return;
            }
            rpi = rpi2 + 1;
        }
    }

    private static int countTailoredNodes(long[] nodesArray, int i, int strength) {
        int count = 0;
        while (i != 0) {
            long node = nodesArray[i];
            if (strengthFromNode(node) < strength) {
                break;
            }
            if (strengthFromNode(node) == strength) {
                if (!isTailoredNode(node)) {
                    break;
                }
                count++;
            }
            i = nextIndexFromNode(node);
        }
        return count;
    }

    private void finalizeCEs() {
        CollationDataBuilder newBuilder = new CollationDataBuilder();
        newBuilder.initForTailoring(this.baseData);
        newBuilder.copyFrom(this.dataBuilder, new CEFinalizer(this.nodes.getBuffer()));
        this.dataBuilder = newBuilder;
    }

    private static long tempCEFromIndexAndStrength(int index, int strength) {
        return (((4629700417037541376L + (((long) (1040384 & index)) << 43)) + (((long) (index & 8128)) << 42)) + ((long) ((index & 63) << 24))) + ((long) (strength << 8));
    }

    private static int indexFromTempCE(long tempCE) {
        tempCE -= 4629700417037541376L;
        return ((((int) (tempCE >> 43)) & 1040384) | (((int) (tempCE >> 42)) & 8128)) | (((int) (tempCE >> 24)) & 63);
    }

    private static int strengthFromTempCE(long tempCE) {
        return (((int) tempCE) >> 8) & 3;
    }

    private static boolean isTempCE(long ce) {
        int sec = ((int) ce) >>> 24;
        return 6 <= sec && sec <= 69;
    }

    private static int indexFromTempCE32(int tempCE32) {
        tempCE32 -= 1077937696;
        return (((tempCE32 >> 11) & 1040384) | ((tempCE32 >> 10) & 8128)) | ((tempCE32 >> 8) & 63);
    }

    private static boolean isTempCE32(int ce32) {
        return (ce32 & 255) >= 2 && 6 <= ((ce32 >> 8) & 255) && ((ce32 >> 8) & 255) <= 69;
    }

    private static int ceStrength(long ce) {
        if (isTempCE(ce)) {
            return strengthFromTempCE(ce);
        }
        if ((-72057594037927936L & ce) != 0) {
            return 0;
        }
        if ((((int) ce) & -16777216) != 0) {
            return 1;
        }
        if (ce != 0) {
            return 2;
        }
        return 15;
    }

    private static long nodeFromWeight32(long weight32) {
        return weight32 << 32;
    }

    private static long nodeFromWeight16(int weight16) {
        return ((long) weight16) << 48;
    }

    private static long nodeFromPreviousIndex(int previous) {
        return ((long) previous) << 28;
    }

    private static long nodeFromNextIndex(int next) {
        return (long) (next << 8);
    }

    private static long nodeFromStrength(int strength) {
        return (long) strength;
    }

    private static long weight32FromNode(long node) {
        return node >>> 32;
    }

    private static int weight16FromNode(long node) {
        return ((int) (node >> 48)) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
    }

    private static int previousIndexFromNode(long node) {
        return ((int) (node >> 28)) & MAX_INDEX;
    }

    private static int nextIndexFromNode(long node) {
        return (((int) node) >> 8) & MAX_INDEX;
    }

    private static int strengthFromNode(long node) {
        return ((int) node) & 3;
    }

    private static boolean nodeHasBefore2(long node) {
        return (64 & node) != 0;
    }

    private static boolean nodeHasBefore3(long node) {
        return (32 & node) != 0;
    }

    private static boolean nodeHasAnyBefore(long node) {
        return (96 & node) != 0;
    }

    private static boolean isTailoredNode(long node) {
        return (8 & node) != 0;
    }

    private static long changeNodePreviousIndex(long node, int previous) {
        return (-281474708275201L & node) | nodeFromPreviousIndex(previous);
    }

    private static long changeNodeNextIndex(long node, int next) {
        return (-268435201 & node) | nodeFromNextIndex(next);
    }
}
