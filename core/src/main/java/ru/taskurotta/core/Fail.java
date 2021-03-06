package ru.taskurotta.core;

import java.util.Arrays;

/**
 * Created by void 25.11.13 19:51
 */
public class Fail extends RuntimeException {
    private String[] types;
    private String message;

    public Fail(String type, String message) {
        this(new String[]{type}, message);
    }

    public Fail(String[] types, String message) {
        if (types == null || types.length < 1) {
            throw new IllegalArgumentException("Type of the fail cannot be empty");
        }
        this.types = types;
        this.message = message;
    }

    public String getType() {
        return types[0];
    }

    public String getMessage() {
        return message;
    }

    public boolean instanceOf(String className) {
        for (String type : types) {
            if (type.equals(className)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Fail{" +
                "message='" + message + '\'' +
                ", types=" + Arrays.toString(types) +
                '}';
    }
}
