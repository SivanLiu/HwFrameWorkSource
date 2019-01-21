package com.google.gson;

import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public final class JsonParser {
    public JsonElement parse(String json) throws JsonSyntaxException {
        return parse(new StringReader(json));
    }

    public JsonElement parse(Reader json) throws JsonIOException, JsonSyntaxException {
        try {
            JsonReader jsonReader = new JsonReader(json);
            JsonElement element = parse(jsonReader);
            if (!element.isJsonNull()) {
                if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                    throw new JsonSyntaxException("Did not consume the entire document.");
                }
            }
            return element;
        } catch (MalformedJsonException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e2) {
            throw new JsonIOException(e2);
        } catch (NumberFormatException e22) {
            throw new JsonSyntaxException(e22);
        }
    }

    public JsonElement parse(JsonReader json) throws JsonIOException, JsonSyntaxException {
        StringBuilder stringBuilder;
        boolean lenient = json.isLenient();
        json.setLenient(true);
        try {
            JsonElement parse = Streams.parse(json);
            json.setLenient(lenient);
            return parse;
        } catch (StackOverflowError e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed parsing JSON source: ");
            stringBuilder.append(json);
            stringBuilder.append(" to Json");
            throw new JsonParseException(stringBuilder.toString(), e);
        } catch (OutOfMemoryError e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed parsing JSON source: ");
            stringBuilder.append(json);
            stringBuilder.append(" to Json");
            throw new JsonParseException(stringBuilder.toString(), e2);
        } catch (Throwable th) {
            json.setLenient(lenient);
        }
    }
}
