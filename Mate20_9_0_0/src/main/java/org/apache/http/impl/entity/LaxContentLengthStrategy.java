package org.apache.http.impl.entity;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;

@Deprecated
public class LaxContentLengthStrategy implements ContentLengthStrategy {
    public long determineLength(HttpMessage message) throws HttpException {
        if (message != null) {
            boolean strict = message.getParams().isParameterTrue(CoreProtocolPNames.STRICT_TRANSFER_ENCODING);
            Header transferEncodingHeader = message.getFirstHeader(HTTP.TRANSFER_ENCODING);
            Header contentLengthHeader = message.getFirstHeader(HTTP.CONTENT_LEN);
            StringBuilder stringBuilder;
            if (transferEncodingHeader != null) {
                try {
                    int i;
                    HeaderElement[] encodings = transferEncodingHeader.getElements();
                    if (strict) {
                        i = 0;
                        while (i < encodings.length) {
                            String encoding = encodings[i].getName();
                            if (encoding == null || encoding.length() <= 0 || encoding.equalsIgnoreCase(HTTP.CHUNK_CODING) || encoding.equalsIgnoreCase(HTTP.IDENTITY_CODING)) {
                                i++;
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unsupported transfer encoding: ");
                                stringBuilder.append(encoding);
                                throw new ProtocolException(stringBuilder.toString());
                            }
                        }
                    }
                    i = encodings.length;
                    if (HTTP.IDENTITY_CODING.equalsIgnoreCase(transferEncodingHeader.getValue())) {
                        return -1;
                    }
                    if (i > 0 && HTTP.CHUNK_CODING.equalsIgnoreCase(encodings[i - 1].getName())) {
                        return -2;
                    }
                    if (!strict) {
                        return -1;
                    }
                    throw new ProtocolException("Chunk-encoding must be the last one applied");
                } catch (ParseException px) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid Transfer-Encoding header value: ");
                    stringBuilder2.append(transferEncodingHeader);
                    throw new ProtocolException(stringBuilder2.toString(), px);
                }
            } else if (contentLengthHeader == null) {
                return -1;
            } else {
                long contentlen = -1;
                Header[] headers = message.getHeaders(HTTP.CONTENT_LEN);
                if (!strict || headers.length <= 1) {
                    int i2 = headers.length - 1;
                    while (true) {
                        int i3 = i2;
                        if (i3 < 0) {
                            break;
                        }
                        Header header = headers[i3];
                        try {
                            contentlen = Long.parseLong(header.getValue());
                            break;
                        } catch (NumberFormatException e) {
                            if (strict) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Invalid content length: ");
                                stringBuilder.append(header.getValue());
                                throw new ProtocolException(stringBuilder.toString());
                            }
                            i2 = i3 - 1;
                        }
                    }
                    if (contentlen >= 0) {
                        return contentlen;
                    }
                    return -1;
                }
                throw new ProtocolException("Multiple content length headers");
            }
        }
        throw new IllegalArgumentException("HTTP message may not be null");
    }
}
