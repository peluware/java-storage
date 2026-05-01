package com.peluware.storage;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface HasStoragePaths {
    List<@Nullable String> getStoredPaths();
}
