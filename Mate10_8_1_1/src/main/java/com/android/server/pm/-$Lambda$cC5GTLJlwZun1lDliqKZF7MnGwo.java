package com.android.server.pm;

import java.io.File;
import java.io.FileFilter;

final /* synthetic */ class -$Lambda$cC5GTLJlwZun1lDliqKZF7MnGwo implements FileFilter {
    public static final /* synthetic */ -$Lambda$cC5GTLJlwZun1lDliqKZF7MnGwo $INST$0 = new -$Lambda$cC5GTLJlwZun1lDliqKZF7MnGwo();

    private final /* synthetic */ boolean $m$0(File arg0) {
        return arg0.isFile();
    }

    private /* synthetic */ -$Lambda$cC5GTLJlwZun1lDliqKZF7MnGwo() {
    }

    public final boolean accept(File file) {
        return $m$0(file);
    }
}
