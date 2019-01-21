package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public abstract class CertPath implements Serializable {
    private static final long serialVersionUID = 6068470306649138683L;
    private String type;

    protected static class CertPathRep implements Serializable {
        private static final long serialVersionUID = 3015633072427920915L;
        private byte[] data;
        private String type;

        protected CertPathRep(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        protected Object readResolve() throws ObjectStreamException {
            try {
                return CertificateFactory.getInstance(this.type).generateCertPath(new ByteArrayInputStream(this.data));
            } catch (CertificateException ce) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("java.security.cert.CertPath: ");
                stringBuilder.append(this.type);
                NotSerializableException nse = new NotSerializableException(stringBuilder.toString());
                nse.initCause(ce);
                throw nse;
            }
        }
    }

    public abstract List<? extends Certificate> getCertificates();

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract byte[] getEncoded(String str) throws CertificateEncodingException;

    public abstract Iterator<String> getEncodings();

    protected CertPath(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CertPath)) {
            return false;
        }
        CertPath otherCP = (CertPath) other;
        if (otherCP.getType().equals(this.type)) {
            return getCertificates().equals(otherCP.getCertificates());
        }
        return false;
    }

    public int hashCode() {
        return (31 * this.type.hashCode()) + getCertificates().hashCode();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append(this.type);
        stringBuilder.append(" Cert Path: length = ");
        stringBuilder.append(getCertificates().size());
        stringBuilder.append(".\n");
        sb.append(stringBuilder.toString());
        sb.append("[\n");
        int i = 1;
        for (Certificate stringCert : getCertificates()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("=========================================================Certificate ");
            stringBuilder2.append(i);
            stringBuilder2.append(" start.\n");
            sb.append(stringBuilder2.toString());
            sb.append(stringCert.toString());
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("\n=========================================================Certificate ");
            stringBuilder3.append(i);
            stringBuilder3.append(" end.\n\n\n");
            sb.append(stringBuilder3.toString());
            i++;
        }
        sb.append("\n]");
        return sb.toString();
    }

    protected Object writeReplace() throws ObjectStreamException {
        try {
            return new CertPathRep(this.type, getEncoded());
        } catch (CertificateException ce) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("java.security.cert.CertPath: ");
            stringBuilder.append(this.type);
            NotSerializableException nse = new NotSerializableException(stringBuilder.toString());
            nse.initCause(ce);
            throw nse;
        }
    }
}
