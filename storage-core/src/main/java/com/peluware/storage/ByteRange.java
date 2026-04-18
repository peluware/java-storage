package com.peluware.storage;

/**
 * Rango de bytes para descargas parciales (equivalente al header HTTP Range: bytes=start-end).
 * El {@code end} puede omitirse con {@link #from(long)} para indicar "hasta el final del archivo".
 */
public record ByteRange(long start, long end) {

    public static final long OPEN_END = Long.MAX_VALUE;

    public ByteRange {
        if (start < 0) throw new IllegalArgumentException("start must be >= 0, got: " + start);
        if (end < start) throw new IllegalArgumentException("end must be >= start, got start=" + start + " end=" + end);
    }

    /** Rango cerrado: bytes [start, end] inclusive. */
    public static ByteRange of(long start, long end) {
        return new ByteRange(start, end);
    }

    /** Desde {@code start} hasta el final del archivo. */
    public static ByteRange from(long start) {
        return new ByteRange(start, OPEN_END);
    }

    /** Los primeros {@code count} bytes (bytes 0 a count-1). */
    public static ByteRange first(long count) {
        return new ByteRange(0, count - 1);
    }

    public boolean isOpenEnd() {
        return end == OPEN_END;
    }

    /** Produce el valor del header HTTP {@code Range}, e.g. {@code bytes=0-1023}. */
    public String toHttpHeader() {
        return isOpenEnd() ? "bytes=" + start + "-" : "bytes=" + start + "-" + end;
    }
}
