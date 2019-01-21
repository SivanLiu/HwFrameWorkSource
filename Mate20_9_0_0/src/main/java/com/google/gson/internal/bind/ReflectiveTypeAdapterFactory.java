package com.google.gson.internal.bind;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.C$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
    private final ConstructorConstructor constructorConstructor;
    private final Excluder excluder;
    private final FieldNamingStrategy fieldNamingPolicy;
    private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;

    static abstract class BoundField {
        final boolean deserialized;
        final String name;
        final boolean serialized;

        abstract void read(JsonReader jsonReader, Object obj) throws IOException, IllegalAccessException;

        abstract void write(JsonWriter jsonWriter, Object obj) throws IOException, IllegalAccessException;

        abstract boolean writeField(Object obj) throws IOException, IllegalAccessException;

        protected BoundField(String name, boolean serialized, boolean deserialized) {
            this.name = name;
            this.serialized = serialized;
            this.deserialized = deserialized;
        }
    }

    public static final class Adapter<T> extends TypeAdapter<T> {
        private final Map<String, BoundField> boundFields;
        private final ObjectConstructor<T> constructor;

        Adapter(ObjectConstructor<T> constructor, Map<String, BoundField> boundFields) {
            this.constructor = constructor;
            this.boundFields = boundFields;
        }

        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            T instance = this.constructor.construct();
            try {
                in.beginObject();
                while (in.hasNext()) {
                    BoundField field = (BoundField) this.boundFields.get(in.nextName());
                    if (field != null) {
                        if (field.deserialized) {
                            field.read(in, instance);
                        }
                    }
                    in.skipValue();
                }
                in.endObject();
                return instance;
            } catch (IllegalStateException e) {
                throw new JsonSyntaxException(e);
            } catch (IllegalAccessException e2) {
                throw new AssertionError(e2);
            }
        }

        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            try {
                for (BoundField boundField : this.boundFields.values()) {
                    if (boundField.writeField(value)) {
                        out.name(boundField.name);
                        boundField.write(out, value);
                    }
                }
                out.endObject();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    public ReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor, FieldNamingStrategy fieldNamingPolicy, Excluder excluder, JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingPolicy = fieldNamingPolicy;
        this.excluder = excluder;
        this.jsonAdapterFactory = jsonAdapterFactory;
    }

    public boolean excludeField(Field f, boolean serialize) {
        return excludeField(f, serialize, this.excluder);
    }

    static boolean excludeField(Field f, boolean serialize, Excluder excluder) {
        return (excluder.excludeClass(f.getType(), serialize) || excluder.excludeField(f, serialize)) ? false : true;
    }

    private List<String> getFieldNames(Field f) {
        SerializedName annotation = (SerializedName) f.getAnnotation(SerializedName.class);
        if (annotation == null) {
            return Collections.singletonList(this.fieldNamingPolicy.translateName(f));
        }
        String serializedName = annotation.value();
        String[] alternates = annotation.alternate();
        if (alternates.length == 0) {
            return Collections.singletonList(serializedName);
        }
        List<String> fieldNames = new ArrayList(alternates.length + 1);
        fieldNames.add(serializedName);
        for (String alternate : alternates) {
            fieldNames.add(alternate);
        }
        return fieldNames;
    }

    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();
        if (Object.class.isAssignableFrom(raw)) {
            return new Adapter(this.constructorConstructor.get(type), getBoundFields(gson, type, raw));
        }
        return null;
    }

    private BoundField createBoundField(Gson context, Field field, String name, TypeToken<?> fieldType, boolean serialize, boolean deserialize) {
        Gson gson = context;
        TypeToken typeToken = fieldType;
        boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
        Field field2 = field;
        JsonAdapter annotation = (JsonAdapter) field2.getAnnotation(JsonAdapter.class);
        TypeAdapter<?> mapped = null;
        if (annotation != null) {
            mapped = this.jsonAdapterFactory.getTypeAdapter(this.constructorConstructor, gson, typeToken, annotation);
        }
        final boolean jsonAdapterPresent = mapped != null;
        if (mapped == null) {
            mapped = gson.getAdapter(typeToken);
        }
        final TypeAdapter<?> typeAdapter = mapped;
        final Field field3 = field2;
        final Gson gson2 = gson;
        final TypeToken typeToken2 = typeToken;
        annotation = isPrimitive;
        return new BoundField(name, serialize, deserialize) {
            void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException {
                TypeAdapter t;
                Object fieldValue = field3.get(value);
                if (jsonAdapterPresent) {
                    t = typeAdapter;
                } else {
                    t = new TypeAdapterRuntimeTypeWrapper(gson2, typeAdapter, typeToken2.getType());
                }
                t.write(writer, fieldValue);
            }

            void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {
                Object fieldValue = typeAdapter.read(reader);
                if (fieldValue != null || !annotation) {
                    field3.set(value, fieldValue);
                }
            }

            public boolean writeField(Object value) throws IOException, IllegalAccessException {
                boolean z = false;
                if (!this.serialized) {
                    return false;
                }
                if (field3.get(value) != value) {
                    z = true;
                }
                return z;
            }
        };
    }

    private Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw) {
        ReflectiveTypeAdapterFactory reflectiveTypeAdapterFactory = this;
        LinkedHashMap result = new LinkedHashMap();
        if (raw.isInterface()) {
            return result;
        }
        Type declaredType = type.getType();
        TypeToken<?> type2 = type;
        Class<?> raw2 = raw;
        while (true) {
            Type declaredType2 = declaredType;
            if (raw2 == Object.class) {
                return result;
            }
            Field[] fields = raw2.getDeclaredFields();
            int length = fields.length;
            boolean z = false;
            int i = 0;
            while (i < length) {
                Field field = fields[i];
                boolean serialize = reflectiveTypeAdapterFactory.excludeField(field, true);
                boolean deserialize = reflectiveTypeAdapterFactory.excludeField(field, z);
                if (serialize || deserialize) {
                    BoundField previous;
                    List<String> fieldNames;
                    Type fieldType;
                    Field field2;
                    field.setAccessible(true);
                    Type fieldType2 = C$Gson$Types.resolve(type2.getType(), raw2, field.getGenericType());
                    List<String> fieldNames2 = reflectiveTypeAdapterFactory.getFieldNames(field);
                    int i2 = 0;
                    int size = fieldNames2.size();
                    boolean z2 = serialize;
                    BoundField previous2 = null;
                    boolean serialize2 = z2;
                    while (i2 < size) {
                        String name = (String) fieldNames2.get(i2);
                        if (i2 != 0) {
                            serialize2 = false;
                        }
                        boolean serialize3 = serialize2;
                        ReflectiveTypeAdapterFactory boundField = reflectiveTypeAdapterFactory;
                        previous = previous2;
                        int i3 = i2;
                        int size2 = size;
                        fieldNames = fieldNames2;
                        fieldType = fieldType2;
                        field2 = field;
                        previous2 = (BoundField) result.put(name, boundField.createBoundField(context, field, name, TypeToken.get(fieldType2), serialize3, deserialize));
                        if (previous == null) {
                        } else {
                            previous2 = previous;
                        }
                        i2 = i3 + 1;
                        serialize2 = serialize3;
                        fieldType2 = fieldType;
                        size = size2;
                        fieldNames2 = fieldNames;
                        field = field2;
                        reflectiveTypeAdapterFactory = this;
                    }
                    previous = previous2;
                    fieldNames = fieldNames2;
                    fieldType = fieldType2;
                    field2 = field;
                    if (previous != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(declaredType2);
                        stringBuilder.append(" declares multiple JSON fields named ");
                        stringBuilder.append(previous.name);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                i++;
                reflectiveTypeAdapterFactory = this;
                z = false;
            }
            type2 = TypeToken.get(C$Gson$Types.resolve(type2.getType(), raw2, raw2.getGenericSuperclass()));
            raw2 = type2.getRawType();
            declaredType = declaredType2;
            reflectiveTypeAdapterFactory = this;
        }
    }
}
