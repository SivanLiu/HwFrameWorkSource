package java.sql;

public class SQLWarning extends SQLException {
    private static final long serialVersionUID = 3917336774604784856L;

    public SQLWarning(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLWarning: reason(");
        stringBuilder.append(reason);
        stringBuilder.append(") SQLState(");
        stringBuilder.append(SQLState);
        stringBuilder.append(") vendor code(");
        stringBuilder.append(vendorCode);
        stringBuilder.append(")");
        DriverManager.println(stringBuilder.toString());
    }

    public SQLWarning(String reason, String SQLState) {
        super(reason, SQLState);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLWarning: reason(");
        stringBuilder.append(reason);
        stringBuilder.append(") SQLState(");
        stringBuilder.append(SQLState);
        stringBuilder.append(")");
        DriverManager.println(stringBuilder.toString());
    }

    public SQLWarning(String reason) {
        super(reason);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLWarning: reason(");
        stringBuilder.append(reason);
        stringBuilder.append(")");
        DriverManager.println(stringBuilder.toString());
    }

    public SQLWarning() {
        DriverManager.println("SQLWarning: ");
    }

    public SQLWarning(Throwable cause) {
        super(cause);
        DriverManager.println("SQLWarning");
    }

    public SQLWarning(String reason, Throwable cause) {
        super(reason, cause);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLWarning : reason(");
        stringBuilder.append(reason);
        stringBuilder.append(")");
        DriverManager.println(stringBuilder.toString());
    }

    public SQLWarning(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLWarning: reason(");
        stringBuilder.append(reason);
        stringBuilder.append(") SQLState(");
        stringBuilder.append(SQLState);
        stringBuilder.append(")");
        DriverManager.println(stringBuilder.toString());
    }

    public SQLWarning(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLWarning: reason(");
        stringBuilder.append(reason);
        stringBuilder.append(") SQLState(");
        stringBuilder.append(SQLState);
        stringBuilder.append(") vendor code(");
        stringBuilder.append(vendorCode);
        stringBuilder.append(")");
        DriverManager.println(stringBuilder.toString());
    }

    public SQLWarning getNextWarning() {
        try {
            return (SQLWarning) getNextException();
        } catch (ClassCastException e) {
            throw new Error("SQLWarning chain holds value that is not a SQLWarning");
        }
    }

    public void setNextWarning(SQLWarning w) {
        setNextException(w);
    }
}
