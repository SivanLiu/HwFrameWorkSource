package sun.nio.fs;

import java.io.IOException;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Map;

final class FileOwnerAttributeViewImpl implements FileOwnerAttributeView, DynamicFileAttributeView {
    private static final String OWNER_NAME = "owner";
    private final boolean isPosixView = false;
    private final FileAttributeView view;

    FileOwnerAttributeViewImpl(PosixFileAttributeView view) {
        this.view = view;
    }

    FileOwnerAttributeViewImpl(AclFileAttributeView view) {
        this.view = view;
    }

    public String name() {
        return OWNER_NAME;
    }

    public void setAttribute(String attribute, Object value) throws IOException {
        if (attribute.equals(OWNER_NAME)) {
            setOwner((UserPrincipal) value);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'");
        stringBuilder.append(name());
        stringBuilder.append(":");
        stringBuilder.append(attribute);
        stringBuilder.append("' not recognized");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public Map<String, Object> readAttributes(String[] attributes) throws IOException {
        Map<String, Object> result = new HashMap();
        int length = attributes.length;
        int i = 0;
        while (i < length) {
            String attribute = attributes[i];
            if (attribute.equals("*") || attribute.equals(OWNER_NAME)) {
                result.put(OWNER_NAME, getOwner());
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("'");
                stringBuilder.append(name());
                stringBuilder.append(":");
                stringBuilder.append(attribute);
                stringBuilder.append("' not recognized");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return result;
    }

    public UserPrincipal getOwner() throws IOException {
        if (this.isPosixView) {
            return ((PosixFileAttributeView) this.view).readAttributes().owner();
        }
        return ((AclFileAttributeView) this.view).getOwner();
    }

    public void setOwner(UserPrincipal owner) throws IOException {
        if (this.isPosixView) {
            ((PosixFileAttributeView) this.view).setOwner(owner);
        } else {
            ((AclFileAttributeView) this.view).setOwner(owner);
        }
    }
}
