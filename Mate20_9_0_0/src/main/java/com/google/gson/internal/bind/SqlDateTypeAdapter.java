package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public final class SqlDateTypeAdapter extends TypeAdapter<Date> {
    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == Date.class ? new SqlDateTypeAdapter() : null;
        }
    };
    private final DateFormat format = new SimpleDateFormat("MMM d, yyyy");

    public synchronized Date read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        try {
            return new Date(this.format.parse(in.nextString()).getTime());
        } catch (Throwable e) {
            throw new JsonSyntaxException(e);
        }
    }

    public synchronized void write(JsonWriter out, Date value) throws IOException {
        out.value(value == null ? null : this.format.format(value));
    }
}
