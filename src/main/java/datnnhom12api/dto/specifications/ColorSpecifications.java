package datnnhom12api.dto.specifications;

import datnnhom12api.core.BaseSpecifications;
import datnnhom12api.entity.ColorEntity;

public class ColorSpecifications extends BaseSpecifications<ColorEntity> {
    private static ColorSpecifications INSTANCE;

    public static ColorSpecifications getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ColorSpecifications();
        }

        return INSTANCE;
    }
}
