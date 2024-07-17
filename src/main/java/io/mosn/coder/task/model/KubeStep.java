package io.mosn.coder.task.model;

import com.alibaba.fastjson.annotation.JSONField;

public class KubeStep {
    private String name;

    private Data data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        @JSONField(name = "currentstep")
        private String currentStep;

        private String message;

        @JSONField(name = "totalsteps")
        private String totalSteps;

        private String name;

        public String getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(String currentStep) {
            this.currentStep = currentStep;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTotalSteps() {
            return totalSteps;
        }

        public void setTotalSteps(String totalSteps) {
            this.totalSteps = totalSteps;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

