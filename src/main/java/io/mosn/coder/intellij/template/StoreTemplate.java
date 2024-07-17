package io.mosn.coder.intellij.template;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class StoreTemplate {

    private static List<Template> staticTemplates = new ArrayList<>();

    /**
     * registered template should be executed when project create first time.
     * <p>
     * important:
     * <p>
     * Template#create(PluginOption option) method argument `option` maybe is null.
     */
    public static void register(Template template) {
        if (template != null) {
            staticTemplates.add(template);
        }
    }

    /**
     * <p>
     * important:
     * <p>
     * Template#create(PluginOption option) method argument `option` maybe is null.
     */
    public static void forEach(TemplateAction action) {
        for (Template template : staticTemplates) {
            if (action != null) {
                action.execute(template);
            }
        }
    }
}
