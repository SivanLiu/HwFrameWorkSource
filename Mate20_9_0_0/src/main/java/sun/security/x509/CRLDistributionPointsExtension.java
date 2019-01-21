package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class CRLDistributionPointsExtension extends Extension implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.extensions.CRLDistributionPoints";
    public static final String NAME = "CRLDistributionPoints";
    public static final String POINTS = "points";
    private List<DistributionPoint> distributionPoints;
    private String extensionName;

    public CRLDistributionPointsExtension(List<DistributionPoint> distributionPoints) throws IOException {
        this(false, (List) distributionPoints);
    }

    public CRLDistributionPointsExtension(boolean isCritical, List<DistributionPoint> distributionPoints) throws IOException {
        this(PKIXExtensions.CRLDistributionPoints_Id, isCritical, (List) distributionPoints, NAME);
    }

    protected CRLDistributionPointsExtension(ObjectIdentifier extensionId, boolean isCritical, List<DistributionPoint> distributionPoints, String extensionName) throws IOException {
        this.extensionId = extensionId;
        this.critical = isCritical;
        this.distributionPoints = distributionPoints;
        encodeThis();
        this.extensionName = extensionName;
    }

    public CRLDistributionPointsExtension(Boolean critical, Object value) throws IOException {
        this(PKIXExtensions.CRLDistributionPoints_Id, critical, value, NAME);
    }

    protected CRLDistributionPointsExtension(ObjectIdentifier extensionId, Boolean critical, Object value, String extensionName) throws IOException {
        this.extensionId = extensionId;
        this.critical = critical.booleanValue();
        if (value instanceof byte[]) {
            this.extensionValue = (byte[]) value;
            DerValue val = new DerValue(this.extensionValue);
            if (val.tag == (byte) 48) {
                this.distributionPoints = new ArrayList();
                while (val.data.available() != 0) {
                    this.distributionPoints.add(new DistributionPoint(val.data.getDerValue()));
                }
                this.extensionName = extensionName;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid encoding for ");
            stringBuilder.append(extensionName);
            stringBuilder.append(" extension.");
            throw new IOException(stringBuilder.toString());
        }
        throw new IOException("Illegal argument type");
    }

    public String getName() {
        return this.extensionName;
    }

    public void encode(OutputStream out) throws IOException {
        encode(out, PKIXExtensions.CRLDistributionPoints_Id, false);
    }

    protected void encode(OutputStream out, ObjectIdentifier extensionId, boolean isCritical) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = extensionId;
            this.critical = isCritical;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    public void set(String name, Object obj) throws IOException {
        if (!name.equalsIgnoreCase(POINTS)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attribute name [");
            stringBuilder.append(name);
            stringBuilder.append("] not recognized by CertAttrSet:");
            stringBuilder.append(this.extensionName);
            stringBuilder.append(".");
            throw new IOException(stringBuilder.toString());
        } else if (obj instanceof List) {
            this.distributionPoints = (List) obj;
            encodeThis();
        } else {
            throw new IOException("Attribute value should be of type List.");
        }
    }

    public List<DistributionPoint> get(String name) throws IOException {
        if (name.equalsIgnoreCase(POINTS)) {
            return this.distributionPoints;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute name [");
        stringBuilder.append(name);
        stringBuilder.append("] not recognized by CertAttrSet:");
        stringBuilder.append(this.extensionName);
        stringBuilder.append(".");
        throw new IOException(stringBuilder.toString());
    }

    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(POINTS)) {
            this.distributionPoints = Collections.emptyList();
            encodeThis();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute name [");
        stringBuilder.append(name);
        stringBuilder.append("] not recognized by CertAttrSet:");
        stringBuilder.append(this.extensionName);
        stringBuilder.append('.');
        throw new IOException(stringBuilder.toString());
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(POINTS);
        return elements.elements();
    }

    private void encodeThis() throws IOException {
        if (this.distributionPoints.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream pnts = new DerOutputStream();
        for (DistributionPoint point : this.distributionPoints) {
            point.encode(pnts);
        }
        DerOutputStream seq = new DerOutputStream();
        seq.write((byte) 48, pnts);
        this.extensionValue = seq.toByteArray();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append(this.extensionName);
        stringBuilder.append(" [\n  ");
        stringBuilder.append(this.distributionPoints);
        stringBuilder.append("]\n");
        return stringBuilder.toString();
    }
}
