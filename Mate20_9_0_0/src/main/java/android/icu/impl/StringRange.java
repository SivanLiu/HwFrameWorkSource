package android.icu.impl;

import android.icu.lang.CharSequences;
import android.icu.util.ICUException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class StringRange {
    public static final Comparator<int[]> COMPARE_INT_ARRAYS = new Comparator<int[]>() {
        public int compare(int[] o1, int[] o2) {
            int minIndex = Math.min(o1.length, o2.length);
            for (int i = 0; i < minIndex; i++) {
                int diff = o1[i] - o2[i];
                if (diff != 0) {
                    return diff;
                }
            }
            return o1.length - o2.length;
        }
    };
    private static final boolean DEBUG = false;

    public interface Adder {
        void add(String str, String str2);
    }

    static final class Range implements Comparable<Range> {
        int max;
        int min;

        public Range(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && (obj instanceof Range) && compareTo((Range) obj) == 0);
        }

        public int compareTo(Range that) {
            int diff = this.min - that.min;
            if (diff != 0) {
                return diff;
            }
            return this.max - that.max;
        }

        public int hashCode() {
            return (this.min * 37) + this.max;
        }

        public String toString() {
            StringBuilder result = new StringBuilder().appendCodePoint(this.min);
            if (this.min == this.max) {
                return result.toString();
            }
            result.append('~');
            return result.appendCodePoint(this.max).toString();
        }
    }

    static final class Ranges implements Comparable<Ranges> {
        private final Range[] ranges;

        public Ranges(String s) {
            int[] array = CharSequences.codePoints(s);
            this.ranges = new Range[array.length];
            for (int i = 0; i < array.length; i++) {
                this.ranges[i] = new Range(array[i], array[i]);
            }
        }

        public boolean merge(int pivot, Ranges other) {
            for (int i = this.ranges.length - 1; i >= 0; i--) {
                if (i == pivot) {
                    if (this.ranges[i].max != other.ranges[i].min - 1) {
                        return false;
                    }
                } else if (!this.ranges[i].equals(other.ranges[i])) {
                    return false;
                }
            }
            this.ranges[pivot].max = other.ranges[pivot].max;
            return true;
        }

        public String start() {
            StringBuilder result = new StringBuilder();
            for (Range range : this.ranges) {
                result.appendCodePoint(range.min);
            }
            return result.toString();
        }

        public String end(boolean mostCompact) {
            int firstDiff = firstDifference();
            if (firstDiff == this.ranges.length) {
                return null;
            }
            StringBuilder result = new StringBuilder();
            int i = mostCompact ? firstDiff : 0;
            while (i < this.ranges.length) {
                result.appendCodePoint(this.ranges[i].max);
                i++;
            }
            return result.toString();
        }

        public int firstDifference() {
            for (int i = 0; i < this.ranges.length; i++) {
                if (this.ranges[i].min != this.ranges[i].max) {
                    return i;
                }
            }
            return this.ranges.length;
        }

        public Integer size() {
            return Integer.valueOf(this.ranges.length);
        }

        public int compareTo(Ranges other) {
            int diff = this.ranges.length - other.ranges.length;
            if (diff != 0) {
                return diff;
            }
            for (diff = 0; diff < this.ranges.length; diff++) {
                int diff2 = this.ranges[diff].compareTo(other.ranges[diff]);
                if (diff2 != 0) {
                    return diff2;
                }
            }
            return 0;
        }

        public String toString() {
            String start = start();
            String end = end(null);
            if (end == null) {
                return start;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(start);
            stringBuilder.append("~");
            stringBuilder.append(end);
            return stringBuilder.toString();
        }
    }

    public static void compact(Set<String> source, Adder adder, boolean shorterPairs, boolean moreCompact) {
        Iterator it;
        if (moreCompact) {
            Relation<Integer, Ranges> lengthToArrays = Relation.of(new TreeMap(), TreeSet.class);
            for (String s : source) {
                Ranges item = new Ranges(s);
                lengthToArrays.put(item.size(), item);
            }
            for (Entry<Integer, Set<Ranges>> entry : lengthToArrays.keyValuesSet()) {
                it = compact(((Integer) entry.getKey()).intValue(), (Set) entry.getValue()).iterator();
                while (it.hasNext()) {
                    Ranges ranges = (Ranges) it.next();
                    adder.add(ranges.start(), ranges.end(shorterPairs));
                }
            }
            return;
        }
        String start = null;
        String end = null;
        int lastCp = 0;
        int prefixLen = 0;
        it = source.iterator();
        while (true) {
            String str = null;
            if (!it.hasNext()) {
                break;
            }
            String s2 = (String) it.next();
            if (start != null) {
                if (s2.regionMatches(0, start, 0, prefixLen)) {
                    int currentCp = s2.codePointAt(prefixLen);
                    if (currentCp == 1 + lastCp && s2.length() == Character.charCount(currentCp) + prefixLen) {
                        end = s2;
                        lastCp = currentCp;
                    }
                }
                if (end != null) {
                    if (shorterPairs) {
                        str = end.substring(prefixLen, end.length());
                    } else {
                        str = end;
                    }
                }
                adder.add(start, str);
            }
            start = s2;
            end = null;
            lastCp = s2.codePointBefore(s2.length());
            prefixLen = s2.length() - Character.charCount(lastCp);
        }
        String substring = end == null ? null : !shorterPairs ? end : end.substring(prefixLen, end.length());
        adder.add(start, substring);
    }

    public static void compact(Set<String> source, Adder adder, boolean shorterPairs) {
        compact(source, adder, shorterPairs, false);
    }

    private static LinkedList<Ranges> compact(int size, Set<Ranges> inputRanges) {
        LinkedList<Ranges> ranges = new LinkedList(inputRanges);
        for (int i = size - 1; i >= 0; i--) {
            Ranges last = null;
            Iterator<Ranges> it = ranges.iterator();
            while (it.hasNext()) {
                Ranges item = (Ranges) it.next();
                if (last == null) {
                    last = item;
                } else if (last.merge(i, item)) {
                    it.remove();
                } else {
                    last = item;
                }
            }
        }
        return ranges;
    }

    public static Collection<String> expand(String start, String end, boolean requireSameLength, Collection<String> output) {
        if (start == null || end == null) {
            throw new ICUException("Range must have 2 valid strings");
        }
        int[] startCps = CharSequences.codePoints(start);
        int[] endCps = CharSequences.codePoints(end);
        int startOffset = startCps.length - endCps.length;
        if (requireSameLength && startOffset != 0) {
            throw new ICUException("Range must have equal-length strings");
        } else if (startOffset < 0) {
            throw new ICUException("Range must have start-length ≥ end-length");
        } else if (endCps.length != 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < startOffset; i++) {
                builder.appendCodePoint(startCps[i]);
            }
            add(0, startOffset, startCps, endCps, builder, output);
            return output;
        } else {
            throw new ICUException("Range must have end-length > 0");
        }
    }

    private static void add(int endIndex, int startOffset, int[] starts, int[] ends, StringBuilder builder, Collection<String> output) {
        int i = endIndex;
        int[] iArr = ends;
        StringBuilder stringBuilder = builder;
        int start = starts[i + startOffset];
        int end = iArr[i];
        if (start <= end) {
            boolean z = true;
            if (i != iArr.length - 1) {
                z = false;
            }
            boolean last = z;
            int startLen = builder.length();
            int i2 = start;
            while (true) {
                int i3 = i2;
                if (i3 <= end) {
                    stringBuilder.appendCodePoint(i3);
                    if (last) {
                        output.add(builder.toString());
                    } else {
                        add(i + 1, startOffset, starts, iArr, stringBuilder, output);
                    }
                    stringBuilder.setLength(startLen);
                    i2 = i3 + 1;
                } else {
                    return;
                }
            }
        }
        throw new ICUException("Range must have xᵢ ≤ yᵢ for each index i");
    }
}
