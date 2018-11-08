package com.android.server.wm;

import android.os.Environment;
import java.io.File;

final /* synthetic */ class -$Lambda$v2Yn08uofw54W8n_7KsmBjqR0Z8 implements DirectoryResolver {
    public static final /* synthetic */ -$Lambda$v2Yn08uofw54W8n_7KsmBjqR0Z8 $INST$0 = new -$Lambda$v2Yn08uofw54W8n_7KsmBjqR0Z8();

    private final /* synthetic */ File $m$0(int arg0) {
        return Environment.getDataSystemCeDirectory(arg0);
    }

    private /* synthetic */ -$Lambda$v2Yn08uofw54W8n_7KsmBjqR0Z8() {
    }

    public final File getSystemDirectoryForUser(int i) {
        return $m$0(i);
    }
}
