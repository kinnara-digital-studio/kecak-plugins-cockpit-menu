package com.kinnara.kecakplugins.cockpit;

import com.kinnara.kecakplugins.cockpit.exception.CockpitException;
import com.kinnarastudio.commons.Try;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.lib.RunProcess;
import org.joget.apps.userview.model.Userview;
import org.joget.apps.userview.model.UserviewCategory;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CockpitUserviewMenu extends UserviewMenu implements PluginWebSupport {
    @Override
    public String getCategory() {
        return "Kecak";
    }

    @Override
    public String getIcon() {
        return "/plugin/org.joget.apps.userview.lib.RunProcess/images/grid_icon.gif";
    }

    @Override
    public String getRenderPage() {
        try {
            return getRenderPage("/templates/CockpitUserviewMenu.ftl");
        } catch (CockpitException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return e.getMessage();
        }
    }

    @Override
    public boolean isHomePageSupported() {
        return true;
    }

    @Override
    public String getDecoratedMenu() {
        return null;
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return "Cockpit";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/CockpitUserviewMenu.json");
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected String getRenderPage(String templatePath) throws CockpitException {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");

        final Object[] menus = (Object[]) getProperty("menus");
        final Map<String, Map<String, Object>> renderedMenus = Optional.ofNullable(menus)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, String>)o)
                .map(m -> m.get("menuId"))
                .map(Try.onFunction(this::getUserviewMenu))
                .filter(m -> !(m instanceof CockpitUserviewMenu))
                .collect(Collectors.toMap(Try.onFunction(menu -> menu.getProperty("id").toString()), Try.onFunction(menu -> {
                    final Map<String, Object> value = new HashMap<>();

                    final String renderPage = Optional.of(menu)
                            .map(UserviewUtil::getUserviewMenuHtml)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .orElseGet(menu::getRenderPage);

                    value.put("renderPage", renderPage);
                    value.put("properties", menu.getProperties());

                    return value;
                })));

        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("renderedMenus", renderedMenus);
        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClassName(), templatePath, null);
    }

    /**
     *
     * @param menuId
     * @return
     * @throws CockpitException
     */
    public final UserviewMenu getUserviewMenu(String menuId) throws CockpitException {
        final Userview userview = getUserview();
        return userview.getCategories()
                .stream()
                .map(UserviewCategory::getMenus)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(menu -> menuId.equals(Optional.ofNullable(menu.getPropertyString("customId")).orElse(menu.getPropertyString("id"))))
                .findFirst()
                .orElseThrow(() -> new CockpitException("Error generating userview menu [" + menuId + "]"));
    }
}
