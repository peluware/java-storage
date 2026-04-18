package com.peluware.storage;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface StorageContentLoader {
    InputStream load() throws IOException;
}
