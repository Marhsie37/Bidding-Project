package com.auction.shared.utils;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    private static class TestObject {
        private String name;
        private int value;
        private LocalDateTime timestamp;

        public TestObject() {}

        public TestObject(String name, int value, LocalDateTime timestamp) {
            this.name = name;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestObject that = (TestObject) obj;
            return value == that.value &&
                    name.equals(that.name) &&
                    timestamp.equals(that.timestamp);
        }
    }

    private TestObject testObject;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        testObject = new TestObject("test", 42, testDateTime);
    }

    @Test
    void testToJson() {
        String json = JsonUtils.toJson(testObject);

        assertNotNull(json);
        assertTrue(json.contains("\"name\": \"test\""));
        assertTrue(json.contains("\"value\": 42"));
        assertTrue(json.contains(testDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    @Test
    void testToJsonWithNull() {
        String json = JsonUtils.toJson(null);
        assertEquals("null", json);
    }

    @Test
    void testFromJson() {
        String json = JsonUtils.toJson(testObject);
        TestObject deserialized = JsonUtils.fromJson(json, TestObject.class);

        assertNotNull(deserialized);
        assertEquals(testObject.getName(), deserialized.getName());
        assertEquals(testObject.getValue(), deserialized.getValue());
        assertEquals(testObject.getTimestamp(), deserialized.getTimestamp());
    }

    @Test
    void testFromJsonWithInvalidJson() {
        String invalidJson = "{invalid json}";

        assertThrows(Exception.class, () -> {
            JsonUtils.fromJson(invalidJson, TestObject.class);
        });
    }

    @Test
    void testLocalDateTimeSerialization() {
        String json = JsonUtils.toJson(testObject);

        assertTrue(json.contains(testDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        TestObject deserialized = JsonUtils.fromJson(json, TestObject.class);
        assertEquals(testDateTime, deserialized.getTimestamp());
    }

    @Test
    void testLocalDateTimeWithNullValue() {
        TestObject objWithNullTime = new TestObject("test", 42, null);
        String json = JsonUtils.toJson(objWithNullTime);

        TestObject deserialized = JsonUtils.fromJson(json, TestObject.class);
        assertNull(deserialized.getTimestamp());
    }

    @Test
    void testComplexObjectSerialization() {
        Map<String, Object> complexMap = new HashMap<>();
        complexMap.put("string", "hello");
        complexMap.put("number", 123);
        complexMap.put("datetime", testDateTime);
        complexMap.put("nested", testObject);

        String json = JsonUtils.toJson(complexMap);
        assertNotNull(json);

        Map<String, Object> deserialized = JsonUtils.fromJson(json, Map.class);
        assertNotNull(deserialized);
    }

    @Test
    void testPrettyPrinting() {
        String json = JsonUtils.toJson(testObject);

        // Pretty printing should add newlines and indentation
        assertTrue(json.contains("\n") || json.contains("\r\n"));
    }
}