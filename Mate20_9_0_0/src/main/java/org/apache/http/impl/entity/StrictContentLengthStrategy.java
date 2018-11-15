package org.apache.http.impl.entity;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.protocol.HTTP;

@Deprecated
public class StrictContentLengthStrategy implements ContentLengthStrategy {
    public long determineLength(HttpMessage message) throws HttpException {
        if (message != null) {
            Header transferEncodingHeader = message.getFirstHeader(HTTP.TRANSFER_ENCODING);
            Header contentLengthHeader = message.getFirstHeader(HTTP.CONTENT_LEN);
            if (transferEncodingHeader != null) {
                String s = transferEncodingHeader.getValue();
                StringBuilder stringBuilder;
                if (HTTP.CHUNK_CODING.equalsIgnoreCase(s)) {
                    if (!message.getProtocolVersion().lessEquals(HttpVersion.HTTP_1_0)) {
                        return -2;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Chunked transfer encoding not allowed for ");
                    stringBuilder.append(message.getProtocolVersion());
                    throw new ProtocolException(stringBuilder.toString());
                } else if (HTTP.IDENTITY_CODING.equalsIgnoreCase(s)) {
                    return -1;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported transfer encoding: ");
                    stringBuilder.append(s);
                    throw new ProtocolException(stringBuilder.toString());
                }
            } else if (contentLengthHeader == null) {
                return -1;
            } else {
                String s2 = contentLengthHeader.getValue();
                try {
                    return Long.parseLong(s2);
                } catch (NumberFormatException e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid content length: ");
                    stringBuilder2.append(s2);
                    throw new ProtocolException(stringBuilder2.toString());
                }
            }
        }
        throw new IllegalArgumentException("HTTP message may not be null");
    }
}
