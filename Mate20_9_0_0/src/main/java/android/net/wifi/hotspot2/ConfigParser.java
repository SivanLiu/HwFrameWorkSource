package android.net.wifi.hotspot2;

import android.net.wifi.hotspot2.omadm.PpsMoParser;
import android.security.KeyChain;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ConfigParser {
    private static final String BOUNDARY = "boundary=";
    private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ENCODING_BASE64 = "base64";
    private static final String TAG = "ConfigParser";
    private static final String TYPE_CA_CERT = "application/x-x509-ca-cert";
    private static final String TYPE_MULTIPART_MIXED = "multipart/mixed";
    private static final String TYPE_PASSPOINT_PROFILE = "application/x-passpoint-profile";
    private static final String TYPE_PKCS12 = "application/x-pkcs12";
    private static final String TYPE_WIFI_CONFIG = "application/x-wifi-config";

    private static class MimeHeader {
        public String boundary;
        public String contentType;
        public String encodingType;

        private MimeHeader() {
            this.contentType = null;
            this.boundary = null;
            this.encodingType = null;
        }
    }

    private static class MimePart {
        public byte[] data;
        public boolean isLast;
        public String type;

        private MimePart() {
            this.type = null;
            this.data = null;
            this.isLast = false;
        }
    }

    public static PasspointConfiguration parsePasspointConfig(String mimeType, byte[] data) {
        if (TextUtils.equals(mimeType, TYPE_WIFI_CONFIG)) {
            try {
                return createPasspointConfig(parseMimeMultipartMessage(new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(Base64.decode(new String(data, StandardCharsets.ISO_8859_1), 0)), StandardCharsets.ISO_8859_1))));
            } catch (IOException | IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse installation file: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unexpected MIME type: ");
        stringBuilder2.append(mimeType);
        Log.e(str2, stringBuilder2.toString());
        return null;
    }

    private static PasspointConfiguration createPasspointConfig(Map<String, byte[]> mimeParts) throws IOException {
        byte[] profileData = (byte[]) mimeParts.get(TYPE_PASSPOINT_PROFILE);
        if (profileData != null) {
            PasspointConfiguration config = PpsMoParser.parseMoText(new String(profileData));
            if (config == null) {
                throw new IOException("Failed to parse Passpoint profile");
            } else if (config.getCredential() != null) {
                byte[] caCertData = (byte[]) mimeParts.get(TYPE_CA_CERT);
                if (caCertData != null) {
                    try {
                        config.getCredential().setCaCertificate(parseCACert(caCertData));
                    } catch (CertificateException e) {
                        throw new IOException("Failed to parse CA Certificate");
                    }
                }
                byte[] pkcs12Data = (byte[]) mimeParts.get(TYPE_PKCS12);
                if (pkcs12Data != null) {
                    try {
                        Pair<PrivateKey, List<X509Certificate>> clientKey = parsePkcs12(pkcs12Data);
                        config.getCredential().setClientPrivateKey((PrivateKey) clientKey.first);
                        config.getCredential().setClientCertificateChain((X509Certificate[]) ((List) clientKey.second).toArray(new X509Certificate[((List) clientKey.second).size()]));
                    } catch (IOException | GeneralSecurityException e2) {
                        throw new IOException("Failed to parse PCKS12 string");
                    }
                }
                return config;
            } else {
                throw new IOException("Passpoint profile missing credential");
            }
        }
        throw new IOException("Missing Passpoint Profile");
    }

    private static Map<String, byte[]> parseMimeMultipartMessage(LineNumberReader in) throws IOException {
        MimeHeader header = parseHeaders(in);
        StringBuilder stringBuilder;
        if (!TextUtils.equals(header.contentType, TYPE_MULTIPART_MIXED)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid content type: ");
            stringBuilder.append(header.contentType);
            throw new IOException(stringBuilder.toString());
        } else if (TextUtils.isEmpty(header.boundary)) {
            throw new IOException("Missing boundary string");
        } else if (TextUtils.equals(header.encodingType, ENCODING_BASE64)) {
            while (true) {
                String line = in.readLine();
                if (line != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("--");
                    stringBuilder.append(header.boundary);
                    if (line.equals(stringBuilder.toString())) {
                        line = new HashMap();
                        MimePart mimePart;
                        do {
                            mimePart = parseMimePart(in, header.boundary);
                            line.put(mimePart.type, mimePart.data);
                        } while (!mimePart.isLast);
                        return line;
                    }
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected EOF before first boundary @ ");
                    stringBuilder2.append(in.getLineNumber());
                    throw new IOException(stringBuilder2.toString());
                }
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected encoding: ");
            stringBuilder.append(header.encodingType);
            throw new IOException(stringBuilder.toString());
        }
    }

    private static MimePart parseMimePart(LineNumberReader in, String boundary) throws IOException {
        MimeHeader header = parseHeaders(in);
        StringBuilder stringBuilder;
        if (!TextUtils.equals(header.encodingType, ENCODING_BASE64)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected encoding type: ");
            stringBuilder.append(header.encodingType);
            throw new IOException(stringBuilder.toString());
        } else if (TextUtils.equals(header.contentType, TYPE_PASSPOINT_PROFILE) || TextUtils.equals(header.contentType, TYPE_CA_CERT) || TextUtils.equals(header.contentType, TYPE_PKCS12)) {
            StringBuilder text = new StringBuilder();
            boolean isLast = false;
            String partBoundary = new StringBuilder();
            partBoundary.append("--");
            partBoundary.append(boundary);
            partBoundary = partBoundary.toString();
            String endBoundary = new StringBuilder();
            endBoundary.append(partBoundary);
            endBoundary.append("--");
            endBoundary = endBoundary.toString();
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected EOF file in body @ ");
                    stringBuilder2.append(in.getLineNumber());
                    throw new IOException(stringBuilder2.toString());
                } else if (line.startsWith(partBoundary)) {
                    if (line.equals(endBoundary)) {
                        isLast = true;
                    }
                    line = new MimePart();
                    line.type = header.contentType;
                    line.data = Base64.decode(text.toString(), 0);
                    line.isLast = isLast;
                    return line;
                } else {
                    text.append(line);
                }
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected content type: ");
            stringBuilder.append(header.contentType);
            throw new IOException(stringBuilder.toString());
        }
    }

    private static MimeHeader parseHeaders(LineNumberReader in) throws IOException {
        MimeHeader header = new MimeHeader();
        for (Entry<String, String> entry : readHeaders(in).entrySet()) {
            String str = (String) entry.getKey();
            Object obj = -1;
            int hashCode = str.hashCode();
            if (hashCode != 747297921) {
                if (hashCode == 949037134 && str.equals(CONTENT_TYPE)) {
                    obj = null;
                }
            } else if (str.equals(CONTENT_TRANSFER_ENCODING)) {
                obj = 1;
            }
            switch (obj) {
                case null:
                    Pair<String, String> value = parseContentType((String) entry.getValue());
                    header.contentType = (String) value.first;
                    header.boundary = (String) value.second;
                    break;
                case 1:
                    header.encodingType = (String) entry.getValue();
                    break;
                default:
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignore header: ");
                    stringBuilder.append((String) entry.getKey());
                    Log.d(str, stringBuilder.toString());
                    break;
            }
        }
        return header;
    }

    private static Pair<String, String> parseContentType(String contentType) throws IOException {
        String[] attributes = contentType.split(";");
        if (attributes.length >= 1) {
            String type = attributes[0].trim();
            String boundary = null;
            for (int i = 1; i < attributes.length; i++) {
                String attribute = attributes[i].trim();
                if (attribute.startsWith(BOUNDARY)) {
                    boundary = attribute.substring(BOUNDARY.length());
                    if (boundary.length() > 1 && boundary.startsWith("\"") && boundary.endsWith("\"")) {
                        boundary = boundary.substring(1, boundary.length() - 1);
                    }
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignore Content-Type attribute: ");
                    stringBuilder.append(attributes[i]);
                    Log.d(str, stringBuilder.toString());
                }
            }
            return new Pair(type, boundary);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid Content-Type: ");
        stringBuilder2.append(contentType);
        throw new IOException(stringBuilder2.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x00af  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Map<String, String> readHeaders(LineNumberReader in) throws IOException {
        Map<String, String> headers = new HashMap();
        String name = null;
        StringBuilder value = null;
        while (true) {
            String line = in.readLine();
            if (line == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Missing line @ ");
                stringBuilder.append(in.getLineNumber());
                throw new IOException(stringBuilder.toString());
            } else if (line.length() != 0 && line.trim().length() != 0) {
                int nameEnd = line.indexOf(58);
                StringBuilder stringBuilder2;
                if (nameEnd < 0) {
                    if (value != null) {
                        value.append(' ');
                        value.append(line.trim());
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Bad header line: '");
                        stringBuilder2.append(line);
                        stringBuilder2.append("' @ ");
                        stringBuilder2.append(in.getLineNumber());
                        throw new IOException(stringBuilder2.toString());
                    }
                } else if (Character.isWhitespace(line.charAt(0))) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Illegal blank prefix in header line '");
                    stringBuilder2.append(line);
                    stringBuilder2.append("' @ ");
                    stringBuilder2.append(in.getLineNumber());
                    throw new IOException(stringBuilder2.toString());
                } else {
                    if (name != null) {
                        headers.put(name, value.toString());
                    }
                    name = line.substring(0, nameEnd).trim();
                    value = new StringBuilder();
                    value.append(line.substring(nameEnd + 1).trim());
                }
            } else if (name != null) {
                headers.put(name, value.toString());
            }
        }
        if (name != null) {
        }
        return headers;
    }

    private static X509Certificate parseCACert(byte[] octets) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(octets));
    }

    private static Pair<PrivateKey, List<X509Certificate>> parsePkcs12(byte[] octets) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyChain.EXTRA_PKCS12);
        ByteArrayInputStream in = new ByteArrayInputStream(octets);
        int i = 0;
        ks.load(in, new char[0]);
        in.close();
        if (ks.size() == 1) {
            String alias = (String) ks.aliases().nextElement();
            if (alias != null) {
                PrivateKey clientKey = (PrivateKey) ks.getKey(alias, null);
                List<X509Certificate> clientCertificateChain = null;
                Certificate[] chain = ks.getCertificateChain(alias);
                if (chain != null) {
                    clientCertificateChain = new ArrayList();
                    int length = chain.length;
                    while (i < length) {
                        Certificate certificate = chain[i];
                        if (certificate instanceof X509Certificate) {
                            clientCertificateChain.add((X509Certificate) certificate);
                            i++;
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unexpceted certificate type: ");
                            stringBuilder.append(certificate.getClass());
                            throw new IOException(stringBuilder.toString());
                        }
                    }
                }
                return new Pair(clientKey, clientCertificateChain);
            }
            throw new IOException("No alias found");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unexpected key size: ");
        stringBuilder2.append(ks.size());
        throw new IOException(stringBuilder2.toString());
    }
}
