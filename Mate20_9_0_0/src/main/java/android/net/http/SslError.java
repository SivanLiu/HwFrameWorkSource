package android.net.http;

import java.security.cert.X509Certificate;

public class SslError {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int SSL_DATE_INVALID = 4;
    public static final int SSL_EXPIRED = 1;
    public static final int SSL_IDMISMATCH = 2;
    public static final int SSL_INVALID = 5;
    @Deprecated
    public static final int SSL_MAX_ERROR = 6;
    public static final int SSL_NOTYETVALID = 0;
    public static final int SSL_UNTRUSTED = 3;
    final SslCertificate mCertificate;
    int mErrors;
    final String mUrl;

    @Deprecated
    public SslError(int error, SslCertificate certificate) {
        this(error, certificate, "");
    }

    @Deprecated
    public SslError(int error, X509Certificate certificate) {
        this(error, certificate, "");
    }

    public SslError(int error, SslCertificate certificate, String url) {
        addError(error);
        this.mCertificate = certificate;
        this.mUrl = url;
    }

    public SslError(int error, X509Certificate certificate, String url) {
        this(error, new SslCertificate(certificate), url);
    }

    public static SslError SslErrorFromChromiumErrorCode(int error, SslCertificate cert, String url) {
        if (error == -200) {
            return new SslError(2, cert, url);
        }
        if (error == -201) {
            return new SslError(4, cert, url);
        }
        if (error == -202) {
            return new SslError(3, cert, url);
        }
        return new SslError(5, cert, url);
    }

    public SslCertificate getCertificate() {
        return this.mCertificate;
    }

    public String getUrl() {
        return this.mUrl;
    }

    public boolean addError(int error) {
        boolean rval = error >= 0 && error < 6;
        if (rval) {
            this.mErrors = (1 << error) | this.mErrors;
        }
        return rval;
    }

    public boolean hasError(int error) {
        boolean z = false;
        boolean rval = error >= 0 && error < 6;
        if (!rval) {
            return rval;
        }
        if ((this.mErrors & (1 << error)) != 0) {
            z = true;
        }
        return z;
    }

    public int getPrimaryError() {
        if (this.mErrors != 0) {
            for (int error = 5; error >= 0; error--) {
                if ((this.mErrors & (1 << error)) != 0) {
                    return error;
                }
            }
        }
        return -1;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("primary error: ");
        stringBuilder.append(getPrimaryError());
        stringBuilder.append(" certificate: ");
        stringBuilder.append(getCertificate());
        stringBuilder.append(" on URL: ");
        stringBuilder.append(getUrl());
        return stringBuilder.toString();
    }
}
