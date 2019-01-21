package java.util.logging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import libcore.net.NetworkSecurityPolicy;

public class SocketHandler extends StreamHandler {
    private String host;
    private int port;
    private Socket sock;

    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".level");
        setLevel(manager.getLevelProperty(stringBuilder.toString(), Level.ALL));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".filter");
        setFilter(manager.getFilterProperty(stringBuilder.toString(), null));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".formatter");
        setFormatter(manager.getFormatterProperty(stringBuilder.toString(), new XMLFormatter()));
        try {
            stringBuilder = new StringBuilder();
            stringBuilder.append(cname);
            stringBuilder.append(".encoding");
            setEncoding(manager.getStringProperty(stringBuilder.toString(), null));
        } catch (Exception e) {
            try {
                setEncoding(null);
            } catch (Exception e2) {
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".port");
        this.port = manager.getIntProperty(stringBuilder.toString(), 0);
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".host");
        this.host = manager.getStringProperty(stringBuilder.toString(), null);
    }

    public SocketHandler() throws IOException {
        this.sealed = false;
        configure();
        try {
            connect();
            this.sealed = true;
        } catch (IOException ix) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SocketHandler: connect failed to ");
            stringBuilder.append(this.host);
            stringBuilder.append(":");
            stringBuilder.append(this.port);
            printStream.println(stringBuilder.toString());
            throw ix;
        }
    }

    public SocketHandler(String host, int port) throws IOException {
        this.sealed = false;
        configure();
        this.sealed = true;
        this.port = port;
        this.host = host;
        connect();
    }

    private void connect() throws IOException {
        StringBuilder stringBuilder;
        if (this.port == 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad port: ");
            stringBuilder.append(this.port);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.host == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Null host name: ");
            stringBuilder.append(this.host);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted()) {
            this.sock = new Socket(this.host, this.port);
            setOutputStream(new BufferedOutputStream(this.sock.getOutputStream()));
        } else {
            throw new IOException("Cleartext traffic not permitted");
        }
    }

    public synchronized void close() throws SecurityException {
        super.close();
        if (this.sock != null) {
            try {
                this.sock.close();
            } catch (IOException e) {
            }
        }
        this.sock = null;
    }

    public synchronized void publish(LogRecord record) {
        if (isLoggable(record)) {
            super.publish(record);
            flush();
        }
    }
}
