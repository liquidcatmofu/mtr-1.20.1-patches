package dev.liquidcatmofu.mtrpatches;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WeakCachedResourceRegistry {
    private static final List<WeakReference<Object>> TRACKED = new ArrayList<>();
    private static final AtomicBoolean INIT_ERROR_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean RUNTIME_ERROR_LOGGED = new AtomicBoolean();

    private static volatile boolean initialized;
    private static Field registryField;
    private static Field dataField;
    private static Field expiryField;

    private WeakCachedResourceRegistry() {
    }

    public static void detachFromStrongRegistry(Object cachedResource) {
        try {
            if (!initialize()) {
                return;
            }

            Object registryObject = registryField.get(null);
            if (!(registryObject instanceof Collection<?> registry)) {
                logInitError(new IllegalStateException("CACHED_RESOURCES is not a Collection"));
                return;
            }

            if (registry.remove(cachedResource)) {
                synchronized (TRACKED) {
                    TRACKED.add(new WeakReference<>(cachedResource));
                }
            }
        } catch (Throwable throwable) {
            logRuntimeError(throwable);
        }
    }

    public static void expireTrackedResources() {
        if (!initialized) {
            return;
        }

        final long now = System.currentTimeMillis();
        synchronized (TRACKED) {
            Iterator<WeakReference<Object>> iterator = TRACKED.iterator();
            while (iterator.hasNext()) {
                Object cachedResource = iterator.next().get();
                if (cachedResource == null) {
                    iterator.remove();
                    continue;
                }

                try {
                    if (now > expiryField.getLong(cachedResource)) {
                        dataField.set(cachedResource, null);
                    }
                } catch (Throwable throwable) {
                    logRuntimeError(throwable);
                    return;
                }
            }
        }
    }

    private static boolean initialize() {
        if (initialized) {
            return true;
        }

        synchronized (WeakCachedResourceRegistry.class) {
            if (initialized) {
                return true;
            }

            try {
                Class<?> clazz = Class.forName("org.mtr.mod.resource.CachedResource", false, WeakCachedResourceRegistry.class.getClassLoader());
                registryField = clazz.getDeclaredField("CACHED_RESOURCES");
                dataField = clazz.getDeclaredField("data");
                expiryField = clazz.getDeclaredField("expiry");
                registryField.setAccessible(true);
                dataField.setAccessible(true);
                expiryField.setAccessible(true);
                initialized = true;
                return true;
            } catch (Throwable throwable) {
                logInitError(throwable);
                return false;
            }
        }
    }

    private static void logInitError(Throwable throwable) {
        if (INIT_ERROR_LOGGED.compareAndSet(false, true)) {
            MtrPatches.LOGGER.error("Unable to initialize the CachedResource weak-registry patch; leaving MTR behavior unchanged", throwable);
        }
    }

    private static void logRuntimeError(Throwable throwable) {
        if (RUNTIME_ERROR_LOGGED.compareAndSet(false, true)) {
            MtrPatches.LOGGER.error("CachedResource weak-registry patch encountered an error", throwable);
        }
    }
}
