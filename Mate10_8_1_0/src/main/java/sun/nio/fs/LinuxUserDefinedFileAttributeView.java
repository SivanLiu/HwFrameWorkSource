package sun.nio.fs;

import java.awt.font.NumericShaper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

class LinuxUserDefinedFileAttributeView extends AbstractUserDefinedFileAttributeView {
    static final /* synthetic */ boolean -assertionsDisabled = (LinuxUserDefinedFileAttributeView.class.desiredAssertionStatus() ^ 1);
    private static final String USER_NAMESPACE = "user.";
    private static final int XATTR_NAME_MAX = 255;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private final UnixPath file;
    private final boolean followLinks;

    private byte[] nameAsBytes(UnixPath file, String name) throws IOException {
        if (name == null) {
            throw new NullPointerException("'name' is null");
        }
        name = USER_NAMESPACE + name;
        byte[] bytes = Util.toBytes(name);
        if (bytes.length <= XATTR_NAME_MAX) {
            return bytes;
        }
        throw new FileSystemException(file.getPathForExceptionMessage(), null, "'" + name + "' is too big");
    }

    private List<String> asList(long address, int size) {
        List<String> list = new ArrayList();
        int start = 0;
        for (int pos = 0; pos < size; pos++) {
            if (unsafe.getByte(((long) pos) + address) == (byte) 0) {
                int len = pos - start;
                byte[] value = new byte[len];
                for (int i = 0; i < len; i++) {
                    value[i] = unsafe.getByte((((long) start) + address) + ((long) i));
                }
                String s = Util.toString(value);
                if (s.startsWith(USER_NAMESPACE)) {
                    list.add(s.substring(USER_NAMESPACE.length()));
                }
                start = pos + 1;
            }
        }
        return list;
    }

