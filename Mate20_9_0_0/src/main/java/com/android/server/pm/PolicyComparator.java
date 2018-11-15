package com.android.server.pm;

import android.util.Slog;
import java.util.Collections;
import java.util.Comparator;

/* compiled from: SELinuxMMAC */
final class PolicyComparator implements Comparator<Policy> {
    private boolean duplicateFound = false;

    PolicyComparator() {
    }

    public boolean foundDuplicate() {
        return this.duplicateFound;
    }

    public int compare(Policy p1, Policy p2) {
        int i = 1;
        if (p1.hasInnerPackages() != p2.hasInnerPackages()) {
            if (p1.hasInnerPackages()) {
                i = -1;
            }
            return i;
        }
        if (p1.getSignatures().equals(p2.getSignatures())) {
            if (p1.hasGlobalSeinfo()) {
                this.duplicateFound = true;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Duplicate policy entry: ");
                stringBuilder.append(p1.toString());
                Slog.e("SELinuxMMAC", stringBuilder.toString());
            }
            if (!Collections.disjoint(p1.getInnerPackages().keySet(), p2.getInnerPackages().keySet())) {
                this.duplicateFound = true;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Duplicate policy entry: ");
                stringBuilder2.append(p1.toString());
                Slog.e("SELinuxMMAC", stringBuilder2.toString());
            }
        }
        return 0;
    }
}
