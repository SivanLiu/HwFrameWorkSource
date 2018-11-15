package com.android.internal.http.multipart;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.util.EncodingUtils;

public class StringPart extends PartBase {
    public static final String DEFAULT_CHARSET = "US-ASCII";
    public static final String DEFAULT_CONTENT_TYPE = "text/plain";
    public static final String DEFAULT_TRANSFER_ENCODING = "8bit";
    private static final Log LOG = LogFactory.getLog(StringPart.class);
    private byte[] content;
    private String value;

    public StringPart(String name, String value, String charset) {
        super(name, "text/plain", charset == null ? "US-ASCII" : charset, DEFAULT_TRANSFER_ENCODING);
        if (value == null) {
            throw new IllegalArgumentException("Value may not be null");
        } else if (value.indexOf(0) == -1) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("NULs may not be present in string parts");
        }
    }

    public StringPart(String name, String value) {
        this(name, value, null);
    }

    private byte[] getContent() {
        if (this.content == null) {
            this.content = EncodingUtils.getBytes(this.value, getCharSet());
        }
        return this.content;
    }

    protected void sendData(OutputStream out) throws IOException {
        LOG.trace("enter sendData(OutputStream)");
        out.write(getContent());
    }

    protected long lengthOfData() {
        LOG.trace("enter lengthOfData()");
        return (long) getContent().length;
    }

    public void setCharSet(String charSet) {
        super.setCharSet(charSet);
        this.content = null;
    }
}
