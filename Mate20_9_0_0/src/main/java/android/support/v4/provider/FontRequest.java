package android.support.v4.provider;

import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.util.Preconditions;
import android.util.Base64;
import java.util.List;

public final class FontRequest {
    private final List<List<byte[]>> mCertificates;
    private final int mCertificatesArray;
    private final String mIdentifier;
    private final String mProviderAuthority;
    private final String mProviderPackage;
    private final String mQuery;

    public FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage, @NonNull String query, @NonNull List<List<byte[]>> certificates) {
        this.mProviderAuthority = (String) Preconditions.checkNotNull(providerAuthority);
        this.mProviderPackage = (String) Preconditions.checkNotNull(providerPackage);
        this.mQuery = (String) Preconditions.checkNotNull(query);
        this.mCertificates = (List) Preconditions.checkNotNull(certificates);
        this.mCertificatesArray = 0;
        StringBuilder stringBuilder = new StringBuilder(this.mProviderAuthority);
        stringBuilder.append("-");
        stringBuilder.append(this.mProviderPackage);
        stringBuilder.append("-");
        stringBuilder.append(this.mQuery);
        this.mIdentifier = stringBuilder.toString();
    }

    public FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage, @NonNull String query, @ArrayRes int certificates) {
        this.mProviderAuthority = (String) Preconditions.checkNotNull(providerAuthority);
        this.mProviderPackage = (String) Preconditions.checkNotNull(providerPackage);
        this.mQuery = (String) Preconditions.checkNotNull(query);
        this.mCertificates = null;
        Preconditions.checkArgument(certificates != 0);
        this.mCertificatesArray = certificates;
        StringBuilder stringBuilder = new StringBuilder(this.mProviderAuthority);
        stringBuilder.append("-");
        stringBuilder.append(this.mProviderPackage);
        stringBuilder.append("-");
        stringBuilder.append(this.mQuery);
        this.mIdentifier = stringBuilder.toString();
    }

    @NonNull
    public String getProviderAuthority() {
        return this.mProviderAuthority;
    }

    @NonNull
    public String getProviderPackage() {
        return this.mProviderPackage;
    }

    @NonNull
    public String getQuery() {
        return this.mQuery;
    }

    @Nullable
    public List<List<byte[]>> getCertificates() {
        return this.mCertificates;
    }

    @ArrayRes
    public int getCertificatesArrayResId() {
        return this.mCertificatesArray;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
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
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCertificatesArray: ");
        stringBuilder.append(this.mCertificatesArray);
        builder.append(stringBuilder.toString());
        return builder.toString();
    }
}