    LinuxUserDefinedFileAttributeView(UnixPath file, boolean followLinks) {
        this.file = file;
        this.followLinks = followLinks;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> list() throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, -assertionsDisabled);
        }
        int fd = this.file.openForAttributeAccess(this.followLinks);
        NativeBuffer nativeBuffer = null;
        int size = 1024;
        try {
            nativeBuffer = NativeBuffers.getNativeBuffer(1024);
            while (true) {
                List<String> unmodifiableList = Collections.unmodifiableList(asList(nativeBuffer.address(), LinuxNativeDispatcher.flistxattr(fd, nativeBuffer.address(), size)));
                break;
            }
        } catch (UnixException x) {
            if (x.errno() != UnixConstants.ERANGE || size >= NumericShaper.MYANMAR) {
                throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Unable to get list of extended attributes: " + x.getMessage());
            }
            nativeBuffer.release();
            size *= 2;
            nativeBuffer = NativeBuffers.getNativeBuffer(size);
        } catch (Throwable th) {
            if (nativeBuffer != null) {
                nativeBuffer.release();
            }
            UnixNativeDispatcher.close(fd);
        }
        throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Unable to get list of extended attributes: " + x.getMessage());
    }

    public int size(String name) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, -assertionsDisabled);
        }
        int fd = this.file.openForAttributeAccess(this.followLinks);
        try {
            int fgetxattr = LinuxNativeDispatcher.fgetxattr(fd, nameAsBytes(this.file, name), 0, 0);
            UnixNativeDispatcher.close(fd);
            return fgetxattr;
        } catch (UnixException x) {
            throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Unable to get size of extended attribute '" + name + "': " + x.getMessage());
        } catch (Throwable th) {
            UnixNativeDispatcher.close(fd);
        }
    }

    public int read(String name, ByteBuffer dst) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, -assertionsDisabled);
        }
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        int pos = dst.position();
        int lim = dst.limit();
        if (-assertionsDisabled || pos <= lim) {
            NativeBuffer nb;
            long address;
            int rem = pos <= lim ? lim - pos : 0;
            if (dst instanceof DirectBuffer) {
                nb = null;
                address = ((DirectBuffer) dst).address() + ((long) pos);
            } else {
                nb = NativeBuffers.getNativeBuffer(rem);
                address = nb.address();
            }
            int fd = this.file.openForAttributeAccess(this.followLinks);
            try {
                int n = LinuxNativeDispatcher.fgetxattr(fd, nameAsBytes(this.file, name), address, rem);
                if (rem != 0) {
                    if (nb != null) {
                        for (int i = 0; i < n; i++) {
                            dst.put(unsafe.getByte(((long) i) + address));
                        }
                    }
                    dst.position(pos + n);
                    UnixNativeDispatcher.close(fd);
                    if (nb != null) {
                        nb.release();
                    }
                    return n;
                } else if (n > 0) {
                    throw new UnixException(UnixConstants.ERANGE);
                } else {
                    UnixNativeDispatcher.close(fd);
                    if (nb != null) {
                        nb.release();
                    }
                    return 0;
                }
            } catch (UnixException x) {
                throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Error reading extended attribute '" + name + "': " + (x.errno() == UnixConstants.ERANGE ? "Insufficient space in buffer" : x.getMessage()));
            } catch (Throwable th) {
                try {
                    UnixNativeDispatcher.close(fd);
                } catch (Throwable th2) {
                    if (nb != null) {
                        nb.release();
                    }
                }
            }
        } else {
            throw new AssertionError();
        }
    }

    public int write(String name, ByteBuffer src) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), -assertionsDisabled, true);
        }
        int pos = src.position();
        int lim = src.limit();
        if (-assertionsDisabled || pos <= lim) {
            NativeBuffer nb;
            long address;
            int rem = pos <= lim ? lim - pos : 0;
            if (src instanceof DirectBuffer) {
                nb = null;
                address = ((DirectBuffer) src).address() + ((long) pos);
            } else {
                nb = NativeBuffers.getNativeBuffer(rem);
                address = nb.address();
                int i;
                if (src.hasArray()) {
                    for (i = 0; i < rem; i++) {
                        unsafe.putByte(((long) i) + address, src.get());
                    }
                } else {
                    byte[] tmp = new byte[rem];
                    src.get(tmp);
                    src.position(pos);
                    for (i = 0; i < rem; i++) {
                        unsafe.putByte(((long) i) + address, tmp[i]);
                    }
                }
            }
            int fd = this.file.openForAttributeAccess(this.followLinks);
            try {
                LinuxNativeDispatcher.fsetxattr(fd, nameAsBytes(this.file, name), address, rem);
                src.position(pos + rem);
                try {
                    UnixNativeDispatcher.close(fd);
                    return rem;
                } finally {
                    if (nb != null) {
                        nb.release();
                    }
                }
            } catch (UnixException x) {
                throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Error writing extended attribute '" + name + "': " + x.getMessage());
            } catch (Throwable th) {
                UnixNativeDispatcher.close(fd);
            }
        } else {
            throw new AssertionError();
        }
    }

    public void delete(String name) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), -assertionsDisabled, true);
        }
        int fd = this.file.openForAttributeAccess(this.followLinks);
        try {
            LinuxNativeDispatcher.fremovexattr(fd, nameAsBytes(this.file, name));
            UnixNativeDispatcher.close(fd);
        } catch (UnixException x) {
            throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Unable to delete extended attribute '" + name + "': " + x.getMessage());
        } catch (Throwable th) {
            UnixNativeDispatcher.close(fd);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void copyExtendedAttributes(int ofd, int nfd) {
        NativeBuffer nativeBuffer = null;
        int size = 1024;
        try {
            nativeBuffer = NativeBuffers.getNativeBuffer(1024);
            while (true) {
                size = LinuxNativeDispatcher.flistxattr(ofd, nativeBuffer.address(), size);
                break;
            }
        } catch (UnixException x) {
            if (x.errno() != UnixConstants.ERANGE || size >= NumericShaper.MYANMAR) {
                if (nativeBuffer != null) {
                    nativeBuffer.release();
                }
                return;
            }
            nativeBuffer.release();
            size *= 2;
            nativeBuffer = NativeBuffers.getNativeBuffer(size);
        } catch (Throwable th) {
            if (nativeBuffer != null) {
                nativeBuffer.release();
            }
        }
        if (nativeBuffer != null) {
            nativeBuffer.release();
        }
        return;
        int start = pos + 1;
        int pos++;
    }

    private static void copyExtendedAttribute(int ofd, byte[] name, int nfd) throws UnixException {
        int size = LinuxNativeDispatcher.fgetxattr(ofd, name, 0, 0);
        NativeBuffer buffer = NativeBuffers.getNativeBuffer(size);
        try {
            long address = buffer.address();
            LinuxNativeDispatcher.fsetxattr(nfd, name, address, LinuxNativeDispatcher.fgetxattr(ofd, name, address, size));
        } finally {
            buffer.release();
        }
    }
}
