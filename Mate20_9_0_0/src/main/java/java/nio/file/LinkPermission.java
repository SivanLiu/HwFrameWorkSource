package java.nio.file;

import java.security.BasicPermission;

public final class LinkPermission extends BasicPermission {
    static final long serialVersionUID = -1441492453772213220L;

    private void checkName(String name) {
        if (!name.equals("hard") && !name.equals("symbolic")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("name: ");
            stringBuilder.append(name);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public LinkPermission(String name) {
        super(name);
        checkName(name);
    }

    public LinkPermission(String name, String actions) {
        super(name);
        checkName(name);
        if (actions != null && actions.length() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("actions: ");
            stringBuilder.append(actions);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }
}
