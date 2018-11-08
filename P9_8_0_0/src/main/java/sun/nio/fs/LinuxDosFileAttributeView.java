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

    /* renamed from: sun.nio.fs.LinuxDosFileAttributeView$1 */
    class AnonymousClass1 implements DosFileAttributes {
        final /* synthetic */ UnixFileAttributes val$attrs;
        final /* synthetic */ int val$dosAttribute;

        AnonymousClass1(UnixFileAttributes unixFileAttributes, int i) {
            this.val$attrs = unixFileAttributes;
            this.val$dosAttribute = i;
        }

        public FileTime lastModifiedTime() {
            return this.val$attrs.lastModifiedTime();
        }

        public FileTime lastAccessTime() {
            return this.val$attrs.lastAccessTime();
        }

        public FileTime creationTime() {
            return this.val$attrs.creationTime();
        }

        public boolean isRegularFile() {
            return this.val$attrs.isRegularFile();
        }

        public boolean isDirectory() {
            return this.val$attrs.isDirectory();
        }

        public boolean isSymbolicLink() {
            return this.val$attrs.isSymbolicLink();
        }

        public boolean isOther() {
            return this.val$attrs.isOther();
        }

        public long size() {
            return this.val$attrs.size();
        }

        public Object fileKey() {
            return this.val$attrs.fileKey();
        }

        public boolean isReadOnly() {
            return (this.val$dosAttribute & 1) != 0;
        }

        public boolean isHidden() {
            return (this.val$dosAttribute & 2) != 0;
        }

        public boolean isArchive() {
            return (this.val$dosAttribute & 32) != 0;
        }

        public boolean isSystem() {
            return (this.val$dosAttribute & 4) != 0;
        }
    }

    private int getDosAttribute(int r15) throws sun.nio.fs.UnixException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:35:? in {6, 9, 18, 19, 22, 28, 30, 31, 33, 36, 37} preds:[]
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:129)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r14 = this;
        r4 = 24;
        r8 = 24;
        r1 = sun.nio.fs.NativeBuffers.getNativeBuffer(r8);
        r8 = DOS_XATTR_NAME_AS_BYTES;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r10 = r1.address();	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r9 = 24;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r3 = sun.nio.fs.LinuxNativeDispatcher.fgetxattr(r15, r8, r10, r9);	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        if (r3 <= 0) goto L_0x0063;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x0016:
        r8 = unsafe;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r10 = r1.address();	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r12 = (long) r3;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r10 = r10 + r12;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r12 = 1;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r10 = r10 - r12;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r8 = r8.getByte(r10);	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        if (r8 != 0) goto L_0x0029;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x0027:
        r3 = r3 + -1;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x0029:
        r0 = new byte[r3];	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r2 = 0;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x002c:
        if (r2 >= r3) goto L_0x003f;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x002e:
        r8 = unsafe;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r10 = r1.address();	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r12 = (long) r2;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r10 = r10 + r12;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r8 = r8.getByte(r10);	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r0[r2] = r8;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r2 = r2 + 1;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        goto L_0x002c;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x003f:
        r5 = sun.nio.fs.Util.toString(r0);	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r8 = r5.length();	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r9 = 3;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        if (r8 < r9) goto L_0x0063;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x004a:
        r8 = "0x";	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r8 = r5.startsWith(r8);	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        if (r8 == 0) goto L_0x0063;
    L_0x0053:
        r8 = 2;
        r8 = r5.substring(r8);	 Catch:{ NumberFormatException -> 0x0062 }
        r9 = 16;	 Catch:{ NumberFormatException -> 0x0062 }
        r8 = java.lang.Integer.parseInt(r8, r9);	 Catch:{ NumberFormatException -> 0x0062 }
        r1.release();
        return r8;
    L_0x0062:
        r6 = move-exception;
    L_0x0063:
        r8 = new sun.nio.fs.UnixException;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r9 = "Value of user.DOSATTRIB attribute is invalid";	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r8.<init>(r9);	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        throw r8;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x006c:
        r7 = move-exception;
        r8 = r7.errno();	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        r9 = sun.nio.fs.UnixConstants.ENODATA;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
        if (r8 != r9) goto L_0x007a;
    L_0x0075:
        r8 = 0;
        r1.release();
        return r8;
    L_0x007a:
        throw r7;	 Catch:{ UnixException -> 0x006c, all -> 0x007b }
    L_0x007b:
        r8 = move-exception;
        r1.release();
        throw r8;
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.nio.fs.LinuxDosFileAttributeView.getDosAttribute(int):int");
    }

    private void updateDosAttribute(int r1, boolean r2) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: sun.nio.fs.LinuxDosFileAttributeView.updateDosAttribute(int, boolean):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.nio.fs.LinuxDosFileAttributeView.updateDosAttribute(int, boolean):void");
    }

    public java.nio.file.attribute.DosFileAttributes readAttributes() throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:12:? in {3, 8, 9, 11, 13, 14} preds:[]
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:129)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r6 = this;
        r4 = r6.file;
        r4.checkRead();
        r4 = r6.file;
        r5 = r6.followLinks;
        r2 = r4.openForAttributeAccess(r5);
        r0 = sun.nio.fs.UnixFileAttributes.get(r2);	 Catch:{ UnixException -> 0x001e, all -> 0x0029 }
        r1 = r6.getDosAttribute(r2);	 Catch:{ UnixException -> 0x001e, all -> 0x0029 }
        r4 = new sun.nio.fs.LinuxDosFileAttributeView$1;	 Catch:{ UnixException -> 0x001e, all -> 0x0029 }
        r4.<init>(r0, r1);	 Catch:{ UnixException -> 0x001e, all -> 0x0029 }
        sun.nio.fs.UnixNativeDispatcher.close(r2);
        return r4;
    L_0x001e:
        r3 = move-exception;
        r4 = r6.file;	 Catch:{ UnixException -> 0x001e, all -> 0x0029 }
        r3.rethrowAsIOException(r4);	 Catch:{ UnixException -> 0x001e, all -> 0x0029 }
        r4 = 0;
        sun.nio.fs.UnixNativeDispatcher.close(r2);
        return r4;
    L_0x0029:
        r4 = move-exception;
        sun.nio.fs.UnixNativeDispatcher.close(r2);
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.nio.fs.LinuxDosFileAttributeView.readAttributes():java.nio.file.attribute.DosFileAttributes");
    }

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
}
