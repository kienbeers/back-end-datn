package datnnhom12api.dto.specifications;

import datnnhom12api.core.BaseSpecifications;
import datnnhom12api.entity.DiscountEntity;

public class DiscountSpecifications extends BaseSpecifications<DiscountEntity> {
    private static DiscountSpecifications INSTANCE;

    public static DiscountSpecifications getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DiscountSpecifications();
        }

        return INSTANCE;
    }
}
