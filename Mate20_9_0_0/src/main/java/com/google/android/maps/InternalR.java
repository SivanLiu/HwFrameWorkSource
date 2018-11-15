package com.google.android.maps;

import java.lang.reflect.Field;
import java.util.MissingResourceException;

class InternalR {

    static final class array {
        public static final int maps_starting_lat_lng = get("maps_starting_lat_lng");
        public static final int maps_starting_zoom = get("maps_starting_zoom");

        private static int get(String fieldName) {
            return InternalR.get(com.android.internal.R.array.class, fieldName);
        }
    }

    static final class attr {
        public static final int mapViewStyle = get("mapViewStyle");
        public static final int state_focused = get("state_focused");
        public static final int state_pressed = get("state_pressed");
        public static final int state_selected = get("state_selected");

        private static int get(String fieldName) {
            return InternalR.get(com.android.internal.R.attr.class, fieldName);
        }
    }

    static final class drawable {
        public static final int compass_arrow = get("compass_arrow");
        public static final int compass_base = get("compass_base");
        public static final int ic_maps_indicator_current_position_anim = get("ic_maps_indicator_current_position_anim");
        public static final int loading_tile_android = get("loading_tile_android");
        public static final int maps_google_logo = get("maps_google_logo");
        public static final int no_tile_256 = get("no_tile_256");
        public static final int reticle = get("reticle");

        private static int get(String fieldName) {
            return InternalR.get(com.android.internal.R.drawable.class, fieldName);
        }
    }

    static final class styleable {
        public static final int[] MapView = ((int[]) InternalR.getObject(cls, "MapView"));
        public static final int MapView_apiKey = InternalR.get(cls, "MapView_apiKey");
        private static final Class cls = com.android.internal.R.styleable.class;
    }

    private static int get(Class<?> cls, String fieldName) {
        try {
            return getField(cls, fieldName).getInt(null);
        } catch (IllegalAccessException e) {
            throw translateException(e, cls, fieldName);
        }
    }

    private static Object getObject(Class<?> cls, String fieldName) {
        try {
            return getField(cls, fieldName).get(null);
        } catch (IllegalAccessException e) {
            throw translateException(e, cls, fieldName);
        }
    }

    private static Field getField(Class<?> cls, String fieldName) {
        try {
            return cls.getField(fieldName);
        } catch (NoSuchFieldException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find required resource ");
            stringBuilder.append(cls.getName());
            stringBuilder.append(".");
            stringBuilder.append(fieldName);
            stringBuilder.append("(");
            stringBuilder.append(e);
            stringBuilder.append(")");
            throw new MissingResourceException(stringBuilder.toString(), cls.getName(), fieldName);
        }
    }

    private static RuntimeException translateException(IllegalAccessException e, Class<?> cls, String fieldName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not access required resource ");
        stringBuilder.append(cls.getName());
        stringBuilder.append(".");
        stringBuilder.append(fieldName);
        stringBuilder.append("(");
        stringBuilder.append(e);
        stringBuilder.append(")");
        return new MissingResourceException(stringBuilder.toString(), cls.getName(), fieldName);
    }
}
