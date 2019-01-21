package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import org.apache.xml.dtm.DTMFilter;

final class WriterToUTF8Buffered extends Writer implements WriterChain {
    private static final int BYTES_MAX = 16384;
    private static final int CHARS_MAX = 5461;
    private int count = 0;
    private final char[] m_inputChars = new char[5463];
    private final OutputStream m_os;
    private final byte[] m_outputBytes = new byte[16387];

    public WriterToUTF8Buffered(OutputStream out) {
        this.m_os = out;
    }

    public void write(int c) throws IOException {
        if (this.count >= 16384) {
            flushBuffer();
        }
        byte[] bArr;
        int i;
        if (c < 128) {
            byte[] bArr2 = this.m_outputBytes;
            int i2 = this.count;
            this.count = i2 + 1;
            bArr2[i2] = (byte) c;
        } else if (c < DTMFilter.SHOW_NOTATION) {
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (192 + (c >> 6));
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (128 + (c & 63));
        } else if (c < 65536) {
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (224 + (c >> 12));
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (((c >> 6) & 63) + 128);
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (128 + (c & 63));
        } else {
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (240 + (c >> 18));
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (((c >> 12) & 63) + 128);
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (((c >> 6) & 63) + 128);
            bArr = this.m_outputBytes;
            i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) (128 + (c & 63));
        }
    }

    public void write(char[] chars, int start, int length) throws IOException {
        int split;
        int end_chunk;
        int start_chunk;
        char c;
        char c2;
        char[] cArr = chars;
        int i = length;
        int lengthx3 = 3 * i;
        char c3 = 56319;
        char c4 = 55296;
        int chunk = 1;
        if (lengthx3 >= 16384 - this.count) {
            flushBuffer();
            if (lengthx3 > 16384) {
                int chunks;
                split = i / CHARS_MAX;
                if (i % CHARS_MAX > 0) {
                    chunks = split + 1;
                } else {
                    chunks = split;
                }
                end_chunk = start;
                while (chunk <= chunks) {
                    start_chunk = end_chunk;
                    end_chunk = start + ((int) ((((long) i) * ((long) chunk)) / ((long) chunks)));
                    char c5 = cArr[end_chunk - 1];
                    int ic = cArr[end_chunk - 1];
                    if (c5 >= 55296 && c5 <= 56319) {
                        end_chunk = end_chunk < start + i ? end_chunk + 1 : end_chunk - 1;
                    }
                    write(cArr, start_chunk, end_chunk - start_chunk);
                    chunk++;
                }
                return;
            }
        }
        split = i + start;
        byte[] buf_loc = this.m_outputBytes;
        start_chunk = this.count;
        end_chunk = start;
        while (end_chunk < split) {
            c = cArr[end_chunk];
            c2 = c;
            if (c >= 128) {
                break;
            }
            int count_loc = start_chunk + 1;
            buf_loc[start_chunk] = (byte) c2;
            end_chunk++;
            start_chunk = count_loc;
        }
        while (end_chunk < split) {
            c = cArr[end_chunk];
            int count_loc2;
            if (c < 128) {
                count_loc2 = start_chunk + 1;
                buf_loc[start_chunk] = (byte) c;
                start_chunk = count_loc2;
            } else if (c < 2048) {
                count_loc2 = start_chunk + 1;
                buf_loc[start_chunk] = (byte) (192 + (c >> 6));
                start_chunk = count_loc2 + 1;
                buf_loc[count_loc2] = (byte) ((c & 63) + 128);
            } else {
                int count_loc3;
                int count_loc4;
                if (c < c4 || c > c3) {
                    count_loc3 = start_chunk + 1;
                    buf_loc[start_chunk] = (byte) (224 + (c >> 12));
                    count_loc4 = count_loc3 + 1;
                    buf_loc[count_loc3] = (byte) (((c >> 6) & 63) + 128);
                    count_loc3 = count_loc4 + 1;
                    buf_loc[count_loc4] = (byte) ((c & 63) + 128);
                } else {
                    c2 = c;
                    end_chunk++;
                    char low = cArr[end_chunk];
                    int count_loc5 = start_chunk + 1;
                    buf_loc[start_chunk] = (byte) ((((c2 + 64) >> 8) & 240) | 240);
                    count_loc3 = count_loc5 + 1;
                    buf_loc[count_loc5] = (byte) ((((c2 + 64) >> 2) & 63) | 128);
                    count_loc4 = count_loc3 + 1;
                    buf_loc[count_loc3] = (byte) ((((low >> 6) & 15) + ((c2 << 4) & 48)) | 128);
                    count_loc3 = count_loc4 + 1;
                    buf_loc[count_loc4] = (byte) ((low & 63) | 128);
                }
                start_chunk = count_loc3;
            }
            end_chunk++;
            c3 = 56319;
            c4 = 55296;
        }
        this.count = start_chunk;
    }

    public void write(String s) throws IOException {
        int split;
        int chunks;
        int start_chunk;
        int len_chunk;
        char c;
        String str = s;
        int length = s.length();
        int lengthx3 = 3 * length;
        int i = 0;
        int chunk = 1;
        if (lengthx3 >= 16384 - this.count) {
            flushBuffer();
            if (lengthx3 > 16384) {
                split = length / CHARS_MAX;
                if (length % CHARS_MAX > 0) {
                    chunks = split + 1;
                } else {
                    chunks = split;
                }
                int end_chunk = 0;
                while (chunk <= chunks) {
                    start_chunk = end_chunk;
                    int end_chunk2 = ((int) ((((long) length) * ((long) chunk)) / ((long) chunks))) + 0;
                    str.getChars(start_chunk, end_chunk2, this.m_inputChars, 0);
                    len_chunk = end_chunk2 - start_chunk;
                    c = this.m_inputChars[len_chunk - 1];
                    if (c >= 55296 && c <= 56319) {
                        end_chunk2--;
                        len_chunk--;
                    }
                    write(this.m_inputChars, 0, len_chunk);
                    chunk++;
                    end_chunk = end_chunk2;
                }
                return;
            }
        }
        str.getChars(0, length, this.m_inputChars, 0);
        char[] chars = this.m_inputChars;
        split = length;
        byte[] buf_loc = this.m_outputBytes;
        len_chunk = this.count;
        while (i < split) {
            c = chars[i];
            char c2 = c;
            if (c >= 128) {
                break;
            }
            chunks = len_chunk + 1;
            buf_loc[len_chunk] = (byte) c2;
            i++;
            len_chunk = chunks;
        }
        while (i < split) {
            c = chars[i];
            if (c < 128) {
                start_chunk = len_chunk + 1;
                buf_loc[len_chunk] = (byte) c;
            } else {
                if (c < 2048) {
                    start_chunk = len_chunk + 1;
                    buf_loc[len_chunk] = (byte) (192 + (c >> 6));
                    len_chunk = start_chunk + 1;
                    buf_loc[start_chunk] = (byte) ((c & 63) + 128);
                } else if (c < 55296 || c > 56319) {
                    start_chunk = len_chunk + 1;
                    buf_loc[len_chunk] = (byte) (224 + (c >> 12));
                    len_chunk = start_chunk + 1;
                    buf_loc[start_chunk] = (byte) (((c >> 6) & 63) + 128);
                    start_chunk = len_chunk + 1;
                    buf_loc[len_chunk] = (byte) ((c & 63) + 128);
                } else {
                    char high = c;
                    i++;
                    char low = chars[i];
                    int count_loc = len_chunk + 1;
                    buf_loc[len_chunk] = (byte) ((((high + 64) >> 8) & 240) | 240);
                    len_chunk = count_loc + 1;
                    buf_loc[count_loc] = (byte) ((((high + 64) >> 2) & 63) | 128);
                    start_chunk = len_chunk + 1;
                    buf_loc[len_chunk] = (byte) ((((low >> 6) & 15) + ((high << 4) & 48)) | 128);
                    len_chunk = start_chunk + 1;
                    buf_loc[start_chunk] = (byte) ((low & 63) | 128);
                }
                i++;
            }
            len_chunk = start_chunk;
            i++;
        }
        this.count = len_chunk;
    }

    public void flushBuffer() throws IOException {
        if (this.count > 0) {
            this.m_os.write(this.m_outputBytes, 0, this.count);
            this.count = 0;
        }
    }

    public void flush() throws IOException {
        flushBuffer();
        this.m_os.flush();
    }

    public void close() throws IOException {
        flushBuffer();
        this.m_os.close();
    }

    public OutputStream getOutputStream() {
        return this.m_os;
    }

    public Writer getWriter() {
        return null;
    }
}
