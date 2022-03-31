package com.kinnara.kecakplugins.cockpit;

import com.kinnara.kecakplugins.cockpit.commons.Utilities;
import com.kinnara.kecakplugins.cockpit.exception.CockpitException;
import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.userview.lib.DataListMenu;
import org.joget.apps.userview.lib.FormMenu;
import org.joget.apps.userview.lib.InboxMenu;
import org.joget.apps.userview.model.Userview;
import org.joget.apps.userview.model.UserviewCategory;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kecak.apps.exception.ApiException;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CockpitUserviewMenu extends UserviewMenu implements PluginWebSupport {
    public final static String PARAM_USERVIEW_ID = "_userview";
    public final static String PARAM_MENU_ID = "_menu";

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
        try {
            final String userviewId = getParameter(request, PARAM_USERVIEW_ID);
            final String menuId = getParameter(request, PARAM_MENU_ID);

            final Userview userview = Utilities.getUserview(userviewId);
            final CockpitUserviewMenu cockpitMenu = (CockpitUserviewMenu) Utilities.getUserviewMenu(userview, menuId)
                    .filter(m -> m instanceof CockpitUserviewMenu)
                    .orElseThrow(() -> new ApiException(HttpServletResponse.SC_NOT_FOUND, "Menu [" + menuId + "] is not found"));

            final JSONArray jsonResponse = Arrays.stream(cockpitMenu.getPropertyMenus())
                    .map(s -> Utilities.getUserviewMenu(userview, s))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Try.onFunction(m -> {
                        final JSONObject json = new JSONObject();
                        final CockpitItemType type;
                        final JSONObject jsonProperties = new JSONObject();
                        if (m instanceof FormMenu) {
                            type = CockpitItemType.FORM;
                            jsonProperties.put("formId", m.getPropertyString("formId"));
                        } else if (m instanceof DataListMenu || m instanceof InboxMenu ||
                        "com.kinnara.kecakplugins.crudmenu.CrudMenu".equals(m.getClassName())) {
                            type = CockpitItemType.DATALIST;
                            jsonProperties.put("dataListId", m.getPropertyString("datalistId"));
                        } else {
                            type = CockpitItemType.WEBVIEW;
                        }

                        json.put("type", type.name());
                        json.put("className", m.getClassName());
                        json.put("properties", jsonProperties);

                        return json;
                    }))
                    .collect(JSONCollectors.toJSONArray());

            response.getWriter().write(jsonResponse.toString());
        } catch (ApiException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            response.sendError(e.getErrorCode(), e.getMessage());
        } catch (CockpitException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    protected String getRenderPage(String templatePath) throws CockpitException {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");
        final Object[] menus = (Object[]) getProperty("menus");
        final List<Map<String, Object>> renderedMenus = Optional.ofNullable(menus)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, String>) o)
                .map(m -> m.get("menuId"))
                .map(this::getUserviewMenu)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(m -> !(m instanceof CockpitUserviewMenu))
                .map(Try.onFunction(menu -> {
                    final Map<String, Object> value = new HashMap<>();

                    final String renderPage = Optional.of(menu)
                            .map(UserviewUtil::getUserviewMenuHtml)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .orElseGet(menu::getRenderPage);

                    value.put("renderPage", renderPage);
                    value.put("properties", menu.getProperties());

                    return value;
                }))
                .collect(Collectors.toList());

        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("renderedMenus", renderedMenus);
        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClassName(), templatePath, null);
    }

    /**
     * @param menuId
     * @return
     */
    public final Optional<UserviewMenu> getUserviewMenu(String menuId) {
        final Userview userview = getUserview();
        return userview.getCategories()
                .stream()
                .map(UserviewCategory::getMenus)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(menu -> menuId.equals(Optional.ofNullable(menu.getPropertyString("customId")).orElse(menu.getPropertyString("id"))))
                .findFirst();
    }

    protected final Optional<String> optParameter(HttpServletRequest request, String parameterName) {
        return Optional.of(parameterName)
                .map(request::getParameter)
                .filter(s -> !s.isEmpty());
    }

    protected final String getParameter(HttpServletRequest request, String parameterName) throws ApiException {
        return optParameter(request, parameterName)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Parameter [" + parameterName + "] is not supplied"));
    }

    public String[] getPropertyMenus() {
        return Optional.of("menus")
                .map(this::getProperty)
                .map(o -> (Object[]) o)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, String>) o)
                .map(m -> m.get("menuId"))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }


    /**
     * Cockpit Item Type
     */
    public enum CockpitItemType {
        WEBVIEW,
        DATALIST,
        FORM
    }
}
