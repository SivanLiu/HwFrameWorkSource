package java.nio.charset;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

public abstract class CharsetEncoder {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int ST_CODING = 1;
    private static final int ST_END = 2;
    private static final int ST_FLUSHED = 3;
    private static final int ST_RESET = 0;
    private static String[] stateNames = new String[]{"RESET", "CODING", "CODING_END", "FLUSHED"};
    private final float averageBytesPerChar;
    private WeakReference<CharsetDecoder> cachedDecoder;
    private final Charset charset;
    private CodingErrorAction malformedInputAction;
    private final float maxBytesPerChar;
    private byte[] replacement;
    private int state;
    private CodingErrorAction unmappableCharacterAction;

    protected abstract CoderResult encodeLoop(CharBuffer charBuffer, ByteBuffer byteBuffer);

    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement) {
        this(cs, averageBytesPerChar, maxBytesPerChar, replacement, $assertionsDisabled);
    }

    CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement, boolean trusted) {
        this.malformedInputAction = CodingErrorAction.REPORT;
        this.unmappableCharacterAction = CodingErrorAction.REPORT;
        this.state = 0;
        this.cachedDecoder = null;
        this.charset = cs;
        if (averageBytesPerChar <= 0.0f) {
            throw new IllegalArgumentException("Non-positive averageBytesPerChar");
        } else if (maxBytesPerChar <= 0.0f) {
            throw new IllegalArgumentException("Non-positive maxBytesPerChar");
        } else if (Charset.atBugLevel("1.4") || averageBytesPerChar <= maxBytesPerChar) {
            this.replacement = replacement;
            this.averageBytesPerChar = averageBytesPerChar;
            this.maxBytesPerChar = maxBytesPerChar;
            if (!trusted) {
                replaceWith(replacement);
            }
        } else {
            throw new IllegalArgumentException("averageBytesPerChar exceeds maxBytesPerChar");
        }
    }

    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar) {
        this(cs, averageBytesPerChar, maxBytesPerChar, new byte[]{(byte) 63});
    }

    public final Charset charset() {
        return this.charset;
    }

    public final byte[] replacement() {
        return Arrays.copyOf(this.replacement, this.replacement.length);
    }

    public final CharsetEncoder replaceWith(byte[] newReplacement) {
        if (newReplacement != null) {
            int len = newReplacement.length;
            if (len == 0) {
                throw new IllegalArgumentException("Empty replacement");
            } else if (((float) len) > this.maxBytesPerChar) {
                throw new IllegalArgumentException("Replacement too long");
            } else if (isLegalReplacement(newReplacement)) {
                this.replacement = Arrays.copyOf(newReplacement, newReplacement.length);
                implReplaceWith(this.replacement);
                return this;
            } else {
                throw new IllegalArgumentException("Illegal replacement");
            }
        }
        throw new IllegalArgumentException("Null replacement");
    }

    protected void implReplaceWith(byte[] newReplacement) {
    }

    public boolean isLegalReplacement(byte[] repl) {
        CharsetDecoder dec;
        ByteBuffer bb;
        WeakReference<CharsetDecoder> wr = this.cachedDecoder;
        if (wr != null) {
            CharsetDecoder charsetDecoder = (CharsetDecoder) wr.get();
            dec = charsetDecoder;
            if (charsetDecoder != null) {
                dec.reset();
                bb = ByteBuffer.wrap(repl);
                return 1 ^ dec.decode(bb, CharBuffer.allocate((int) (((float) bb.remaining()) * dec.maxCharsPerByte())), true).isError();
            }
        }
        dec = charset().newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPORT);
        dec.onUnmappableCharacter(CodingErrorAction.REPORT);
        this.cachedDecoder = new WeakReference(dec);
        bb = ByteBuffer.wrap(repl);
        return 1 ^ dec.decode(bb, CharBuffer.allocate((int) (((float) bb.remaining()) * dec.maxCharsPerByte())), true).isError();
    }

    public CodingErrorAction malformedInputAction() {
        return this.malformedInputAction;
    }

    public final CharsetEncoder onMalformedInput(CodingErrorAction newAction) {
        if (newAction != null) {
            this.malformedInputAction = newAction;
            implOnMalformedInput(newAction);
            return this;
        }
        throw new IllegalArgumentException("Null action");
    }

    protected void implOnMalformedInput(CodingErrorAction newAction) {
    }

    public CodingErrorAction unmappableCharacterAction() {
        return this.unmappableCharacterAction;
    }

    public final CharsetEncoder onUnmappableCharacter(CodingErrorAction newAction) {
        if (newAction != null) {
            this.unmappableCharacterAction = newAction;
            implOnUnmappableCharacter(newAction);
            return this;
        }
        throw new IllegalArgumentException("Null action");
    }

    protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
    }

    public final float averageBytesPerChar() {
        return this.averageBytesPerChar;
    }

    public final float maxBytesPerChar() {
        return this.maxBytesPerChar;
    }

    public final CoderResult encode(CharBuffer in, ByteBuffer out, boolean endOfInput) {
        CoderResult cr;
        int newState = endOfInput ? 2 : 1;
        if (!(this.state == 0 || this.state == 1 || (endOfInput && this.state == 2))) {
            throwIllegalStateException(this.state, newState);
        }
        this.state = newState;
        while (true) {
            try {
                cr = encodeLoop(in, out);
                if (cr.isOverflow()) {
                    return cr;
                }
                if (cr.isUnderflow()) {
                    if (!endOfInput || !in.hasRemaining()) {
                        return cr;
                    }
                    cr = CoderResult.malformedForLength(in.remaining());
                }
                CodingErrorAction action = null;
                if (cr.isMalformed()) {
                    action = this.malformedInputAction;
                } else if (cr.isUnmappable()) {
                    action = this.unmappableCharacterAction;
                }
                if (action == CodingErrorAction.REPORT) {
                    return cr;
                }
                if (action == CodingErrorAction.REPLACE) {
                    if (out.remaining() < this.replacement.length) {
                        return CoderResult.OVERFLOW;
                    }
                    out.put(this.replacement);
                }
                if (action == CodingErrorAction.IGNORE || action == CodingErrorAction.REPLACE) {
                    in.position(in.position() + cr.length());
                }
            } catch (BufferUnderflowException x) {
                throw new CoderMalfunctionError(x);
            } catch (BufferOverflowException x2) {
                throw new CoderMalfunctionError(x2);
            }
        }
        return cr;
    }

    public final CoderResult flush(ByteBuffer out) {
        if (this.state == 2) {
            CoderResult cr = implFlush(out);
            if (cr.isUnderflow()) {
                this.state = 3;
            }
            return cr;
        }
        if (this.state != 3) {
            throwIllegalStateException(this.state, 3);
        }
        return CoderResult.UNDERFLOW;
    }

    protected CoderResult implFlush(ByteBuffer out) {
        return CoderResult.UNDERFLOW;
    }

    public final CharsetEncoder reset() {
        implReset();
        this.state = 0;
        return this;
    }

    protected void implReset() {
    }

    public final ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
        int n = (int) (((float) in.remaining()) * averageBytesPerChar());
        ByteBuffer out = ByteBuffer.allocate(n);
        if (n == 0 && in.remaining() == 0) {
            return out;
        }
        reset();
        while (true) {
            CoderResult cr = in.hasRemaining() ? encode(in, out, true) : CoderResult.UNDERFLOW;
            if (cr.isUnderflow()) {
                cr = flush(out);
            }
            if (cr.isUnderflow()) {
                out.flip();
                return out;
            } else if (cr.isOverflow()) {
                n = (2 * n) + 1;
                ByteBuffer o = ByteBuffer.allocate(n);
                out.flip();
                o.put(out);
                out = o;
            } else {
                cr.throwException();
            }
        }
    }

    private boolean canEncode(CharBuffer cb) {
        boolean x = true;
        if (this.state == 3) {
            reset();
        } else if (this.state != 0) {
            throwIllegalStateException(this.state, 1);
        }
        if (!cb.hasRemaining()) {
            return true;
        }
        CodingErrorAction ma = malformedInputAction();
        CodingErrorAction ua = unmappableCharacterAction();
        boolean z;
        try {
            onMalformedInput(CodingErrorAction.REPORT);
            z = CodingErrorAction.REPORT;
            onUnmappableCharacter(z);
            encode(cb);
            return x;
        } catch (CharacterCodingException e) {
            x = e;
            z = $assertionsDisabled;
            return z;
        } finally {
            onMalformedInput(ma);
            onUnmappableCharacter(ua);
            reset();
        }
    }

    public boolean canEncode(char c) {
        CharBuffer cb = CharBuffer.allocate(1);
        cb.put(c);
        cb.flip();
        return canEncode(cb);
    }

    public boolean canEncode(CharSequence cs) {
        CharBuffer cb;
        if (cs instanceof CharBuffer) {
            cb = ((CharBuffer) cs).duplicate();
        } else {
            cb = CharBuffer.wrap(cs);
        }
        return canEncode(cb);
    }

    private void throwIllegalStateException(int from, int to) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Current state = ");
        stringBuilder.append(stateNames[from]);
        stringBuilder.append(", new state = ");
        stringBuilder.append(stateNames[to]);
        throw new IllegalStateException(stringBuilder.toString());
    }
}
