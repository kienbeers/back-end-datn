package datnnhom12api.dto.specifications;

import datnnhom12api.core.BaseSpecifications;
import datnnhom12api.entity.ProductEntity;

public class ProductSpecifications extends BaseSpecifications<ProductEntity> {
    private static ProductSpecifications INSTANCE;

    public static ProductSpecifications getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProductSpecifications();
        }

        return INSTANCE;
    }
}
