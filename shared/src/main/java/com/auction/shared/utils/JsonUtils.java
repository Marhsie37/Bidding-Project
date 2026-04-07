package com.auction.shared.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class JsonUtils {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    private static class LocalDateTimeAdapter extends com.google.gson.TypeAdapter<LocalDateTime> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(formatter));
            }
        }

        @Override
        public LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString(), formatter);
        }
    }
}