package android.icu.text;

import android.icu.impl.Normalizer2Impl;
import dalvik.bytecode.Opcodes;

public final class UnicodeDecompressor implements SCSU {
    private static final int BUFSIZE = 3;
    private byte[] fBuffer = new byte[3];
    private int fBufferLength = 0;
    private int fCurrentWindow = 0;
    private int fMode = 0;
    private int[] fOffsets = new int[8];

    public UnicodeDecompressor() {
        reset();
    }

    public static String decompress(byte[] buffer) {
        return new String(decompress(buffer, 0, buffer.length));
    }

    public static char[] decompress(byte[] buffer, int start, int limit) {
        UnicodeDecompressor comp = new UnicodeDecompressor();
        int len = Math.max(2, (limit - start) * 2);
        char[] temp = new char[len];
        int charCount = comp.decompress(buffer, start, limit, null, temp, 0, len);
        char[] result = new char[charCount];
        System.arraycopy(temp, 0, result, 0, charCount);
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int decompress(byte[] byteBuffer, int byteBufferStart, int byteBufferLimit, int[] bytesRead, char[] charBuffer, int charBufferStart, int charBufferLimit) {
        int bytePos = byteBufferStart;
        int ucPos = charBufferStart;
        if (charBuffer.length < 2 || charBufferLimit - charBufferStart < 2) {
            throw new IllegalArgumentException("charBuffer.length < 2");
        }
        if (this.fBufferLength > 0) {
            int newBytes = 0;
            if (this.fBufferLength != 3) {
                newBytes = this.fBuffer.length - this.fBufferLength;
                if (byteBufferLimit - byteBufferStart < newBytes) {
                    newBytes = byteBufferLimit - byteBufferStart;
                }
                System.arraycopy(byteBuffer, byteBufferStart, this.fBuffer, this.fBufferLength, newBytes);
            }
            this.fBufferLength = 0;
            ucPos = charBufferStart + decompress(this.fBuffer, 0, this.fBuffer.length, null, charBuffer, charBufferStart, charBufferLimit);
            bytePos = byteBufferStart + newBytes;
        }
        while (bytePos < byteBufferLimit && ucPos < charBufferLimit) {
            int ucPos2;
            int bytePos2;
            int aByte;
            switch (this.fMode) {
                case 0:
                    ucPos2 = ucPos;
                    bytePos2 = bytePos;
                    while (bytePos2 < byteBufferLimit && ucPos2 < charBufferLimit) {
                        bytePos = bytePos2 + 1;
                        aByte = byteBuffer[bytePos2] & 255;
                        switch (aByte) {
                            case 0:
                            case 9:
                            case 10:
                            case 13:
                            case 32:
                            case 33:
                            case 34:
                            case 35:
                            case 36:
                            case 37:
                            case 38:
                            case 39:
                            case 40:
                            case 41:
                            case 42:
                            case 43:
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                            case 48:
                            case 49:
                            case 50:
                            case 51:
                            case 52:
                            case 53:
                            case 54:
                            case 55:
                            case 56:
                            case 57:
                            case 58:
                            case 59:
                            case 60:
                            case 61:
                            case 62:
                            case 63:
                            case 64:
                            case 65:
                            case 66:
                            case 67:
                            case 68:
                            case 69:
                            case 70:
                            case 71:
                            case 72:
                            case 73:
                            case 74:
                            case 75:
                            case 76:
                            case 77:
                            case 78:
                            case 79:
                            case 80:
                            case 81:
                            case 82:
                            case 83:
                            case 84:
                            case 85:
                            case 86:
                            case 87:
                            case 88:
                            case 89:
                            case 90:
                            case 91:
                            case 92:
                            case 93:
                            case 94:
                            case 95:
                            case 96:
                            case 97:
                            case 98:
                            case 99:
                            case 100:
                            case 101:
                            case 102:
                            case 103:
                            case 104:
                            case 105:
                            case 106:
                            case 107:
                            case 108:
                            case 109:
                            case 110:
                            case 111:
                            case 112:
                            case 113:
                            case 114:
                            case 115:
                            case 116:
                            case 117:
                            case 118:
                            case 119:
                            case 120:
                            case 121:
                            case 122:
                            case 123:
                            case 124:
                            case 125:
                            case 126:
                            case 127:
                                ucPos = ucPos2 + 1;
                                charBuffer[ucPos2] = (char) aByte;
                                break;
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                                if (bytePos < byteBufferLimit) {
                                    int i;
                                    bytePos2 = bytePos + 1;
                                    int dByte = byteBuffer[bytePos] & 255;
                                    ucPos = ucPos2 + 1;
                                    if (dByte < 0 || dByte >= 128) {
                                        i = this.fOffsets[aByte - 1] - 128;
                                    } else {
                                        i = sOffsets[aByte - 1];
                                    }
                                    charBuffer[ucPos2] = (char) (i + dByte);
                                    bytePos = bytePos2;
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                            case 11:
                                if (bytePos + 1 < byteBufferLimit) {
                                    bytePos2 = bytePos + 1;
                                    aByte = byteBuffer[bytePos] & 255;
                                    this.fCurrentWindow = (aByte & 224) >> 5;
                                    bytePos = bytePos2 + 1;
                                    this.fOffsets[this.fCurrentWindow] = ((((aByte & 31) << 8) | (byteBuffer[bytePos2] & 255)) * 128) + 65536;
                                    ucPos = ucPos2;
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                            case 12:
                                ucPos = ucPos2;
                                break;
                            case 14:
                                if (bytePos + 1 < byteBufferLimit) {
                                    bytePos2 = bytePos + 1;
                                    ucPos = ucPos2 + 1;
                                    bytePos = bytePos2 + 1;
                                    charBuffer[ucPos2] = (char) ((byteBuffer[bytePos] << 8) | (byteBuffer[bytePos2] & 255));
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                            case 15:
                                this.fMode = 1;
                                break;
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                            case 21:
                            case 22:
                            case 23:
                                this.fCurrentWindow = aByte - 16;
                                ucPos = ucPos2;
                                break;
                            case 24:
                            case 25:
                            case 26:
                            case 27:
                            case 28:
                            case 29:
                            case 30:
                            case 31:
                                if (bytePos < byteBufferLimit) {
                                    this.fCurrentWindow = aByte - 24;
                                    bytePos2 = bytePos + 1;
                                    this.fOffsets[this.fCurrentWindow] = sOffsetTable[byteBuffer[bytePos] & 255];
                                    ucPos = ucPos2;
                                    bytePos = bytePos2;
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                            case 128:
                            case 129:
                            case 130:
                            case 131:
                            case 132:
                            case 133:
                            case 134:
                            case 135:
                            case 136:
                            case 137:
                            case 138:
                            case 139:
                            case 140:
                            case 141:
                            case 142:
                            case 143:
                            case 144:
                            case 145:
                            case 146:
                            case 147:
                            case 148:
                            case 149:
                            case 150:
                            case 151:
                            case 152:
                            case 153:
                            case 154:
                            case 155:
                            case 156:
                            case 157:
                            case 158:
                            case 159:
                            case 160:
                            case 161:
                            case 162:
                            case 163:
                            case 164:
                            case 165:
                            case 166:
                            case 167:
                            case 168:
                            case 169:
                            case 170:
                            case 171:
                            case 172:
                            case 173:
                            case 174:
                            case 175:
                            case 176:
                            case 177:
                            case 178:
                            case 179:
                            case 180:
                            case 181:
                            case 182:
                            case 183:
                            case 184:
                            case 185:
                            case 186:
                            case 187:
                            case 188:
                            case 189:
                            case 190:
                            case 191:
                            case 192:
                            case 193:
                            case 194:
                            case 195:
                            case 196:
                            case 197:
                            case 198:
                            case 199:
                            case 200:
                            case 201:
                            case 202:
                            case 203:
                            case 204:
                            case 205:
                            case 206:
                            case 207:
                            case 208:
                            case 209:
                            case 210:
                            case 211:
                            case 212:
                            case 213:
                            case 214:
                            case 215:
                            case 216:
                            case 217:
                            case 218:
                            case 219:
                            case 220:
                            case 221:
                            case 222:
                            case 223:
                            case 224:
                            case 225:
                            case 226:
                            case 227:
                            case 228:
                            case 229:
                            case 230:
                            case 231:
                            case 232:
                            case 233:
                            case 234:
                            case 235:
                            case 236:
                            case 237:
                            case 238:
                            case 239:
                            case 240:
                            case 241:
                            case 242:
                            case 243:
                            case 244:
                            case 245:
                            case 246:
                            case 247:
                            case 248:
                            case 249:
                            case 250:
                            case 251:
                            case 252:
                            case 253:
                            case 254:
                            case 255:
                                if (this.fOffsets[this.fCurrentWindow] > 65535) {
                                    if (ucPos2 + 1 < charBufferLimit) {
                                        int normalizedBase = this.fOffsets[this.fCurrentWindow] - 65536;
                                        ucPos = ucPos2 + 1;
                                        charBuffer[ucPos2] = (char) ((normalizedBase >> 10) + 55296);
                                        ucPos2 = ucPos + 1;
                                        charBuffer[ucPos] = (char) (((normalizedBase & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE) + (aByte & 127));
                                        ucPos = ucPos2;
                                        break;
                                    }
                                    bytePos--;
                                    System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                    this.fBufferLength = byteBufferLimit - bytePos;
                                    bytePos += this.fBufferLength;
                                    ucPos = ucPos2;
                                    break;
                                }
                                ucPos = ucPos2 + 1;
                                charBuffer[ucPos2] = (char) ((this.fOffsets[this.fCurrentWindow] + aByte) - 128);
                                break;
                            default:
                                ucPos = ucPos2;
                                break;
                        }
                    }
                    bytePos = bytePos2;
                    ucPos = ucPos2;
                    continue;
                case 1:
                    ucPos2 = ucPos;
                    bytePos2 = bytePos;
                    while (bytePos2 < byteBufferLimit && ucPos2 < charBufferLimit) {
                        bytePos = bytePos2 + 1;
                        aByte = byteBuffer[bytePos2] & 255;
                        switch (aByte) {
                            case 224:
                            case 225:
                            case 226:
                            case 227:
                            case 228:
                            case 229:
                            case 230:
                            case 231:
                                this.fCurrentWindow = aByte - 224;
                                this.fMode = 0;
                                break;
                            case 232:
                            case 233:
                            case 234:
                            case 235:
                            case 236:
                            case 237:
                            case 238:
                            case 239:
                                if (bytePos < byteBufferLimit) {
                                    this.fCurrentWindow = aByte - 232;
                                    bytePos2 = bytePos + 1;
                                    this.fOffsets[this.fCurrentWindow] = sOffsetTable[byteBuffer[bytePos] & 255];
                                    this.fMode = 0;
                                    bytePos = bytePos2;
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                            case 240:
                                if (bytePos < byteBufferLimit - 1) {
                                    bytePos2 = bytePos + 1;
                                    ucPos = ucPos2 + 1;
                                    bytePos = bytePos2 + 1;
                                    charBuffer[ucPos2] = (char) ((byteBuffer[bytePos] << 8) | (byteBuffer[bytePos2] & 255));
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                            case 241:
                                if (bytePos + 1 < byteBufferLimit) {
                                    bytePos2 = bytePos + 1;
                                    aByte = byteBuffer[bytePos] & 255;
                                    this.fCurrentWindow = (aByte & 224) >> 5;
                                    bytePos = bytePos2 + 1;
                                    this.fOffsets[this.fCurrentWindow] = ((((aByte & 31) << 8) | (byteBuffer[bytePos2] & 255)) * 128) + 65536;
                                    this.fMode = 0;
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                            default:
                                if (bytePos < byteBufferLimit) {
                                    ucPos = ucPos2 + 1;
                                    bytePos2 = bytePos + 1;
                                    charBuffer[ucPos2] = (char) ((aByte << 8) | (byteBuffer[bytePos] & 255));
                                    bytePos = bytePos2;
                                    break;
                                }
                                bytePos--;
                                System.arraycopy(byteBuffer, bytePos, this.fBuffer, 0, byteBufferLimit - bytePos);
                                this.fBufferLength = byteBufferLimit - bytePos;
                                bytePos += this.fBufferLength;
                                ucPos = ucPos2;
                                break;
                        }
                    }
                    bytePos = bytePos2;
                    ucPos = ucPos2;
                    continue;
                default:
                    continue;
            }
        }
        if (bytesRead != null) {
            bytesRead[0] = bytePos - byteBufferStart;
        }
        return ucPos - charBufferStart;
    }

    public void reset() {
        this.fOffsets[0] = 128;
        this.fOffsets[1] = 192;
        this.fOffsets[2] = 1024;
        this.fOffsets[3] = 1536;
        this.fOffsets[4] = 2304;
        this.fOffsets[5] = 12352;
        this.fOffsets[6] = 12448;
        this.fOffsets[7] = Normalizer2Impl.JAMO_VT;
        this.fCurrentWindow = 0;
        this.fMode = 0;
        this.fBufferLength = 0;
    }
}
