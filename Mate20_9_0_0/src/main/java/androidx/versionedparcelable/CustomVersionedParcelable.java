package androidx.versionedparcelable;

import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

@RestrictTo({Scope.LIBRARY_GROUP})
public abstract class CustomVersionedParcelable implements VersionedParcelable {
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void onPreParceling(boolean isStream) {
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void onPostParceling() {
    }
}
