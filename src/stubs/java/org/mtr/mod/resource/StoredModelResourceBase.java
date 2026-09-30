package org.mtr.mod.resource;

import org.mtr.mod.render.DynamicVehicleModel;

public interface StoredModelResourceBase {
    OptimizedModelWrapper getOptimizedModel();

    DynamicVehicleModel getDynamicVehicleModel();
}
