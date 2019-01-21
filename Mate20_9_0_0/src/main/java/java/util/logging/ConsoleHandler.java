package java.util.logging;

public class ConsoleHandler extends StreamHandler {
    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".level");
        setLevel(manager.getLevelProperty(stringBuilder.toString(), Level.INFO));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".filter");
        setFilter(manager.getFilterProperty(stringBuilder.toString(), null));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".formatter");
        setFormatter(manager.getFormatterProperty(stringBuilder.toString(), new SimpleFormatter()));
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
    }

    public ConsoleHandler() {
        this.sealed = false;
        configure();
        setOutputStream(System.err);
        this.sealed = true;
    }

    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    public void close() {
        flush();
    }
}
