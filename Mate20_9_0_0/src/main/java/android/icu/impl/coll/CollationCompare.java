package android.icu.impl.coll;

public final class CollationCompare {
    static final /* synthetic */ boolean $assertionsDisabled = false;

    /* JADX WARNING: Missing block: B:99:0x017c, code skipped:
            if (r15 != 1) goto L_0x017f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int compareUpToQuaternary(CollationIterator left, CollationIterator right, CollationSettings settings) {
        long variableTop;
        CollationIterator collationIterator = left;
        CollationIterator collationIterator2 = right;
        CollationSettings collationSettings = settings;
        int options = collationSettings.options;
        if ((options & 12) == 0) {
            variableTop = 0;
        } else {
            variableTop = collationSettings.variableTop + 1;
        }
        boolean anyVariable = false;
        while (true) {
            long ce = left.nextCE();
            long j = 32;
            long leftPrimary = ce >>> 32;
            if (leftPrimary < variableTop && leftPrimary > Collation.MERGE_SEPARATOR_PRIMARY) {
                do {
                    collationIterator.setCurrentCE(ce & -4294967296L);
                    while (true) {
                        ce = left.nextCE();
                        leftPrimary = ce >>> 32;
                        if (leftPrimary != 0) {
                            break;
                        }
                        collationIterator.setCurrentCE(0);
                    }
                    if (leftPrimary >= variableTop) {
                        break;
                    }
                } while (leftPrimary > Collation.MERGE_SEPARATOR_PRIMARY);
                anyVariable = true;
            }
            long j2;
            if (leftPrimary != 0) {
                long leftPrimary2;
                while (true) {
                    long ce2 = right.nextCE();
                    ce = ce2 >>> j;
                    if (ce >= variableTop || ce <= Collation.MERGE_SEPARATOR_PRIMARY) {
                        leftPrimary2 = leftPrimary;
                    } else {
                        while (true) {
                            leftPrimary2 = leftPrimary;
                            collationIterator2.setCurrentCE(ce2 & -4294967296L);
                            while (true) {
                                ce2 = right.nextCE();
                                ce = ce2 >>> 32;
                                if (ce != 0) {
                                    break;
                                }
                                collationIterator2.setCurrentCE(0);
                            }
                            if (ce >= variableTop || ce <= Collation.MERGE_SEPARATOR_PRIMARY) {
                                anyVariable = true;
                            } else {
                                leftPrimary = leftPrimary2;
                            }
                        }
                        anyVariable = true;
                    }
                    if (ce != 0) {
                        break;
                    }
                    j = 32;
                    leftPrimary = leftPrimary2;
                }
                if (leftPrimary2 != ce) {
                    if (settings.hasReordering()) {
                        leftPrimary = collationSettings.reorder(leftPrimary2);
                        ce = collationSettings.reorder(ce);
                    } else {
                        leftPrimary = leftPrimary2;
                    }
                    return leftPrimary < ce ? -1 : 1;
                } else if (leftPrimary2 == 1) {
                    int leftIndex;
                    int rightIndex;
                    int leftIndex2;
                    int rightIndex2;
                    long p;
                    int rightIndex3;
                    int leftIndex3;
                    int leftSecondary;
                    int rightSecondary;
                    if (CollationSettings.getStrength(options) >= 1) {
                        if ((options & 2048) == 0) {
                            leftIndex = 0;
                            rightIndex = 0;
                            while (true) {
                                leftIndex2 = leftIndex + 1;
                                leftIndex = ((int) collationIterator.getCE(leftIndex)) >>> 16;
                                if (leftIndex != 0) {
                                    while (true) {
                                        rightIndex2 = rightIndex + 1;
                                        rightIndex = ((int) collationIterator2.getCE(rightIndex)) >>> 16;
                                        if (rightIndex != 0) {
                                            break;
                                        }
                                        rightIndex = rightIndex2;
                                    }
                                    if (leftIndex != rightIndex) {
                                        return leftIndex < rightIndex ? -1 : 1;
                                    } else if (leftIndex == 256) {
                                        break;
                                    } else {
                                        leftIndex = leftIndex2;
                                        rightIndex = rightIndex2;
                                    }
                                } else {
                                    leftIndex = leftIndex2;
                                }
                            }
                        } else {
                            leftIndex = 0;
                            rightIndex = 0;
                            while (true) {
                                leftIndex2 = leftIndex;
                                while (true) {
                                    leftPrimary = collationIterator.getCE(leftIndex2) >>> 32;
                                    p = leftPrimary;
                                    if (leftPrimary <= Collation.MERGE_SEPARATOR_PRIMARY && p != 0) {
                                        break;
                                    }
                                    leftIndex2++;
                                    leftIndex = leftIndex;
                                    rightIndex = rightIndex;
                                }
                                rightIndex2 = rightIndex;
                                while (true) {
                                    leftPrimary2 = collationIterator2.getCE(rightIndex2) >>> 32;
                                    p = leftPrimary2;
                                    if (leftPrimary2 <= Collation.MERGE_SEPARATOR_PRIMARY && p != 0) {
                                        break;
                                    }
                                    rightIndex2++;
                                    leftIndex = leftIndex;
                                    rightIndex = rightIndex;
                                }
                                int leftIndex4 = leftIndex2;
                                rightIndex3 = rightIndex2;
                                while (true) {
                                    int leftStart;
                                    leftIndex3 = leftIndex4;
                                    leftIndex4 = 0;
                                    while (true) {
                                        leftSecondary = leftIndex4;
                                        if (leftSecondary != 0 || leftIndex3 <= leftIndex) {
                                            leftStart = leftIndex;
                                            rightSecondary = 0;
                                        } else {
                                            leftIndex3--;
                                            leftIndex4 = ((int) collationIterator.getCE(leftIndex3)) >>> 16;
                                            leftIndex = leftIndex;
                                        }
                                    }
                                    leftStart = leftIndex;
                                    rightSecondary = 0;
                                    while (rightSecondary == 0 && rightIndex3 > rightIndex) {
                                        rightIndex3--;
                                        rightSecondary = ((int) collationIterator2.getCE(rightIndex3)) >>> 16;
                                        rightIndex = rightIndex;
                                        leftIndex3 = leftIndex3;
                                    }
                                    int rightStart = rightIndex;
                                    int leftIndex5 = leftIndex3;
                                    if (leftSecondary != rightSecondary) {
                                        return leftSecondary < rightSecondary ? -1 : 1;
                                    } else if (leftSecondary == 0) {
                                        break;
                                    } else {
                                        leftIndex = leftStart;
                                        rightIndex = rightStart;
                                        leftIndex4 = leftIndex5;
                                    }
                                }
                                leftIndex = leftIndex2 + 1;
                                rightIndex = rightIndex2 + 1;
                            }
                        }
                    }
                    rightIndex = options & 1024;
                    leftIndex3 = -65536;
                    leftSecondary = Collation.CASE_MASK;
                    if (rightIndex != 0) {
                        rightIndex = CollationSettings.getStrength(options);
                        leftIndex = 0;
                        rightSecondary = 0;
                        while (true) {
                            int leftLower32;
                            long variableTop2;
                            if (rightIndex == 0) {
                                int rightIndex4;
                                while (true) {
                                    leftIndex2 = leftIndex + 1;
                                    leftPrimary = collationIterator.getCE(leftIndex);
                                    leftIndex = (int) leftPrimary;
                                    if ((leftPrimary >>> 32) != 0 && leftIndex != 0) {
                                        break;
                                    }
                                    leftIndex = leftIndex2;
                                }
                                leftLower32 = leftIndex;
                                leftIndex &= leftSecondary;
                                while (true) {
                                    rightIndex4 = rightSecondary + 1;
                                    leftPrimary = collationIterator2.getCE(rightSecondary);
                                    rightSecondary = (int) leftPrimary;
                                    if ((leftPrimary >>> 32) != 0 && rightSecondary != 0) {
                                        break;
                                    }
                                    rightSecondary = rightIndex4;
                                }
                                rightSecondary &= leftSecondary;
                                variableTop2 = variableTop;
                                rightIndex2 = rightIndex4;
                            } else {
                                while (true) {
                                    leftIndex2 = leftIndex + 1;
                                    leftIndex = (int) collationIterator.getCE(leftIndex);
                                    if ((leftIndex & -65536) != 0) {
                                        break;
                                    }
                                    leftIndex = leftIndex2;
                                }
                                leftLower32 = leftIndex;
                                leftIndex &= leftSecondary;
                                while (true) {
                                    rightIndex2 = rightSecondary + 1;
                                    variableTop2 = variableTop;
                                    leftSecondary = (int) collationIterator2.getCE(rightSecondary);
                                    if ((leftSecondary & -65536) != null) {
                                        break;
                                    }
                                    rightSecondary = rightIndex2;
                                    variableTop = variableTop2;
                                }
                                rightSecondary = leftSecondary & 49152;
                            }
                            if (leftIndex != rightSecondary) {
                                if ((options & 256) == 0) {
                                    return leftIndex < rightSecondary ? -1 : 1;
                                }
                                return leftIndex < rightSecondary ? 1 : -1;
                            } else if ((leftLower32 >>> 16) == 256) {
                                break;
                            } else {
                                leftIndex = leftIndex2;
                                rightSecondary = rightIndex2;
                                variableTop = variableTop2;
                                leftSecondary = Collation.CASE_MASK;
                            }
                        }
                    }
                    long rightQuaternary = 1;
                    if (CollationSettings.getStrength(options) <= 1) {
                        return 0;
                    }
                    rightIndex = CollationSettings.getTertiaryMask(options);
                    rightSecondary = 0;
                    int leftIndex6 = 0;
                    variableTop = null;
                    while (true) {
                        leftIndex = leftIndex6 + 1;
                        leftIndex6 = (int) collationIterator.getCE(leftIndex6);
                        variableTop |= leftIndex6;
                        leftIndex2 = leftIndex6 & rightIndex;
                        if (leftIndex2 != 0) {
                            int anyQuaternaries;
                            while (true) {
                                rightIndex2 = rightSecondary + 1;
                                rightSecondary = (int) collationIterator2.getCE(rightSecondary);
                                variableTop |= rightSecondary;
                                rightIndex3 = rightSecondary & rightIndex;
                                if (rightIndex3 != 0) {
                                    break;
                                }
                                anyQuaternaries = variableTop;
                                rightSecondary = rightIndex2;
                            }
                            if (leftIndex2 != rightIndex3) {
                                if (CollationSettings.sortsTertiaryUpperCaseFirst(options)) {
                                    if (leftIndex2 > 256) {
                                        if ((leftIndex6 & leftIndex3) != 0) {
                                            leftIndex2 ^= Collation.CASE_MASK;
                                        } else {
                                            leftIndex2 += 16384;
                                        }
                                    }
                                    if (rightIndex3 > 256) {
                                        rightIndex3 = (leftIndex3 & rightSecondary) != 0 ? rightIndex3 ^ Collation.CASE_MASK : rightIndex3 + 16384;
                                    }
                                }
                                return leftIndex2 < rightIndex3 ? -1 : rightQuaternary;
                            } else if (leftIndex2 != 256) {
                                anyQuaternaries = variableTop;
                                leftIndex6 = leftIndex;
                                rightSecondary = rightIndex2;
                                leftIndex3 = -65536;
                                rightQuaternary = 1;
                            } else if (CollationSettings.getStrength(options) <= 2) {
                                return 0;
                            } else {
                                if (!anyVariable && (variableTop & 192) == 0) {
                                    return 0;
                                }
                                leftIndex3 = 0;
                                leftIndex6 = 0;
                                while (true) {
                                    rightSecondary = leftIndex3 + 1;
                                    long ce3 = collationIterator.getCE(leftIndex3);
                                    p = ce3 & 65535;
                                    if (p <= 256) {
                                        p = ce3 >>> 32;
                                    } else {
                                        p |= 4294967103L;
                                    }
                                    leftIndex = p;
                                    if (leftIndex != 0) {
                                        while (true) {
                                            leftIndex3 = leftIndex6 + 1;
                                            p = collationIterator2.getCE(leftIndex6);
                                            leftPrimary2 = p & 65535;
                                            if (leftPrimary2 <= 256) {
                                                leftPrimary2 = p >>> 32;
                                            } else {
                                                leftPrimary2 |= 4294967103L;
                                            }
                                            anyQuaternaries = variableTop;
                                            rightQuaternary = leftPrimary2;
                                            if (rightQuaternary != 0) {
                                                break;
                                            }
                                            leftIndex6 = leftIndex3;
                                            variableTop = anyQuaternaries;
                                        }
                                        if (leftIndex != rightQuaternary) {
                                            if (settings.hasReordering()) {
                                                leftIndex = collationSettings.reorder(leftIndex);
                                                rightQuaternary = collationSettings.reorder(rightQuaternary);
                                            }
                                            return leftIndex < rightQuaternary ? -1 : 1;
                                        } else if (leftIndex == 1) {
                                            return 0;
                                        } else {
                                            leftIndex6 = leftIndex3;
                                            leftIndex3 = rightSecondary;
                                            variableTop = anyQuaternaries;
                                        }
                                    } else {
                                        leftIndex3 = rightSecondary;
                                    }
                                }
                            }
                        } else {
                            leftIndex6 = leftIndex;
                        }
                    }
                } else {
                    j2 = 1;
                }
            } else {
                j2 = 1;
            }
        }
    }
}
