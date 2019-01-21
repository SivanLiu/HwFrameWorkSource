package android.icu.text;

import android.icu.impl.CharacterIteration;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICUDebug;
import android.icu.impl.Trie2;
import android.icu.impl.number.Padder;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RuleBasedBreakIterator extends BreakIterator {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String RBBI_DEBUG_ARG = "rbbi";
    private static final int RBBI_END = 2;
    private static final int RBBI_RUN = 1;
    private static final int RBBI_START = 0;
    private static final int START_STATE = 1;
    private static final int STOP_STATE = 0;
    private static final boolean TRACE;
    static final String fDebugEnv = (ICUDebug.enabled(RBBI_DEBUG_ARG) ? ICUDebug.value(RBBI_DEBUG_ARG) : null);
    private static final List<LanguageBreakEngine> gAllBreakEngines = new ArrayList();
    private static final UnhandledBreakEngine gUnhandledBreakEngine = new UnhandledBreakEngine();
    private static final int kMaxLookaheads = 8;
    private BreakCache fBreakCache;
    private List<LanguageBreakEngine> fBreakEngines;
    private int fBreakType;
    private DictionaryCache fDictionaryCache;
    private int fDictionaryCharCount;
    private boolean fDone;
    private LookAheadResults fLookAheadMatches;
    private int fPosition;
    RBBIDataWrapper fRData;
    private int fRuleStatusIndex;
    private CharacterIterator fText;

    class BreakCache {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        static final int CACHE_SIZE = 128;
        static final boolean RetainCachePosition = false;
        static final boolean UpdateCachePosition = true;
        int[] fBoundaries;
        int fBufIdx;
        int fEndBufIdx;
        DequeI fSideBuffer;
        int fStartBufIdx;
        short[] fStatuses;
        int fTextIdx;

        static {
            Class cls = RuleBasedBreakIterator.class;
        }

        BreakCache() {
            this.fBoundaries = new int[128];
            this.fStatuses = new short[128];
            this.fSideBuffer = new DequeI();
            reset();
        }

        void reset(int pos, int ruleStatus) {
            this.fStartBufIdx = 0;
            this.fEndBufIdx = 0;
            this.fTextIdx = pos;
            this.fBufIdx = 0;
            this.fBoundaries[0] = pos;
            this.fStatuses[0] = (short) ruleStatus;
        }

        void reset() {
            reset(0, 0);
        }

        void next() {
            if (this.fBufIdx == this.fEndBufIdx) {
                RuleBasedBreakIterator.this.fDone = populateFollowing() ^ 1;
                RuleBasedBreakIterator.this.fPosition = this.fTextIdx;
                RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
                return;
            }
            this.fBufIdx = modChunkSize(this.fBufIdx + 1);
            this.fTextIdx = RuleBasedBreakIterator.this.fPosition = this.fBoundaries[this.fBufIdx];
            RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
        }

        void previous() {
            int initialBufIdx = this.fBufIdx;
            boolean z = true;
            if (this.fBufIdx == this.fStartBufIdx) {
                populatePreceding();
            } else {
                this.fBufIdx = modChunkSize(this.fBufIdx - 1);
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
            }
            RuleBasedBreakIterator ruleBasedBreakIterator = RuleBasedBreakIterator.this;
            if (this.fBufIdx != initialBufIdx) {
                z = false;
            }
            ruleBasedBreakIterator.fDone = z;
            RuleBasedBreakIterator.this.fPosition = this.fTextIdx;
            RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
        }

        void following(int startPos) {
            if (startPos == this.fTextIdx || seek(startPos) || populateNear(startPos)) {
                RuleBasedBreakIterator.this.fDone = false;
                next();
            }
        }

        void preceding(int startPos) {
            if (startPos != this.fTextIdx && !seek(startPos) && !populateNear(startPos)) {
                return;
            }
            if (startPos == this.fTextIdx) {
                previous();
            } else {
                current();
            }
        }

        int current() {
            RuleBasedBreakIterator.this.fPosition = this.fTextIdx;
            RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
            RuleBasedBreakIterator.this.fDone = false;
            return this.fTextIdx;
        }

        boolean populateNear(int position) {
            if (position < this.fBoundaries[this.fStartBufIdx] - 15 || position > this.fBoundaries[this.fEndBufIdx] + 15) {
                int aBoundary = RuleBasedBreakIterator.this.fText.getBeginIndex();
                int ruleStatusIndex = 0;
                if (position > aBoundary + 20) {
                    RuleBasedBreakIterator.this.fPosition = RuleBasedBreakIterator.this.handlePrevious(position);
                    aBoundary = RuleBasedBreakIterator.this.handleNext();
                    ruleStatusIndex = RuleBasedBreakIterator.this.fRuleStatusIndex;
                }
                reset(aBoundary, ruleStatusIndex);
            }
            if (this.fBoundaries[this.fEndBufIdx] < position) {
                while (this.fBoundaries[this.fEndBufIdx] < position) {
                    if (!populateFollowing()) {
                        return false;
                    }
                }
                this.fBufIdx = this.fEndBufIdx;
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                while (this.fTextIdx > position) {
                    previous();
                }
                return true;
            } else if (this.fBoundaries[this.fStartBufIdx] <= position) {
                return true;
            } else {
                while (this.fBoundaries[this.fStartBufIdx] > position) {
                    populatePreceding();
                }
                this.fBufIdx = this.fStartBufIdx;
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                while (this.fTextIdx < position) {
                    next();
                }
                if (this.fTextIdx > position) {
                    previous();
                }
                return true;
            }
        }

        boolean populateFollowing() {
            int fromPosition = this.fBoundaries[this.fEndBufIdx];
            int fromRuleStatusIdx = this.fStatuses[this.fEndBufIdx];
            if (RuleBasedBreakIterator.this.fDictionaryCache.following(fromPosition)) {
                addFollowing(RuleBasedBreakIterator.this.fDictionaryCache.fBoundary, RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex, true);
                return true;
            }
            RuleBasedBreakIterator.this.fPosition = fromPosition;
            int pos = RuleBasedBreakIterator.this.handleNext();
            if (pos == -1) {
                return false;
            }
            int ruleStatusIdx = RuleBasedBreakIterator.this.fRuleStatusIndex;
            if (RuleBasedBreakIterator.this.fDictionaryCharCount > 0) {
                RuleBasedBreakIterator.this.fDictionaryCache.populateDictionary(fromPosition, pos, fromRuleStatusIdx, ruleStatusIdx);
                if (RuleBasedBreakIterator.this.fDictionaryCache.following(fromPosition)) {
                    addFollowing(RuleBasedBreakIterator.this.fDictionaryCache.fBoundary, RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex, true);
                    return true;
                }
            }
            addFollowing(pos, ruleStatusIdx, true);
            for (pos = 0; pos < 6; pos++) {
                int pos2 = RuleBasedBreakIterator.this.handleNext();
                if (pos2 == -1 || RuleBasedBreakIterator.this.fDictionaryCharCount > 0) {
                    break;
                }
                addFollowing(pos2, RuleBasedBreakIterator.this.fRuleStatusIndex, false);
            }
            return true;
        }

        boolean populatePreceding() {
            int textBegin = RuleBasedBreakIterator.this.fText.getBeginIndex();
            int fromPosition = this.fBoundaries[this.fStartBufIdx];
            if (fromPosition == textBegin) {
                return false;
            }
            int position = textBegin;
            if (RuleBasedBreakIterator.this.fDictionaryCache.preceding(fromPosition)) {
                addPreceding(RuleBasedBreakIterator.this.fDictionaryCache.fBoundary, RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex, true);
                return true;
            }
            int position2;
            int positionStatusIdx = 0;
            position = fromPosition;
            do {
                position -= 30;
                if (position <= textBegin) {
                    position = textBegin;
                } else {
                    position = RuleBasedBreakIterator.this.handlePrevious(position);
                }
                if (position == -1 || position == textBegin) {
                    position2 = textBegin;
                    positionStatusIdx = 0;
                    continue;
                } else {
                    RuleBasedBreakIterator.this.fPosition = position;
                    position2 = RuleBasedBreakIterator.this.handleNext();
                    positionStatusIdx = RuleBasedBreakIterator.this.fRuleStatusIndex;
                    continue;
                }
            } while (position2 >= fromPosition);
            this.fSideBuffer.removeAllElements();
            this.fSideBuffer.push(position2);
            this.fSideBuffer.push(positionStatusIdx);
            do {
                int prevPosition = RuleBasedBreakIterator.this.fPosition = position2;
                int prevStatusIdx = positionStatusIdx;
                position2 = RuleBasedBreakIterator.this.handleNext();
                positionStatusIdx = RuleBasedBreakIterator.this.fRuleStatusIndex;
                if (position2 == -1) {
                    break;
                }
                boolean segmentHandledByDictionary = false;
                if (RuleBasedBreakIterator.this.fDictionaryCharCount != 0) {
                    RuleBasedBreakIterator.this.fDictionaryCache.populateDictionary(prevPosition, position2, prevStatusIdx, positionStatusIdx);
                    while (RuleBasedBreakIterator.this.fDictionaryCache.following(prevPosition)) {
                        position2 = RuleBasedBreakIterator.this.fDictionaryCache.fBoundary;
                        positionStatusIdx = RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex;
                        segmentHandledByDictionary = true;
                        if (position2 >= fromPosition) {
                            break;
                        }
                        this.fSideBuffer.push(position2);
                        this.fSideBuffer.push(positionStatusIdx);
                        prevPosition = position2;
                    }
                }
                if (!segmentHandledByDictionary && position2 < fromPosition) {
                    this.fSideBuffer.push(position2);
                    this.fSideBuffer.push(positionStatusIdx);
                    continue;
                }
            } while (position2 < fromPosition);
            boolean success = false;
            if (!this.fSideBuffer.isEmpty()) {
                addPreceding(this.fSideBuffer.pop(), this.fSideBuffer.pop(), true);
                success = true;
            }
            while (!this.fSideBuffer.isEmpty()) {
                if (!addPreceding(this.fSideBuffer.pop(), this.fSideBuffer.pop(), false)) {
                    break;
                }
            }
            return success;
        }

        void addFollowing(int position, int ruleStatusIdx, boolean update) {
            int nextIdx = modChunkSize(this.fEndBufIdx + 1);
            if (nextIdx == this.fStartBufIdx) {
                this.fStartBufIdx = modChunkSize(this.fStartBufIdx + 6);
            }
            this.fBoundaries[nextIdx] = position;
            this.fStatuses[nextIdx] = (short) ruleStatusIdx;
            this.fEndBufIdx = nextIdx;
            if (update) {
                this.fBufIdx = nextIdx;
                this.fTextIdx = position;
            }
        }

        boolean addPreceding(int position, int ruleStatusIdx, boolean update) {
            int nextIdx = modChunkSize(this.fStartBufIdx - 1);
            if (nextIdx == this.fEndBufIdx) {
                if (this.fBufIdx == this.fEndBufIdx && !update) {
                    return false;
                }
                this.fEndBufIdx = modChunkSize(this.fEndBufIdx - 1);
            }
            this.fBoundaries[nextIdx] = position;
            this.fStatuses[nextIdx] = (short) ruleStatusIdx;
            this.fStartBufIdx = nextIdx;
            if (update) {
                this.fBufIdx = nextIdx;
                this.fTextIdx = position;
            }
            return true;
        }

        boolean seek(int pos) {
            if (pos < this.fBoundaries[this.fStartBufIdx] || pos > this.fBoundaries[this.fEndBufIdx]) {
                return false;
            }
            if (pos == this.fBoundaries[this.fStartBufIdx]) {
                this.fBufIdx = this.fStartBufIdx;
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                return true;
            } else if (pos == this.fBoundaries[this.fEndBufIdx]) {
                this.fBufIdx = this.fEndBufIdx;
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                return true;
            } else {
                int min = this.fStartBufIdx;
                int max = this.fEndBufIdx;
                while (min != max) {
                    int probe = modChunkSize(((min + max) + (min > max ? 128 : 0)) / 2);
                    if (this.fBoundaries[probe] > pos) {
                        max = probe;
                    } else {
                        min = modChunkSize(probe + 1);
                    }
                }
                this.fBufIdx = modChunkSize(max - 1);
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                return true;
            }
        }

        BreakCache(BreakCache src) {
            this.fBoundaries = new int[128];
            this.fStatuses = new short[128];
            this.fSideBuffer = new DequeI();
            this.fStartBufIdx = src.fStartBufIdx;
            this.fEndBufIdx = src.fEndBufIdx;
            this.fTextIdx = src.fTextIdx;
            this.fBufIdx = src.fBufIdx;
            this.fBoundaries = (int[]) src.fBoundaries.clone();
            this.fStatuses = (short[]) src.fStatuses.clone();
            this.fSideBuffer = new DequeI();
        }

        void dumpCache() {
            System.out.printf("fTextIdx:%d   fBufIdx:%d%n", new Object[]{Integer.valueOf(this.fTextIdx), Integer.valueOf(this.fBufIdx)});
            int i = this.fStartBufIdx;
            while (true) {
                System.out.printf("%d  %d%n", new Object[]{Integer.valueOf(i), Integer.valueOf(this.fBoundaries[i])});
                if (i != this.fEndBufIdx) {
                    i = modChunkSize(i + 1);
                } else {
                    return;
                }
            }
        }

        private final int modChunkSize(int index) {
            return index & 127;
        }
    }

    class DictionaryCache {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        int fBoundary;
        DequeI fBreaks;
        int fFirstRuleStatusIndex;
        int fLimit;
        int fOtherRuleStatusIndex;
        int fPositionInCache;
        int fStart;
        int fStatusIndex;

        static {
            Class cls = RuleBasedBreakIterator.class;
        }

        void reset() {
            this.fPositionInCache = -1;
            this.fStart = 0;
            this.fLimit = 0;
            this.fFirstRuleStatusIndex = 0;
            this.fOtherRuleStatusIndex = 0;
            this.fBreaks.removeAllElements();
        }

        boolean following(int fromPos) {
            if (fromPos >= this.fLimit || fromPos < this.fStart) {
                this.fPositionInCache = -1;
                return false;
            } else if (this.fPositionInCache < 0 || this.fPositionInCache >= this.fBreaks.size() || this.fBreaks.elementAt(this.fPositionInCache) != fromPos) {
                this.fPositionInCache = 0;
                while (this.fPositionInCache < this.fBreaks.size()) {
                    int r = this.fBreaks.elementAt(this.fPositionInCache);
                    if (r > fromPos) {
                        this.fBoundary = r;
                        this.fStatusIndex = this.fOtherRuleStatusIndex;
                        return true;
                    }
                    this.fPositionInCache++;
                }
                this.fPositionInCache = -1;
                return false;
            } else {
                this.fPositionInCache++;
                if (this.fPositionInCache >= this.fBreaks.size()) {
                    this.fPositionInCache = -1;
                    return false;
                }
                this.fBoundary = this.fBreaks.elementAt(this.fPositionInCache);
                this.fStatusIndex = this.fOtherRuleStatusIndex;
                return true;
            }
        }

        boolean preceding(int fromPos) {
            if (fromPos <= this.fStart || fromPos > this.fLimit) {
                this.fPositionInCache = -1;
                return false;
            }
            if (fromPos == this.fLimit) {
                this.fPositionInCache = this.fBreaks.size() - 1;
                if (this.fPositionInCache >= 0) {
                }
            }
            int r;
            if (this.fPositionInCache > 0 && this.fPositionInCache < this.fBreaks.size() && this.fBreaks.elementAt(this.fPositionInCache) == fromPos) {
                this.fPositionInCache--;
                r = this.fBreaks.elementAt(this.fPositionInCache);
                this.fBoundary = r;
                this.fStatusIndex = r == this.fStart ? this.fFirstRuleStatusIndex : this.fOtherRuleStatusIndex;
                return true;
            } else if (this.fPositionInCache == 0) {
                this.fPositionInCache = -1;
                return false;
            } else {
                this.fPositionInCache = this.fBreaks.size() - 1;
                while (this.fPositionInCache >= 0) {
                    r = this.fBreaks.elementAt(this.fPositionInCache);
                    if (r < fromPos) {
                        this.fBoundary = r;
                        this.fStatusIndex = r == this.fStart ? this.fFirstRuleStatusIndex : this.fOtherRuleStatusIndex;
                        return true;
                    }
                    this.fPositionInCache--;
                }
                this.fPositionInCache = -1;
                return false;
            }
        }

        void populateDictionary(int startPos, int endPos, int firstRuleStatus, int otherRuleStatus) {
            int i = startPos;
            int i2 = endPos;
            if (i2 - i > 1) {
                reset();
                this.fFirstRuleStatusIndex = firstRuleStatus;
                this.fOtherRuleStatusIndex = otherRuleStatus;
                int rangeStart = i;
                int rangeEnd = i2;
                RuleBasedBreakIterator.this.fText.setIndex(rangeStart);
                int c = CharacterIteration.current32(RuleBasedBreakIterator.this.fText);
                int category = (short) RuleBasedBreakIterator.this.fRData.fTrie.get(c);
                int foundBreakCount = 0;
                int c2 = c;
                while (true) {
                    int category2 = category;
                    int index = RuleBasedBreakIterator.this.fText.getIndex();
                    int current = index;
                    if (index < rangeEnd && (category2 & 16384) == 0) {
                        c2 = CharacterIteration.next32(RuleBasedBreakIterator.this.fText);
                        category = (short) RuleBasedBreakIterator.this.fRData.fTrie.get(c2);
                    } else if (current >= rangeEnd) {
                        break;
                    } else {
                        LanguageBreakEngine lbe = RuleBasedBreakIterator.this.getLanguageBreakEngine(c2);
                        if (lbe != null) {
                            foundBreakCount += lbe.findBreaks(RuleBasedBreakIterator.this.fText, rangeStart, rangeEnd, RuleBasedBreakIterator.this.fBreakType, this.fBreaks);
                        }
                        c2 = CharacterIteration.current32(RuleBasedBreakIterator.this.fText);
                        category = (short) RuleBasedBreakIterator.this.fRData.fTrie.get(c2);
                    }
                }
                if (foundBreakCount > 0) {
                    if (i < this.fBreaks.elementAt(0)) {
                        this.fBreaks.offer(i);
                    }
                    if (i2 > this.fBreaks.peek()) {
                        this.fBreaks.push(i2);
                    }
                    this.fPositionInCache = 0;
                    this.fStart = this.fBreaks.elementAt(0);
                    this.fLimit = this.fBreaks.peek();
                }
            }
        }

        DictionaryCache() {
            this.fPositionInCache = -1;
            this.fBreaks = new DequeI();
        }

        DictionaryCache(DictionaryCache src) {
            try {
                this.fBreaks = (DequeI) src.fBreaks.clone();
                this.fPositionInCache = src.fPositionInCache;
                this.fStart = src.fStart;
                this.fLimit = src.fLimit;
                this.fFirstRuleStatusIndex = src.fFirstRuleStatusIndex;
                this.fOtherRuleStatusIndex = src.fOtherRuleStatusIndex;
                this.fBoundary = src.fBoundary;
                this.fStatusIndex = src.fStatusIndex;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class LookAheadResults {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        int[] fKeys = new int[8];
        int[] fPositions = new int[8];
        int fUsedSlotLimit = 0;

        static {
            Class cls = RuleBasedBreakIterator.class;
        }

        LookAheadResults() {
        }

        int getPosition(int key) {
            for (int i = 0; i < this.fUsedSlotLimit; i++) {
                if (this.fKeys[i] == key) {
                    return this.fPositions[i];
                }
            }
            return -1;
        }

        void setPosition(int key, int position) {
            int i = 0;
            while (i < this.fUsedSlotLimit) {
                if (this.fKeys[i] == key) {
                    this.fPositions[i] = position;
                    return;
                }
                i++;
            }
            if (i >= 8) {
                i = 7;
            }
            this.fKeys[i] = key;
            this.fPositions[i] = position;
            this.fUsedSlotLimit = i + 1;
        }

        void reset() {
            this.fUsedSlotLimit = 0;
        }
    }

    static {
        boolean z = ICUDebug.enabled(RBBI_DEBUG_ARG) && ICUDebug.value(RBBI_DEBUG_ARG).indexOf("trace") >= 0;
        TRACE = z;
        gAllBreakEngines.add(gUnhandledBreakEngine);
    }

    private RuleBasedBreakIterator() {
        this.fText = new StringCharacterIterator("");
        this.fBreakCache = new BreakCache();
        this.fDictionaryCache = new DictionaryCache();
        this.fBreakType = 1;
        this.fLookAheadMatches = new LookAheadResults();
        this.fDictionaryCharCount = 0;
        synchronized (gAllBreakEngines) {
            this.fBreakEngines = new ArrayList(gAllBreakEngines);
        }
    }

    public static RuleBasedBreakIterator getInstanceFromCompiledRules(InputStream is) throws IOException {
        RuleBasedBreakIterator This = new RuleBasedBreakIterator();
        This.fRData = RBBIDataWrapper.get(ICUBinary.getByteBufferFromInputStreamAndCloseStream(is));
        return This;
    }

    @Deprecated
    public static RuleBasedBreakIterator getInstanceFromCompiledRules(ByteBuffer bytes) throws IOException {
        RuleBasedBreakIterator This = new RuleBasedBreakIterator();
        This.fRData = RBBIDataWrapper.get(bytes);
        return This;
    }

    public RuleBasedBreakIterator(String rules) {
        this();
        try {
            ByteArrayOutputStream ruleOS = new ByteArrayOutputStream();
            compileRules(rules, ruleOS);
            this.fRData = RBBIDataWrapper.get(ByteBuffer.wrap(ruleOS.toByteArray()));
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RuleBasedBreakIterator rule compilation internal error: ");
            stringBuilder.append(e.getMessage());
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public Object clone() {
        RuleBasedBreakIterator result = (RuleBasedBreakIterator) super.clone();
        if (this.fText != null) {
            result.fText = (CharacterIterator) this.fText.clone();
        }
        synchronized (gAllBreakEngines) {
            result.fBreakEngines = new ArrayList(gAllBreakEngines);
        }
        result.fLookAheadMatches = new LookAheadResults();
        Objects.requireNonNull(result);
        result.fBreakCache = new BreakCache(this.fBreakCache);
        Objects.requireNonNull(result);
        result.fDictionaryCache = new DictionaryCache(this.fDictionaryCache);
        return result;
    }

    public boolean equals(Object that) {
        boolean z = false;
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        try {
            RuleBasedBreakIterator other = (RuleBasedBreakIterator) that;
            if (this.fRData != other.fRData && (this.fRData == null || other.fRData == null)) {
                return false;
            }
            if (this.fRData != null && other.fRData != null && !this.fRData.fRuleSource.equals(other.fRData.fRuleSource)) {
                return false;
            }
            if (this.fText == null && other.fText == null) {
                return true;
            }
            if (!(this.fText == null || other.fText == null)) {
                if (this.fText.equals(other.fText)) {
                    if (this.fPosition == other.fPosition) {
                        z = true;
                    }
                    return z;
                }
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        String retStr = "";
        if (this.fRData != null) {
            return this.fRData.fRuleSource;
        }
        return retStr;
    }

    public int hashCode() {
        return this.fRData.fRuleSource.hashCode();
    }

    @Deprecated
    public void dump(PrintStream out) {
        if (out == null) {
            out = System.out;
        }
        this.fRData.dump(out);
    }

    public static void compileRules(String rules, OutputStream ruleBinary) throws IOException {
        RBBIRuleBuilder.compileRules(rules, ruleBinary);
    }

    public int first() {
        if (this.fText == null) {
            return -1;
        }
        this.fText.first();
        int start = this.fText.getIndex();
        if (!this.fBreakCache.seek(start)) {
            this.fBreakCache.populateNear(start);
        }
        this.fBreakCache.current();
        return this.fPosition;
    }

    public int last() {
        if (this.fText == null) {
            return -1;
        }
        int endPos = this.fText.getEndIndex();
        boolean endShouldBeBoundary = isBoundary(endPos);
        if (this.fPosition != endPos) {
        }
        return endPos;
    }

    public int next(int n) {
        int result = 0;
        if (n > 0) {
            while (n > 0 && result != -1) {
                result = next();
                n--;
            }
            return result;
        } else if (n >= 0) {
            return current();
        } else {
            while (n < 0 && result != -1) {
                result = previous();
                n++;
            }
            return result;
        }
    }

    public int next() {
        this.fBreakCache.next();
        return this.fDone ? -1 : this.fPosition;
    }

    public int previous() {
        this.fBreakCache.previous();
        return this.fDone ? -1 : this.fPosition;
    }

    public int following(int startPos) {
        if (startPos < this.fText.getBeginIndex()) {
            return first();
        }
        this.fBreakCache.following(CISetIndex32(this.fText, startPos));
        return this.fDone ? -1 : this.fPosition;
    }

    public int preceding(int offset) {
        if (this.fText == null || offset > this.fText.getEndIndex()) {
            return last();
        }
        if (offset < this.fText.getBeginIndex()) {
            return first();
        }
        this.fBreakCache.preceding(offset);
        return this.fDone ? -1 : this.fPosition;
    }

    protected static final void checkOffset(int offset, CharacterIterator text) {
        if (offset < text.getBeginIndex() || offset > text.getEndIndex()) {
            throw new IllegalArgumentException("offset out of bounds");
        }
    }

    public boolean isBoundary(int offset) {
        checkOffset(offset, this.fText);
        int adjustedOffset = CISetIndex32(this.fText, offset);
        boolean result = false;
        if (this.fBreakCache.seek(adjustedOffset) || this.fBreakCache.populateNear(adjustedOffset)) {
            result = this.fBreakCache.current() == offset;
        }
        if (!result) {
            next();
        }
        return result;
    }

    public int current() {
        return this.fText != null ? this.fPosition : -1;
    }

    public int getRuleStatus() {
        return this.fRData.fStatusTable[this.fRuleStatusIndex + this.fRData.fStatusTable[this.fRuleStatusIndex]];
    }

    public int getRuleStatusVec(int[] fillInArray) {
        int numStatusVals = this.fRData.fStatusTable[this.fRuleStatusIndex];
        if (fillInArray != null) {
            int numToCopy = Math.min(numStatusVals, fillInArray.length);
            for (int i = 0; i < numToCopy; i++) {
                fillInArray[i] = this.fRData.fStatusTable[(this.fRuleStatusIndex + i) + 1];
            }
        }
        return numStatusVals;
    }

    public CharacterIterator getText() {
        return this.fText;
    }

    public void setText(CharacterIterator newText) {
        if (newText != null) {
            this.fBreakCache.reset(newText.getBeginIndex(), 0);
        } else {
            this.fBreakCache.reset();
        }
        this.fDictionaryCache.reset();
        this.fText = newText;
        first();
    }

    void setBreakType(int type) {
        this.fBreakType = type;
    }

    int getBreakType() {
        return this.fBreakType;
    }

    /* JADX WARNING: Missing block: B:51:0x00bf, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private LanguageBreakEngine getLanguageBreakEngine(int c) {
        for (LanguageBreakEngine candidate : this.fBreakEngines) {
            if (candidate.handles(c, this.fBreakType)) {
                return candidate;
            }
        }
        synchronized (gAllBreakEngines) {
            LanguageBreakEngine candidate2;
            for (LanguageBreakEngine candidate22 : gAllBreakEngines) {
                if (candidate22.handles(c, this.fBreakType)) {
                    this.fBreakEngines.add(candidate22);
                    return candidate22;
                }
            }
            int script = UCharacter.getIntPropertyValue(c, UProperty.SCRIPT);
            if (script == 22 || script == 20) {
                script = 17;
            }
            switch (script) {
                case 17:
                    if (getBreakType() != 1) {
                        gUnhandledBreakEngine.handleChar(c, getBreakType());
                        candidate22 = gUnhandledBreakEngine;
                        break;
                    }
                    candidate22 = new CjkBreakEngine(false);
                case 18:
                    if (getBreakType() != 1) {
                        gUnhandledBreakEngine.handleChar(c, getBreakType());
                        candidate22 = gUnhandledBreakEngine;
                        break;
                    }
                    candidate22 = new CjkBreakEngine(true);
                case 23:
                    candidate22 = new KhmerBreakEngine();
                    break;
                case 24:
                    candidate22 = new LaoBreakEngine();
                    break;
                case 28:
                    candidate22 = new BurmeseBreakEngine();
                    break;
                case 38:
                    candidate22 = new ThaiBreakEngine();
                    break;
                default:
                    try {
                        gUnhandledBreakEngine.handleChar(c, getBreakType());
                        candidate22 = gUnhandledBreakEngine;
                        break;
                    } catch (IOException e) {
                        candidate22 = null;
                        break;
                    }
            }
            if (candidate22 != null) {
                if (candidate22 != gUnhandledBreakEngine) {
                    gAllBreakEngines.add(candidate22);
                    this.fBreakEngines.add(candidate22);
                }
            }
        }
    }

    private int handleNext() {
        PrintStream printStream;
        StringBuilder stringBuilder;
        if (TRACE) {
            System.out.println("Handle Next   pos      char  state category");
        }
        this.fRuleStatusIndex = 0;
        this.fDictionaryCharCount = 0;
        CharacterIterator text = this.fText;
        Trie2 trie = this.fRData.fTrie;
        short[] stateTable = this.fRData.fFTable;
        int initialPosition = this.fPosition;
        text.setIndex(initialPosition);
        int result = initialPosition;
        int c = text.current();
        if (c >= 55296) {
            c = CharacterIteration.nextTrail32(text, c);
            if (c == Integer.MAX_VALUE) {
                this.fDone = true;
                return -1;
            }
        }
        int state = 1;
        int row = this.fRData.getRowIndex(1);
        short category = (short) 3;
        int mode = 1;
        int i = 5;
        if ((this.fRData.getStateTableFlags(stateTable) & 2) != 0) {
            category = (short) 2;
            mode = 0;
            if (TRACE) {
                printStream = System.out;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("            ");
                stringBuilder2.append(RBBIDataWrapper.intToString(text.getIndex(), 5));
                printStream.print(stringBuilder2.toString());
                System.out.print(RBBIDataWrapper.intToHexString(c, 10));
                PrintStream printStream2 = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append(RBBIDataWrapper.intToString(1, 7));
                stringBuilder.append(RBBIDataWrapper.intToString(2, 6));
                printStream2.println(stringBuilder.toString());
            }
        }
        this.fLookAheadMatches.reset();
        int mode2 = mode;
        while (state != 0) {
            if (c == Integer.MAX_VALUE) {
                if (mode2 == 2) {
                    break;
                }
                mode2 = 2;
                category = (short) 1;
            } else if (mode2 == 1) {
                short category2 = (short) trie.get(c);
                if ((category2 & 16384) != 0) {
                    this.fDictionaryCharCount++;
                    category2 = (short) (category2 & -16385);
                }
                if (TRACE) {
                    PrintStream printStream3 = System.out;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("            ");
                    stringBuilder.append(RBBIDataWrapper.intToString(text.getIndex(), i));
                    printStream3.print(stringBuilder.toString());
                    System.out.print(RBBIDataWrapper.intToHexString(c, 10));
                    printStream = System.out;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(RBBIDataWrapper.intToString(state, 7));
                    stringBuilder3.append(RBBIDataWrapper.intToString(category2, 6));
                    printStream.println(stringBuilder3.toString());
                }
                c = text.next();
                if (c >= 55296) {
                    c = CharacterIteration.nextTrail32(text, c);
                }
                category = category2;
            } else {
                mode2 = 1;
            }
            state = stateTable[(row + 4) + category];
            row = this.fRData.getRowIndex(state);
            if (stateTable[row + 0] == (short) -1) {
                result = text.getIndex();
                if (c >= 65536 && c <= 1114111) {
                    result--;
                }
                this.fRuleStatusIndex = stateTable[row + 2];
            }
            int completedRule = stateTable[row + 0];
            if (completedRule > 0) {
                i = this.fLookAheadMatches.getPosition(completedRule);
                if (i >= 0) {
                    this.fRuleStatusIndex = stateTable[row + 2];
                    this.fPosition = i;
                    return i;
                }
            }
            i = stateTable[row + 1];
            if (i != 0) {
                mode = text.getIndex();
                if (c >= 65536 && c <= 1114111) {
                    mode--;
                }
                this.fLookAheadMatches.setPosition(i, mode);
            }
            i = 5;
        }
        if (result == initialPosition) {
            if (TRACE) {
                System.out.println("Iterator did not move. Advancing by 1.");
            }
            text.setIndex(initialPosition);
            CharacterIteration.next32(text);
            result = text.getIndex();
            this.fRuleStatusIndex = 0;
        }
        this.fPosition = result;
        if (TRACE) {
            printStream = System.out;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("result = ");
            stringBuilder4.append(result);
            printStream.println(stringBuilder4.toString());
        }
        return result;
    }

    private int handlePrevious(int fromPosition) {
        int i = fromPosition;
        if (this.fText == null) {
            return 0;
        }
        int initialPosition = i;
        this.fLookAheadMatches.reset();
        short[] stateTable = this.fRData.fSRTable;
        CISetIndex32(this.fText, i);
        short mode = (short) -1;
        if (i == this.fText.getBeginIndex()) {
            return -1;
        }
        int result = initialPosition;
        int c = CharacterIteration.previous32(this.fText);
        int state = 1;
        int row = this.fRData.getRowIndex(1);
        int category = 3;
        int mode2 = 1;
        if ((this.fRData.getStateTableFlags(stateTable) & 2) != 0) {
            category = 2;
            mode2 = 0;
        }
        if (TRACE) {
            System.out.println("Handle Prev   pos   char  state category ");
        }
        while (true) {
            int lookaheadResult;
            if (c == Integer.MAX_VALUE) {
                if (mode2 == 2) {
                    break;
                }
                mode2 = 2;
                category = 1;
            }
            if (mode2 == 1) {
                category = ((short) this.fRData.fTrie.get(c)) & -16385;
            }
            if (TRACE) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("             ");
                stringBuilder.append(this.fText.getIndex());
                stringBuilder.append("   ");
                printStream.print(stringBuilder.toString());
                if (32 > c || c >= 127) {
                    printStream = System.out;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                    stringBuilder.append(Integer.toHexString(c));
                    stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                    printStream.print(stringBuilder.toString());
                } else {
                    printStream = System.out;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  ");
                    stringBuilder.append(c);
                    stringBuilder.append("  ");
                    printStream.print(stringBuilder.toString());
                }
                printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                stringBuilder.append(state);
                stringBuilder.append("  ");
                stringBuilder.append(category);
                stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                printStream.println(stringBuilder.toString());
            }
            state = stateTable[(row + 4) + category];
            row = this.fRData.getRowIndex(state);
            if (stateTable[row + 0] == mode) {
                result = this.fText.getIndex();
            }
            int completedRule = stateTable[row + 0];
            if (completedRule > 0) {
                lookaheadResult = this.fLookAheadMatches.getPosition(completedRule);
                if (lookaheadResult >= 0) {
                    result = lookaheadResult;
                    break;
                }
            }
            lookaheadResult = stateTable[row + 1];
            if (lookaheadResult != 0) {
                this.fLookAheadMatches.setPosition(lookaheadResult, this.fText.getIndex());
            }
            if (state == 0) {
                break;
            }
            if (mode2 == 1) {
                c = CharacterIteration.previous32(this.fText);
            } else if (mode2 == 0) {
                mode2 = 1;
            }
            mode = (short) -1;
        }
        if (result == initialPosition) {
            CISetIndex32(this.fText, initialPosition);
            CharacterIteration.previous32(this.fText);
            result = this.fText.getIndex();
        }
        if (TRACE) {
            PrintStream printStream2 = System.out;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Result = ");
            stringBuilder2.append(result);
            printStream2.println(stringBuilder2.toString());
        }
        return result;
    }

    private static int CISetIndex32(CharacterIterator ci, int index) {
        if (index <= ci.getBeginIndex()) {
            ci.first();
        } else if (index >= ci.getEndIndex()) {
            ci.setIndex(ci.getEndIndex());
        } else if (Character.isLowSurrogate(ci.setIndex(index)) && !Character.isHighSurrogate(ci.previous())) {
            ci.next();
        }
        return ci.getIndex();
    }
}
