package java.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ProcessBuilder {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private List<String> command;
    private File directory;
    private Map<String, String> environment;
    private boolean redirectErrorStream;
    private Redirect[] redirects;

    public static abstract class Redirect {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        public static final Redirect INHERIT = new Redirect() {
            public Type type() {
                return Type.INHERIT;
            }

            public String toString() {
                return type().toString();
            }
        };
        public static final Redirect PIPE = new Redirect() {
            public Type type() {
                return Type.PIPE;
            }

            public String toString() {
                return type().toString();
            }
        };

        public enum Type {
            PIPE,
            INHERIT,
            READ,
            WRITE,
            APPEND
        }

        public abstract Type type();

        static {
            Class cls = ProcessBuilder.class;
        }

        public File file() {
            return null;
        }

        boolean append() {
            throw new UnsupportedOperationException();
        }

        public static Redirect from(final File file) {
            if (file != null) {
                return new Redirect() {
                    public Type type() {
                        return Type.READ;
                    }

                    public File file() {
                        return file;
                    }

                    public String toString() {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("redirect to read from file \"");
                        stringBuilder.append(file);
                        stringBuilder.append("\"");
                        return stringBuilder.toString();
                    }
                };
            }
            throw new NullPointerException();
        }

        public static Redirect to(final File file) {
            if (file != null) {
                return new Redirect() {
                    public Type type() {
                        return Type.WRITE;
                    }

                    public File file() {
                        return file;
                    }

                    public String toString() {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("redirect to write to file \"");
                        stringBuilder.append(file);
                        stringBuilder.append("\"");
                        return stringBuilder.toString();
                    }

                    boolean append() {
                        return false;
                    }
                };
            }
            throw new NullPointerException();
        }

        public static Redirect appendTo(final File file) {
            if (file != null) {
                return new Redirect() {
                    public Type type() {
                        return Type.APPEND;
                    }

                    public File file() {
                        return file;
                    }

                    public String toString() {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("redirect to append to file \"");
                        stringBuilder.append(file);
                        stringBuilder.append("\"");
                        return stringBuilder.toString();
                    }

                    boolean append() {
                        return true;
                    }
                };
            }
            throw new NullPointerException();
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Redirect)) {
                return false;
            }
            Redirect r = (Redirect) obj;
            if (r.type() != type()) {
                return false;
            }
            return file().equals(r.file());
        }

        public int hashCode() {
            File file = file();
            if (file == null) {
                return super.hashCode();
            }
            return file.hashCode();
        }

        private Redirect() {
        }
    }

    static class NullInputStream extends InputStream {
        static final NullInputStream INSTANCE = new NullInputStream();

        private NullInputStream() {
        }

        public int read() {
            return -1;
        }

        public int available() {
            return 0;
        }
    }

    static class NullOutputStream extends OutputStream {
        static final NullOutputStream INSTANCE = new NullOutputStream();

        private NullOutputStream() {
        }

        public void write(int b) throws IOException {
            throw new IOException("Stream closed");
        }
    }

    public ProcessBuilder(List<String> command) {
        if (command != null) {
            this.command = command;
            return;
        }
        throw new NullPointerException();
    }

    public ProcessBuilder(String... command) {
        this.command = new ArrayList(command.length);
        for (String arg : command) {
            this.command.add(arg);
        }
    }

    public ProcessBuilder command(List<String> command) {
        if (command != null) {
            this.command = command;
            return this;
        }
        throw new NullPointerException();
    }

    public ProcessBuilder command(String... command) {
        this.command = new ArrayList(command.length);
        for (String arg : command) {
            this.command.add(arg);
        }
        return this;
    }

    public List<String> command() {
        return this.command;
    }

    public Map<String, String> environment() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("getenv.*"));
        }
        if (this.environment == null) {
            this.environment = ProcessEnvironment.environment();
        }
        return this.environment;
    }

    ProcessBuilder environment(String[] envp) {
        if (envp != null) {
            this.environment = ProcessEnvironment.emptyEnvironment(envp.length);
            for (String envstring : envp) {
                String envstring2;
                if (envstring2.indexOf(0) != -1) {
                    envstring2 = envstring2.replaceFirst("\u0000.*", "");
                }
                int eqlsign = envstring2.indexOf(61, 0);
                if (eqlsign != -1) {
                    this.environment.put(envstring2.substring(0, eqlsign), envstring2.substring(eqlsign + 1));
                }
            }
        }
        return this;
    }

    public File directory() {
        return this.directory;
    }

    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    private Redirect[] redirects() {
        if (this.redirects == null) {
            this.redirects = new Redirect[]{Redirect.PIPE, Redirect.PIPE, Redirect.PIPE};
        }
        return this.redirects;
    }

    public ProcessBuilder redirectInput(Redirect source) {
        if (source.type() == Type.WRITE || source.type() == Type.APPEND) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Redirect invalid for reading: ");
            stringBuilder.append((Object) source);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        redirects()[0] = source;
        return this;
    }

    public ProcessBuilder redirectOutput(Redirect destination) {
        if (destination.type() != Type.READ) {
            redirects()[1] = destination;
            return this;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Redirect invalid for writing: ");
        stringBuilder.append((Object) destination);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ProcessBuilder redirectError(Redirect destination) {
        if (destination.type() != Type.READ) {
            redirects()[2] = destination;
            return this;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Redirect invalid for writing: ");
        stringBuilder.append((Object) destination);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ProcessBuilder redirectInput(File file) {
        return redirectInput(Redirect.from(file));
    }

    public ProcessBuilder redirectOutput(File file) {
        return redirectOutput(Redirect.to(file));
    }

    public ProcessBuilder redirectError(File file) {
        return redirectError(Redirect.to(file));
    }

    public Redirect redirectInput() {
        return this.redirects == null ? Redirect.PIPE : this.redirects[0];
    }

    public Redirect redirectOutput() {
        return this.redirects == null ? Redirect.PIPE : this.redirects[1];
    }

    public Redirect redirectError() {
        return this.redirects == null ? Redirect.PIPE : this.redirects[2];
    }

    public ProcessBuilder inheritIO() {
        Arrays.fill(redirects(), Redirect.INHERIT);
        return this;
    }

    public boolean redirectErrorStream() {
        return this.redirectErrorStream;
    }

    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    public Process start() throws IOException {
        String[] cmdarray = (String[]) ((String[]) this.command.toArray(new String[this.command.size()])).clone();
        int length = cmdarray.length;
        int i = 0;
        while (i < length) {
            if (cmdarray[i] != null) {
                i++;
            } else {
                throw new NullPointerException();
            }
        }
        String prog = cmdarray[0];
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkExec(prog);
        }
        String dir = this.directory == null ? null : this.directory.toString();
        int i2 = 1;
        while (i2 < cmdarray.length) {
            if (cmdarray[i2].indexOf(0) < 0) {
                i2++;
            } else {
                throw new IOException("invalid null character in command");
            }
        }
        try {
            return ProcessImpl.start(cmdarray, this.environment, dir, this.redirects, this.redirectErrorStream);
        } catch (IOException | IllegalArgumentException e) {
            String str;
            String exceptionInfo = new StringBuilder();
            exceptionInfo.append(": ");
            exceptionInfo.append(e.getMessage());
            exceptionInfo = exceptionInfo.toString();
            Throwable cause = e;
            if ((e instanceof IOException) && security != null) {
                try {
                    security.checkRead(prog);
                } catch (SecurityException se) {
                    exceptionInfo = "";
                    cause = se;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot run program \"");
            stringBuilder.append(prog);
            stringBuilder.append("\"");
            if (dir == null) {
                str = "";
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" (in directory \"");
                stringBuilder2.append(dir);
                stringBuilder2.append("\")");
                str = stringBuilder2.toString();
            }
            stringBuilder.append(str);
            stringBuilder.append(exceptionInfo);
            throw new IOException(stringBuilder.toString(), cause);
        }
    }
}
