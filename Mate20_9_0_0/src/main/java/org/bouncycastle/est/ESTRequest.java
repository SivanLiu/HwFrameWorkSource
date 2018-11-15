package org.bouncycastle.est;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

public class ESTRequest {
    final byte[] data;
    final ESTClient estClient;
    Headers headers = new Headers();
    final ESTHijacker hijacker;
    final ESTSourceConnectionListener listener;
    final String method;
    final URL url;

    ESTRequest(String str, URL url, byte[] bArr, ESTHijacker eSTHijacker, ESTSourceConnectionListener eSTSourceConnectionListener, Headers headers, ESTClient eSTClient) {
        this.method = str;
        this.url = url;
        this.data = bArr;
        this.hijacker = eSTHijacker;
        this.listener = eSTSourceConnectionListener;
        this.headers = headers;
        this.estClient = eSTClient;
    }

    public ESTClient getClient() {
        return this.estClient;
    }

    public Map<String, String[]> getHeaders() {
        return (Map) this.headers.clone();
    }

    public ESTHijacker getHijacker() {
        return this.hijacker;
    }

    public ESTSourceConnectionListener getListener() {
        return this.listener;
    }

    public String getMethod() {
        return this.method;
    }

    public URL getURL() {
        return this.url;
    }

    public void writeData(OutputStream outputStream) throws IOException {
        if (this.data != null) {
            outputStream.write(this.data);
        }
    }
}
