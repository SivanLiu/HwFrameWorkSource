package sun.security.x509;

import java.lang.reflect.Field;
import sun.misc.HexDumpEncoder;

/* compiled from: CertificateExtensions */
class UnparseableExtension extends Extension {
    private String name = "";
    private Throwable why;

    public UnparseableExtension(Extension ext, Throwable why) {
        super(ext);
        try {
            Class<?> extClass = OIDMap.getClass(ext.getExtensionId());
            if (extClass != null) {
                Field field = extClass.getDeclaredField("NAME");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append((String) field.get(null));
                stringBuilder.append(" ");
                this.name = stringBuilder.toString();
            }
        } catch (Exception e) {
        }
        this.why = why;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("Unparseable ");
        stringBuilder.append(this.name);
        stringBuilder.append("extension due to\n");
        stringBuilder.append(this.why);
        stringBuilder.append("\n\n");
        stringBuilder.append(new HexDumpEncoder().encodeBuffer(getExtensionValue()));
        return stringBuilder.toString();
    }
}
