package android.icu.text;

import android.icu.lang.UCharacter;

final class BidiWriter {
    static final char LRM_CHAR = '‎';
    static final int MASK_R_AL = 8194;
    static final char RLM_CHAR = '‏';

    BidiWriter() {
    }

    private static boolean IsCombining(int type) {
        return ((1 << type) & 448) != 0;
    }

    private static String doWriteForward(String src, int options) {
        int i = options & 10;
        if (i == 0) {
            return src;
        }
        int i2 = 0;
        StringBuffer dest;
        int i3;
        if (i == 2) {
            dest = new StringBuffer(src.length());
            while (true) {
                i3 = i2;
                i2 = UTF16.charAt(src, i3);
                i3 += UTF16.getCharCount(i2);
                UTF16.append(dest, UCharacter.getMirror(i2));
                if (i3 >= src.length()) {
                    return dest.toString();
                }
                i2 = i3;
            }
        } else if (i != 8) {
            dest = new StringBuffer(src.length());
            while (true) {
                i3 = i2;
                i2 = UTF16.charAt(src, i3);
                i3 += UTF16.getCharCount(i2);
                if (!Bidi.IsBidiControlChar(i2)) {
                    UTF16.append(dest, UCharacter.getMirror(i2));
                }
                if (i3 >= src.length()) {
                    return dest.toString();
                }
                i2 = i3;
            }
        } else {
            StringBuilder dest2 = new StringBuilder(src.length());
            char i4;
            do {
                char c = i4;
                i4 = c + 1;
                c = src.charAt(c);
                if (!Bidi.IsBidiControlChar(c)) {
                    dest2.append(c);
                }
            } while (i4 < src.length());
            return dest2.toString();
        }
    }

