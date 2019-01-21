package android.icu.util;

import android.icu.text.UTF16;
import android.icu.util.BytesTrie.Result;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public final class CharsTrie implements Cloneable, Iterable<Entry> {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int kMaxBranchLinearSubNodeLength = 5;
    static final int kMaxLinearMatchLength = 16;
    static final int kMaxOneUnitDelta = 64511;
    static final int kMaxOneUnitNodeValue = 255;
    static final int kMaxOneUnitValue = 16383;
    static final int kMaxTwoUnitDelta = 67043327;
    static final int kMaxTwoUnitNodeValue = 16646143;
    static final int kMaxTwoUnitValue = 1073676287;
    static final int kMinLinearMatch = 48;
    static final int kMinTwoUnitDeltaLead = 64512;
    static final int kMinTwoUnitNodeValueLead = 16448;
    static final int kMinTwoUnitValueLead = 16384;
    static final int kMinValueLead = 64;
    static final int kNodeTypeMask = 63;
    static final int kThreeUnitDeltaLead = 65535;
    static final int kThreeUnitNodeValueLead = 32704;
    static final int kThreeUnitValueLead = 32767;
    static final int kValueIsFinal = 32768;
    private static Result[] valueResults_ = new Result[]{Result.INTERMEDIATE_VALUE, Result.FINAL_VALUE};
    private CharSequence chars_;
    private int pos_;
    private int remainingMatchLength_ = -1;
    private int root_;

    public static final class Entry {
        public CharSequence chars;
        public int value;

        private Entry() {
        }
    }

    public static final class Iterator implements java.util.Iterator<Entry> {
        private CharSequence chars_;
        private Entry entry_;
        private int initialPos_;
        private int initialRemainingMatchLength_;
        private int maxLength_;
        private int pos_;
        private int remainingMatchLength_;
        private boolean skipValue_;
        private ArrayList<Long> stack_;
        private StringBuilder str_;

        private Iterator(CharSequence trieChars, int offset, int remainingMatchLength, int maxStringLength) {
            this.str_ = new StringBuilder();
            this.entry_ = new Entry();
            this.stack_ = new ArrayList();
            this.chars_ = trieChars;
            this.initialPos_ = offset;
            this.pos_ = offset;
            this.initialRemainingMatchLength_ = remainingMatchLength;
            this.remainingMatchLength_ = remainingMatchLength;
            this.maxLength_ = maxStringLength;
            int length = this.remainingMatchLength_;
            if (length >= 0) {
                length++;
                if (this.maxLength_ > 0 && length > this.maxLength_) {
                    length = this.maxLength_;
                }
                this.str_.append(this.chars_, this.pos_, this.pos_ + length);
                this.pos_ += length;
                this.remainingMatchLength_ -= length;
            }
        }

        public Iterator reset() {
            this.pos_ = this.initialPos_;
            this.remainingMatchLength_ = this.initialRemainingMatchLength_;
            this.skipValue_ = false;
            int length = this.remainingMatchLength_ + 1;
            if (this.maxLength_ > 0 && length > this.maxLength_) {
                length = this.maxLength_;
            }
            this.str_.setLength(length);
            this.pos_ += length;
            this.remainingMatchLength_ -= length;
            this.stack_.clear();
            return this;
        }

        public boolean hasNext() {
            return this.pos_ >= 0 || !this.stack_.isEmpty();
        }

        public Entry next() {
            int length;
            int node = this.pos_;
            if (node < 0) {
                if (this.stack_.isEmpty()) {
                    throw new NoSuchElementException();
                }
                long top = ((Long) this.stack_.remove(this.stack_.size() - 1)).longValue();
                length = (int) top;
                node = (int) (top >> 32);
                this.str_.setLength(65535 & length);
                length >>>= 16;
                if (length > 1) {
                    node = branchNext(node, length);
                    if (node < 0) {
                        return this.entry_;
                    }
                }
                int pos = node + 1;
                this.str_.append(this.chars_.charAt(node));
                node = pos;
            }
            if (this.remainingMatchLength_ >= 0) {
                return truncateAndStop();
            }
            while (true) {
                int pos2 = node + 1;
                node = this.chars_.charAt(node);
                if (node >= 64) {
                    boolean z = false;
                    if (this.skipValue_) {
                        pos2 = CharsTrie.skipNodeValue(pos2, node);
                        node &= 63;
                        this.skipValue_ = false;
                    } else {
                        if ((32768 & node) != 0) {
                            z = true;
                        }
                        boolean isFinal = z;
                        if (isFinal) {
                            this.entry_.value = CharsTrie.readValue(this.chars_, pos2, node & CharsTrie.kThreeUnitValueLead);
                        } else {
                            this.entry_.value = CharsTrie.readNodeValue(this.chars_, pos2, node);
                        }
                        if (isFinal || (this.maxLength_ > 0 && this.str_.length() == this.maxLength_)) {
                            this.pos_ = -1;
                        } else {
                            this.pos_ = pos2 - 1;
                            this.skipValue_ = true;
                        }
                        this.entry_.chars = this.str_;
                        return this.entry_;
                    }
                }
                if (this.maxLength_ > 0 && this.str_.length() == this.maxLength_) {
                    return truncateAndStop();
                }
                int pos3;
                if (node < 48) {
                    if (node == 0) {
                        length = pos2 + 1;
                        node = this.chars_.charAt(pos2);
                        pos2 = length;
                    }
                    pos3 = branchNext(pos2, node + 1);
                    if (pos3 < 0) {
                        return this.entry_;
                    }
                    node = pos3;
                } else {
                    pos3 = (node - 48) + 1;
                    if (this.maxLength_ <= 0 || this.str_.length() + pos3 <= this.maxLength_) {
                        this.str_.append(this.chars_, pos2, pos2 + pos3);
                        node = pos2 + pos3;
                    } else {
                        this.str_.append(this.chars_, pos2, (this.maxLength_ + pos2) - this.str_.length());
                        return truncateAndStop();
                    }
                }
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Entry truncateAndStop() {
            this.pos_ = -1;
            this.entry_.chars = this.str_;
            this.entry_.value = -1;
            return this.entry_;
        }

        private int branchNext(int trieUnit, int length) {
            char trieUnit2;
            while (length > 5) {
                trieUnit2 = trieUnit2 + 1;
                this.stack_.add(Long.valueOf(((((long) CharsTrie.skipDelta(this.chars_, trieUnit2)) << 32) | ((long) ((length - (length >> 1)) << 16))) | ((long) this.str_.length())));
                length >>= 1;
                trieUnit2 = CharsTrie.jumpByDelta(this.chars_, trieUnit2);
            }
            int pos = trieUnit2 + 1;
            trieUnit2 = this.chars_.charAt(trieUnit2);
            int pos2 = pos + 1;
            int node = this.chars_.charAt(pos);
            boolean isFinal = (32768 & node) != 0;
            int i = node & CharsTrie.kThreeUnitValueLead;
            node = i;
            int value = CharsTrie.readValue(this.chars_, pos2, i);
            pos2 = CharsTrie.skipValue(pos2, node);
            this.stack_.add(Long.valueOf(((((long) pos2) << 32) | ((long) ((length - 1) << 16))) | ((long) this.str_.length())));
            this.str_.append(trieUnit2);
            if (!isFinal) {
                return pos2 + value;
            }
            this.pos_ = -1;
            this.entry_.chars = this.str_;
            this.entry_.value = value;
            return -1;
        }
    }

    public static final class State {
        private CharSequence chars;
        private int pos;
        private int remainingMatchLength;
        private int root;
    }

    public CharsTrie(CharSequence trieChars, int offset) {
        this.chars_ = trieChars;
        this.root_ = offset;
        this.pos_ = offset;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public CharsTrie reset() {
        this.pos_ = this.root_;
        this.remainingMatchLength_ = -1;
        return this;
    }

    public CharsTrie saveState(State state) {
        state.chars = this.chars_;
        state.root = this.root_;
        state.pos = this.pos_;
        state.remainingMatchLength = this.remainingMatchLength_;
        return this;
    }

    public CharsTrie resetToState(State state) {
        if (this.chars_ == state.chars && this.chars_ != null && this.root_ == state.root) {
            this.pos_ = state.pos;
            this.remainingMatchLength_ = state.remainingMatchLength;
            return this;
        }
        throw new IllegalArgumentException("incompatible trie state");
    }

    public Result current() {
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        Result result;
        if (this.remainingMatchLength_ < 0) {
            char charAt = this.chars_.charAt(pos);
            char node = charAt;
            if (charAt >= '@') {
                result = valueResults_[node >> 15];
                return result;
            }
        }
        result = Result.NO_VALUE;
        return result;
    }

    public Result first(int inUnit) {
        this.remainingMatchLength_ = -1;
        return nextImpl(this.root_, inUnit);
    }

    public Result firstForCodePoint(int cp) {
        if (cp <= 65535) {
            return first(cp);
        }
        if (first(UTF16.getLeadSurrogate(cp)).hasNext()) {
            return next(UTF16.getTrailSurrogate(cp));
        }
        return Result.NO_MATCH;
    }

    public Result next(int inUnit) {
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        int length = this.remainingMatchLength_;
        if (length < 0) {
            return nextImpl(pos, inUnit);
        }
        int pos2 = pos + 1;
        if (inUnit == this.chars_.charAt(pos)) {
            Result result;
            length--;
            this.remainingMatchLength_ = length;
            this.pos_ = pos2;
            if (length < 0) {
                char charAt = this.chars_.charAt(pos2);
                char node = charAt;
                if (charAt >= '@') {
                    result = valueResults_[node >> 15];
                    return result;
                }
            }
            result = Result.NO_VALUE;
            return result;
        }
        stop();
        return Result.NO_MATCH;
    }

    public Result nextForCodePoint(int cp) {
        if (cp <= 65535) {
            return next(cp);
        }
        if (next(UTF16.getLeadSurrogate(cp)).hasNext()) {
            return next(UTF16.getTrailSurrogate(cp));
        }
        return Result.NO_MATCH;
    }

    public Result next(CharSequence s, int inUnit, int sLimit) {
        if (inUnit >= sLimit) {
            return current();
        }
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        char sIndex;
        Result result;
        int length = this.remainingMatchLength_;
        char inUnit2;
        while (inUnit2 != sLimit) {
            sIndex = inUnit2 + 1;
            inUnit2 = s.charAt(inUnit2);
            if (length < 0) {
                this.remainingMatchLength_ = length;
                int pos2 = pos + 1;
                pos = this.chars_.charAt(pos);
                while (true) {
                    if (pos < 48) {
                        Result result2 = branchNext(pos2, pos, inUnit2);
                        if (result2 == Result.NO_MATCH) {
                            return Result.NO_MATCH;
                        }
                        int sIndex2;
                        if (sIndex2 == sLimit) {
                            return result2;
                        }
                        if (result2 == Result.FINAL_VALUE) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        int sIndex3 = sIndex2 + 1;
                        inUnit2 = s.charAt(sIndex2);
                        sIndex2 = this.pos_;
                        int pos3 = sIndex2 + 1;
                        pos = this.chars_.charAt(sIndex2);
                        sIndex2 = sIndex3;
                        pos2 = pos3;
                    } else if (pos < 64) {
                        length = pos - 48;
                        if (inUnit2 != this.chars_.charAt(pos2)) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        length--;
                        pos = pos2 + 1;
                    } else if ((32768 & pos) != 0) {
                        stop();
                        return Result.NO_MATCH;
                    } else {
                        pos2 = skipNodeValue(pos2, pos);
                        pos &= 63;
                    }
                }
            } else if (inUnit2 != this.chars_.charAt(pos)) {
                stop();
                return Result.NO_MATCH;
            } else {
                pos++;
                length--;
            }
            inUnit2 = sIndex;
        }
        this.remainingMatchLength_ = length;
        this.pos_ = pos;
        if (length < 0) {
            sIndex = this.chars_.charAt(pos);
            char node = sIndex;
            if (sIndex >= '@') {
                result = valueResults_[node >> 15];
                return result;
            }
        }
        result = Result.NO_VALUE;
        return result;
    }

    public int getValue() {
        int leadUnit = this.pos_;
        int pos = leadUnit + 1;
        leadUnit = this.chars_.charAt(leadUnit);
        return (32768 & leadUnit) != 0 ? readValue(this.chars_, pos, leadUnit & kThreeUnitValueLead) : readNodeValue(this.chars_, pos, leadUnit);
    }

    public long getUniqueValue() {
        int pos = this.pos_;
        if (pos < 0) {
            return 0;
        }
        return (findUniqueValue(this.chars_, (this.remainingMatchLength_ + pos) + 1, 0) << 31) >> 31;
    }

    public int getNextChars(Appendable out) {
        int node = this.pos_;
        if (node < 0) {
            return 0;
        }
        if (this.remainingMatchLength_ >= 0) {
            append(out, this.chars_.charAt(node));
            return 1;
        }
        int pos = node + 1;
        node = this.chars_.charAt(node);
        if (node >= 64) {
            if ((32768 & node) != 0) {
                return 0;
            }
            pos = skipNodeValue(pos, node);
            node &= 63;
        }
        if (node < 48) {
            int pos2;
            if (node == 0) {
                pos2 = pos + 1;
                node = this.chars_.charAt(pos);
            } else {
                pos2 = pos;
            }
            node++;
            getNextBranchChars(this.chars_, pos2, node, out);
            return node;
        }
        append(out, this.chars_.charAt(pos));
        return 1;
    }

    public Iterator iterator() {
        return new Iterator(this.chars_, this.pos_, this.remainingMatchLength_, 0);
    }

    public Iterator iterator(int maxStringLength) {
        return new Iterator(this.chars_, this.pos_, this.remainingMatchLength_, maxStringLength);
    }

    public static Iterator iterator(CharSequence trieChars, int offset, int maxStringLength) {
        return new Iterator(trieChars, offset, -1, maxStringLength);
    }

    private void stop() {
        this.pos_ = -1;
    }

    private static int readValue(CharSequence chars, int pos, int leadUnit) {
        if (leadUnit < 16384) {
            return leadUnit;
        }
        if (leadUnit < kThreeUnitValueLead) {
            return ((leadUnit - 16384) << 16) | chars.charAt(pos);
        }
        return (chars.charAt(pos) << 16) | chars.charAt(pos + 1);
    }

    private static int skipValue(int pos, int leadUnit) {
        if (leadUnit < 16384) {
            return pos;
        }
        if (leadUnit < kThreeUnitValueLead) {
            return pos + 1;
        }
        return pos + 2;
    }

    private static int skipValue(CharSequence chars, int leadUnit) {
        return skipValue(leadUnit + 1, chars.charAt(leadUnit) & kThreeUnitValueLead);
    }

    private static int readNodeValue(CharSequence chars, int pos, int leadUnit) {
        if (leadUnit < kMinTwoUnitNodeValueLead) {
            return (leadUnit >> 6) - 1;
        }
        if (leadUnit < kThreeUnitNodeValueLead) {
            return (((leadUnit & kThreeUnitNodeValueLead) - kMinTwoUnitNodeValueLead) << 10) | chars.charAt(pos);
        }
        return (chars.charAt(pos) << 16) | chars.charAt(pos + 1);
    }

    private static int skipNodeValue(int pos, int leadUnit) {
        if (leadUnit < kMinTwoUnitNodeValueLead) {
            return pos;
        }
        if (leadUnit < kThreeUnitNodeValueLead) {
            return pos + 1;
        }
        return pos + 2;
    }

    private static int jumpByDelta(CharSequence chars, int delta) {
        int pos = delta + 1;
        delta = chars.charAt(delta);
        if (delta >= 64512) {
            if (delta == 65535) {
                delta = (chars.charAt(pos) << 16) | chars.charAt(pos + 1);
                pos += 2;
            } else {
                delta = ((delta - 64512) << 16) | chars.charAt(pos);
                pos++;
            }
        }
        return pos + delta;
    }

    private static int skipDelta(CharSequence chars, int delta) {
        int pos = delta + 1;
        delta = chars.charAt(delta);
        if (delta < 64512) {
            return pos;
        }
        if (delta == 65535) {
            return pos + 2;
        }
        return pos + 1;
    }

    private Result branchNext(int pos, int length, int inUnit) {
        int pos2;
        if (length == 0) {
            int pos3 = pos + 1;
            length = this.chars_.charAt(pos);
            pos = pos3;
        }
        length++;
        while (length > 5) {
            pos2 = pos + 1;
            if (inUnit < this.chars_.charAt(pos)) {
                length >>= 1;
                pos = jumpByDelta(this.chars_, pos2);
            } else {
                length -= length >> 1;
                pos = skipDelta(this.chars_, pos2);
            }
        }
        do {
            pos2 = pos + 1;
            if (inUnit == this.chars_.charAt(pos)) {
                Result result;
                pos = this.chars_.charAt(pos2);
                if ((32768 & pos) != 0) {
                    result = Result.FINAL_VALUE;
                } else {
                    int delta;
                    pos2++;
                    if (pos < 16384) {
                        delta = pos;
                    } else if (pos < kThreeUnitValueLead) {
                        delta = ((pos - 16384) << 16) | this.chars_.charAt(pos2);
                        pos2++;
                    } else {
                        delta = (this.chars_.charAt(pos2) << 16) | this.chars_.charAt(pos2 + 1);
                        pos2 += 2;
                    }
                    pos2 += delta;
                    pos = this.chars_.charAt(pos2);
                    result = pos >= 64 ? valueResults_[pos >> 15] : Result.NO_VALUE;
                }
                this.pos_ = pos2;
                return result;
            }
            length--;
            pos = skipValue(this.chars_, pos2);
        } while (length > 1);
        pos2 = pos + 1;
        if (inUnit == this.chars_.charAt(pos)) {
            this.pos_ = pos2;
            pos = this.chars_.charAt(pos2);
            return pos >= 64 ? valueResults_[pos >> 15] : Result.NO_VALUE;
        }
        stop();
        return Result.NO_MATCH;
    }

    private Result nextImpl(int node, int inUnit) {
        int pos = node + 1;
        node = this.chars_.charAt(node);
        while (node >= 48) {
            if (node < 64) {
                int length = node - 48;
                int pos2 = pos + 1;
                if (inUnit == this.chars_.charAt(pos)) {
                    Result result;
                    length--;
                    this.remainingMatchLength_ = length;
                    this.pos_ = pos2;
                    if (length < 0) {
                        char charAt = this.chars_.charAt(pos2);
                        char node2 = charAt;
                        if (charAt >= '@') {
                            result = valueResults_[node2 >> 15];
                            return result;
                        }
                    }
                    result = Result.NO_VALUE;
                    return result;
                }
                pos = pos2;
            } else if ((32768 & node) == 0) {
                pos = skipNodeValue(pos, node);
                node &= 63;
            }
            stop();
            return Result.NO_MATCH;
        }
        return branchNext(pos, node, inUnit);
    }

    private static long findUniqueValueFromBranch(CharSequence chars, int pos, int length, long uniqueValue) {
        while (length > 5) {
            pos++;
            uniqueValue = findUniqueValueFromBranch(chars, jumpByDelta(chars, pos), length >> 1, uniqueValue);
            if (uniqueValue == 0) {
                return 0;
            }
            length -= length >> 1;
            pos = skipDelta(chars, pos);
        }
        while (true) {
            pos++;
            int pos2 = pos + 1;
            pos = chars.charAt(pos);
            boolean isFinal = (32768 & pos) != 0;
            pos &= kThreeUnitValueLead;
            int value = readValue(chars, pos2, pos);
            pos2 = skipValue(pos2, pos);
            if (!isFinal) {
                uniqueValue = findUniqueValue(chars, pos2 + value, uniqueValue);
                if (uniqueValue == 0) {
                    return 0;
                }
            } else if (uniqueValue == 0) {
                uniqueValue = (((long) value) << 1) | 1;
            } else if (value != ((int) (uniqueValue >> 1))) {
                return 0;
            }
            length--;
            if (length <= 1) {
                return (((long) (pos2 + 1)) << 33) | (8589934591L & uniqueValue);
            }
            pos = pos2;
        }
    }

    private static long findUniqueValue(CharSequence chars, int node, long uniqueValue) {
        int pos = node + 1;
        node = chars.charAt(node);
        while (true) {
            int pos2;
            if (node < 48) {
                if (node == 0) {
                    pos2 = pos + 1;
                    node = chars.charAt(pos);
                    pos = pos2;
                }
                uniqueValue = findUniqueValueFromBranch(chars, pos, node + 1, uniqueValue);
                if (uniqueValue == 0) {
                    return 0;
                }
                pos = (int) (uniqueValue >>> 33);
                pos2 = pos + 1;
                node = chars.charAt(pos);
            } else if (node < 64) {
                pos += (node - 48) + 1;
                pos2 = pos + 1;
                node = chars.charAt(pos);
            } else {
                int value;
                boolean isFinal = (32768 & node) != 0;
                if (isFinal) {
                    value = readValue(chars, pos, node & kThreeUnitValueLead);
                } else {
                    value = readNodeValue(chars, pos, node);
                }
                if (uniqueValue == 0) {
                    uniqueValue = (((long) value) << 1) | 1;
                } else if (value != ((int) (uniqueValue >> 1))) {
                    return 0;
                }
                if (isFinal) {
                    return uniqueValue;
                }
                pos = skipNodeValue(pos, node);
                node &= 63;
            }
            pos = pos2;
        }
    }

    private static void getNextBranchChars(CharSequence chars, int pos, int length, Appendable out) {
        while (length > 5) {
            pos++;
            getNextBranchChars(chars, jumpByDelta(chars, pos), length >> 1, out);
            length -= length >> 1;
            pos = skipDelta(chars, pos);
        }
        do {
            int pos2 = pos + 1;
            append(out, chars.charAt(pos));
            pos = skipValue(chars, pos2);
            length--;
        } while (length > 1);
        append(out, chars.charAt(pos));
    }

    private static void append(Appendable out, int c) {
        try {
            out.append((char) c);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
