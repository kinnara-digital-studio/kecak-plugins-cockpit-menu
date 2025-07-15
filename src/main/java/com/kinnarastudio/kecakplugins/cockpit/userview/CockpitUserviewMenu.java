package com.kinnarastudio.kecakplugins.cockpit.userview;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.kecakplugins.cockpit.commons.Utilities;
import com.kinnarastudio.kecakplugins.cockpit.exception.CockpitException;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.userview.lib.DataListMenu;
import org.joget.apps.userview.lib.FormMenu;
import org.joget.apps.userview.lib.InboxMenu;
import org.joget.apps.userview.lib.RunProcess;
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
    public final static String LABEL = "Cockpit";

    @Override
    public String getCategory() {
        return "Kecak";
    }

    @Override
    public String getIcon() {
        return "/plugin/" + RunProcess.class.getName() + "/images/grid_icon.gif";
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
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return LABEL;
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
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

        try {
            final String userviewId = getParameter(request, PARAM_USERVIEW_ID);
            final String menuId = getParameter(request, PARAM_MENU_ID);

            final Userview userview = Utilities.getUserview(userviewId);
            final CockpitUserviewMenu cockpitMenu = (CockpitUserviewMenu) Utilities.getUserviewMenu(userview, menuId)
                    .filter(m -> m instanceof CockpitUserviewMenu)
                    .orElseThrow(() -> new ApiException(HttpServletResponse.SC_NOT_FOUND, "Menu [" + menuId + "] is not found"));

            final JSONArray jsonResponse = Optional.of(cockpitMenu)
                    .map(CockpitUserviewMenu::getPropertyMenus)
                    .stream()
                    .flatMap(Arrays::stream)
                    .map(s -> Utilities.getUserviewMenu(userview, s))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Try.onFunction(menu -> {
                        final JSONObject json = new JSONObject();

                        final JSONObject jsonProperties = new JSONObject();

                        final CockpitItemType type;
                        if (isForm(menu)) {
                            type = CockpitItemType.FORM;
                            jsonProperties.put("formId", menu.getPropertyString("formId"));
                        } else if (isDataList(menu)) {
                            type = CockpitItemType.DATALIST;
                            jsonProperties.put("dataListId", menu.getPropertyString("datalistId"));
                        } else if (isNative(menu)) {
                            type = CockpitItemType.NATIVE;
                        } else {
                            type = CockpitItemType.WEBVIEW;
                        }

                        request.setAttribute("embed", true);

                        json.put("type", type.name());
                        json.put("className", menu.getClassName());
                        json.put("properties", jsonProperties);
                        json.put("url", request.getContextPath() + "/embed/mobile/" + appDefinition.getAppId() + "/" + userviewId + "/_/" + menuId);
                        json.put("content", getInternalRenderPage(menu));

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
                .map(map -> Optional.of(map)
                        .map(m -> m.getOrDefault("menuId", ""))
                        .filter(s -> !s.isEmpty())
                        .map(this::getUserviewMenu)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(m -> !(m instanceof CockpitUserviewMenu))
                        .map(Try.onFunction(menu -> {
                            final Map<String, Object> value = new HashMap<>();
                            value.put("renderPage", getInternalRenderPage(menu));
                            value.put("properties", menu.getProperties());
                            value.put("columnSize", map.getOrDefault("columnSize", "full"));

                            return value;
                        })))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("renderedMenus", renderedMenus);
        dataModel.put("element", this);
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

    protected boolean isForm(UserviewMenu menu) {
        return menu instanceof FormMenu;
    }

    protected boolean isDataList(UserviewMenu menu) {
        return menu instanceof DataListMenu
                || menu instanceof InboxMenu
                || menu.getClassName().equals("com.kinnara.kecakplugins.datalistinboxmenu.DataListInboxMenu")
                || menu.getClassName().equals("com.kinnara.kecakplugins.crudmenu.CrudMenu");
    }

    protected boolean isNative(UserviewMenu menu) {
        final String menuClassName = menu.getClassName();
        return menu instanceof FormMenu
                || menu instanceof DataListMenu
                || menu instanceof InboxMenu
                || menuClassName.equals("com.kinnara.kecakplugins.datalistinboxmenu.DataListInboxMenu")
                || menuClassName.equals("com.kinnarastudio.kecakplugins.datalistinboxmenu.DataListInboxMenu")
                || menuClassName.equals("com.kinnara.kecakplugins.crudmenu.CrudMenu")
                || menuClassName.equals("com.kinnarastudio.kecakplugins.crudmenu.CrudMenu")
                || menuClassName.equals("com.kinnara.kecakplugins.DashboardWidget")
                || menuClassName.equals("com.kinnarastudio.kecakplugins.DashboardWidget");
    }

    protected String getInternalRenderPage(UserviewMenu menu) {
        return Optional.of(menu)
                .map(UserviewUtil::getUserviewMenuHtml)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseGet(menu::getRenderPage);
    }

    public String[] getPropertyMenus() {
        return Optional.of("menus")
                .map(this::getProperty)
                .map(o -> (Object[]) o)
                .stream()
                .flatMap(Arrays::stream)
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
        FORM,
        NATIVE
    }
}
