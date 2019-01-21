package sun.util.locale;

class Extension {
    private String id;
    private final char key;
    private String value;

    protected Extension(char key) {
        this.key = key;
    }

    Extension(char key, String value) {
        this.key = key;
        setValue(value);
    }

    protected void setValue(String value) {
        this.value = value;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.key);
        stringBuilder.append(LanguageTag.SEP);
        stringBuilder.append(value);
        this.id = stringBuilder.toString();
    }

    public char getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public String getID() {
        return this.id;
    }

    public String toString() {
        return getID();
    }
}
