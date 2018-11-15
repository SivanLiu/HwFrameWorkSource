package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class TransferOwnershipMetadataManager {
    static final String ADMIN_TYPE_DEVICE_OWNER = "device-owner";
    static final String ADMIN_TYPE_PROFILE_OWNER = "profile-owner";
    public static final String OWNER_TRANSFER_METADATA_XML = "owner-transfer-metadata.xml";
    private static final String TAG = TransferOwnershipMetadataManager.class.getName();
    @VisibleForTesting
    static final String TAG_ADMIN_TYPE = "admin-type";
    @VisibleForTesting
    static final String TAG_SOURCE_COMPONENT = "source-component";
    @VisibleForTesting
    static final String TAG_TARGET_COMPONENT = "target-component";
    @VisibleForTesting
    static final String TAG_USER_ID = "user-id";
    private final Injector mInjector;

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        public File getOwnerTransferMetadataDir() {
            return Environment.getDataSystemDirectory();
        }
    }

    static class Metadata {
        final String adminType;
        final ComponentName sourceComponent;
        final ComponentName targetComponent;
        final int userId;

        Metadata(ComponentName sourceComponent, ComponentName targetComponent, int userId, String adminType) {
            this.sourceComponent = sourceComponent;
            this.targetComponent = targetComponent;
            Preconditions.checkNotNull(sourceComponent);
            Preconditions.checkNotNull(targetComponent);
            Preconditions.checkStringNotEmpty(adminType);
            this.userId = userId;
            this.adminType = adminType;
        }

        Metadata(String flatSourceComponent, String flatTargetComponent, int userId, String adminType) {
            this(unflattenComponentUnchecked(flatSourceComponent), unflattenComponentUnchecked(flatTargetComponent), userId, adminType);
        }

        private static ComponentName unflattenComponentUnchecked(String flatComponent) {
            Preconditions.checkNotNull(flatComponent);
            return ComponentName.unflattenFromString(flatComponent);
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof Metadata)) {
                return false;
            }
            Metadata params = (Metadata) obj;
            if (this.userId == params.userId && this.sourceComponent.equals(params.sourceComponent) && this.targetComponent.equals(params.targetComponent) && TextUtils.equals(this.adminType, params.adminType)) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * 1) + this.userId)) + this.sourceComponent.hashCode())) + this.targetComponent.hashCode())) + this.adminType.hashCode();
        }
    }

    TransferOwnershipMetadataManager() {
        this(new Injector());
    }

    @VisibleForTesting
    TransferOwnershipMetadataManager(Injector injector) {
        this.mInjector = injector;
    }

    boolean saveMetadataFile(Metadata params) {
        File transferOwnershipMetadataFile = new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML);
        AtomicFile atomicFile = new AtomicFile(transferOwnershipMetadataFile);
        FileOutputStream stream = null;
        try {
            stream = atomicFile.startWrite();
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(stream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            insertSimpleTag(serializer, TAG_USER_ID, Integer.toString(params.userId));
            insertSimpleTag(serializer, TAG_SOURCE_COMPONENT, params.sourceComponent.flattenToString());
            insertSimpleTag(serializer, TAG_TARGET_COMPONENT, params.targetComponent.flattenToString());
            insertSimpleTag(serializer, TAG_ADMIN_TYPE, params.adminType);
            serializer.endDocument();
            atomicFile.finishWrite(stream);
            return true;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caught exception while trying to save Owner Transfer Params to file ");
            stringBuilder.append(transferOwnershipMetadataFile);
            Slog.e(str, stringBuilder.toString(), e);
            transferOwnershipMetadataFile.delete();
            atomicFile.failWrite(stream);
            return false;
        }
    }

    private void insertSimpleTag(XmlSerializer serializer, String tagName, String value) throws IOException {
        serializer.startTag(null, tagName);
        serializer.text(value);
        serializer.endTag(null, tagName);
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0058 A:{Splitter: B:4:0x002c, ExcHandler: java.io.IOException (r1_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0058 A:{Splitter: B:4:0x002c, ExcHandler: java.io.IOException (r1_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:26:0x0058, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:27:0x0059, code:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Caught exception while trying to load the owner transfer params from file ");
            r4.append(r0);
            android.util.Slog.e(r3, r4.toString(), r1);
     */
    /* JADX WARNING: Missing block: B:28:0x006f, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    Metadata loadMetadataFile() {
        FileInputStream stream;
        Throwable th;
        Throwable th2;
        File transferOwnershipMetadataFile = new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML);
        if (!transferOwnershipMetadataFile.exists()) {
            return null;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Loading TransferOwnershipMetadataManager from ");
        stringBuilder.append(transferOwnershipMetadataFile);
        Slog.d(str, stringBuilder.toString());
        try {
            stream = new FileInputStream(transferOwnershipMetadataFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, null);
                Metadata parseMetadataFile = parseMetadataFile(parser);
                stream.close();
                return parseMetadataFile;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
        } catch (Exception e) {
        }
        throw th;
        if (th22 != null) {
            try {
                stream.close();
            } catch (Throwable th4) {
                th22.addSuppressed(th4);
            }
        } else {
            stream.close();
        }
        throw th;
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x0084  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x007c  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0074  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006c  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0084  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x007c  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0074  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006c  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0084  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x007c  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0074  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006c  */
    /* JADX WARNING: Missing block: B:29:0x0064, code:
            if (r8.equals(TAG_TARGET_COMPONENT) != false) goto L_0x0068;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Metadata parseMetadataFile(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int userId = 0;
        String adminComponent = null;
        String targetComponent = null;
        String adminType = null;
        while (true) {
            int next = parser.next();
            int type = next;
            Object obj = 1;
            if (next != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type != 3) {
                    if (type != 4) {
                        String name = parser.getName();
                        int hashCode = name.hashCode();
                        if (hashCode != -337219647) {
                            if (hashCode == -147180963) {
                                if (name.equals(TAG_USER_ID)) {
                                    obj = null;
                                    switch (obj) {
                                        case null:
                                            break;
                                        case 1:
                                            break;
                                        case 2:
                                            break;
                                        case 3:
                                            break;
                                    }
                                }
                            } else if (hashCode == 281362891) {
                                if (name.equals(TAG_SOURCE_COMPONENT)) {
                                    obj = 2;
                                    switch (obj) {
                                        case null:
                                            break;
                                        case 1:
                                            break;
                                        case 2:
                                            break;
                                        case 3:
                                            break;
                                    }
                                }
                            } else if (hashCode == 641951480 && name.equals(TAG_ADMIN_TYPE)) {
                                obj = 3;
                                switch (obj) {
                                    case null:
                                        parser.next();
                                        userId = Integer.parseInt(parser.getText());
                                        break;
                                    case 1:
                                        parser.next();
                                        targetComponent = parser.getText();
                                        break;
                                    case 2:
                                        parser.next();
                                        adminComponent = parser.getText();
                                        break;
                                    case 3:
                                        parser.next();
                                        adminType = parser.getText();
                                        break;
                                }
                            }
                        }
                        obj = -1;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                        }
                    }
                }
            }
        }
        return new Metadata(adminComponent, targetComponent, userId, adminType);
    }

    void deleteMetadataFile() {
        new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML).delete();
    }

    boolean metadataFileExists() {
        return new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML).exists();
    }
}
