package android.provider;

import android.util.Base64;
import com.android.internal.util.Preconditions;
import java.util.Collections;
import java.util.List;

public final class FontRequest {
    private final List<List<byte[]>> mCertificates;
    private final String mIdentifier;
    private final String mProviderAuthority;
    private final String mProviderPackage;
    private final String mQuery;

    public FontRequest(String providerAuthority, String providerPackage, String query) {
        this.mProviderAuthority = (String) Preconditions.checkNotNull(providerAuthority);
        this.mQuery = (String) Preconditions.checkNotNull(query);
        this.mProviderPackage = (String) Preconditions.checkNotNull(providerPackage);
        this.mCertificates = Collections.emptyList();
        StringBuilder stringBuilder = new StringBuilder(this.mProviderAuthority);
        stringBuilder.append("-");
        stringBuilder.append(this.mProviderPackage);
        stringBuilder.append("-");
        stringBuilder.append(this.mQuery);
        this.mIdentifier = stringBuilder.toString();
    }

    public FontRequest(String providerAuthority, String providerPackage, String query, List<List<byte[]>> certificates) {
        this.mProviderAuthority = (String) Preconditions.checkNotNull(providerAuthority);
        this.mProviderPackage = (String) Preconditions.checkNotNull(providerPackage);
        this.mQuery = (String) Preconditions.checkNotNull(query);
        this.mCertificates = (List) Preconditions.checkNotNull(certificates);
        StringBuilder stringBuilder = new StringBuilder(this.mProviderAuthority);
        stringBuilder.append("-");
        stringBuilder.append(this.mProviderPackage);
        stringBuilder.append("-");
        stringBuilder.append(this.mQuery);
        this.mIdentifier = stringBuilder.toString();
    }

    public String getProviderAuthority() {
        return this.mProviderAuthority;
    }

    public String getProviderPackage() {
        return this.mProviderPackage;
    }

    public String getQuery() {
        return this.mQuery;
    }

    public List<List<byte[]>> getCertificates() {
        return this.mCertificates;
    }

    public String getIdentifier() {
        return this.mIdentifier;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("FontRequest {mProviderAuthority: ");
        stringBuilder.append(this.mProviderAuthority);
        stringBuilder.append(", mProviderPackage: ");
        stringBuilder.append(this.mProviderPackage);
        stringBuilder.append(", mQuery: ");
        stringBuilder.append(this.mQuery);
        stringBuilder.append(", mCertificates:");
        builder.append(stringBuilder.toString());
        for (int i = 0; i < this.mCertificates.size(); i++) {
            builder.append(" [");
            List<byte[]> set = (List) this.mCertificates.get(i);
            for (int j = 0; j < set.size(); j++) {
                builder.append(" \"");
                builder.append(Base64.encodeToString((byte[]) set.get(j), 0));
                builder.append("\"");
            }
            builder.append(" ]");
        }
        builder.append("}");
        return builder.toString();
    }
}
