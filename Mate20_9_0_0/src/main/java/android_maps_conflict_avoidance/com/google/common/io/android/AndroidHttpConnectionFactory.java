package android_maps_conflict_avoidance.com.google.common.io.android;

import android.content.Context;
import android_maps_conflict_avoidance.com.google.common.io.BaseHttpConnectionFactory;
import android_maps_conflict_avoidance.com.google.common.io.GoogleHttpConnection;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.entity.ByteArrayEntity;

public class AndroidHttpConnectionFactory extends BaseHttpConnectionFactory {
    private static final Object lock = new Object();
    private static int numOpenConnection;
    private GoogleHttpClient client;
    private final Context context;
    private String userAgent = "GoogleMobile/1.0";

    private class AndroidGoogleHttpConnection implements GoogleHttpConnection {
        private ByteArrayOutputStream baos;
        private boolean closed;
        private final HttpUriRequest request;
        private HttpResponse response;

        private AndroidGoogleHttpConnection(String url, boolean usePost) throws IOException {
            this.baos = null;
            this.response = null;
            if (usePost) {
                try {
                    this.request = new HttpPost(url);
                } catch (RuntimeException e) {
                    String message = new StringBuilder();
                    message.append("URISyntaxException in HttpUriRequest, post=");
                    message.append(usePost);
                    message.append(", url=");
                    message.append(url);
                    IOException f = new IOException(message.toString());
                    f.initCause(e);
                    throw f;
                }
            }
            this.request = new HttpGet(url);
            synchronized (AndroidHttpConnectionFactory.lock) {
                AndroidHttpConnectionFactory.access$208();
            }
        }

        private HttpResponse getResponse() throws IOException {
            if (this.response == null) {
                if (this.baos != null) {
                    ((HttpPost) this.request).setEntity(new ByteArrayEntity(this.baos.toByteArray()));
                }
                try {
                    this.response = AndroidHttpConnectionFactory.this.client.execute(this.request);
                } catch (IOException e) {
                    throw e;
                }
            }
            return this.response;
        }

        public DataOutputStream openDataOutputStream() throws IOException {
            if (this.request instanceof HttpPost) {
                this.baos = new ByteArrayOutputStream();
                return new DataOutputStream(this.baos);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't open output stream on a GET to ");
            stringBuilder.append(this.request.getURI());
            throw new IOException(stringBuilder.toString());
        }

        public DataInputStream openDataInputStream() throws IOException {
            return new DataInputStream(getResponse().getEntity().getContent());
        }

        public void setConnectionProperty(String property, String value) throws IOException {
            if (!"Content-Length".equals(property) && !"Transfer-Encoding".equals(property)) {
                this.request.setHeader(property, value);
            }
        }

        public long getLength() throws IOException {
            return getResponse().getEntity().getContentLength();
        }

        public int getResponseCode() throws IOException {
            return getResponse().getStatusLine().getStatusCode();
        }

        public String getContentType() throws IOException {
            Header header = getResponse().getEntity().getContentType();
            return header == null ? "" : header.getValue();
        }

        public void close() throws IOException {
            if (this.response != null) {
                HttpEntity entity = this.response.getEntity();
                if (entity != null) {
                    entity.consumeContent();
                }
            }
            synchronized (AndroidHttpConnectionFactory.lock) {
                if (this.closed) {
                    return;
                }
                this.closed = true;
                AndroidHttpConnectionFactory.access$210();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Connection closed.  # of open connections=");
                stringBuilder.append(AndroidHttpConnectionFactory.numOpenConnection);
                String logMessage = stringBuilder.toString();
            }
        }
    }

    static /* synthetic */ int access$208() {
        int i = numOpenConnection;
        numOpenConnection = i + 1;
        return i;
    }

    static /* synthetic */ int access$210() {
        int i = numOpenConnection;
        numOpenConnection = i - 1;
        return i;
    }

    public AndroidHttpConnectionFactory(Context context) {
        this.context = context;
    }

    public GoogleHttpConnection createConnection(String url, boolean usePost) throws IOException {
        if (this.client == null) {
            this.client = new GoogleHttpClient(this.context.getContentResolver(), this.userAgent, true);
            ConnManagerParams.setMaxConnectionsPerRoute(this.client.getParams(), new ConnPerRouteBean(4));
        }
        return new AndroidGoogleHttpConnection(url, usePost);
    }

    public void close() {
        if (this.client != null) {
            this.client.close();
        }
    }

    public GoogleHttpClient getClient() {
        return this.client;
    }
}
