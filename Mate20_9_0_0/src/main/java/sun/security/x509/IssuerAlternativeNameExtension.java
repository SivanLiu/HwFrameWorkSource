package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class IssuerAlternativeNameExtension extends Extension implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.extensions.IssuerAlternativeName";
    public static final String ISSUER_NAME = "issuer_name";
    public static final String NAME = "IssuerAlternativeName";
    GeneralNames names;

    private void encodeThis() throws IOException {
        if (this.names == null || this.names.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream os = new DerOutputStream();
        this.names.encode(os);
        this.extensionValue = os.toByteArray();
    }

    public IssuerAlternativeNameExtension(GeneralNames names) throws IOException {
        this.names = null;
        this.names = names;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = false;
        encodeThis();
    }

    public IssuerAlternativeNameExtension(Boolean critical, GeneralNames names) throws IOException {
        this.names = null;
        this.names = names;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = critical.booleanValue();
        encodeThis();
    }

    public IssuerAlternativeNameExtension() {
        this.names = null;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = false;
        this.names = new GeneralNames();
    }

    public IssuerAlternativeNameExtension(Boolean critical, Object value) throws IOException {
        this.names = null;
        this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
        this.critical = critical.booleanValue();
        this.extensionValue = (byte[]) value;
        DerValue val = new DerValue(this.extensionValue);
        if (val.data == null) {
            this.names = new GeneralNames();
        } else {
            this.names = new GeneralNames(val);
        }
    }

    public String toString() {
        StringBuilder stringBuilder;
        String result = new StringBuilder();
        result.append(super.toString());
        result.append("IssuerAlternativeName [\n");
        result = result.toString();
        if (this.names == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append("  null\n");
            result = stringBuilder.toString();
        } else {
            for (Object name : this.names.names()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(result);
                stringBuilder2.append("  ");
                stringBuilder2.append(name);
                stringBuilder2.append("\n");
                result = stringBuilder2.toString();
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(result);
        stringBuilder.append("]\n");
        return stringBuilder.toString();
    }

    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.IssuerAlternativeName_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    public void set(String name, Object obj) throws IOException {
        if (!name.equalsIgnoreCase(ISSUER_NAME)) {
            throw new IOException("Attribute name not recognized by CertAttrSet:IssuerAlternativeName.");
        } else if (obj instanceof GeneralNames) {
            this.names = (GeneralNames) obj;
            encodeThis();
        } else {
            throw new IOException("Attribute value should be of type GeneralNames.");
        }
    }

    public GeneralNames get(String name) throws IOException {
        if (name.equalsIgnoreCase(ISSUER_NAME)) {
            return this.names;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:IssuerAlternativeName.");
    }

    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(ISSUER_NAME)) {
            this.names = null;
            encodeThis();
            return;
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:IssuerAlternativeName.");
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(ISSUER_NAME);
        return elements.elements();
    }

    public String getName() {
        return NAME;
    }
}
