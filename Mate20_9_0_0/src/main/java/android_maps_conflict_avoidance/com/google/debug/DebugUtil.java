package android_maps_conflict_avoidance.com.google.debug;

public class DebugUtil {
    public static boolean isAntPropertyExpanded(String property) {
        return property.startsWith("${") ^ 1;
    }

    public static String getAntProperty(String property, String def) {
        return isAntPropertyExpanded(property) ? property : def;
    }

    public static String getAntPropertyOrNull(String property) {
        return getAntProperty(property, null);
    }

    public static Object newInstance(Class cls) {
        StringBuilder stringBuilder;
        try {
            return cls.newInstance();
        } catch (InstantiationException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot instantiate instance of class ");
            stringBuilder.append(cls.getName());
            throw new RuntimeException(stringBuilder.toString());
        } catch (IllegalAccessException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No public default constructor for class ");
            stringBuilder.append(cls.getName());
            throw new RuntimeException(stringBuilder.toString());
        }
    }
}
