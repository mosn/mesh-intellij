package io.mosn.coder.intellij.view;

import io.mosn.coder.plugin.model.PluginBundleRule;

import java.util.Map;

/**
 * @author yiji@apache.org
 */
public class SidecarInjectRuleModel extends MutableCollectionComboBoxModel<PluginBundleRule.PluginRule> {

    static abstract class PluginRuleDelegate extends PluginBundleRule.PluginRule {
        PluginBundleRule.PluginRule parent;

        public PluginRuleDelegate(PluginBundleRule.PluginRule parent) {
            this.parent = parent;
        }

        @Override
        public Long getId() {
            return parent.getId();
        }

        @Override
        public void setId(Long id) {
            parent.setId(id);
        }

        @Override
        public String getInstanceId() {
            return parent.getInstanceId();
        }

        @Override
        public void setInstanceId(String instanceId) {
            parent.setInstanceId(instanceId);
        }

        @Override
        public String getName() {
            return parent.getName();
        }

        @Override
        public void setName(String name) {
            parent.setName(name);
        }

        @Override
        public String getSidecarVersion() {
            return parent.getSidecarVersion();
        }

        @Override
        public void setSidecarVersion(String sidecarVersion) {
            parent.setSidecarVersion(sidecarVersion);
        }

        @Override
        public String getImage() {
            return parent.getImage();
        }

        @Override
        public void setImage(String image) {
            parent.setImage(image);
        }

        @Override
        public Map<String, String> getAttributes() {
            return parent.getAttributes();
        }

        @Override
        public void setAttributes(Map<String, String> attributes) {
            parent.setAttributes(attributes);
        }

        @Override
        public String getCluster() {
            return parent.getCluster();
        }

        @Override
        public void setCluster(String cluster) {
            parent.setCluster(cluster);
        }

        @Override
        public PluginBundleRule.PluginTask getPluginTask() {
            return parent.getPluginTask();
        }

        @Override
        public void setPluginTask(PluginBundleRule.PluginTask pluginTask) {
            parent.setPluginTask(pluginTask);
        }
    }

    public static class InjectRule extends PluginRuleDelegate {

        public InjectRule(PluginBundleRule.PluginRule parent) {
            super(parent);
        }

        @Override
        public String toString() {
            return parent.getName();
        }
    }

    public static class UpgradeImage extends PluginRuleDelegate {

        public UpgradeImage(PluginBundleRule.PluginRule parent) {
            super(parent);
        }

        @Override
        public String toString() {
            if (parent.getSidecarVersion() != null && parent.getSidecarVersion().length() > 0) {
                return parent.getSidecarVersion() + ": " + parent.getImage();
            }
            return parent.getImage();
        }
    }
}