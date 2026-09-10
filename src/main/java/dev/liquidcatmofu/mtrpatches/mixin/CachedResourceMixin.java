package dev.liquidcatmofu.mtrpatches.mixin;

import dev.liquidcatmofu.mtrpatches.PatchConfig;
import dev.liquidcatmofu.mtrpatches.WeakCachedResourceRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "org.mtr.mod.resource.CachedResource", remap = false)
public abstract class CachedResourceMixin {
    @Shadow
    private Object data;

    @Shadow
    private long expiry;

    @Shadow
    @Final
    private long lifespan;

    @Shadow
    private static boolean canFetchCache;

    @Inject(
            method = "<init>(Ljava/util/function/Supplier;J)V",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void mtrPatches$detachStrongReference(Supplier<?> dataSupplier, long lifespan, CallbackInfo ci) {
        if (PatchConfig.FIX_CACHED_RESOURCE_REGISTRY_LEAK.get()) {
            WeakCachedResourceRegistry.detachFromStrongRegistry(this);
        }
    }

    @Inject(
            method = "getData(Z)Ljava/lang/Object;",
            at = @At("HEAD"),
            remap = false,
            require = 1
    )
    private void mtrPatches$refreshExpiryOnSuppressedAccess(boolean force, CallbackInfoReturnable<Object> cir) {
        // Stock MTR already refreshes expiry whenever force=true or the global
        // rebuild throttle is open. Only intervene in the branch where stock
        // MTR would skip the refresh despite returning an existing value.
        if (!PatchConfig.FIX_CACHED_RESOURCE_ACCESS_EXPIRY.get()
                || force
                || data == null
                || canFetchCache) {
            return;
        }

        final long now = System.currentTimeMillis();
        // Do not revive data whose previous expiry has already elapsed. If the
        // value is still valid, record this access by extending its idle TTL.
        if (now <= expiry) {
            expiry = now + lifespan;
        }
    }

    @Inject(method = "tick()V", at = @At("RETURN"), remap = false, require = 0)
    private static void mtrPatches$expireWeaklyTrackedResources(CallbackInfo ci) {
        WeakCachedResourceRegistry.expireTrackedResources();
    }
}
