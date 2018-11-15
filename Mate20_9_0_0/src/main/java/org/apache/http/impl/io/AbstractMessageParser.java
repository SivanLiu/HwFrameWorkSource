package org.apache.http.impl.io;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public abstract class AbstractMessageParser implements HttpMessageParser {
    protected final LineParser lineParser;
    private final int maxHeaderCount;
    private final int maxLineLen;
    private final SessionInputBuffer sessionBuffer;

    protected abstract HttpMessage parseHead(SessionInputBuffer sessionInputBuffer) throws IOException, HttpException, ParseException;

    public AbstractMessageParser(SessionInputBuffer buffer, LineParser parser, HttpParams params) {
        if (buffer == null) {
            throw new IllegalArgumentException("Session input buffer may not be null");
        } else if (params != null) {
            this.sessionBuffer = buffer;
            this.maxHeaderCount = params.getIntParameter("http.connection.max-header-count", -1);
            this.maxLineLen = params.getIntParameter("http.connection.max-line-length", -1);
            this.lineParser = parser != null ? parser : BasicLineParser.DEFAULT;
        } else {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
    }

    public static Header[] parseHeaders(SessionInputBuffer inbuffer, int maxHeaderCount, int maxLineLen, LineParser parser) throws HttpException, IOException {
        if (inbuffer != null) {
            int i;
            Header[] headers;
            int i2;
            if (parser == null) {
                parser = BasicLineParser.DEFAULT;
            }
            ArrayList headerLines = new ArrayList();
            CharArrayBuffer current = null;
            CharArrayBuffer previous = null;
            while (true) {
                if (current == null) {
                    current = new CharArrayBuffer(64);
                } else {
                    current.clear();
                }
                i = 0;
                if (inbuffer.readLine(current) == -1 || current.length() < 1) {
                    headers = new Header[headerLines.size()];
                } else {
                    if ((current.charAt(0) == ' ' || current.charAt(0) == 9) && previous != null) {
                        while (true) {
                            i2 = i;
                            if (i2 >= current.length()) {
                                break;
                            }
                            char ch = current.charAt(i2);
                            if (ch != ' ' && ch != 9) {
                                break;
                            }
                            i = i2 + 1;
                        }
                        if (maxLineLen <= 0 || ((previous.length() + 1) + current.length()) - i2 <= maxLineLen) {
                            previous.append(' ');
                            previous.append(current, i2, current.length() - i2);
                        } else {
                            throw new IOException("Maximum line length limit exceeded");
                        }
                    }
                    headerLines.add(current);
                    previous = current;
                    current = null;
                    if (maxHeaderCount > 0 && headerLines.size() >= maxHeaderCount) {
                        throw new IOException("Maximum header count exceeded");
                    }
                }
            }
            headers = new Header[headerLines.size()];
            while (true) {
                i2 = i;
                if (i2 >= headerLines.size()) {
                    return headers;
                }
                try {
                    headers[i2] = parser.parseHeader((CharArrayBuffer) headerLines.get(i2));
                    i = i2 + 1;
                } catch (ParseException ex) {
                    throw new ProtocolException(ex.getMessage());
                }
            }
        }
        throw new IllegalArgumentException("Session input buffer may not be null");
    }

    public HttpMessage parse() throws IOException, HttpException {
        try {
            HttpMessage message = parseHead(this.sessionBuffer);
            message.setHeaders(parseHeaders(this.sessionBuffer, this.maxHeaderCount, this.maxLineLen, this.lineParser));
            return message;
        } catch (ParseException px) {
            throw new ProtocolException(px.getMessage(), px);
        }
    }
}
