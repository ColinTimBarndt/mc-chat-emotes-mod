package io.github.colintimbarndt.chat_emotes.data;

public final class PackExportException extends Exception {
    public PackExportException(String name) {
        super(name);
    }
    public PackExportException(String name, Throwable cause) {
        super(name, cause);
    }
}
