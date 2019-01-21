package javax.security.cert;

import com.sun.security.cert.internal.x509.X509V1CertImpl;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Date;

public abstract class X509Certificate extends Certificate {
    private static final String DEFAULT_X509_CERT_CLASS = X509V1CertImpl.class.getName();
    private static String X509Provider = ((String) AccessController.doPrivileged(new PrivilegedAction<String>() {
        public String run() {
            return Security.getProperty(X509Certificate.X509_PROVIDER);
        }
    }));
    private static final String X509_PROVIDER = "cert.provider.x509v1";

    public abstract void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException;

    public abstract void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException;

    public abstract Principal getIssuerDN();

    public abstract Date getNotAfter();

    public abstract Date getNotBefore();

    public abstract BigInteger getSerialNumber();

    public abstract String getSigAlgName();

    public abstract String getSigAlgOID();

    public abstract byte[] getSigAlgParams();

    public abstract Principal getSubjectDN();

    public abstract int getVersion();

    public static final X509Certificate getInstance(InputStream inStream) throws CertificateException {
        return getInst(inStream);
    }

    public static final X509Certificate getInstance(byte[] certData) throws CertificateException {
        return getInst(certData);
    }

    private static final X509Certificate getInst(Object value) throws CertificateException {
        StringBuilder stringBuilder;
        String className = X509Provider;
        if (className == null || className.length() == 0) {
            className = DEFAULT_X509_CERT_CLASS;
        }
        try {
            Class<?>[] params;
            if (value instanceof InputStream) {
                params = new Class[]{InputStream.class};
            } else if (value instanceof byte[]) {
                params = new Class[]{value.getClass()};
            } else {
                throw new CertificateException("Unsupported argument type");
            }
            return (X509Certificate) Class.forName(className).getConstructor(params).newInstance(value);
        } catch (ClassNotFoundException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find class: ");
            stringBuilder.append(e);
            throw new CertificateException(stringBuilder.toString());
        } catch (IllegalAccessException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not access class: ");
            stringBuilder.append(e2);
            throw new CertificateException(stringBuilder.toString());
        } catch (InstantiationException e22) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Problems instantiating: ");
            stringBuilder.append(e22);
            throw new CertificateException(stringBuilder.toString());
        } catch (InvocationTargetException e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("InvocationTargetException: ");
            stringBuilder.append(e3.getTargetException());
            throw new CertificateException(stringBuilder.toString());
        } catch (NoSuchMethodException e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find class method: ");
            stringBuilder.append(e4.getMessage());
            throw new CertificateException(stringBuilder.toString());
        }
    }
}
