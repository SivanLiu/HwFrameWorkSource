package org.bouncycastle.est.jcajce;

import com.huawei.security.hccm.common.connection.HttpConnection.HttpHeaders;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Set;
import org.bouncycastle.est.ESTClient;
import org.bouncycastle.est.ESTClientSourceProvider;
import org.bouncycastle.est.ESTException;
import org.bouncycastle.est.ESTRequest;
import org.bouncycastle.est.ESTRequestBuilder;
import org.bouncycastle.est.ESTResponse;
import org.bouncycastle.est.Source;
import org.bouncycastle.util.Properties;

class DefaultESTClient implements ESTClient {
    private static byte[] CRLF = new byte[]{(byte) 13, (byte) 10};
    private static final Charset utf8 = Charset.forName("UTF-8");
    private final ESTClientSourceProvider sslSocketProvider;

    private class PrintingOutputStream extends OutputStream {
        private final OutputStream tgt;

        public PrintingOutputStream(OutputStream outputStream) {
            this.tgt = outputStream;
        }

        public void write(int i) throws IOException {
            System.out.print(String.valueOf((char) i));
            this.tgt.write(i);
        }
    }

    public DefaultESTClient(ESTClientSourceProvider eSTClientSourceProvider) {
        this.sslSocketProvider = eSTClientSourceProvider;
    }

    private static void writeLine(OutputStream outputStream, String str) throws IOException {
        outputStream.write(str.getBytes());
        outputStream.write(CRLF);
    }

    public ESTResponse doRequest(ESTRequest eSTRequest) throws IOException {
        ESTResponse performRequest;
        int i = 15;
        while (true) {
            performRequest = performRequest(eSTRequest);
            ESTRequest redirectURL = redirectURL(performRequest);
            if (redirectURL == null) {
                break;
            }
            i--;
            if (i <= 0) {
                break;
            }
            eSTRequest = redirectURL;
        }
        if (i != 0) {
            return performRequest;
        }
        throw new ESTException("Too many redirects..");
    }

    /* JADX WARNING: Removed duplicated region for block: B:46:0x015b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ESTResponse performRequest(ESTRequest eSTRequest) throws IOException {
        Throwable th;
        Source makeSource;
        try {
            makeSource = this.sslSocketProvider.makeSource(eSTRequest.getURL().getHost(), eSTRequest.getURL().getPort());
            try {
                String str;
                String format;
                if (eSTRequest.getListener() != null) {
                    eSTRequest = eSTRequest.getListener().onConnection(makeSource, eSTRequest);
                }
                Set asKeySet = Properties.asKeySet("org.bouncycastle.debug.est");
                OutputStream printingOutputStream = (asKeySet.contains("output") || asKeySet.contains("all")) ? new PrintingOutputStream(makeSource.getOutputStream()) : makeSource.getOutputStream();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(eSTRequest.getURL().getPath());
                stringBuilder.append(eSTRequest.getURL().getQuery() != null ? eSTRequest.getURL().getQuery() : "");
                String stringBuilder2 = stringBuilder.toString();
                ESTRequestBuilder eSTRequestBuilder = new ESTRequestBuilder(eSTRequest);
                if (!eSTRequest.getHeaders().containsKey(HttpHeaders.CONNECTION)) {
                    eSTRequestBuilder.addHeader(HttpHeaders.CONNECTION, HttpHeaders.CONNECTION_CLOSE);
                }
                URL url = eSTRequest.getURL();
                if (url.getPort() > -1) {
                    str = "Host";
                    format = String.format("%s:%d", new Object[]{url.getHost(), Integer.valueOf(url.getPort())});
                } else {
                    str = "Host";
                    format = url.getHost();
                }
                eSTRequestBuilder.setHeader(str, format);
                eSTRequest = eSTRequestBuilder.build();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(eSTRequest.getMethod());
                stringBuilder3.append(" ");
                stringBuilder3.append(stringBuilder2);
                stringBuilder3.append(" HTTP/1.1");
                writeLine(printingOutputStream, stringBuilder3.toString());
                for (Entry entry : eSTRequest.getHeaders().entrySet()) {
                    String[] strArr = (String[]) entry.getValue();
                    for (int i = 0; i != strArr.length; i++) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append((String) entry.getKey());
                        stringBuilder4.append(": ");
                        stringBuilder4.append(strArr[i]);
                        writeLine(printingOutputStream, stringBuilder4.toString());
                    }
                }
                printingOutputStream.write(CRLF);
                printingOutputStream.flush();
                eSTRequest.writeData(printingOutputStream);
                printingOutputStream.flush();
                if (eSTRequest.getHijacker() == null) {
                    return new ESTResponse(eSTRequest, makeSource);
                }
                ESTResponse hijack = eSTRequest.getHijacker().hijack(eSTRequest, makeSource);
                if (makeSource != null && hijack == null) {
                    makeSource.close();
                }
                return hijack;
            } catch (Throwable th2) {
                th = th2;
                if (makeSource != null) {
                    makeSource.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            makeSource = null;
            if (makeSource != null) {
            }
            throw th;
        }
    }

    protected ESTRequest redirectURL(ESTResponse eSTResponse) throws IOException {
        ESTRequest eSTRequest;
        if (eSTResponse.getStatusCode() < 300 || eSTResponse.getStatusCode() > 399) {
            eSTRequest = null;
        } else {
            StringBuilder stringBuilder;
            switch (eSTResponse.getStatusCode()) {
                case 301:
                case 302:
                case 303:
                case 306:
                case 307:
                    String header = eSTResponse.getHeader("Location");
                    if (!"".equals(header)) {
                        ESTRequestBuilder withURL;
                        ESTRequestBuilder eSTRequestBuilder = new ESTRequestBuilder(eSTResponse.getOriginalRequest());
                        if (header.startsWith("http")) {
                            withURL = eSTRequestBuilder.withURL(new URL(header));
                        } else {
                            URL url = eSTResponse.getOriginalRequest().getURL();
                            withURL = eSTRequestBuilder.withURL(new URL(url.getProtocol(), url.getHost(), url.getPort(), header));
                        }
                        eSTRequest = withURL.build();
                        break;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Redirect status type: ");
                    stringBuilder.append(eSTResponse.getStatusCode());
                    stringBuilder.append(" but no location header");
                    throw new ESTException(stringBuilder.toString());
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Client does not handle http status code: ");
                    stringBuilder.append(eSTResponse.getStatusCode());
                    throw new ESTException(stringBuilder.toString());
            }
        }
        if (eSTRequest != null) {
            eSTResponse.close();
        }
        return eSTRequest;
    }
}
