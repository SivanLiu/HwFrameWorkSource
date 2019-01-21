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
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String USER_NAMESPACE = "user.";
    private static final int XATTR_NAME_MAX = 255;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private final UnixPath file;
    private final boolean followLinks;

    private byte[] nameAsBytes(UnixPath file, String name) throws IOException {
        if (name != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(USER_NAMESPACE);
            stringBuilder.append(name);
            name = stringBuilder.toString();
            byte[] bytes = Util.toBytes(name);
            if (bytes.length <= XATTR_NAME_MAX) {
                return bytes;
            }
            String pathForExceptionMessage = file.getPathForExceptionMessage();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("'");
            stringBuilder2.append(name);
            stringBuilder2.append("' is too big");
            throw new FileSystemException(pathForExceptionMessage, null, stringBuilder2.toString());
        }
        throw new NullPointerException("'name' is null");
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

    public List<String> list() throws IOException {
        String pathForExceptionMessage;
        StringBuilder stringBuilder;
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, $assertionsDisabled);
        }
        int fd = this.file.openForAttributeAccess(this.followLinks);
        NativeBuffer buffer = null;
        int size = 1024;
        try {
            buffer = NativeBuffers.getNativeBuffer(1024);
            while (true) {
                List unmodifiableList = Collections.unmodifiableList(asList(buffer.address(), LinuxNativeDispatcher.flistxattr(fd, buffer.address(), size)));
                if (buffer != null) {
                    buffer.release();
                }
                UnixNativeDispatcher.close(fd);
                return unmodifiableList;
            }
        } catch (UnixException x) {
            if (x.errno() != UnixConstants.ERANGE || size >= NumericShaper.MYANMAR) {
                pathForExceptionMessage = this.file.getPathForExceptionMessage();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to get list of extended attributes: ");
                stringBuilder.append(x.getMessage());
                throw new FileSystemException(pathForExceptionMessage, null, stringBuilder.toString());
            }
            buffer.release();
            size *= 2;
            buffer = NativeBuffers.getNativeBuffer(size);
        } catch (Throwable th) {
            if (buffer != null) {
                buffer.release();
            }
            UnixNativeDispatcher.close(fd);
        }
        pathForExceptionMessage = this.file.getPathForExceptionMessage();
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to get list of extended attributes: ");
        stringBuilder.append(x.getMessage());
        throw new FileSystemException(pathForExceptionMessage, null, stringBuilder.toString());
    }

    public int size(String name) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, $assertionsDisabled);
        }
        int fd = this.file.openForAttributeAccess(this.followLinks);
        try {
            int fgetxattr = LinuxNativeDispatcher.fgetxattr(fd, nameAsBytes(this.file, name), 0, 0);
            UnixNativeDispatcher.close(fd);
            return fgetxattr;
        } catch (UnixException x) {
            String pathForExceptionMessage = this.file.getPathForExceptionMessage();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to get size of extended attribute '");
            stringBuilder.append(name);
            stringBuilder.append("': ");
            stringBuilder.append(x.getMessage());
            throw new FileSystemException(pathForExceptionMessage, null, stringBuilder.toString());
        } catch (Throwable th) {
            UnixNativeDispatcher.close(fd);
        }
    }

    public int read(String name, ByteBuffer dst) throws IOException {
        String str = name;
        ByteBuffer byteBuffer = dst;
        int i = 0;
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, $assertionsDisabled);
        }
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        NativeBuffer nb;
        long address;
        int pos = dst.position();
        int lim = dst.limit();
        int rem = pos <= lim ? lim - pos : 0;
        if (byteBuffer instanceof DirectBuffer) {
            nb = null;
            address = ((DirectBuffer) byteBuffer).address() + ((long) pos);
        } else {
            nb = NativeBuffers.getNativeBuffer(rem);
            address = nb.address();
        }
        NativeBuffer nb2 = nb;
        int fd = this.file.openForAttributeAccess(this.followLinks);
        try {
            int n = LinuxNativeDispatcher.fgetxattr(fd, nameAsBytes(this.file, str), address, rem);
            if (rem != 0) {
                if (nb2 != null) {
                    while (i < n) {
                        byteBuffer.put(unsafe.getByte(((long) i) + address));
                        i++;
                    }
                }
                byteBuffer.position(pos + n);
                UnixNativeDispatcher.close(fd);
                if (nb2 != null) {
                    nb2.release();
                }
                return n;
            } else if (n <= 0) {
                UnixNativeDispatcher.close(fd);
                if (nb2 != null) {
                    nb2.release();
                }
                return 0;
            } else {
                throw new UnixException(UnixConstants.ERANGE);
            }
        } catch (UnixException x) {
            String msg = x.errno() == UnixConstants.ERANGE ? "Insufficient space in buffer" : x.getMessage();
            String pathForExceptionMessage = this.file.getPathForExceptionMessage();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error reading extended attribute '");
            stringBuilder.append(str);
            stringBuilder.append("': ");
            stringBuilder.append(msg);
            throw new FileSystemException(pathForExceptionMessage, null, stringBuilder.toString());
        } catch (Throwable th) {
            if (nb2 != null) {
                nb2.release();
            }
        }
    }

    public int write(String name, ByteBuffer src) throws IOException {
        NativeBuffer nb;
        long address;
        int i = 0;
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), $assertionsDisabled, true);
        }
        int pos = src.position();
        int lim = src.limit();
        int rem = pos <= lim ? lim - pos : 0;
        if (src instanceof DirectBuffer) {
            nb = null;
            address = ((DirectBuffer) src).address() + ((long) pos);
        } else {
            NativeBuffer nb2 = NativeBuffers.getNativeBuffer(rem);
            long address2 = nb2.address();
            if (src.hasArray()) {
                while (i < rem) {
                    unsafe.putByte(((long) i) + address2, src.get());
                    i++;
                }
            } else {
                byte[] tmp = new byte[rem];
                src.get(tmp);
                src.position(pos);
                while (i < rem) {
                    unsafe.putByte(((long) i) + address2, tmp[i]);
                    i++;
                }
            }
            nb = nb2;
            address = address2;
        }
        int fd = this.file.openForAttributeAccess(this.followLinks);
        try {
            LinuxNativeDispatcher.fsetxattr(fd, nameAsBytes(this.file, name), address, rem);
            src.position(pos + rem);
            UnixNativeDispatcher.close(fd);
            if (nb != null) {
                nb.release();
            }
            return rem;
        } catch (UnixException x) {
            String pathForExceptionMessage = this.file.getPathForExceptionMessage();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error writing extended attribute '");
            stringBuilder.append(name);
            stringBuilder.append("': ");
            stringBuilder.append(x.getMessage());
            throw new FileSystemException(pathForExceptionMessage, null, stringBuilder.toString());
        } catch (Throwable th) {
            if (nb != null) {
                nb.release();
            }
        }
    }

    public void delete(String name) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), $assertionsDisabled, true);
        }
        int fd = this.file.openForAttributeAccess(this.followLinks);
        try {
            LinuxNativeDispatcher.fremovexattr(fd, nameAsBytes(this.file, name));
            UnixNativeDispatcher.close(fd);
        } catch (UnixException x) {
            String pathForExceptionMessage = this.file.getPathForExceptionMessage();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to delete extended attribute '");
            stringBuilder.append(name);
            stringBuilder.append("': ");
            stringBuilder.append(x.getMessage());
            throw new FileSystemException(pathForExceptionMessage, null, stringBuilder.toString());
        } catch (Throwable th) {
            UnixNativeDispatcher.close(fd);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0078  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0083  */
    /* JADX WARNING: Missing block: B:6:0x0014, code skipped:
            r3 = sun.nio.fs.LinuxNativeDispatcher.flistxattr(r1, r2.address(), r3);
     */
    /* JADX WARNING: Missing block: B:8:?, code skipped:
            r4 = r2.address();
            r7 = 0;
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:9:0x001e, code skipped:
            r8 = r0;
     */
    /* JADX WARNING: Missing block: B:10:0x001f, code skipped:
            if (r8 >= r3) goto L_0x0052;
     */
    /* JADX WARNING: Missing block: B:12:0x0029, code skipped:
            if (unsafe.getByte(((long) r8) + r4) != (byte) 0) goto L_0x004d;
     */
    /* JADX WARNING: Missing block: B:13:0x002b, code skipped:
            r9 = r8 - r7;
            r10 = new byte[r9];
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:14:0x0031, code skipped:
            if (r0 >= r9) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:15:0x0033, code skipped:
            r10[r0] = unsafe.getByte((((long) r7) + r4) + ((long) r0));
     */
    /* JADX WARNING: Missing block: B:16:0x003f, code skipped:
            r0 = r0 + 1;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            copyExtendedAttribute(r1, r10, r17);
     */
    /* JADX WARNING: Missing block: B:22:0x004d, code skipped:
            r11 = r17;
     */
    /* JADX WARNING: Missing block: B:24:0x0052, code skipped:
            r11 = r17;
     */
    /* JADX WARNING: Missing block: B:25:0x0054, code skipped:
            if (r2 == null) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:26:0x0056, code skipped:
            r2.release();
     */
    /* JADX WARNING: Missing block: B:27:0x0059, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void copyExtendedAttributes(int ofd, int nfd) {
        int i;
        Throwable th;
        int i2 = ofd;
        NativeBuffer buffer = null;
        try {
            buffer = NativeBuffers.getNativeBuffer(1024);
            int size = 1024;
            while (true) {
                try {
                    break;
                } catch (UnixException x) {
                    i = nfd;
                    if (x.errno() != UnixConstants.ERANGE || size >= NumericShaper.MYANMAR) {
                        if (buffer != null) {
                        }
                        return;
                    }
                    buffer.release();
                    size *= 2;
                    buffer = NativeBuffers.getNativeBuffer(size);
                } catch (Throwable th2) {
                    th = th2;
                    if (buffer != null) {
                    }
                    throw th;
                }
            }
            if (buffer != null) {
                buffer.release();
            }
            return;
            int start = pos + 1;
            int pos = pos + 1;
        } catch (Throwable th3) {
            th = th3;
            i = nfd;
            if (buffer != null) {
                buffer.release();
            }
            throw th;
        }
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
