package dev.liquidcatmofu.mtrpatches.mixin;

import dev.liquidcatmofu.mtrpatches.LiftModelCache;
import dev.liquidcatmofu.mtrpatches.PatchConfig;
import org.mtr.mod.model.ModelLift1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "org.mtr.mod.render.RenderLifts", remap = false)
public abstract class RenderLiftsMixin {
    /**
     * The ModelLift1 allocation currently lives inside a compiler-generated
     * lambda. Match all methods in RenderLifts and constrain the injection to
     * the exact constructor instead of depending on a synthetic lambda number.
     */
    @Redirect(
            method = "*",
            at = @At(
                    value = "NEW",
                    target = "(IIIZ)Lorg/mtr/mod/model/ModelLift1;"
            ),
            remap = false,
            require = 1
    )
    private static ModelLift1 mtrPatches$reuseLiftModel(int height, int width, int depth, boolean isDoubleSided) {
        if (!PatchConfig.FIX_LIFT_MODEL_REBUILD.get()) {
            return new ModelLift1(height, width, depth, isDoubleSided);
        }
        return LiftModelCache.get(height, width, depth, isDoubleSided);
    }
}
