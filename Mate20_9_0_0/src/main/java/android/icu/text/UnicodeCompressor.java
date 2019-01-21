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
        int len = Math.max(4, (3 * (limit - start)) + 1);
        byte[] temp = new byte[len];
        int byteCount = comp.compress(buffer, start, limit, null, temp, 0, len);
        byte[] result = new byte[byteCount];
        System.arraycopy(temp, 0, result, 0, byteCount);
        return result;
    }

    /* JADX WARNING: Missing block: B:40:0x00b4, code skipped:
            r16 = r6;
            r6 = r7;
            r25 = r18;
            r10 = r19;
     */
    /* JADX WARNING: Missing block: B:107:0x0265, code skipped:
            r16 = r5;
            r5 = r7;
     */
    /* JADX WARNING: Missing block: B:127:0x02df, code skipped:
            r16 = r5;
            r5 = r7;
            r10 = r28;
     */
    /* JADX WARNING: Missing block: B:182:0x0439, code skipped:
            r16 = r5;
            r25 = r8;
            r12 = r31;
            r5 = r1;
     */
    /* JADX WARNING: Missing block: B:187:0x0499, code skipped:
            r12 = r31;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int compress(char[] charBuffer, int charBufferStart, int charBufferLimit, int[] charsRead, byte[] byteBuffer, int byteBufferStart, int byteBufferLimit) {
        int bytePos = charBufferLimit;
        byte[] bArr = byteBuffer;
        int i = byteBufferLimit;
        int curUC = -1;
        int curIndex = -1;
        int forwardUC = -1;
        int hiByte = 0;
        int bytePos2 = byteBufferStart;
        int ucPos = charBufferStart;
        if (bArr.length < 4 || i - byteBufferStart < 4) {
            throw new IllegalArgumentException("byteBuffer.length < 4");
        }
        int bytePos3 = bytePos2;
        int ucPos2 = ucPos;
        while (true) {
            int curUC2 = curUC;
            int hiByte2;
            int i2;
            if (ucPos2 >= bytePos || bytePos3 >= i) {
                hiByte2 = hiByte;
                i2 = curIndex;
                ucPos2 = ucPos2;
            } else {
                int ucPos3 = ucPos2;
                int nextUC;
                int loByte;
                int whichWindow;
                int[] iArr;
                int i3;
                switch (this.fMode) {
                    case 0:
                        ucPos2 = ucPos3;
                        while (ucPos2 < bytePos && bytePos3 < i) {
                            curUC = ucPos2 + 1;
                            ucPos2 = charBuffer[ucPos2];
                            if (curUC < bytePos) {
                                nextUC = charBuffer[curUC];
                            } else {
                                nextUC = -1;
                            }
                            i2 = curIndex;
                            if (ucPos2 < 128) {
                                loByte = ucPos2 & 255;
                                if (sSingleTagTable[loByte]) {
                                    if (bytePos3 + 1 >= i) {
                                        curUC--;
                                        break;
                                    }
                                    curIndex = bytePos3 + 1;
                                    bArr[bytePos3] = (byte) 1;
                                    bytePos3 = curIndex;
                                }
                                int bytePos4 = bytePos3 + 1;
                                bArr[bytePos3] = (byte) loByte;
                                curUC2 = ucPos2;
                                ucPos2 = curUC;
                                curIndex = i2;
                                bytePos3 = bytePos4;
                            } else {
                                int forwardUC2;
                                if (!inDynamicWindow(ucPos2, this.fCurrentWindow)) {
                                    forwardUC2 = forwardUC;
                                    if (isCompressible(ucPos2)) {
                                        curIndex = findDynamicWindow(ucPos2);
                                        whichWindow = curIndex;
                                        if (curIndex != -1) {
                                            if (curUC + 1 < bytePos) {
                                                curIndex = charBuffer[curUC + 1];
                                            } else {
                                                curIndex = -1;
                                            }
                                            forwardUC = curIndex;
                                            int forwardUC3;
                                            if (!inDynamicWindow(nextUC, whichWindow) || !inDynamicWindow(forwardUC, whichWindow)) {
                                                forwardUC3 = forwardUC;
                                                if (bytePos3 + 1 >= i) {
                                                    curUC2 = ucPos2;
                                                    ucPos2 = curUC - 1;
                                                    forwardUC = forwardUC3;
                                                    break;
                                                }
                                                curIndex = bytePos3 + 1;
                                                hiByte2 = hiByte;
                                                bArr[bytePos3] = (byte) (1 + whichWindow);
                                                bytePos3 = curIndex + 1;
                                                bArr[curIndex] = (byte) ((ucPos2 - this.fOffsets[whichWindow]) + 128);
                                                curUC2 = ucPos2;
                                                ucPos2 = curUC;
                                                curIndex = i2;
                                                forwardUC = forwardUC3;
                                            } else if (bytePos3 + 1 >= i) {
                                                curUC--;
                                                break;
                                            } else {
                                                curIndex = bytePos3 + 1;
                                                forwardUC3 = forwardUC;
                                                bArr[bytePos3] = (byte) (16 + whichWindow);
                                                int bytePos5 = curIndex + 1;
                                                bArr[curIndex] = (byte) ((ucPos2 - this.fOffsets[whichWindow]) + 128);
                                                bytePos3 = this.fTimeStamps;
                                                curIndex = this.fTimeStamp + 1;
                                                this.fTimeStamp = curIndex;
                                                bytePos3[whichWindow] = curIndex;
                                                this.fCurrentWindow = whichWindow;
                                                curUC2 = ucPos2;
                                                ucPos2 = curUC;
                                                curIndex = i2;
                                                forwardUC = forwardUC3;
                                                bytePos3 = bytePos5;
                                            }
                                        } else {
                                            hiByte2 = hiByte;
                                            curIndex = findStaticWindow(ucPos2);
                                            whichWindow = curIndex;
                                            if (curIndex == -1 || inStaticWindow(nextUC, whichWindow)) {
                                                curIndex = makeIndex(ucPos2);
                                                iArr = this.fIndexCount;
                                                iArr[curIndex] = iArr[curIndex] + 1;
                                                if (curUC + 1 < bytePos) {
                                                    forwardUC = charBuffer[curUC + 1];
                                                } else {
                                                    forwardUC = -1;
                                                }
                                                int ucPos4;
                                                if (this.fIndexCount[curIndex] > 1 || (curIndex == makeIndex(nextUC) && curIndex == makeIndex(forwardUC))) {
                                                    if (bytePos3 + 2 >= i) {
                                                        ucPos4 = -1 + curUC;
                                                        break;
                                                    }
                                                    whichWindow = getLRDefinedWindow();
                                                    bytePos = bytePos3 + 1;
                                                    bArr[bytePos3] = (byte) (24 + whichWindow);
                                                    bytePos3 = bytePos + 1;
                                                    bArr[bytePos] = (byte) curIndex;
                                                    bytePos = bytePos3 + 1;
                                                    bArr[bytePos3] = (byte) ((ucPos2 - sOffsetTable[curIndex]) + 128);
                                                    this.fOffsets[whichWindow] = sOffsetTable[curIndex];
                                                    this.fCurrentWindow = whichWindow;
                                                    int[] iArr2 = this.fTimeStamps;
                                                    hiByte = this.fTimeStamp + 1;
                                                    this.fTimeStamp = hiByte;
                                                    iArr2[whichWindow] = hiByte;
                                                    bytePos3 = bytePos;
                                                    curUC2 = ucPos2;
                                                    ucPos2 = curUC;
                                                    hiByte = hiByte2;
                                                    bytePos = charBufferLimit;
                                                } else if (bytePos3 + 3 >= i) {
                                                    ucPos4 = -1 + curUC;
                                                    break;
                                                } else {
                                                    ucPos4 = bytePos3 + 1;
                                                    bArr[bytePos3] = (byte) 15;
                                                    bytePos3 = ucPos2 >>> 8;
                                                    hiByte = ucPos2 & 255;
                                                    if (sUnicodeTagTable[bytePos3]) {
                                                        loByte = ucPos4 + 1;
                                                        bArr[ucPos4] = (byte) -16;
                                                    } else {
                                                        loByte = ucPos4;
                                                    }
                                                    ucPos4 = loByte + 1;
                                                    bArr[loByte] = (byte) bytePos3;
                                                    bytePos = ucPos4 + 1;
                                                    bArr[ucPos4] = (byte) hiByte;
                                                    this.fMode = 1;
                                                    hiByte = bytePos3;
                                                    bytePos3 = bytePos;
                                                }
                                            } else if (bytePos3 + 1 >= i) {
                                                curUC2 = ucPos2;
                                                ucPos2 = curUC - 1;
                                                forwardUC = forwardUC2;
                                                break;
                                            } else {
                                                curIndex = bytePos3 + 1;
                                                bArr[bytePos3] = (byte) (1 + whichWindow);
                                                bytePos3 = curIndex + 1;
                                                bArr[curIndex] = (byte) (ucPos2 - sOffsets[whichWindow]);
                                                curUC2 = ucPos2;
                                                ucPos2 = curUC;
                                                curIndex = i2;
                                                forwardUC = forwardUC2;
                                            }
                                        }
                                        hiByte = hiByte2;
                                    } else if (nextUC == -1 || !isCompressible(nextUC)) {
                                        if (bytePos3 + 3 >= i) {
                                            curUC--;
                                            break;
                                        }
                                        curIndex = bytePos3 + 1;
                                        bArr[bytePos3] = (byte) 15;
                                        bytePos3 = ucPos2 >>> 8;
                                        forwardUC = ucPos2 & 255;
                                        if (sUnicodeTagTable[bytePos3]) {
                                            hiByte = curIndex + 1;
                                            bArr[curIndex] = (byte) -16;
                                            curIndex = hiByte;
                                        }
                                        hiByte = curIndex + 1;
                                        bArr[curIndex] = (byte) bytePos3;
                                        curIndex = hiByte + 1;
                                        bArr[hiByte] = (byte) forwardUC;
                                        this.fMode = 1;
                                        hiByte = bytePos3;
                                        bytePos3 = curIndex;
                                        loByte = forwardUC;
                                        curIndex = i2;
                                        forwardUC = forwardUC2;
                                    } else if (bytePos3 + 2 >= i) {
                                        curUC--;
                                        break;
                                    } else {
                                        curIndex = bytePos3 + 1;
                                        bArr[bytePos3] = (byte) 14;
                                        bytePos3 = curIndex + 1;
                                        bArr[curIndex] = (byte) (ucPos2 >>> 8);
                                        curIndex = bytePos3 + 1;
                                        bArr[bytePos3] = (byte) (ucPos2 & 255);
                                        curUC2 = ucPos2;
                                        ucPos2 = curUC;
                                        bytePos3 = curIndex;
                                        curIndex = i2;
                                    }
                                    i3 = curUC;
                                    curUC = ucPos2;
                                    ucPos2 = i3;
                                    break;
                                }
                                int bytePos6 = bytePos3 + 1;
                                forwardUC2 = forwardUC;
                                bArr[bytePos3] = (byte) ((ucPos2 - this.fOffsets[this.fCurrentWindow]) + 128);
                                curUC2 = ucPos2;
                                ucPos2 = curUC;
                                curIndex = i2;
                                bytePos3 = bytePos6;
                                forwardUC = forwardUC2;
                            }
                        }
                        curUC = curUC2;
                        curIndex = curIndex;
                        forwardUC = forwardUC;
                        hiByte = hiByte;
                        continue;
                    case 1:
                        curUC = bytePos3;
                        bytePos3 = ucPos3;
                        while (bytePos3 < bytePos && curUC < i) {
                            int curIndex2;
                            int forwardUC4;
                            ucPos2 = bytePos3 + 1;
                            bytePos3 = charBuffer[bytePos3];
                            if (ucPos2 < bytePos) {
                                nextUC = charBuffer[ucPos2];
                            } else {
                                nextUC = -1;
                            }
                            int hiByte3;
                            int nextUC2;
                            if (isCompressible(bytePos3)) {
                                curIndex2 = curIndex;
                                if (nextUC == -1 || isCompressible(nextUC)) {
                                    if (bytePos3 >= 128) {
                                        forwardUC4 = forwardUC;
                                        curIndex = findDynamicWindow(bytePos3);
                                        whichWindow = curIndex;
                                        int[] iArr3;
                                        if (curIndex == -1) {
                                            curIndex = makeIndex(bytePos3);
                                            iArr = this.fIndexCount;
                                            iArr[curIndex] = iArr[curIndex] + 1;
                                            if (ucPos2 + 1 < bytePos) {
                                                forwardUC = charBuffer[ucPos2 + 1];
                                            } else {
                                                forwardUC = -1;
                                            }
                                            int whichWindow2 = whichWindow;
                                            hiByte3 = hiByte;
                                            if (this.fIndexCount[curIndex] > 1) {
                                                nextUC2 = nextUC;
                                            } else if (curIndex == makeIndex(nextUC) && curIndex == makeIndex(forwardUC)) {
                                                nextUC2 = nextUC;
                                            } else if (curUC + 2 >= i) {
                                                ucPos2--;
                                                curUC2 = bytePos3;
                                                bytePos3 = curUC;
                                                i2 = curIndex;
                                                whichWindow = whichWindow2;
                                                hiByte = hiByte3;
                                                break;
                                            } else {
                                                hiByte = bytePos3 >>> 8;
                                                loByte = bytePos3 & 255;
                                                if (sUnicodeTagTable[hiByte]) {
                                                    whichWindow = curUC + 1;
                                                    bArr[curUC] = (byte) -16;
                                                    curUC = whichWindow;
                                                }
                                                whichWindow = curUC + 1;
                                                nextUC2 = nextUC;
                                                bArr[curUC] = (byte) hiByte;
                                                curUC = whichWindow + 1;
                                                bArr[whichWindow] = (byte) loByte;
                                                curUC2 = bytePos3;
                                                whichWindow = whichWindow2;
                                                bytePos3 = ucPos2;
                                            }
                                            if (curUC + 2 < i) {
                                                nextUC = getLRDefinedWindow();
                                                whichWindow = curUC + 1;
                                                bArr[curUC] = (byte) (232 + nextUC);
                                                curUC = whichWindow + 1;
                                                bArr[whichWindow] = (byte) curIndex;
                                                whichWindow = curUC + 1;
                                                int forwardUC5 = forwardUC;
                                                bArr[curUC] = (byte) ((bytePos3 - sOffsetTable[curIndex]) + 128);
                                                this.fOffsets[nextUC] = sOffsetTable[curIndex];
                                                this.fCurrentWindow = nextUC;
                                                iArr3 = this.fTimeStamps;
                                                forwardUC = this.fTimeStamp + 1;
                                                this.fTimeStamp = forwardUC;
                                                iArr3[nextUC] = forwardUC;
                                                this.fMode = 0;
                                                curUC = bytePos3;
                                                bytePos3 = whichWindow;
                                                hiByte = hiByte3;
                                                forwardUC = forwardUC5;
                                                break;
                                            }
                                            ucPos2--;
                                            curUC2 = bytePos3;
                                            bytePos3 = curUC;
                                            i2 = curIndex;
                                            whichWindow = whichWindow2;
                                            break;
                                        } else if (inDynamicWindow(nextUC, whichWindow)) {
                                            if (curUC + 1 < i) {
                                                curIndex = curUC + 1;
                                                bArr[curUC] = (byte) (224 + whichWindow);
                                                int bytePos7 = curIndex + 1;
                                                bArr[curIndex] = (byte) ((bytePos3 - this.fOffsets[whichWindow]) + 128);
                                                iArr3 = this.fTimeStamps;
                                                curIndex = this.fTimeStamp + 1;
                                                this.fTimeStamp = curIndex;
                                                iArr3[whichWindow] = curIndex;
                                                this.fCurrentWindow = whichWindow;
                                                this.fMode = 0;
                                                curUC = bytePos3;
                                                curIndex = curIndex2;
                                                forwardUC = forwardUC4;
                                                bytePos3 = bytePos7;
                                                break;
                                            }
                                            ucPos2--;
                                            break;
                                        } else if (curUC + 2 >= i) {
                                            ucPos2--;
                                            break;
                                        } else {
                                            hiByte = bytePos3 >>> 8;
                                            loByte = bytePos3 & 255;
                                            if (sUnicodeTagTable[hiByte]) {
                                                curIndex = curUC + 1;
                                                bArr[curUC] = (byte) -16;
                                                curUC = curIndex;
                                            }
                                            curIndex = curUC + 1;
                                            bArr[curUC] = (byte) hiByte;
                                            curUC = curIndex + 1;
                                            bArr[curIndex] = (byte) loByte;
                                            curUC2 = bytePos3;
                                            curIndex = curIndex2;
                                            forwardUC = forwardUC4;
                                            bytePos3 = ucPos2;
                                        }
                                    } else {
                                        loByte = bytePos3 & 255;
                                        if (nextUC == -1 || nextUC >= 128 || sSingleTagTable[loByte]) {
                                            forwardUC4 = forwardUC;
                                            if (curUC + 1 >= i) {
                                                ucPos2--;
                                                break;
                                            }
                                            curIndex = curUC + 1;
                                            bArr[curUC] = (byte) 0;
                                            curUC = curIndex + 1;
                                            bArr[curIndex] = (byte) loByte;
                                            curUC2 = bytePos3;
                                            curIndex = curIndex2;
                                            forwardUC = forwardUC4;
                                            bytePos3 = ucPos2;
                                        } else if (curUC + 1 >= i) {
                                            ucPos2--;
                                            curUC2 = bytePos3;
                                            bytePos3 = curUC;
                                            break;
                                        } else {
                                            curIndex = this.fCurrentWindow;
                                            whichWindow = curUC + 1;
                                            forwardUC4 = forwardUC;
                                            bArr[curUC] = (byte) (224 + curIndex);
                                            curUC = whichWindow + 1;
                                            bArr[whichWindow] = (byte) loByte;
                                            iArr = this.fTimeStamps;
                                            whichWindow = this.fTimeStamp + 1;
                                            this.fTimeStamp = whichWindow;
                                            iArr[curIndex] = whichWindow;
                                            this.fMode = 0;
                                            curIndex = curIndex2;
                                            forwardUC = forwardUC4;
                                            i3 = curUC;
                                            curUC = bytePos3;
                                            bytePos3 = i3;
                                            continue;
                                        }
                                    }
                                } else {
                                    nextUC2 = nextUC;
                                    forwardUC4 = forwardUC;
                                    hiByte3 = hiByte;
                                }
                            } else {
                                curIndex2 = curIndex;
                                nextUC2 = nextUC;
                                forwardUC4 = forwardUC;
                                hiByte3 = hiByte;
                            }
                            if (curUC + 2 >= i) {
                                ucPos2--;
                                curUC2 = bytePos3;
                                bytePos3 = curUC;
                                i2 = curIndex2;
                                forwardUC = forwardUC4;
                                break;
                            }
                            hiByte = bytePos3 >>> 8;
                            loByte = bytePos3 & 255;
                            if (sUnicodeTagTable[hiByte]) {
                                curIndex = curUC + 1;
                                bArr[curUC] = (byte) -16;
                                curUC = curIndex;
                            }
                            curIndex = curUC + 1;
                            bArr[curUC] = (byte) hiByte;
                            curUC = curIndex + 1;
                            bArr[curIndex] = (byte) loByte;
                            curUC2 = bytePos3;
                            curIndex = curIndex2;
                            forwardUC = forwardUC4;
                            bytePos3 = ucPos2;
                        }
                        ucPos2 = bytePos3;
                        bytePos3 = curUC;
                        curUC = curUC2;
                        curIndex = curIndex;
                        forwardUC = forwardUC;
                        hiByte = hiByte;
                        break;
                    default:
                        curUC = curUC2;
                        ucPos2 = ucPos3;
                        continue;
                }
            }
            bytePos = charBufferLimit;
        }
        if (charsRead != null) {
            charsRead[0] = ucPos2 - charBufferStart;
        }
        return bytePos3 - byteBufferStart;
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
        this.fOffsets[7] = 65280;
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
        return c >= this.fOffsets[whichWindow] && c < this.fOffsets[whichWindow] + 128;
    }

    private static boolean inStaticWindow(int c, int whichWindow) {
        return c >= sOffsets[whichWindow] && c < sOffsets[whichWindow] + 128;
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
