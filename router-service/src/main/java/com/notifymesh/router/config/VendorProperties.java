package com.notifymesh.router.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifymesh.vendors")
public class VendorProperties {

    private Sms sms = new Sms();
    private SingleVendor email = new SingleVendor();
    private SingleVendor whatsapp = new SingleVendor();

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    public SingleVendor getEmail() {
        return email;
    }

    public void setEmail(SingleVendor email) {
        this.email = email;
    }

    public SingleVendor getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(SingleVendor whatsapp) {
        this.whatsapp = whatsapp;
    }

    public static class Sms {
        private double primaryFailureRate;
        private double fallbackFailureRate;

        public double getPrimaryFailureRate() {
            return primaryFailureRate;
        }

        public void setPrimaryFailureRate(double primaryFailureRate) {
            this.primaryFailureRate = primaryFailureRate;
        }

        public double getFallbackFailureRate() {
            return fallbackFailureRate;
        }

        public void setFallbackFailureRate(double fallbackFailureRate) {
            this.fallbackFailureRate = fallbackFailureRate;
        }
    }

    public static class SingleVendor {
        private double failureRate;

        public double getFailureRate() {
            return failureRate;
        }

        public void setFailureRate(double failureRate) {
            this.failureRate = failureRate;
        }
    }
}
