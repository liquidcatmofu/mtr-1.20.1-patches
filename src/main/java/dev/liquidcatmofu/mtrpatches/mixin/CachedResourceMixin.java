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
    private long expiry;

    @Shadow
    @Final
    private long lifespan;

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
            at = @At("RETURN"),
            remap = false,
            require = 1
    )
    private void mtrPatches$refreshExpiryOnAccess(boolean force, CallbackInfoReturnable<Object> cir) {
        if (!PatchConfig.FIX_CACHED_RESOURCE_ACCESS_EXPIRY.get() || cir.getReturnValue() == null) {
            return;
        }

        final long now = System.currentTimeMillis();
        // Do not revive a value that had already expired before this access.
        // Fresh/rebuilt values already have an expiry in the future, and active
        // values are extended here even when MTR's global canFetchCache flag is false.
        if (now <= expiry) {
            expiry = now + lifespan;
        }
    }

    @Inject(method = "tick()V", at = @At("RETURN"), remap = false, require = 0)
    private static void mtrPatches$expireWeaklyTrackedResources(CallbackInfo ci) {
        WeakCachedResourceRegistry.expireTrackedResources();
    }
}
