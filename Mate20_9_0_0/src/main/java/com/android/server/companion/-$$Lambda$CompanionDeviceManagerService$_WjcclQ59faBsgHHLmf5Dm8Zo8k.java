package com.android.server.companion;

import com.android.internal.util.FunctionalUtils.ThrowingConsumer;
import org.xmlpull.v1.XmlSerializer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManagerService$_WjcclQ59faBsgHHLmf5Dm8Zo8k implements ThrowingConsumer {
    private final /* synthetic */ XmlSerializer f$0;

    public /* synthetic */ -$$Lambda$CompanionDeviceManagerService$_WjcclQ59faBsgHHLmf5Dm8Zo8k(XmlSerializer xmlSerializer) {
        this.f$0 = xmlSerializer;
    }

    public final void acceptOrThrow(Object obj) {
        this.f$0.startTag(null, CompanionDeviceManagerService.XML_TAG_ASSOCIATION).attribute(null, "package", ((Association) obj).companionAppPackage).attribute(null, CompanionDeviceManagerService.XML_ATTR_DEVICE, ((Association) obj).deviceAddress).endTag(null, CompanionDeviceManagerService.XML_TAG_ASSOCIATION);
    }
}
