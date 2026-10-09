package com.crowdfund;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the CrowdFund Platform.
 * Configured as AppShellConfigurator with responsive viewport and network push support.
 */
@SpringBootApplication
@EnableScheduling
@Viewport("width=device-width, initial-scale=1")
public class CrowdFundApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        System.setProperty("vaadin.copilot.enable", "false");
        System.setProperty("vaadin.copilot.enabled", "false");
        System.setProperty("vaadin.devTools.enabled", "false");
        System.setProperty("vaadin.devmode.copilot.enabled", "false");
        SpringApplication.run(CrowdFundApplication.class, args);
    }
}
