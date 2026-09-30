package dev.liquidcatmofu.mtrpatches.mixin;

import dev.liquidcatmofu.mtrpatches.PatchConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Pseudo
@Mixin(targets = "org.mtr.mapping.render.batch.BatchManager", remap = false)
public abstract class BatchManagerMixin {
    @Shadow @Final
    private Map<?, ?> translucentBatches;

    @Inject(
            method = "drawAll(Lorg/mtr/mapping/render/shader/ShaderManager;Z)V",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void mtrPatches$clearHiddenTranslucentBatch(@Coerce Object shaderManager, boolean renderTranslucent, CallbackInfo ci) {
        if (!renderTranslucent && PatchConfig.FIX_HIDDEN_TRANSLUCENT_BATCH_LEAK.get()) {
            translucentBatches.clear();
        }
    }
}
