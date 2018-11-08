package android.icu.text;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Normalizer2Impl.Hangul;

public final class UnicodeCompressor implements SCSU {
    private static boolean[] sSingleTagTable = new boolean[]{false, true, true, true, true, true, true, true, true, false, false, true, true, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private static boolean[] sUnicodeTagTable = new boolean[]{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private int fCurrentWindow = 0;
    private int[] fIndexCount = new int[256];
    private int fMode = 0;
    private int[] fOffsets = new int[8];
    private int fTimeStamp = 0;
    private int[] fTimeStamps = new int[8];

    public UnicodeCompressor() {
        reset();
    }

    public static byte[] compress(String buffer) {
        return compress(buffer.toCharArray(), 0, buffer.length());
    }

    public static byte[] compress(char[] buffer, int start, int limit) {
        UnicodeCompressor comp = new UnicodeCompressor();
        int len = Math.max(4, ((limit - start) * 3) + 1);
        byte[] temp = new byte[len];
        int byteCount = comp.compress(buffer, start, limit, null, temp, 0, len);
        byte[] result = new byte[byteCount];
        System.arraycopy(temp, 0, result, 0, byteCount);
        return result;
    }

    public int compress(char[] charBuffer, int charBufferStart, int charBufferLimit, int[] charsRead, byte[] byteBuffer, int byteBufferStart, int byteBufferLimit) {
        int bytePos = byteBufferStart;
        int ucPos = charBufferStart;
        if (byteBuffer.length < 4 || byteBufferLimit - byteBufferStart < 4) {
            throw new IllegalArgumentException("byteBuffer.length < 4");
        }
        while (ucPos < charBufferLimit && bytePos < byteBufferLimit) {
            int ucPos2;
            int bytePos2;
            int curUC;
            int nextUC;
            int loByte;
            int whichWindow;
            int forwardUC;
            int[] iArr;
            int i;
            int curIndex;
            int hiByte;
            switch (this.fMode) {
                case 0:
                    ucPos2 = ucPos;
                    bytePos2 = bytePos;
                    while (ucPos2 < charBufferLimit && bytePos2 < byteBufferLimit) {
                        ucPos = ucPos2 + 1;
                        curUC = charBuffer[ucPos2];
                        if (ucPos < charBufferLimit) {
                            nextUC = charBuffer[ucPos];
                        } else {
                            nextUC = -1;
                        }
                        if (curUC < 128) {
                            loByte = curUC & 255;
                            if (!sSingleTagTable[loByte]) {
                                bytePos = bytePos2;
                            } else if (bytePos2 + 1 >= byteBufferLimit) {
                                ucPos--;
                                bytePos = bytePos2;
                                break;
                            } else {
                                bytePos = bytePos2 + 1;
                                byteBuffer[bytePos2] = (byte) 1;
                            }
                            bytePos2 = bytePos + 1;
                            byteBuffer[bytePos] = (byte) loByte;
                            bytePos = bytePos2;
                        } else if (inDynamicWindow(curUC, this.fCurrentWindow)) {
                            bytePos = bytePos2 + 1;
                            byteBuffer[bytePos2] = (byte) ((curUC - this.fOffsets[this.fCurrentWindow]) + 128);
                        } else if (isCompressible(curUC)) {
                            whichWindow = findDynamicWindow(curUC);
                            if (whichWindow != -1) {
                                if (ucPos + 1 < charBufferLimit) {
                                    forwardUC = charBuffer[ucPos + 1];
                                } else {
                                    forwardUC = -1;
                                }
                                if (inDynamicWindow(nextUC, whichWindow) && inDynamicWindow(forwardUC, whichWindow)) {
                                    if (bytePos2 + 1 >= byteBufferLimit) {
                                        ucPos--;
                                        bytePos = bytePos2;
                                        break;
                                    }
                                    bytePos = bytePos2 + 1;
                                    byteBuffer[bytePos2] = (byte) (whichWindow + 16);
                                    bytePos2 = bytePos + 1;
                                    byteBuffer[bytePos] = (byte) ((curUC - this.fOffsets[whichWindow]) + 128);
                                    iArr = this.fTimeStamps;
                                    i = this.fTimeStamp + 1;
                                    this.fTimeStamp = i;
                                    iArr[whichWindow] = i;
                                    this.fCurrentWindow = whichWindow;
                                    bytePos = bytePos2;
                                } else if (bytePos2 + 1 >= byteBufferLimit) {
                                    ucPos--;
                                    bytePos = bytePos2;
                                    break;
                                } else {
                                    bytePos = bytePos2 + 1;
                                    byteBuffer[bytePos2] = (byte) (whichWindow + 1);
                                    bytePos2 = bytePos + 1;
                                    byteBuffer[bytePos] = (byte) ((curUC - this.fOffsets[whichWindow]) + 128);
                                    bytePos = bytePos2;
                                }
                            } else {
                                whichWindow = findStaticWindow(curUC);
                                if (whichWindow == -1 || (inStaticWindow(nextUC, whichWindow) ^ 1) == 0) {
                                    curIndex = makeIndex(curUC);
                                    iArr = this.fIndexCount;
                                    iArr[curIndex] = iArr[curIndex] + 1;
                                    if (ucPos + 1 < charBufferLimit) {
                                        forwardUC = charBuffer[ucPos + 1];
                                    } else {
                                        forwardUC = -1;
                                    }
                                    if (this.fIndexCount[curIndex] <= 1 && (curIndex != makeIndex(nextUC) || curIndex != makeIndex(forwardUC))) {
                                        if (bytePos2 + 3 < byteBufferLimit) {
                                            bytePos = bytePos2 + 1;
                                            byteBuffer[bytePos2] = (byte) 15;
                                            hiByte = curUC >>> 8;
                                            loByte = curUC & 255;
                                            if (sUnicodeTagTable[hiByte]) {
                                                bytePos2 = bytePos + 1;
                                                byteBuffer[bytePos] = (byte) -16;
                                                bytePos = bytePos2;
                                            }
                                            bytePos2 = bytePos + 1;
                                            byteBuffer[bytePos] = (byte) hiByte;
                                            bytePos = bytePos2 + 1;
                                            byteBuffer[bytePos2] = (byte) loByte;
                                            this.fMode = 1;
                                            break;
                                        }
                                        ucPos--;
                                        bytePos = bytePos2;
                                        break;
                                    } else if (bytePos2 + 2 >= byteBufferLimit) {
                                        ucPos--;
                                        bytePos = bytePos2;
                                        break;
                                    } else {
                                        whichWindow = getLRDefinedWindow();
                                        bytePos = bytePos2 + 1;
                                        byteBuffer[bytePos2] = (byte) (whichWindow + 24);
                                        bytePos2 = bytePos + 1;
                                        byteBuffer[bytePos] = (byte) curIndex;
                                        bytePos = bytePos2 + 1;
                                        byteBuffer[bytePos2] = (byte) ((curUC - sOffsetTable[curIndex]) + 128);
                                        this.fOffsets[whichWindow] = sOffsetTable[curIndex];
                                        this.fCurrentWindow = whichWindow;
                                        iArr = this.fTimeStamps;
                                        i = this.fTimeStamp + 1;
                                        this.fTimeStamp = i;
                                        iArr[whichWindow] = i;
                                    }
                                } else if (bytePos2 + 1 >= byteBufferLimit) {
                                    ucPos--;
                                    bytePos = bytePos2;
                                    break;
                                } else {
                                    bytePos = bytePos2 + 1;
                                    byteBuffer[bytePos2] = (byte) (whichWindow + 1);
                                    bytePos2 = bytePos + 1;
                                    byteBuffer[bytePos] = (byte) (curUC - sOffsets[whichWindow]);
                                    bytePos = bytePos2;
                                }
                            }
                        } else if (nextUC == -1 || !isCompressible(nextUC)) {
                            if (bytePos2 + 3 < byteBufferLimit) {
                                bytePos = bytePos2 + 1;
                                byteBuffer[bytePos2] = (byte) 15;
                                hiByte = curUC >>> 8;
                                loByte = curUC & 255;
                                if (sUnicodeTagTable[hiByte]) {
                                    bytePos2 = bytePos + 1;
                                    byteBuffer[bytePos] = (byte) -16;
                                    bytePos = bytePos2;
                                }
                                bytePos2 = bytePos + 1;
                                byteBuffer[bytePos] = (byte) hiByte;
                                bytePos = bytePos2 + 1;
                                byteBuffer[bytePos2] = (byte) loByte;
                                this.fMode = 1;
                                break;
                            }
                            ucPos--;
                            bytePos = bytePos2;
                            break;
                        } else if (bytePos2 + 2 >= byteBufferLimit) {
                            ucPos--;
                            bytePos = bytePos2;
                            break;
                        } else {
                            bytePos = bytePos2 + 1;
                            byteBuffer[bytePos2] = (byte) 14;
                            bytePos2 = bytePos + 1;
                            byteBuffer[bytePos] = (byte) (curUC >>> 8);
                            bytePos = bytePos2 + 1;
                            byteBuffer[bytePos2] = (byte) (curUC & 255);
                        }
                        ucPos2 = ucPos;
                        bytePos2 = bytePos;
                    }
                    ucPos = ucPos2;
                    bytePos = bytePos2;
                    continue;
                case 1:
                    ucPos2 = ucPos;
                    bytePos2 = bytePos;
                    while (ucPos2 < charBufferLimit && bytePos2 < byteBufferLimit) {
                        ucPos = ucPos2 + 1;
                        curUC = charBuffer[ucPos2];
                        if (ucPos < charBufferLimit) {
                            nextUC = charBuffer[ucPos];
                        } else {
                            nextUC = -1;
                        }
                        if (isCompressible(curUC) && (nextUC == -1 || (isCompressible(nextUC) ^ 1) == 0)) {
                            if (curUC < 128) {
                                loByte = curUC & 255;
                                if (nextUC != -1 && nextUC < 128 && (sSingleTagTable[loByte] ^ 1) != 0) {
                                    if (bytePos2 + 1 < byteBufferLimit) {
                                        whichWindow = this.fCurrentWindow;
                                        bytePos = bytePos2 + 1;
                                        byteBuffer[bytePos2] = (byte) (whichWindow + 224);
                                        bytePos2 = bytePos + 1;
                                        byteBuffer[bytePos] = (byte) loByte;
                                        iArr = this.fTimeStamps;
                                        i = this.fTimeStamp + 1;
                                        this.fTimeStamp = i;
                                        iArr[whichWindow] = i;
                                        this.fMode = 0;
                                        bytePos = bytePos2;
                                        break;
                                    }
                                    ucPos--;
                                    bytePos = bytePos2;
                                    break;
                                } else if (bytePos2 + 1 >= byteBufferLimit) {
                                    ucPos--;
                                    bytePos = bytePos2;
                                    break;
                                } else {
                                    bytePos = bytePos2 + 1;
                                    byteBuffer[bytePos2] = (byte) 0;
                                    bytePos2 = bytePos + 1;
                                    byteBuffer[bytePos] = (byte) loByte;
                                    bytePos = bytePos2;
                                }
                            } else {
                                whichWindow = findDynamicWindow(curUC);
                                if (whichWindow == -1) {
                                    curIndex = makeIndex(curUC);
                                    iArr = this.fIndexCount;
                                    iArr[curIndex] = iArr[curIndex] + 1;
                                    if (ucPos + 1 < charBufferLimit) {
                                        forwardUC = charBuffer[ucPos + 1];
                                    } else {
                                        forwardUC = -1;
                                    }
                                    if (this.fIndexCount[curIndex] > 1 || (curIndex == makeIndex(nextUC) && curIndex == makeIndex(forwardUC))) {
                                        if (bytePos2 + 2 < byteBufferLimit) {
                                            whichWindow = getLRDefinedWindow();
                                            bytePos = bytePos2 + 1;
                                            byteBuffer[bytePos2] = (byte) (whichWindow + 232);
                                            bytePos2 = bytePos + 1;
                                            byteBuffer[bytePos] = (byte) curIndex;
                                            bytePos = bytePos2 + 1;
                                            byteBuffer[bytePos2] = (byte) ((curUC - sOffsetTable[curIndex]) + 128);
                                            this.fOffsets[whichWindow] = sOffsetTable[curIndex];
                                            this.fCurrentWindow = whichWindow;
                                            iArr = this.fTimeStamps;
                                            i = this.fTimeStamp + 1;
                                            this.fTimeStamp = i;
                                            iArr[whichWindow] = i;
                                            this.fMode = 0;
                                            break;
                                        }
                                        ucPos--;
                                        bytePos = bytePos2;
                                        break;
                                    } else if (bytePos2 + 2 >= byteBufferLimit) {
                                        ucPos--;
                                        bytePos = bytePos2;
                                        break;
                                    } else {
                                        hiByte = curUC >>> 8;
                                        loByte = curUC & 255;
                                        if (sUnicodeTagTable[hiByte]) {
                                            bytePos = bytePos2 + 1;
                                            byteBuffer[bytePos2] = (byte) -16;
                                        } else {
                                            bytePos = bytePos2;
                                        }
                                        bytePos2 = bytePos + 1;
                                        byteBuffer[bytePos] = (byte) hiByte;
                                        bytePos = bytePos2 + 1;
                                        byteBuffer[bytePos2] = (byte) loByte;
                                    }
                                } else if (inDynamicWindow(nextUC, whichWindow)) {
                                    if (bytePos2 + 1 < byteBufferLimit) {
                                        bytePos = bytePos2 + 1;
                                        byteBuffer[bytePos2] = (byte) (whichWindow + 224);
                                        bytePos2 = bytePos + 1;
                                        byteBuffer[bytePos] = (byte) ((curUC - this.fOffsets[whichWindow]) + 128);
                                        iArr = this.fTimeStamps;
                                        i = this.fTimeStamp + 1;
                                        this.fTimeStamp = i;
                                        iArr[whichWindow] = i;
                                        this.fCurrentWindow = whichWindow;
                                        this.fMode = 0;
                                        bytePos = bytePos2;
                                        break;
                                    }
                                    ucPos--;
                                    bytePos = bytePos2;
                                    break;
                                } else if (bytePos2 + 2 >= byteBufferLimit) {
                                    ucPos--;
                                    bytePos = bytePos2;
                                    break;
                                } else {
                                    hiByte = curUC >>> 8;
                                    loByte = curUC & 255;
                                    if (sUnicodeTagTable[hiByte]) {
                                        bytePos = bytePos2 + 1;
                                        byteBuffer[bytePos2] = (byte) -16;
                                    } else {
                                        bytePos = bytePos2;
                                    }
                                    bytePos2 = bytePos + 1;
                                    byteBuffer[bytePos] = (byte) hiByte;
                                    bytePos = bytePos2 + 1;
                                    byteBuffer[bytePos2] = (byte) loByte;
                                }
                            }
                        } else if (bytePos2 + 2 >= byteBufferLimit) {
                            ucPos--;
                            bytePos = bytePos2;
                            break;
                        } else {
                            hiByte = curUC >>> 8;
                            loByte = curUC & 255;
                            if (sUnicodeTagTable[hiByte]) {
                                bytePos = bytePos2 + 1;
                                byteBuffer[bytePos2] = (byte) -16;
                            } else {
                                bytePos = bytePos2;
                            }
                            bytePos2 = bytePos + 1;
                            byteBuffer[bytePos] = (byte) hiByte;
                            bytePos = bytePos2 + 1;
                            byteBuffer[bytePos2] = (byte) loByte;
                        }
                        ucPos2 = ucPos;
                        bytePos2 = bytePos;
                    }
                    ucPos = ucPos2;
                    bytePos = bytePos2;
                    continue;
                default:
                    continue;
            }
        }
        if (charsRead != null) {
            charsRead[0] = ucPos - charBufferStart;
        }
        return bytePos - byteBufferStart;
    }

    public void reset() {
        int i;
        this.fOffsets[0] = 128;
        this.fOffsets[1] = 192;
        this.fOffsets[2] = 1024;
        this.fOffsets[3] = 1536;
        this.fOffsets[4] = 2304;
        this.fOffsets[5] = 12352;
        this.fOffsets[6] = 12448;
        this.fOffsets[7] = Normalizer2Impl.JAMO_VT;
        for (i = 0; i < 8; i++) {
            this.fTimeStamps[i] = 0;
        }
        for (i = 0; i <= 255; i++) {
            this.fIndexCount[i] = 0;
        }
        this.fTimeStamp = 0;
        this.fCurrentWindow = 0;
        this.fMode = 0;
    }

    private static int makeIndex(int c) {
        if (c >= 192 && c < 320) {
            return 249;
        }
        if (c >= 592 && c < 720) {
            return 250;
        }
        if (c >= 880 && c < 1008) {
            return 251;
        }
        if (c >= 1328 && c < 1424) {
            return 252;
        }
        if (c >= 12352 && c < 12448) {
            return 253;
        }
        if (c >= 12448 && c < 12576) {
            return 254;
        }
        if (c >= 65376 && c < 65439) {
            return 255;
        }
        if (c >= 128 && c < Normalizer2Impl.COMP_1_TRAIL_LIMIT) {
            return (c / 128) & 255;
        }
        if (c < 57344 || c > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            return 0;
        }
        return ((c - Hangul.HANGUL_BASE) / 128) & 255;
    }

    private boolean inDynamicWindow(int c, int whichWindow) {
        if (c < this.fOffsets[whichWindow] || c >= this.fOffsets[whichWindow] + 128) {
            return false;
        }
        return true;
    }

    private static boolean inStaticWindow(int c, int whichWindow) {
        if (c < sOffsets[whichWindow] || c >= sOffsets[whichWindow] + 128) {
            return false;
        }
        return true;
    }

    private static boolean isCompressible(int c) {
        return c < Normalizer2Impl.COMP_1_TRAIL_LIMIT || c >= 57344;
    }

    private int findDynamicWindow(int c) {
        for (int i = 7; i >= 0; i--) {
            if (inDynamicWindow(c, i)) {
                int[] iArr = this.fTimeStamps;
                iArr[i] = iArr[i] + 1;
                return i;
            }
        }
        return -1;
    }

    private static int findStaticWindow(int c) {
        for (int i = 7; i >= 0; i--) {
            if (inStaticWindow(c, i)) {
                return i;
            }
        }
        return -1;
    }

    private int getLRDefinedWindow() {
        int leastRU = Integer.MAX_VALUE;
        int whichWindow = -1;
        for (int i = 7; i >= 0; i--) {
            if (this.fTimeStamps[i] < leastRU) {
                leastRU = this.fTimeStamps[i];
                whichWindow = i;
            }
        }
        return whichWindow;
    }
}
