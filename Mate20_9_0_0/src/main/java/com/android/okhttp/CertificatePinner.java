package com.android.okhttp;

import com.android.okhttp.internal.Util;
import com.android.okhttp.okio.ByteString;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLPeerUnverifiedException;

public final class CertificatePinner {
    public static final CertificatePinner DEFAULT = new Builder().build();
    private final Map<String, Set<ByteString>> hostnameToPins;

    public static final class Builder {
        private final Map<String, Set<ByteString>> hostnameToPins = new LinkedHashMap();

        public Builder add(String hostname, String... pins) {
            if (hostname != null) {
                Set<ByteString> hostPins = new LinkedHashSet();
                Set<ByteString> previousPins = (Set) this.hostnameToPins.put(hostname, Collections.unmodifiableSet(hostPins));
                if (previousPins != null) {
                    hostPins.addAll(previousPins);
                }
                int length = pins.length;
                int i = 0;
                while (i < length) {
                    String pin = pins[i];
                    StringBuilder stringBuilder;
                    if (pin.startsWith("sha1/")) {
                        ByteString decodedPin = ByteString.decodeBase64(pin.substring("sha1/".length()));
                        if (decodedPin != null) {
                            hostPins.add(decodedPin);
                            i++;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("pins must be base64: ");
                            stringBuilder.append(pin);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("pins must start with 'sha1/': ");
                    stringBuilder.append(pin);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                return this;
            }
            throw new IllegalArgumentException("hostname == null");
        }

        public CertificatePinner build() {
            return new CertificatePinner(this);
        }
    }

    private CertificatePinner(Builder builder) {
        this.hostnameToPins = Util.immutableMap(builder.hostnameToPins);
    }

    public void check(String hostname, List<Certificate> peerCertificates) throws SSLPeerUnverifiedException {
        Set<ByteString> pins = findMatchingPins(hostname);
        if (pins != null) {
            int i = 0;
            int size = peerCertificates.size();
            while (i < size) {
                if (!pins.contains(sha1((X509Certificate) peerCertificates.get(i)))) {
                    i++;
                } else {
                    return;
                }
            }
            StringBuilder message = new StringBuilder();
            message.append("Certificate pinning failure!");
            message = message.append("\n  Peer certificate chain:");
            int size2 = peerCertificates.size();
            for (size = 0; size < size2; size++) {
                X509Certificate x509Certificate = (X509Certificate) peerCertificates.get(size);
                message.append("\n    ");
                message.append(pin(x509Certificate));
                message.append(": ");
                message.append(x509Certificate.getSubjectDN().getName());
            }
            message.append("\n  Pinned certificates for ");
            message.append(hostname);
            message.append(":");
            for (ByteString pin : pins) {
                message.append("\n    sha1/");
                message.append(pin.base64());
            }
            throw new SSLPeerUnverifiedException(message.toString());
        }
    }

    public void check(String hostname, Certificate... peerCertificates) throws SSLPeerUnverifiedException {
        check(hostname, Arrays.asList(peerCertificates));
    }

    Set<ByteString> findMatchingPins(String hostname) {
        Set<ByteString> directPins = (Set) this.hostnameToPins.get(hostname);
        Set<ByteString> wildcardPins = null;
        int indexOfFirstDot = hostname.indexOf(46);
        if (indexOfFirstDot != hostname.lastIndexOf(46)) {
            Map map = this.hostnameToPins;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("*.");
            stringBuilder.append(hostname.substring(indexOfFirstDot + 1));
            wildcardPins = (Set) map.get(stringBuilder.toString());
        }
        if (directPins == null && wildcardPins == null) {
            return null;
        }
        if (directPins != null && wildcardPins != null) {
            Set<ByteString> pins = new LinkedHashSet();
            pins.addAll(directPins);
            pins.addAll(wildcardPins);
            return pins;
        } else if (directPins != null) {
            return directPins;
        } else {
            return wildcardPins;
        }
    }

    public static String pin(Certificate certificate) {
        if (certificate instanceof X509Certificate) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sha1/");
            stringBuilder.append(sha1((X509Certificate) certificate).base64());
            return stringBuilder.toString();
        }
        throw new IllegalArgumentException("Certificate pinning requires X509 certificates");
    }

    private static ByteString sha1(X509Certificate x509Certificate) {
        return Util.sha1(ByteString.of(x509Certificate.getPublicKey().getEncoded()));
    }
}
