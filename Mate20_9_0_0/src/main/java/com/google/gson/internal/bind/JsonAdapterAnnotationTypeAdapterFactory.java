package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.reflect.TypeToken;

public final class JsonAdapterAnnotationTypeAdapterFactory implements TypeAdapterFactory {
    private final ConstructorConstructor constructorConstructor;

    public JsonAdapterAnnotationTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
        this.constructorConstructor = constructorConstructor;
    }

    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> targetType) {
        JsonAdapter annotation = (JsonAdapter) targetType.getRawType().getAnnotation(JsonAdapter.class);
        if (annotation == null) {
            return null;
        }
        return getTypeAdapter(this.constructorConstructor, gson, targetType, annotation);
    }

    TypeAdapter<?> getTypeAdapter(ConstructorConstructor constructorConstructor, Gson gson, TypeToken<?> type, JsonAdapter annotation) {
        TypeAdapter<?> typeAdapter;
        TypeAdapter<?> instance = constructorConstructor.get(TypeToken.get(annotation.value())).construct();
        if (instance instanceof TypeAdapter) {
            typeAdapter = instance;
        } else if (instance instanceof TypeAdapterFactory) {
            typeAdapter = ((TypeAdapterFactory) instance).create(gson, type);
        } else if ((instance instanceof JsonSerializer) || (instance instanceof JsonDeserializer)) {
            JsonDeserializer<?> jsonDeserializer = null;
            JsonSerializer<?> serializer = instance instanceof JsonSerializer ? (JsonSerializer) instance : null;
            if (instance instanceof JsonDeserializer) {
                jsonDeserializer = (JsonDeserializer) instance;
            }
            TypeAdapter<?> treeTypeAdapter = new TreeTypeAdapter(serializer, jsonDeserializer, gson, type, null);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid attempt to bind an instance of ");
            stringBuilder.append(instance.getClass().getName());
            stringBuilder.append(" as a @JsonAdapter for ");
            stringBuilder.append(type.toString());
            stringBuilder.append(". @JsonAdapter value must be a TypeAdapter, TypeAdapterFactory, JsonSerializer or JsonDeserializer.");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        if (typeAdapter == null || !annotation.nullSafe()) {
            return typeAdapter;
        }
        return typeAdapter.nullSafe();
    }
}
