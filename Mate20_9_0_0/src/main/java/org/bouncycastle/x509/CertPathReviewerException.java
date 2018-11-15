package org.bouncycastle.x509;

import java.security.cert.CertPath;
import org.bouncycastle.i18n.ErrorBundle;
import org.bouncycastle.i18n.LocalizedException;

public class CertPathReviewerException extends LocalizedException {
    private CertPath certPath = null;
    private int index = -1;

    public CertPathReviewerException(ErrorBundle errorBundle) {
        super(errorBundle);
    }

    public CertPathReviewerException(ErrorBundle errorBundle, Throwable th) {
        super(errorBundle, th);
    }

    public CertPathReviewerException(ErrorBundle errorBundle, Throwable th, CertPath certPath, int i) {
        super(errorBundle, th);
        if (certPath == null || i == -1) {
            throw new IllegalArgumentException();
        } else if (i < -1 || (certPath != null && i >= certPath.getCertificates().size())) {
            throw new IndexOutOfBoundsException();
        } else {
            this.certPath = certPath;
            this.index = i;
        }
    }

    public CertPathReviewerException(ErrorBundle errorBundle, CertPath certPath, int i) {
        super(errorBundle);
        if (certPath == null || i == -1) {
            throw new IllegalArgumentException();
        } else if (i < -1 || (certPath != null && i >= certPath.getCertificates().size())) {
            throw new IndexOutOfBoundsException();
        } else {
            this.certPath = certPath;
            this.index = i;
        }
    }

    public CertPath getCertPath() {
        return this.certPath;
    }

    public int getIndex() {
        return this.index;
    }
}
