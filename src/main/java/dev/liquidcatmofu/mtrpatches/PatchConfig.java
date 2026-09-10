package dev.liquidcatmofu.mtrpatches;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PatchConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue FIX_HIDDEN_TRANSLUCENT_BATCH_LEAK;
    public static final ForgeConfigSpec.BooleanValue FIX_CACHED_RESOURCE_REGISTRY_LEAK;
    public static final ForgeConfigSpec.BooleanValue FIX_LIFT_MODEL_REBUILD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("patches");
        FIX_HIDDEN_TRANSLUCENT_BATCH_LEAK = builder
                .comment("Clear MTR translucent render batches when translucent rendering is disabled.")
                .define("fixHiddenTranslucentBatchLeak", true);
        FIX_CACHED_RESOURCE_REGISTRY_LEAK = builder
                .comment("Stop MTR CachedResource.CACHED_RESOURCES from permanently strongly retaining every cache instance.")
                .define("fixCachedResourceRegistryLeak", true);
        FIX_LIFT_MODEL_REBUILD = builder
                .comment("Reuse ModelLift1 instances by lift dimensions instead of rebuilding and baking the same model every visible render.")
                .define("fixLiftModelRebuild", true);
        builder.pop();

        SPEC = builder.build();
    }

    private PatchConfig() {
    }
}
