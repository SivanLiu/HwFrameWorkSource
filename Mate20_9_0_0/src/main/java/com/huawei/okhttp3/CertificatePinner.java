package com.huawei.okhttp3;

import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.tls.CertificateChainCleaner;
import com.huawei.okio.ByteString;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLPeerUnverifiedException;

public final class CertificatePinner {
    public static final CertificatePinner DEFAULT = new Builder().build();
    private final CertificateChainCleaner certificateChainCleaner;
    private final Set<Pin> pins;

    public static final class Builder {
        private final List<Pin> pins = new ArrayList();

        public Builder add(String pattern, String... pins) {
            if (pattern != null) {
                for (String pin : pins) {
                    this.pins.add(new Pin(pattern, pin));
                }
                return this;
            }
            throw new NullPointerException("pattern == null");
        }

        public CertificatePinner build() {
            return new CertificatePinner(new LinkedHashSet(this.pins), null);
        }
    }

    static final class Pin {
        private static final String WILDCARD = "*.";
        final String canonicalHostname;
        final ByteString hash;
        final String hashAlgorithm;
        final String pattern;

        Pin(String pattern, String pin) {
            HttpUrl url;
            this.pattern = pattern;
            StringBuilder stringBuilder;
            if (pattern.startsWith(WILDCARD)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("http://");
                stringBuilder.append(pattern.substring(WILDCARD.length()));
                url = HttpUrl.parse(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("http://");
                stringBuilder.append(pattern);
                url = HttpUrl.parse(stringBuilder.toString());
            }
            if (url != null) {
                StringBuilder stringBuilder2;
                this.canonicalHostname = url.host();
                if (pin.startsWith("sha1/")) {
                    this.hashAlgorithm = "sha1/";
                    this.hash = ByteString.decodeBase64(pin.substring("sha1/".length()));
                } else if (pin.startsWith("sha256/")) {
                    this.hashAlgorithm = "sha256/";
                    this.hash = ByteString.decodeBase64(pin.substring("sha256/".length()));
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("pins must start with 'sha256/' or 'sha1/': ");
                    stringBuilder2.append(pin);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
                if (this.hash == null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("pins must be base64: ");
                    stringBuilder2.append(pin);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
                return;
            }
            throw new IllegalArgumentException("pins cannot get the host");
        }

        boolean matches(String hostname) {
            if (!this.pattern.startsWith(WILDCARD)) {
                return hostname.equals(this.canonicalHostname);
            }
            return hostname.regionMatches(false, hostname.indexOf(46) + 1, this.canonicalHostname, 0, this.canonicalHostname.length());
        }

        public boolean equals(Object other) {
            return (other instanceof Pin) && this.pattern.equals(((Pin) other).pattern) && this.hashAlgorithm.equals(((Pin) other).hashAlgorithm) && this.hash.equals(((Pin) other).hash);
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * 17) + this.pattern.hashCode())) + this.hashAlgorithm.hashCode())) + this.hash.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.hashAlgorithm);
            stringBuilder.append(this.hash.base64());
            return stringBuilder.toString();
        }
    }

    CertificatePinner(Set<Pin> pins, CertificateChainCleaner certificateChainCleaner) {
        this.pins = pins;
        this.certificateChainCleaner = certificateChainCleaner;
    }

    public boolean equals(Object other) {
        boolean z = true;
        if (other == this) {
            return true;
        }
        if (!((other instanceof CertificatePinner) && Util.equal(this.certificateChainCleaner, ((CertificatePinner) other).certificateChainCleaner) && this.pins.equals(((CertificatePinner) other).pins))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * (this.certificateChainCleaner != null ? this.certificateChainCleaner.hashCode() : 0)) + this.pins.hashCode();
    }

    public void check(String hostname, List<Certificate> peerCertificates) throws SSLPeerUnverifiedException {
        List<Pin> pins = findMatchingPins(hostname);
        if (!pins.isEmpty()) {
            if (this.certificateChainCleaner != null) {
                peerCertificates = this.certificateChainCleaner.clean(peerCertificates, hostname);
            }
            int certsSize = peerCertificates.size();
            for (int c = 0; c < certsSize; c++) {
                X509Certificate x509Certificate = (X509Certificate) peerCertificates.get(c);
                ByteString sha1 = null;
                ByteString sha256 = null;
                int pinsSize = pins.size();
                for (int p = 0; p < pinsSize; p++) {
                    Pin pin = (Pin) pins.get(p);
                    if (pin.hashAlgorithm.equals("sha256/")) {
                        if (sha256 == null) {
                            sha256 = sha256(x509Certificate);
                        }
                        if (pin.hash.equals(sha256)) {
                            return;
                        }
                    } else if (pin.hashAlgorithm.equals("sha1/")) {
                        if (sha1 == null) {
                            sha1 = sha1(x509Certificate);
                        }
                        if (pin.hash.equals(sha1)) {
                            return;
                        }
                    } else {
                        throw new AssertionError();
                    }
                }
            }
            StringBuilder message = new StringBuilder();
            message.append("Certificate pinning failure!");
            message = message.append("\n  Peer certificate chain:");
            int certsSize2 = peerCertificates.size();
            for (certsSize = 0; certsSize < certsSize2; certsSize++) {
                X509Certificate x509Certificate2 = (X509Certificate) peerCertificates.get(certsSize);
                message.append("\n    ");
                message.append(pin(x509Certificate2));
                message.append(": ");
                message.append(x509Certificate2.getSubjectDN().getName());
            }
            message.append("\n  Pinned certificates for ");
            message.append(hostname);
            message.append(":");
            certsSize2 = pins.size();
            for (certsSize = 0; certsSize < certsSize2; certsSize++) {
                Pin pin2 = (Pin) pins.get(certsSize);
                message.append("\n    ");
                message.append(pin2);
            }
            throw new SSLPeerUnverifiedException(message.toString());
        }
    }

    public void check(String hostname, Certificate... peerCertificates) throws SSLPeerUnverifiedException {
        check(hostname, Arrays.asList(peerCertificates));
    }

    List<Pin> findMatchingPins(String hostname) {
        List<Pin> result = Collections.emptyList();
        for (Pin pin : this.pins) {
            if (pin.matches(hostname)) {
                if (result.isEmpty()) {
                    result = new ArrayList();
                }
                result.add(pin);
            }
        }
        return result;
    }

    CertificatePinner withCertificateChainCleaner(CertificateChainCleaner certificateChainCleaner) {
        if (Util.equal(this.certificateChainCleaner, certificateChainCleaner)) {
            return this;
        }
        return new CertificatePinner(this.pins, certificateChainCleaner);
    }

    public static String pin(Certificate certificate) {
        if (certificate instanceof X509Certificate) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sha256/");
            stringBuilder.append(sha256((X509Certificate) certificate).base64());
            return stringBuilder.toString();
        }
        throw new IllegalArgumentException("Certificate pinning requires X509 certificates");
    }

    static ByteString sha1(X509Certificate x509Certificate) {
        return ByteString.of(x509Certificate.getPublicKey().getEncoded()).sha1();
    }

    static ByteString sha256(X509Certificate x509Certificate) {
        return ByteString.of(x509Certificate.getPublicKey().getEncoded()).sha256();
    }
}
