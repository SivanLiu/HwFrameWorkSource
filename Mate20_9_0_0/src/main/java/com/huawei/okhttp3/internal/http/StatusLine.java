package com.huawei.okhttp3.internal.http;

import com.huawei.okhttp3.Protocol;
import com.huawei.okhttp3.Response;
import java.io.IOException;
import java.net.ProtocolException;

public final class StatusLine {
    public static final int HTTP_CONTINUE = 100;
    public static final int HTTP_PERM_REDIRECT = 308;
    public static final int HTTP_TEMP_REDIRECT = 307;
    public final int code;
    public final String message;
    public final Protocol protocol;

    public StatusLine(Protocol protocol, int code, String message) {
        this.protocol = protocol;
        this.code = code;
        this.message = message;
    }

    public static StatusLine get(Response response) {
        return new StatusLine(response.protocol(), response.code(), response.message());
    }

    public static StatusLine parse(String statusLine) throws IOException {
        int codeStart;
        StringBuilder stringBuilder;
        Protocol protocol;
        StringBuilder stringBuilder2;
        if (statusLine.startsWith("HTTP/1.")) {
            if (statusLine.length() < 9 || statusLine.charAt(8) != ' ') {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unexpected status line: ");
                stringBuilder2.append(statusLine);
                throw new ProtocolException(stringBuilder2.toString());
            }
            Protocol protocol2;
            int httpMinorVersion = statusLine.charAt(7) - 48;
            codeStart = 9;
            if (httpMinorVersion == 0) {
                protocol2 = Protocol.HTTP_1_0;
            } else if (httpMinorVersion == 1) {
                protocol2 = Protocol.HTTP_1_1;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected status line: ");
                stringBuilder.append(statusLine);
                throw new ProtocolException(stringBuilder.toString());
            }
            protocol = protocol2;
        } else if (statusLine.startsWith("ICY ")) {
            protocol = Protocol.HTTP_1_0;
            codeStart = 4;
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unexpected status line: ");
            stringBuilder2.append(statusLine);
            throw new ProtocolException(stringBuilder2.toString());
        }
        if (statusLine.length() >= codeStart + 3) {
            try {
                int code = Integer.parseInt(statusLine.substring(codeStart, codeStart + 3));
                String message = "";
                if (statusLine.length() > codeStart + 3) {
                    if (statusLine.charAt(codeStart + 3) == ' ') {
                        message = statusLine.substring(codeStart + 4);
                    } else {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Unexpected status line: ");
                        stringBuilder3.append(statusLine);
                        throw new ProtocolException(stringBuilder3.toString());
                    }
                }
                return new StatusLine(protocol, code, message);
            } catch (NumberFormatException e) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Unexpected status line: ");
                stringBuilder4.append(statusLine);
                throw new ProtocolException(stringBuilder4.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected status line: ");
        stringBuilder.append(statusLine);
        throw new ProtocolException(stringBuilder.toString());
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.protocol == Protocol.HTTP_1_0 ? "HTTP/1.0" : "HTTP/1.1");
        result.append(' ');
        result.append(this.code);
        if (this.message != null) {
            result.append(' ');
            result.append(this.message);
        }
        return result.toString();
    }
}
