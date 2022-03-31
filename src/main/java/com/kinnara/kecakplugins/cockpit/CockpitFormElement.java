package com.kinnara.kecakplugins.cockpit;

import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;

import java.util.Map;

/**
 * Cockpit Form Element
 *
 * TODO: Implementation
 */
public class CockpitFormElement extends Element {
    @Override
    public String renderTemplate(FormData formData, Map map) {
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
        return getClass().getPackage().getImplementationTitle();
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
        return null;
    }
}
