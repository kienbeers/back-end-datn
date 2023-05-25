package datnnhom12api.dto.specifications;

import datnnhom12api.core.BaseSpecifications;
import datnnhom12api.entity.ScreenEntity;

public class ScreenScpecifications extends BaseSpecifications<ScreenEntity> {

    private static ScreenScpecifications INSTANCE;

    public static ScreenScpecifications getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ScreenScpecifications();
        }
        return INSTANCE;
    }
}
