package eu.greev.dcbot.utils;

public class MutableString {
    private String value;

    private MutableString() {}

    public static MutableString of(String string) {
        MutableString m = new MutableString();
        m.value = string;
        return m;
    }

    public String get() {
        return value;
    }

    public void set(String string) {
        this.value = string;
    }
}
