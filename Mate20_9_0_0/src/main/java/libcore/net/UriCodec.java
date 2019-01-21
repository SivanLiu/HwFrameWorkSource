package libcore.net;

import android.icu.impl.locale.XLocaleDistance;
import android.icu.text.PluralRules;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public abstract class UriCodec {
    private static final char INVALID_INPUT_CHARACTER = '�';

    protected abstract boolean isRetained(char c);

    private static boolean isWhitelisted(char c) {
        return ('a' <= c && c <= 'z') || (('A' <= c && c <= 'Z') || ('0' <= c && c <= '9'));
    }

    private boolean isWhitelistedOrRetained(char c) {
        return isWhitelisted(c) || isRetained(c);
    }

    public final String validate(String uri, int start, int end, String name) throws URISyntaxException {
        int i;
        for (int i2 = start; i2 < end; i2 = i) {
            i = i2 + 1;
            i2 = uri.charAt(i2);
            if (!isWhitelistedOrRetained(i2)) {
                if (i2 == 37) {
                    int j = 0;
                    while (j < 2) {
                        int i3 = i + 1;
                        i2 = getNextCharacter(uri, i, end, name);
                        if (hexCharToValue(i2) >= 0) {
                            j++;
                            i = i3;
                        } else {
                            throw unexpectedCharacterException(uri, name, i2, i3 - 1);
                        }
                    }
                    continue;
                } else {
                    throw unexpectedCharacterException(uri, name, i2, i - 1);
                }
            }
        }
        return uri.substring(start, end);
    }

    private static int hexCharToValue(char c) {
        if ('0' <= c && c <= '9') {
            return c - 48;
        }
        if ('a' <= c && c <= 'f') {
            return (10 + c) - 97;
        }
        if ('A' > c || c > 'F') {
            return -1;
        }
        return (10 + c) - 65;
    }

    private static URISyntaxException unexpectedCharacterException(String uri, String name, char unexpected, int index) {
        String nameString;
        if (name == null) {
            nameString = "";
        } else {
            nameString = new StringBuilder();
            nameString.append(" in [");
            nameString.append(name);
            nameString.append("]");
            nameString = nameString.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected character");
        stringBuilder.append(nameString);
        stringBuilder.append(PluralRules.KEYWORD_RULE_SEPARATOR);
        stringBuilder.append(unexpected);
        return new URISyntaxException(uri, stringBuilder.toString(), index);
    }

    private static char getNextCharacter(String uri, int index, int end, String name) throws URISyntaxException {
        if (index < end) {
            return uri.charAt(index);
        }
        String nameString;
        if (name == null) {
            nameString = "";
        } else {
            nameString = new StringBuilder();
            nameString.append(" in [");
            nameString.append(name);
            nameString.append("]");
            nameString = nameString.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected end of string");
        stringBuilder.append(nameString);
        throw new URISyntaxException(uri, stringBuilder.toString(), index);
    }

    public static void validateSimple(String uri, String legal) throws URISyntaxException {
        int i = 0;
        while (i < uri.length()) {
            char c = uri.charAt(i);
            if (isWhitelisted(c) || legal.indexOf(c) >= 0) {
                i++;
            } else {
                throw unexpectedCharacterException(uri, null, c, i);
            }
        }
    }

    public final String encode(String s, Charset charset) {
        StringBuilder builder = new StringBuilder(s.length());
        appendEncoded(builder, s, charset, false);
        return builder.toString();
    }

    public final void appendEncoded(StringBuilder builder, String s) {
        appendEncoded(builder, s, StandardCharsets.UTF_8, false);
    }

    public final void appendPartiallyEncoded(StringBuilder builder, String s) {
        appendEncoded(builder, s, StandardCharsets.UTF_8, true);
    }

    private void appendEncoded(StringBuilder builder, String s, Charset charset, boolean partiallyEncoded) {
        CharsetEncoder encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer cBuffer = CharBuffer.allocate(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && partiallyEncoded) {
                flushEncodingCharBuffer(builder, encoder, cBuffer);
                builder.append('%');
            } else if (c == ' ' && isRetained(' ')) {
                flushEncodingCharBuffer(builder, encoder, cBuffer);
                builder.append('+');
            } else if (isWhitelistedOrRetained(c)) {
                flushEncodingCharBuffer(builder, encoder, cBuffer);
                builder.append(c);
            } else {
                cBuffer.put(c);
            }
        }
        flushEncodingCharBuffer(builder, encoder, cBuffer);
    }

    private static void flushEncodingCharBuffer(StringBuilder builder, CharsetEncoder encoder, CharBuffer cBuffer) {
        if (cBuffer.position() != 0) {
            cBuffer.flip();
            ByteBuffer byteBuffer = ByteBuffer.allocate(cBuffer.remaining() * ((int) Math.ceil((double) encoder.maxBytesPerChar())));
            byteBuffer.position(0);
            CoderResult result = encoder.encode(cBuffer, byteBuffer, true);
            StringBuilder stringBuilder;
            if (result != CoderResult.UNDERFLOW) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error encoding, unexpected result [");
                stringBuilder.append(result.toString());
                stringBuilder.append("] using encoder for [");
                stringBuilder.append(encoder.charset().name());
                stringBuilder.append("]");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (cBuffer.hasRemaining()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Encoder for [");
                stringBuilder.append(encoder.charset().name());
                stringBuilder.append("] failed with underflow with remaining input [");
                stringBuilder.append(cBuffer);
                stringBuilder.append("]");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else {
                encoder.flush(byteBuffer);
                if (result == CoderResult.UNDERFLOW) {
                    encoder.reset();
                    byteBuffer.flip();
                    while (byteBuffer.hasRemaining()) {
                        byte b = byteBuffer.get();
                        builder.append('%');
                        builder.append(intToHexDigit((b & 240) >>> 4));
                        builder.append(intToHexDigit(b & 15));
                    }
                    cBuffer.flip();
                    cBuffer.limit(cBuffer.capacity());
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error encoding, unexpected result [");
                stringBuilder.append(result.toString());
                stringBuilder.append("] flushing encoder for [");
                stringBuilder.append(encoder.charset().name());
                stringBuilder.append("]");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    private static char intToHexDigit(int b) {
        if (b < 10) {
            return (char) (48 + b);
        }
        return (char) ((65 + b) - 10);
    }

    public static String decode(String s, boolean convertPlus, Charset charset, boolean throwOnFailure) {
        StringBuilder builder = new StringBuilder(s.length());
        appendDecoded(builder, s, convertPlus, charset, throwOnFailure);
        return builder.toString();
    }

    private static void appendDecoded(StringBuilder builder, String s, boolean convertPlus, Charset charset, boolean throwOnFailure) {
        CharsetDecoder decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).replaceWith(XLocaleDistance.ANY).onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer byteBuffer = ByteBuffer.allocate(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            i++;
            char c2;
            if (c != '%') {
                c2 = '+';
                if (c != '+') {
                    flushDecodingByteAccumulator(builder, decoder, byteBuffer, throwOnFailure);
                    builder.append(c);
                } else {
                    flushDecodingByteAccumulator(builder, decoder, byteBuffer, throwOnFailure);
                    if (convertPlus) {
                        c2 = ' ';
                    }
                    builder.append(c2);
                }
            } else {
                byte hexValue = (byte) 0;
                int i2 = i;
                i = 0;
                while (i < 2) {
                    try {
                        c2 = getNextCharacter(s, i2, s.length(), null);
                        i2++;
                        int newDigit = hexCharToValue(c2);
                        if (newDigit >= 0) {
                            hexValue = (byte) ((hexValue * 16) + newDigit);
                            i++;
                        } else if (throwOnFailure) {
                            throw new IllegalArgumentException(unexpectedCharacterException(s, null, c2, i2 - 1));
                        } else {
                            flushDecodingByteAccumulator(builder, decoder, byteBuffer, throwOnFailure);
                            builder.append(INVALID_INPUT_CHARACTER);
                            byteBuffer.put(hexValue);
                            i = i2;
                        }
                    } catch (URISyntaxException e) {
                        if (throwOnFailure) {
                            throw new IllegalArgumentException(e);
                        }
                        flushDecodingByteAccumulator(builder, decoder, byteBuffer, throwOnFailure);
                        builder.append(INVALID_INPUT_CHARACTER);
                        return;
                    }
                }
                byteBuffer.put(hexValue);
                i = i2;
            }
        }
        flushDecodingByteAccumulator(builder, decoder, byteBuffer, throwOnFailure);
    }

    private static void flushDecodingByteAccumulator(StringBuilder builder, CharsetDecoder decoder, ByteBuffer byteBuffer, boolean throwOnFailure) {
        if (byteBuffer.position() != 0) {
            byteBuffer.flip();
            try {
                builder.append(decoder.decode(byteBuffer));
            } catch (CharacterCodingException e) {
                if (throwOnFailure) {
                    throw new IllegalArgumentException(e);
                }
                builder.append(INVALID_INPUT_CHARACTER);
            } catch (Throwable th) {
                byteBuffer.flip();
                byteBuffer.limit(byteBuffer.capacity());
            }
            byteBuffer.flip();
            byteBuffer.limit(byteBuffer.capacity());
        }
    }

    public static String decode(String s) {
        return decode(s, false, StandardCharsets.UTF_8, true);
    }
}
