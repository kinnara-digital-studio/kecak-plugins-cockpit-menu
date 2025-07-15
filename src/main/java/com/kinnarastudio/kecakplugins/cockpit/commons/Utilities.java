package com.kinnarastudio.kecakplugins.cockpit.commons;

import com.kinnarastudio.kecakplugins.cockpit.exception.CockpitException;
import org.joget.apps.app.dao.UserviewDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.UserviewDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.userview.model.Userview;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.service.UserviewService;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.util.Optional;

public final class Utilities {
    @Nonnull
    public static Userview getUserview(String userviewId) throws CockpitException {
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        UserviewService userviewService = (UserviewService) applicationContext.getBean("userviewService");
        UserviewDefinitionDao userviewDefinitionDao = (UserviewDefinitionDao) applicationContext.getBean("userviewDefinitionDao");

        return Optional.of(userviewId)
                .map(s -> userviewDefinitionDao.loadById(s, appDefinition))
                .map(UserviewDefinition::getJson)
                .map(s -> AppUtil.processHashVariable(s, null, null, null))
                .map(s -> userviewService.createUserview(s, null, false, AppUtil.getRequestContextPath(), null, null, false))
                .orElseThrow(() -> new CockpitException("Error generating userview [" + userviewId+ "] in application [" + appDefinition.getAppId() + "] version [" + appDefinition.getVersion() + "]"));
    }

    @Nonnull
    public static Optional<UserviewMenu> getUserviewMenu(Userview userview, String customId) {
        return userview.getCategories().stream()
                .flatMap(c -> c.getMenus().stream())
                .filter(m -> !customId.isEmpty() && customId.equalsIgnoreCase(m.getPropertyString("customId")))
                .findFirst();
    }
}
