# MTR 1.20.1 Patches

Small client-side patches for Minecraft Transit Railway 4.x on Forge 1.20.1.

This project is intentionally separate from MTR itself. It is meant for installations that need to stay on the 1.20.1 branch while applying narrowly-scoped compatibility and memory fixes.

## Current patches

### Hidden translucent batch leak

When MTR is configured to hide translucent parts, `BatchManager#drawAll` skips drawing the translucent batch. In the affected 1.20.1 code path, that batch is also left uncleared, allowing `RenderCall` entries to accumulate every frame.

This patch clears the translucent batch at the end of `drawAll` whenever translucent rendering is disabled.

Observed before the patch / setting workaround:

- 253,329,046 render calls queued
- 252,455,155 clears observed through `drawBatch`
- about 873,891 queued calls were unaccounted for after ~21 minutes

With translucent rendering enabled, queued and observed-cleared counts matched exactly.

### CachedResource strong-registry leak

MTR's `CachedResource` stores every instance in a static `CACHED_RESOURCES` collection so `tick()` can expire cached data. Nested caches may be created repeatedly, so the static collection itself can keep obsolete `CachedResource` instances and their supplier closures alive indefinitely.

This patch removes newly-created resources from MTR's strong registry and tracks them using `WeakReference`s instead. The original expiry behavior is reproduced for still-live resources. If the reflective hook cannot be initialized, the patch leaves the original MTR behavior intact and logs one error.

### Lift model rebuild churn

`RenderLifts#render` constructs a new `ModelLift1` for every visible lift render. `ModelLift1` then builds and bakes its vanilla `ModelPart` tree in the constructor. A profiler capture over 20m06s recorded 46,504 `ModelLift1` builds producing 4,231,864 `ModelPart` instances (91 per build), accounting for about 78% of all `ModelPart` construction in that capture.

The patch redirects that constructor call through a small cache keyed by `(height, width, depth, isDoubleSided)`. Equal lift geometries reuse the same baked model. The cache is an access-ordered LRU capped at 64 entries so unusual lift dimension combinations cannot retain an unbounded number of models.

MTR's lift render code supplies translation, rotation, door offsets, texture and light at render time; those values are not part of the cached geometry key. Runtime validation is still required before this patch should be considered stable.

## Configuration

The Forge client config is written as `config/mtr_patches-client.toml`.

- `fixHiddenTranslucentBatchLeak = true`
- `fixCachedResourceRegistryLeak = true`
- `fixLiftModelRebuild = true`

All patches default to enabled. Restart the client after changing an option.

## Target

- Minecraft 1.20.1
- Forge 47.x
- MTR 4.0.x
- Java 17

This is not intended for newer MTR branches without separate validation.

## Testing

Use the separate `mtr-profiler` diagnostic mod to compare long-session behavior. In particular, watch:

- `BatchManager.RenderCall` queued / observed-cleared / unaccounted counts
- `CachedResource` creation rate
- `ModelPart` live count / heap after GC
- repeated `VehicleModel` rebuilds
- `EntityModelExtension buildModel attribution`; with the lift patch active, `ModelLift1` builds should fall from per-frame rates to roughly one build per distinct cached lift geometry

The profiler and this patch mod can be installed together.

## Building

The project uses a tiny compile-only `ModelLift1` signature stub so the redirect can have the exact MTR constructor type without bundling MTR itself. The stub source set is not included in the output jar; the real class is supplied by MTR at runtime.

## License

MIT
