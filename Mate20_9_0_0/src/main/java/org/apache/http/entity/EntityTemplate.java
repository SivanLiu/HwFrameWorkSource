package org.apache.http.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public class EntityTemplate extends AbstractHttpEntity {
    private final ContentProducer contentproducer;

    public EntityTemplate(ContentProducer contentproducer) {
        if (contentproducer != null) {
            this.contentproducer = contentproducer;
            return;
        }
        throw new IllegalArgumentException("Content producer may not be null");
    }

    public long getContentLength() {
        return -1;
    }

    public InputStream getContent() {
        throw new UnsupportedOperationException("Entity template does not implement getContent()");
    }

    public boolean isRepeatable() {
        return true;
    }

    public void writeTo(OutputStream outstream) throws IOException {
        if (outstream != null) {
            this.contentproducer.writeTo(outstream);
            return;
        }
        throw new IllegalArgumentException("Output stream may not be null");
    }

    public boolean isStreaming() {
        return true;
    }

    public void consumeContent() throws IOException {
    }
}
