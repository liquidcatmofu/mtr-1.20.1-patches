package dev.liquidcatmofu.mtrpatches.mixin;

import dev.liquidcatmofu.mtrpatches.PatchConfig;
import dev.liquidcatmofu.mtrpatches.WeakCachedResourceRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "org.mtr.mod.resource.CachedResource", remap = false)
public abstract class CachedResourceMixin {
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

    @Inject(method = "tick()V", at = @At("RETURN"), remap = false, require = 0)
    private static void mtrPatches$expireWeaklyTrackedResources(CallbackInfo ci) {
        WeakCachedResourceRegistry.expireTrackedResources();
    }
}
