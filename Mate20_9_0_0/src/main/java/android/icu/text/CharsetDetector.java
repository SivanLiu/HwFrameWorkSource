package android.icu.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CharsetDetector {
    private static final List<CSRecognizerInfo> ALL_CS_RECOGNIZERS;
    private static final int kBufSize = 8000;
    short[] fByteStats = new short[256];
    boolean fC1Bytes = false;
    String fDeclaredEncoding;
    private boolean[] fEnabledRecognizers;
    byte[] fInputBytes = new byte[kBufSize];
    int fInputLen;
    InputStream fInputStream;
    byte[] fRawInput;
    int fRawLength;
    private boolean fStripTags = false;

    private static class CSRecognizerInfo {
        boolean isDefaultEnabled;
        CharsetRecognizer recognizer;

        CSRecognizerInfo(CharsetRecognizer recognizer, boolean isDefaultEnabled) {
            this.recognizer = recognizer;
            this.isDefaultEnabled = isDefaultEnabled;
        }
    }

    public CharsetDetector setDeclaredEncoding(String encoding) {
        this.fDeclaredEncoding = encoding;
        return this;
    }

    public CharsetDetector setText(byte[] in) {
        this.fRawInput = in;
        this.fRawLength = in.length;
        return this;
    }

    public CharsetDetector setText(InputStream in) throws IOException {
        this.fInputStream = in;
        InputStream inputStream = this.fInputStream;
        int remainingLength = kBufSize;
        inputStream.mark(kBufSize);
        this.fRawInput = new byte[kBufSize];
        this.fRawLength = 0;
        while (true) {
            int remainingLength2 = remainingLength;
            if (remainingLength2 <= 0) {
                break;
            }
            remainingLength = this.fInputStream.read(this.fRawInput, this.fRawLength, remainingLength2);
            if (remainingLength <= 0) {
                break;
            }
            this.fRawLength += remainingLength;
            remainingLength = remainingLength2 - remainingLength;
        }
        this.fInputStream.reset();
        return this;
    }

    public CharsetMatch detect() {
        CharsetMatch[] matches = detectAll();
        if (matches == null || matches.length == 0) {
            return null;
        }
        return matches[0];
    }

    public CharsetMatch[] detectAll() {
        ArrayList<CharsetMatch> matches = new ArrayList();
        MungeInput();
        int i = 0;
        while (i < ALL_CS_RECOGNIZERS.size()) {
            CSRecognizerInfo rcinfo = (CSRecognizerInfo) ALL_CS_RECOGNIZERS.get(i);
            if (this.fEnabledRecognizers != null ? this.fEnabledRecognizers[i] : rcinfo.isDefaultEnabled) {
                CharsetMatch m = rcinfo.recognizer.match(this);
                if (m != null) {
                    matches.add(m);
                }
            }
            i++;
        }
        Collections.sort(matches);
        Collections.reverse(matches);
        return (CharsetMatch[]) matches.toArray(new CharsetMatch[matches.size()]);
    }

    public Reader getReader(InputStream in, String declaredEncoding) {
        this.fDeclaredEncoding = declaredEncoding;
        try {
            setText(in);
            CharsetMatch match = detect();
            if (match == null) {
                return null;
            }
            return match.getReader();
        } catch (IOException e) {
            return null;
        }
    }

    public String getString(byte[] in, String declaredEncoding) {
        this.fDeclaredEncoding = declaredEncoding;
        try {
            setText(in);
            CharsetMatch match = detect();
            if (match == null) {
                return null;
            }
            return match.getString(-1);
        } catch (IOException e) {
            return null;
        }
    }

    public static String[] getAllDetectableCharsets() {
        String[] allCharsetNames = new String[ALL_CS_RECOGNIZERS.size()];
        for (int i = 0; i < allCharsetNames.length; i++) {
            allCharsetNames[i] = ((CSRecognizerInfo) ALL_CS_RECOGNIZERS.get(i)).recognizer.getName();
        }
        return allCharsetNames;
    }

    public boolean inputFilterEnabled() {
        return this.fStripTags;
    }

    public boolean enableInputFilter(boolean filter) {
        boolean previous = this.fStripTags;
        this.fStripTags = filter;
        return previous;
    }

    private void MungeInput() {
        int srci;
        int limit;
        int dsti = 0;
        boolean inMarkup = false;
        int openTags = 0;
        int badTags = 0;
        if (this.fStripTags) {
            for (srci = 0; srci < this.fRawLength && dsti < this.fInputBytes.length; srci++) {
                byte b = this.fRawInput[srci];
                if (b == (byte) 60) {
                    if (inMarkup) {
                        badTags++;
                    }
                    inMarkup = true;
                    openTags++;
                }
                if (!inMarkup) {
                    int dsti2 = dsti + 1;
                    this.fInputBytes[dsti] = b;
                    dsti = dsti2;
                }
                if (b == (byte) 62) {
                    inMarkup = false;
                }
            }
            this.fInputLen = dsti;
        }
        if (openTags < 5 || openTags / 5 < badTags || (this.fInputLen < 100 && this.fRawLength > 600)) {
            limit = this.fRawLength;
            if (limit > kBufSize) {
                limit = kBufSize;
            }
            srci = 0;
            while (srci < limit) {
                this.fInputBytes[srci] = this.fRawInput[srci];
                srci++;
            }
            this.fInputLen = srci;
        }
        Arrays.fill(this.fByteStats, (short) 0);
        for (srci = 0; srci < this.fInputLen; srci++) {
            limit = this.fInputBytes[srci] & 255;
            short[] sArr = this.fByteStats;
            sArr[limit] = (short) (sArr[limit] + 1);
        }
        this.fC1Bytes = false;
        for (limit = 128; limit <= 159; limit++) {
            if (this.fByteStats[limit] != (short) 0) {
                this.fC1Bytes = true;
                return;
            }
        }
    }

    static {
        List<CSRecognizerInfo> list = new ArrayList();
        list.add(new CSRecognizerInfo(new CharsetRecog_UTF8(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_UTF_16_BE(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_UTF_16_LE(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_UTF_32_BE(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_UTF_32_LE(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_sjis(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_2022JP(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_2022CN(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_2022KR(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_gb_18030(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_euc_jp(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_euc_kr(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_big5(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_1(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_2(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_5_ru(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_6_ar(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_7_el(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_8_I_he(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_8_he(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_windows_1251(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_windows_1256(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_KOI8_R(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_8859_9_tr(), true));
        list.add(new CSRecognizerInfo(new CharsetRecog_IBM424_he_rtl(), false));
        list.add(new CSRecognizerInfo(new CharsetRecog_IBM424_he_ltr(), false));
        list.add(new CSRecognizerInfo(new CharsetRecog_IBM420_ar_rtl(), false));
        list.add(new CSRecognizerInfo(new CharsetRecog_IBM420_ar_ltr(), false));
        ALL_CS_RECOGNIZERS = Collections.unmodifiableList(list);
    }

    @Deprecated
    public String[] getDetectableCharsets() {
        List<String> csnames = new ArrayList(ALL_CS_RECOGNIZERS.size());
        int i = 0;
        while (i < ALL_CS_RECOGNIZERS.size()) {
            CSRecognizerInfo rcinfo = (CSRecognizerInfo) ALL_CS_RECOGNIZERS.get(i);
            if (this.fEnabledRecognizers == null ? rcinfo.isDefaultEnabled : this.fEnabledRecognizers[i]) {
                csnames.add(rcinfo.recognizer.getName());
            }
            i++;
        }
        return (String[]) csnames.toArray(new String[csnames.size()]);
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x002f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    public CharsetDetector setDetectableCharset(String encoding, boolean enabled) {
        int modIdx = -1;
        boolean isDefaultVal = false;
        int i = 0;
        for (int i2 = 0; i2 < ALL_CS_RECOGNIZERS.size(); i2++) {
            CSRecognizerInfo csrinfo = (CSRecognizerInfo) ALL_CS_RECOGNIZERS.get(i2);
            if (csrinfo.recognizer.getName().equals(encoding)) {
                modIdx = i2;
                isDefaultVal = csrinfo.isDefaultEnabled == enabled;
                if (modIdx < 0) {
                    if (this.fEnabledRecognizers == null && !isDefaultVal) {
                        this.fEnabledRecognizers = new boolean[ALL_CS_RECOGNIZERS.size()];
                        while (i < ALL_CS_RECOGNIZERS.size()) {
                            this.fEnabledRecognizers[i] = ((CSRecognizerInfo) ALL_CS_RECOGNIZERS.get(i)).isDefaultEnabled;
                            i++;
                        }
                    }
                    if (this.fEnabledRecognizers != null) {
                        this.fEnabledRecognizers[modIdx] = enabled;
                    }
                    return this;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid encoding: \"");
                stringBuilder.append(encoding);
                stringBuilder.append("\"");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (modIdx < 0) {
        }
    }
}
