package dev.liquidcatmofu.mtrpatches;

import org.mtr.mod.model.ModelLift1;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reuses MTR lift models by their constructor parameters.
 *
 * RenderLifts creates a new ModelLift1 for every visible lift render. Each
 * constructor bakes a full ModelPart tree, even when another lift has the same
 * dimensions. Keeping a small LRU avoids that per-frame model rebuild while
 * bounding the amount of retained geometry.
 */
public final class LiftModelCache {
    private static final int MAX_ENTRIES = 64;
    private static final LinkedHashMap<Key, ModelLift1> CACHE = new LinkedHashMap<>(16, 0.75F, true);

    private LiftModelCache() {
    }

    public static synchronized ModelLift1 get(int height, int width, int depth, boolean isDoubleSided) {
        final Key key = new Key(height, width, depth, isDoubleSided);
        ModelLift1 model = CACHE.get(key);
        if (model == null) {
            model = new ModelLift1(height, width, depth, isDoubleSided);
            CACHE.put(key, model);
            trimToLimit();
        }
        return model;
    }

    private static void trimToLimit() {
        while (CACHE.size() > MAX_ENTRIES) {
            final Iterator<Map.Entry<Key, ModelLift1>> iterator = CACHE.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private record Key(int height, int width, int depth, boolean isDoubleSided) {
    }
}
