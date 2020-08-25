package com.huawei.nb.utils;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.json.JsonSanitizer;
import com.huawei.nb.utils.logger.DSLog;
import java.text.Normalizer;

public final class JsonUtils {
    private JsonUtils() {
    }

    public static <T> T parse(String jsonContent, Class<T> beanClass) {
        if (TextUtils.isEmpty(jsonContent) || beanClass == null) {
            DSLog.e("Failed to parse json file, error: invalid parameter.", new Object[0]);
            return null;
        }
        try {
            return (T) new Gson().fromJson(sanitize(jsonContent), (Class) beanClass);
        } catch (JsonSyntaxException e) {
            DSLog.e("Failed to parse json file, error: %s.", e.getMessage());
            return null;
        }
    }

    public static JsonElement parse(String jsonContent) {
        if (TextUtils.isEmpty(jsonContent)) {
            DSLog.e("Failed to parse json string, error: Json is empty.", new Object[0]);
            return null;
        }
        try {
            return new JsonParser().parse(sanitize(jsonContent));
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public static boolean isValidJson(String jsonContent) {
        if (TextUtils.isEmpty(jsonContent)) {
            DSLog.e(" Json is empty!", new Object[0]);
            return false;
        }
        try {
            new JsonParser().parse(sanitize(jsonContent));
            return true;
        } catch (JsonSyntaxException | IllegalStateException e) {
            return false;
        }
    }

    public static boolean isJsonObject(String jsonString) {
        if (TextUtils.isEmpty(jsonString)) {
            DSLog.e(" Json is empty!", new Object[0]);
            return false;
        }
        try {
            return new JsonParser().parse(sanitize(jsonString)).isJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return false;
        }
    }

    public static boolean isValidJsonArray(String jsonContent) {
        if (TextUtils.isEmpty(jsonContent)) {
            DSLog.e("Failed to parse json string, error: JsonArray is empty.", new Object[0]);
            return false;
        }
        try {
            if (new JsonParser().parse(sanitize(jsonContent)).getAsJsonArray().size() >= 0) {
                return true;
            }
            return false;
        } catch (JsonSyntaxException | IllegalStateException e) {
            return false;
        }
    }

    public static boolean isJsonFormat(String json) {
        if (TextUtils.isEmpty(json)) {
            DSLog.e(" Json is empty! ", new Object[0]);
            return false;
        }
        String tempStr = Normalizer.normalize(json, Normalizer.Form.NFKC);
        if (parse(sanitize(tempStr)) == null || !tempStr.startsWith("{")) {
            return false;
        }
        return true;
    }

    public static String sanitize(String jsonContent) {
        return JsonSanitizer.sanitize(jsonContent);
    }
}
