package org.bouncycastle.est.jcajce;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.bouncycastle.est.ESTClientSourceProvider;
import org.bouncycastle.est.Source;
import org.bouncycastle.util.Strings;

class DefaultESTClientSourceProvider implements ESTClientSourceProvider {
    private final Long absoluteLimit;
    private final ChannelBindingProvider bindingProvider;
    private final Set<String> cipherSuites;
    private final boolean filterSupportedSuites;
    private final JsseHostnameAuthorizer hostNameAuthorizer;
    private final SSLSocketFactory sslSocketFactory;
    private final int timeout;

    public DefaultESTClientSourceProvider(SSLSocketFactory sSLSocketFactory, JsseHostnameAuthorizer jsseHostnameAuthorizer, int i, ChannelBindingProvider channelBindingProvider, Set<String> set, Long l, boolean z) throws GeneralSecurityException {
        this.sslSocketFactory = sSLSocketFactory;
        this.hostNameAuthorizer = jsseHostnameAuthorizer;
        this.timeout = i;
        this.bindingProvider = channelBindingProvider;
        this.cipherSuites = set;
        this.absoluteLimit = l;
        this.filterSupportedSuites = z;
    }

    public Source makeSource(String str, int i) throws IOException {
        SSLSocket sSLSocket = (SSLSocket) this.sslSocketFactory.createSocket(str, i);
        sSLSocket.setSoTimeout(this.timeout);
        if (!(this.cipherSuites == null || this.cipherSuites.isEmpty())) {
            Object[] toArray;
            if (this.filterSupportedSuites) {
                HashSet hashSet = new HashSet();
                String[] supportedCipherSuites = sSLSocket.getSupportedCipherSuites();
                for (int i2 = 0; i2 != supportedCipherSuites.length; i2++) {
                    hashSet.add(supportedCipherSuites[i2]);
                }
                ArrayList arrayList = new ArrayList();
                for (String str2 : this.cipherSuites) {
                    if (hashSet.contains(str2)) {
                        arrayList.add(str2);
                    }
                }
                if (arrayList.isEmpty()) {
                    throw new IllegalStateException("No supplied cipher suite is supported by the provider.");
                }
                toArray = arrayList.toArray(new String[arrayList.size()]);
            } else {
                toArray = this.cipherSuites.toArray(new String[this.cipherSuites.size()]);
            }
            sSLSocket.setEnabledCipherSuites((String[]) toArray);
        }
        sSLSocket.startHandshake();
        if (this.hostNameAuthorizer == null || this.hostNameAuthorizer.verified(str, sSLSocket.getSession())) {
            String toLowerCase = Strings.toLowerCase(sSLSocket.getSession().getCipherSuite());
            if (toLowerCase.contains("_des_") || toLowerCase.contains("_des40_") || toLowerCase.contains("_3des_")) {
                throw new IOException("EST clients must not use DES ciphers");
            } else if (Strings.toLowerCase(sSLSocket.getSession().getCipherSuite()).contains("null")) {
                throw new IOException("EST clients must not use NULL ciphers");
            } else if (Strings.toLowerCase(sSLSocket.getSession().getCipherSuite()).contains("anon")) {
                throw new IOException("EST clients must not use anon ciphers");
            } else if (Strings.toLowerCase(sSLSocket.getSession().getCipherSuite()).contains("export")) {
                throw new IOException("EST clients must not use export ciphers");
            } else if (sSLSocket.getSession().getProtocol().equalsIgnoreCase("tlsv1")) {
                try {
                    sSLSocket.close();
                } catch (Exception e) {
                }
                throw new IOException("EST clients must not use TLSv1");
            } else if (this.hostNameAuthorizer == null || this.hostNameAuthorizer.verified(str, sSLSocket.getSession())) {
                return new LimitedSSLSocketSource(sSLSocket, this.bindingProvider, this.absoluteLimit);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Hostname was not verified: ");
                stringBuilder.append(str);
                throw new IOException(stringBuilder.toString());
            }
        }
        throw new IOException("Host name could not be verified.");
    }
}
