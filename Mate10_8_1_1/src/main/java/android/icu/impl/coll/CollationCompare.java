package android.icu.impl.coll;

public final class CollationCompare {
    static final /* synthetic */ boolean -assertionsDisabled = (CollationCompare.class.desiredAssertionStatus() ^ 1);

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int compareUpToQuaternary(CollationIterator left, CollationIterator right, CollationSettings settings) {
        long variableTop;
        long leftPrimary;
        long rightPrimary;
        int options = settings.options;
        if ((options & 12) == 0) {
            variableTop = 0;
        } else {
            variableTop = settings.variableTop + 1;
        }
        boolean anyVariable = false;
        while (true) {
            long ce = left.nextCE();
            leftPrimary = ce >>> 32;
            if (leftPrimary < variableTop && leftPrimary > Collation.MERGE_SEPARATOR_PRIMARY) {
                anyVariable = true;
                do {
                    left.setCurrentCE(-4294967296L & ce);
                    while (true) {
                        ce = left.nextCE();
                        leftPrimary = ce >>> 32;
                        if (leftPrimary != 0) {
                            break;
                        }
                        left.setCurrentCE(0);
                    }
                    if (leftPrimary >= variableTop) {
                        break;
                    }
                } while (leftPrimary > Collation.MERGE_SEPARATOR_PRIMARY);
            }
            if (leftPrimary != 0) {
                do {
                    ce = right.nextCE();
                    rightPrimary = ce >>> 32;
                    if (rightPrimary < variableTop && rightPrimary > Collation.MERGE_SEPARATOR_PRIMARY) {
                        anyVariable = true;
                        do {
                            right.setCurrentCE(-4294967296L & ce);
                            while (true) {
                                ce = right.nextCE();
                                rightPrimary = ce >>> 32;
                                if (rightPrimary != 0) {
                                    break;
                                }
                                right.setCurrentCE(0);
                            }
                            if (rightPrimary >= variableTop) {
                                break;
                            }
                        } while (rightPrimary > Collation.MERGE_SEPARATOR_PRIMARY);
                    }
                } while (rightPrimary == 0);
                if (leftPrimary != rightPrimary) {
                    break;
                } else if (leftPrimary == 1) {
                    break;
                }
            }
        }
        if (settings.hasReordering()) {
            leftPrimary = settings.reorder(leftPrimary);
            rightPrimary = settings.reorder(rightPrimary);
        }
        return leftPrimary < rightPrimary ? -1 : 1;
    }
}
