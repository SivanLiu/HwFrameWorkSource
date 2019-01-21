package android.icu.text;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CanonicalIterator {
    private static boolean PROGRESS = false;
    private static final Set<String> SET_WITH_NULL_STRING = new HashSet();
    private static boolean SKIP_ZEROS = true;
    private transient StringBuilder buffer = new StringBuilder();
    private int[] current;
    private boolean done;
    private final Normalizer2Impl nfcImpl;
    private final Normalizer2 nfd;
    private String[][] pieces;
    private String source;

    public CanonicalIterator(String source) {
        Norm2AllModes allModes = Norm2AllModes.getNFCInstance();
        this.nfd = allModes.decomp;
        this.nfcImpl = allModes.impl.ensureCanonIterData();
        setSource(source);
    }

    public String getSource() {
        return this.source;
    }

    public void reset() {
        this.done = false;
        for (int i = 0; i < this.current.length; i++) {
            this.current[i] = 0;
        }
    }

    public String next() {
        if (this.done) {
            return null;
        }
        this.buffer.setLength(0);
        for (int i = 0; i < this.pieces.length; i++) {
            this.buffer.append(this.pieces[i][this.current[i]]);
        }
        String result = this.buffer.toString();
        for (int i2 = this.current.length - 1; i2 >= 0; i2--) {
            int[] iArr = this.current;
            iArr[i2] = iArr[i2] + 1;
            if (this.current[i2] < this.pieces[i2].length) {
                break;
            }
            this.current[i2] = 0;
        }
        this.done = true;
        return result;
    }

    public void setSource(String newSource) {
        this.source = this.nfd.normalize(newSource);
        this.done = false;
        if (newSource.length() == 0) {
            this.pieces = new String[1][];
            this.current = new int[1];
            this.pieces[0] = new String[]{""};
            return;
        }
        List<String> segmentList = new ArrayList();
        int start = 0;
        int i = UTF16.findOffsetFromCodePoint(this.source, 1);
        while (i < this.source.length()) {
            int cp = this.source.codePointAt(i);
            if (this.nfcImpl.isCanonSegmentStarter(cp)) {
                segmentList.add(this.source.substring(start, i));
                start = i;
            }
            i += Character.charCount(cp);
        }
        segmentList.add(this.source.substring(start, i));
        this.pieces = new String[segmentList.size()][];
        this.current = new int[segmentList.size()];
        for (i = 0; i < this.pieces.length; i++) {
            if (PROGRESS) {
                System.out.println("SEGMENT");
            }
            this.pieces[i] = getEquivalents((String) segmentList.get(i));
        }
    }

    @Deprecated
    public static void permute(String source, boolean skipZeros, Set<String> output) {
        if (source.length() > 2 || UTF16.countCodePoint(source) > 1) {
            Set<String> subpermute = new HashSet();
            int i = 0;
            while (i < source.length()) {
                int cp = UTF16.charAt(source, i);
                if (!skipZeros || i == 0 || UCharacter.getCombiningClass(cp) != 0) {
                    subpermute.clear();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(source.substring(0, i));
                    stringBuilder.append(source.substring(UTF16.getCharCount(cp) + i));
                    permute(stringBuilder.toString(), skipZeros, subpermute);
                    String chStr = UTF16.valueOf(source, i);
                    for (String s : subpermute) {
                        String piece = new StringBuilder();
                        piece.append(chStr);
                        piece.append(s);
                        output.add(piece.toString());
                    }
                }
                i += UTF16.getCharCount(cp);
            }
            return;
        }
        output.add(source);
    }

    static {
        SET_WITH_NULL_STRING.add("");
    }

    private String[] getEquivalents(String segment) {
        Set<String> result = new HashSet();
        Set<String> basic = getEquivalents2(segment);
        Set<String> permutations = new HashSet();
        for (String item : basic) {
            permutations.clear();
            permute(item, SKIP_ZEROS, permutations);
            for (CharSequence possible : permutations) {
                PrintStream printStream;
                StringBuilder stringBuilder;
                if (Normalizer.compare((String) possible, segment, 0) == 0) {
                    if (PROGRESS) {
                        printStream = System.out;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Adding Permutation: ");
                        stringBuilder.append(Utility.hex(possible));
                        printStream.println(stringBuilder.toString());
                    }
                    result.add(possible);
                } else if (PROGRESS) {
                    printStream = System.out;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("-Skipping Permutation: ");
                    stringBuilder.append(Utility.hex(possible));
                    printStream.println(stringBuilder.toString());
                }
            }
        }
        String[] finalResult = new String[result.size()];
        result.toArray(finalResult);
        return finalResult;
    }

    private Set<String> getEquivalents2(String segment) {
        Set<String> result = new HashSet();
        if (PROGRESS) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adding: ");
            stringBuilder.append(Utility.hex((CharSequence) segment));
            printStream.println(stringBuilder.toString());
        }
        result.add(segment);
        StringBuffer workingBuffer = new StringBuffer();
        UnicodeSet starts = new UnicodeSet();
        int i = 0;
        while (i < segment.length()) {
            int cp = segment.codePointAt(i);
            if (this.nfcImpl.getCanonStartSet(cp, starts)) {
                UnicodeSetIterator iter = new UnicodeSetIterator(starts);
                while (iter.next()) {
                    int cp2 = iter.codepoint;
                    Set<String> remainder = extract(cp2, segment, i, workingBuffer);
                    if (remainder != null) {
                        String prefix = segment.substring(0, i);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(prefix);
                        stringBuilder2.append(UTF16.valueOf(cp2));
                        prefix = stringBuilder2.toString();
                        for (String item : remainder) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(prefix);
                            stringBuilder3.append(item);
                            result.add(stringBuilder3.toString());
                        }
                    }
                }
            }
            i += Character.charCount(cp);
        }
        return result;
    }

    private Set<String> extract(int comp, String segment, int segmentPos, StringBuffer buf) {
        if (PROGRESS) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" extract: ");
            stringBuilder.append(Utility.hex(UTF16.valueOf(comp)));
            stringBuilder.append(", ");
            stringBuilder.append(Utility.hex(segment.substring(segmentPos)));
            printStream.println(stringBuilder.toString());
        }
        String decomp = this.nfcImpl.getDecomposition(comp);
        if (decomp == null) {
            decomp = UTF16.valueOf(comp);
        }
        boolean ok = false;
        int decompCp = UTF16.charAt(decomp, 0);
        int decompPos = 0 + UTF16.getCharCount(decompCp);
        buf.setLength(0);
        int decompPos2 = decompPos;
        decompPos = segmentPos;
        while (decompPos < segment.length()) {
            int cp = UTF16.charAt(segment, decompPos);
            PrintStream printStream2;
            StringBuilder stringBuilder2;
            if (cp == decompCp) {
                if (PROGRESS) {
                    printStream2 = System.out;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  matches: ");
                    stringBuilder2.append(Utility.hex(UTF16.valueOf(cp)));
                    printStream2.println(stringBuilder2.toString());
                }
                if (decompPos2 == decomp.length()) {
                    buf.append(segment.substring(UTF16.getCharCount(cp) + decompPos));
                    ok = true;
                    break;
                }
                decompCp = UTF16.charAt(decomp, decompPos2);
                decompPos2 += UTF16.getCharCount(decompCp);
            } else {
                if (PROGRESS) {
                    printStream2 = System.out;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  buffer: ");
                    stringBuilder2.append(Utility.hex(UTF16.valueOf(cp)));
                    printStream2.println(stringBuilder2.toString());
                }
                UTF16.append(buf, cp);
            }
            decompPos += UTF16.getCharCount(cp);
        }
        if (!ok) {
            return null;
        }
        if (PROGRESS) {
            System.out.println("Matches");
        }
        if (buf.length() == 0) {
            return SET_WITH_NULL_STRING;
        }
        String remainder = buf.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(UTF16.valueOf(comp));
        stringBuilder3.append(remainder);
        if (Normalizer.compare(stringBuilder3.toString(), segment.substring(segmentPos), 0) != 0) {
            return null;
        }
        return getEquivalents2(remainder);
    }
}
