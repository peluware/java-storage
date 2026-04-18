package com.peluware.storage;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class StoreTracking {

    private final ThreadLocal<@Nullable List<String>> storedThreadLocal = new ThreadLocal<>();

    public void start() {
        storedThreadLocal.set(new ArrayList<>());
        log.debug("Started store tracking.");
    }

    void track(String paths) {
        var stored = storedThreadLocal.get();
        if (stored != null) {
            stored.add(paths);
            log.debug("Tracked stored key: {}", paths);
        } else {
            log.debug("Tracking not started, skipping path: {}", paths);
        }
    }

    void track(List<String> paths) {
        var stored = storedThreadLocal.get();
        if (stored != null) {
            stored.addAll(paths);
            log.debug("Tracked stored paths: {}", paths);
        } else {
            log.debug("Tracking not started, skipping paths: {}", paths);
        }
    }

    public List<String> getTracked() {
        var stored = storedThreadLocal.get();
        return stored != null ? stored : List.of();
    }

    public boolean isTracking() {
        return storedThreadLocal.get() != null;
    }

    public void clear() {
        storedThreadLocal.remove();
        log.debug("Cleared store tracking context.");
    }
}
