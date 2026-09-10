# MTR 1.20.1 Patches

Small client-side patches for Minecraft Transit Railway 4.x on Forge 1.20.1.

This project is intentionally separate from MTR itself. It is meant for installations that need to stay on the 1.20.1 branch while applying narrowly-scoped compatibility and memory fixes.

## Current patches

### Hidden translucent batch leak

When MTR is configured to hide translucent parts, `BatchManager#drawAll` skips drawing the translucent batch. In the affected 1.20.1 code path, that batch is also left uncleared, allowing `RenderCall` entries to accumulate every frame.

This patch clears the translucent batch at the end of `drawAll` whenever translucent rendering is disabled.

Observed before the patch / setting workaround:

- 253,329,046 render calls queued
- 252,455,155 cleared
- about 873,891 still pending after ~21 minutes

With translucent rendering enabled, queued and cleared counts matched exactly and pending returned to zero.

### CachedResource strong-registry leak

MTR's `CachedResource` stores every instance in a static `CACHED_RESOURCES` collection so `tick()` can expire cached data. Nested caches may be created repeatedly, so the static collection itself can keep obsolete `CachedResource` instances and their supplier closures alive indefinitely.

This patch removes newly-created resources from MTR's strong registry and tracks them using `WeakReference`s instead. The original expiry behavior is reproduced for still-live resources. If the reflective hook cannot be initialized, the patch leaves the original MTR behavior intact and logs one error.

## Configuration

The Forge client config is written as `config/mtr_patches-client.toml`.

- `fixHiddenTranslucentBatchLeak = true`
- `fixCachedResourceRegistryLeak = true`

Both default to enabled. Restart the client after changing either option.

## Target

- Minecraft 1.20.1
- Forge 47.x
- MTR 4.0.x
- Java 17

This is not intended for newer MTR branches without separate validation.

## Testing

Use the separate `mtr-profiler` diagnostic mod to compare long-session behavior. In particular, watch:

- pending `BatchManager.RenderCall` count
- `CachedResource` creation rate
- `ModelPart` live count / heap after GC
- repeated `VehicleModel` rebuilds

The profiler and this patch mod can be installed together.

## License

MIT
