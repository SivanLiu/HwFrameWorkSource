package com.android.server.companion;

import android.os.Environment;
import android.util.AtomicFile;
import java.io.File;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManagerService$bh5xRJq9-CRJoXvmerYRNjK1xEQ implements Function {
    public static final /* synthetic */ -$$Lambda$CompanionDeviceManagerService$bh5xRJq9-CRJoXvmerYRNjK1xEQ INSTANCE = new -$$Lambda$CompanionDeviceManagerService$bh5xRJq9-CRJoXvmerYRNjK1xEQ();

    private /* synthetic */ -$$Lambda$CompanionDeviceManagerService$bh5xRJq9-CRJoXvmerYRNjK1xEQ() {
    }

    public final Object apply(Object obj) {
        return new AtomicFile(new File(Environment.getUserSystemDirectory(((Integer) obj).intValue()), CompanionDeviceManagerService.XML_FILE_NAME));
    }
}
