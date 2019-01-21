package sun.nio.fs;

import java.io.IOException;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;
import sun.misc.Unsafe;

class LinuxDosFileAttributeView extends Basic implements DosFileAttributeView {
    private static final String ARCHIVE_NAME = "archive";
    private static final int DOS_XATTR_ARCHIVE = 32;
    private static final int DOS_XATTR_HIDDEN = 2;
    private static final String DOS_XATTR_NAME = "user.DOSATTRIB";
    private static final byte[] DOS_XATTR_NAME_AS_BYTES = Util.toBytes(DOS_XATTR_NAME);
    private static final int DOS_XATTR_READONLY = 1;
    private static final int DOS_XATTR_SYSTEM = 4;
    private static final String HIDDEN_NAME = "hidden";
    private static final String READONLY_NAME = "readonly";
    private static final String SYSTEM_NAME = "system";
    private static final Set<String> dosAttributeNames = Util.newSet(basicAttributeNames, READONLY_NAME, ARCHIVE_NAME, SYSTEM_NAME, HIDDEN_NAME);
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    LinuxDosFileAttributeView(UnixPath file, boolean followLinks) {
        super(file, followLinks);
    }

    public String name() {
        return "dos";
    }

    public void setAttribute(String attribute, Object value) throws IOException {
        if (attribute.equals(READONLY_NAME)) {
            setReadOnly(((Boolean) value).booleanValue());
        } else if (attribute.equals(ARCHIVE_NAME)) {
            setArchive(((Boolean) value).booleanValue());
        } else if (attribute.equals(SYSTEM_NAME)) {
            setSystem(((Boolean) value).booleanValue());
        } else if (attribute.equals(HIDDEN_NAME)) {
            setHidden(((Boolean) value).booleanValue());
        } else {
            super.setAttribute(attribute, value);
        }
    }

    public Map<String, Object> readAttributes(String[] attributes) throws IOException {
        AttributesBuilder builder = AttributesBuilder.create(dosAttributeNames, attributes);
        DosFileAttributes attrs = readAttributes();
        addRequestedBasicAttributes(attrs, builder);
        if (builder.match(READONLY_NAME)) {
            builder.add(READONLY_NAME, Boolean.valueOf(attrs.isReadOnly()));
        }
        if (builder.match(ARCHIVE_NAME)) {
            builder.add(ARCHIVE_NAME, Boolean.valueOf(attrs.isArchive()));
        }
        if (builder.match(SYSTEM_NAME)) {
            builder.add(SYSTEM_NAME, Boolean.valueOf(attrs.isSystem()));
        }
        if (builder.match(HIDDEN_NAME)) {
            builder.add(HIDDEN_NAME, Boolean.valueOf(attrs.isHidden()));
        }
        return builder.unmodifiableMap();
    }

    public DosFileAttributes readAttributes() throws IOException {
        this.file.checkRead();
        int fd = this.file.openForAttributeAccess(this.followLinks);
        try {
            final UnixFileAttributes attrs = UnixFileAttributes.get(fd);
            final int dosAttribute = getDosAttribute(fd);
            AnonymousClass1 anonymousClass1 = new DosFileAttributes() {
                public FileTime lastModifiedTime() {
                    return attrs.lastModifiedTime();
                }

                public FileTime lastAccessTime() {
                    return attrs.lastAccessTime();
                }

                public FileTime creationTime() {
                    return attrs.creationTime();
                }

                public boolean isRegularFile() {
                    return attrs.isRegularFile();
                }

                public boolean isDirectory() {
                    return attrs.isDirectory();
                }

                public boolean isSymbolicLink() {
                    return attrs.isSymbolicLink();
                }

                public boolean isOther() {
                    return attrs.isOther();
                }

                public long size() {
                    return attrs.size();
                }

                public Object fileKey() {
                    return attrs.fileKey();
                }

                public boolean isReadOnly() {
                    return (dosAttribute & 1) != 0;
                }

                public boolean isHidden() {
                    return (dosAttribute & 2) != 0;
                }

                public boolean isArchive() {
                    return (dosAttribute & 32) != 0;
                }

                public boolean isSystem() {
                    return (dosAttribute & 4) != 0;
                }
            };
            UnixNativeDispatcher.close(fd);
            return anonymousClass1;
        } catch (UnixException x) {
            x.rethrowAsIOException(this.file);
            UnixNativeDispatcher.close(fd);
            return null;
        } catch (Throwable th) {
            UnixNativeDispatcher.close(fd);
            throw th;
        }
    }

    public void setReadOnly(boolean value) throws IOException {
        updateDosAttribute(1, value);
    }

    public void setHidden(boolean value) throws IOException {
        updateDosAttribute(2, value);
    }

    public void setArchive(boolean value) throws IOException {
        updateDosAttribute(32, value);
    }

    public void setSystem(boolean value) throws IOException {
        updateDosAttribute(4, value);
    }

    private int getDosAttribute(int fd) throws UnixException {
        NativeBuffer buffer = NativeBuffers.getNativeBuffer(24);
        try {
            int len = LinuxNativeDispatcher.fgetxattr(fd, DOS_XATTR_NAME_AS_BYTES, buffer.address(), 24);
            if (len > 0) {
                if (unsafe.getByte((buffer.address() + ((long) len)) - 1) == (byte) 0) {
                    len--;
                }
                byte[] buf = new byte[len];
                for (int i = 0; i < len; i++) {
                    buf[i] = unsafe.getByte(buffer.address() + ((long) i));
                }
                String value = Util.toString(buf);
                int e = value.length();
                if (e >= 3) {
                    e = value.startsWith("0x");
                    if (e != 0) {
                        try {
                            e = Integer.parseInt(value.substring(2), 16);
                            return e;
                        } catch (NumberFormatException e2) {
                            e = e2;
                        }
                    }
                }
            }
            throw new UnixException("Value of user.DOSATTRIB attribute is invalid");
        } catch (UnixException x) {
            if (x.errno() == UnixConstants.ENODATA) {
                return 0;
            }
            throw x;
        } finally {
            buffer.release();
        }
    }

    private void updateDosAttribute(int flag, boolean enable) throws IOException {
        this.file.checkWrite();
        int fd = this.file.openForAttributeAccess(this.followLinks);
        NativeBuffer buffer;
        try {
            int oldValue = getDosAttribute(fd);
            int newValue = oldValue;
            if (enable) {
                newValue |= flag;
            } else {
                newValue &= ~flag;
            }
            if (newValue != oldValue) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("0x");
                stringBuilder.append(Integer.toHexString(newValue));
                byte[] value = Util.toBytes(stringBuilder.toString());
                buffer = NativeBuffers.asNativeBuffer(value);
                LinuxNativeDispatcher.fsetxattr(fd, DOS_XATTR_NAME_AS_BYTES, buffer.address(), value.length + 1);
                buffer.release();
            }
        } catch (UnixException x) {
            try {
                x.rethrowAsIOException(this.file);
            } catch (Throwable th) {
                UnixNativeDispatcher.close(fd);
            }
        } catch (Throwable th2) {
            buffer.release();
        }
        UnixNativeDispatcher.close(fd);
    }
}