    private static String doWriteForward(char[] text, int start, int limit, int options) {
        return doWriteForward(new String(text, start, limit - start), options);
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x003a A:{LOOP_END, LOOP:0: B:4:0x0017->B:11:0x003a} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x00a4 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static String writeReverse(String src, int options) {
        StringBuffer dest = new StringBuffer(src.length());
        int srcLength;
        int i;
        int c;
        switch (options & 11) {
            case 0:
                srcLength = src.length();
                do {
                    i = srcLength;
                    srcLength -= UTF16.getCharCount(UTF16.charAt(src, srcLength - 1));
                    dest.append(src.substring(srcLength, i));
                } while (srcLength > 0);
                break;
            case 1:
                srcLength = src.length();
                while (true) {
                    i = srcLength;
                    do {
                        c = UTF16.charAt(src, i - 1);
                        i -= UTF16.getCharCount(c);
                        if (i > 0) {
                        }
                        dest.append(src.substring(i, srcLength));
                        if (i > 0) {
                            break;
                        }
                        srcLength = i;
                    } while (IsCombining(UCharacter.getType(c)));
                    dest.append(src.substring(i, srcLength));
                    if (i > 0) {
                    }
                }
            default:
                srcLength = src.length();
                do {
                    i = srcLength;
                    c = UTF16.charAt(src, srcLength - 1);
                    srcLength -= UTF16.getCharCount(c);
                    if ((options & 1) != 0) {
                        while (srcLength > 0 && IsCombining(UCharacter.getType(c))) {
                            c = UTF16.charAt(src, srcLength - 1);
                            srcLength -= UTF16.getCharCount(c);
                        }
                    }
                    if ((options & 8) == 0 || !Bidi.IsBidiControlChar(c)) {
                        int j = srcLength;
                        if ((options & 2) != 0) {
                            c = UCharacter.getMirror(c);
                            UTF16.append(dest, c);
                            j += UTF16.getCharCount(c);
                        }
                        dest.append(src.substring(j, i));
                        continue;
                    }
                } while (srcLength > 0);
                break;
        }
        return dest.toString();
    }

    static String doWriteReverse(char[] text, int start, int limit, int options) {
        return writeReverse(new String(text, start, limit - start), options);
    }

    static String writeReordered(Bidi bidi, int options) {
        char[] text = bidi.text;
        int runCount = bidi.countRuns();
        if ((bidi.reorderingOptions & 1) != 0) {
            options = (options | 4) & -9;
        }
        if ((bidi.reorderingOptions & 2) != 0) {
            options = (options | 8) & -5;
        }
        if (!(bidi.reorderingMode == 4 || bidi.reorderingMode == 5 || bidi.reorderingMode == 6 || bidi.reorderingMode == 3)) {
            options &= -5;
        }
        StringBuilder dest = new StringBuilder((options & 4) != 0 ? bidi.length * 2 : bidi.length);
        int run;
        int run2;
        BidiRun bidiRun;
        byte[] dirProps;
        if ((options & 16) == 0) {
            run = 0;
            if ((options & 4) == 0) {
                while (true) {
                    run2 = run;
                    if (run2 >= runCount) {
                        break;
                    }
                    bidiRun = bidi.getVisualRun(run2);
                    if (bidiRun.isEvenRun()) {
                        dest.append(doWriteForward(text, bidiRun.start, bidiRun.limit, options & -3));
                    } else {
                        dest.append(doWriteReverse(text, bidiRun.start, bidiRun.limit, options));
                    }
                    run = run2 + 1;
                }
            } else {
                dirProps = bidi.dirProps;
                int run3 = 0;
                while (run3 < runCount) {
                    BidiRun bidiRun2 = bidi.getVisualRun(run3);
                    int markFlag = bidi.runs[run3].insertRemove;
                    if (markFlag < 0) {
                        markFlag = 0;
                    }
                    char uc;
                    if (bidiRun2.isEvenRun()) {
                        if (bidi.isInverse() && dirProps[bidiRun2.start] != (byte) 0) {
                            markFlag |= 1;
                        }
                        if ((markFlag & 1) != 0) {
                            uc = LRM_CHAR;
                        } else if ((markFlag & 4) != 0) {
                            uc = RLM_CHAR;
                        } else {
                            uc = 0;
                        }
                        if (uc != 0) {
                            dest.append(uc);
                        }
                        dest.append(doWriteForward(text, bidiRun2.start, bidiRun2.limit, options & -3));
                        if (bidi.isInverse() && dirProps[bidiRun2.limit - 1] != (byte) 0) {
                            markFlag |= 2;
                        }
                        if ((markFlag & 2) != 0) {
                            uc = LRM_CHAR;
                        } else if ((markFlag & 8) != 0) {
                            uc = RLM_CHAR;
                        } else {
                            uc = 0;
                        }
                        if (uc != 0) {
                            dest.append(uc);
                        }
                    } else {
                        if (bidi.isInverse() && !bidi.testDirPropFlagAt(MASK_R_AL, bidiRun2.limit - 1)) {
                            markFlag |= 4;
                        }
                        if ((markFlag & 1) != 0) {
                            uc = LRM_CHAR;
                        } else if ((markFlag & 4) != 0) {
                            uc = RLM_CHAR;
                        } else {
                            uc = 0;
                        }
                        if (uc != 0) {
                            dest.append(uc);
                        }
                        dest.append(doWriteReverse(text, bidiRun2.start, bidiRun2.limit, options));
                        if (bidi.isInverse() && (Bidi.DirPropFlag(dirProps[bidiRun2.start]) & MASK_R_AL) == 0) {
                            markFlag |= 8;
                        }
                        if ((markFlag & 2) != 0) {
                            uc = LRM_CHAR;
                        } else if ((markFlag & 8) != 0) {
                            uc = RLM_CHAR;
                        } else {
                            uc = 0;
                        }
                        if (uc != 0) {
                            dest.append(uc);
                        }
                    }
                    run3++;
                }
                run2 = run3;
            }
        } else if ((options & 4) == 0) {
            run2 = runCount;
            while (true) {
                run2--;
                if (run2 < 0) {
                    break;
                }
                bidiRun = bidi.getVisualRun(run2);
                if (bidiRun.isEvenRun()) {
                    dest.append(doWriteReverse(text, bidiRun.start, bidiRun.limit, options & -3));
                } else {
                    dest.append(doWriteForward(text, bidiRun.start, bidiRun.limit, options));
                }
            }
        } else {
            dirProps = bidi.dirProps;
            run = runCount;
            while (true) {
                run--;
                if (run < 0) {
                    break;
                }
                BidiRun bidiRun3 = bidi.getVisualRun(run);
                if (bidiRun3.isEvenRun()) {
                    if (dirProps[bidiRun3.limit - 1] != (byte) 0) {
                        dest.append(LRM_CHAR);
                    }
                    dest.append(doWriteReverse(text, bidiRun3.start, bidiRun3.limit, options & -3));
                    if (dirProps[bidiRun3.start] != (byte) 0) {
                        dest.append(LRM_CHAR);
                    }
                } else {
                    if ((Bidi.DirPropFlag(dirProps[bidiRun3.start]) & MASK_R_AL) == 0) {
                        dest.append(RLM_CHAR);
                    }
                    dest.append(doWriteForward(text, bidiRun3.start, bidiRun3.limit, options));
                    if ((Bidi.DirPropFlag(dirProps[bidiRun3.limit - 1]) & MASK_R_AL) == 0) {
                        dest.append(RLM_CHAR);
                    }
                }
            }
        }
        return dest.toString();
    }
}
