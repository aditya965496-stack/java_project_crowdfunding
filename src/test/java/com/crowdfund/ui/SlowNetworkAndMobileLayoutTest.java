package com.crowdfund.ui;

import com.crowdfund.CrowdFundApplication;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Viewport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class SlowNetworkAndMobileLayoutTest {

    @Test
    @DisplayName("should configure responsive viewport for mobile layouts on AppShell")
    void shouldConfigureResponsiveViewport() {
        assertTrue(AppShellConfigurator.class.isAssignableFrom(CrowdFundApplication.class));
        Viewport viewport = CrowdFundApplication.class.getAnnotation(Viewport.class);
        assertNotNull(viewport);
        assertEquals("width=device-width, initial-scale=1", viewport.value());
    }

    @Test
    @DisplayName("should configure heartbeat and reconnect dialog for slow network resilience")
    void shouldConfigureSlowNetworkProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(is, "application.properties not found");
            props.load(is);
        }

        // Test slow network / reconnect settings
        assertEquals("30", props.getProperty("vaadin.heartbeatInterval"));
        assertEquals("true", props.getProperty("vaadin.closeIdleSessions"));
        assertNotNull(props.getProperty("vaadin.reconnectDialog.dialogText"));
        assertNotNull(props.getProperty("vaadin.reconnectDialog.dialogTextGaveUp"));

        // Test sensitive error hiding
        assertEquals("never", props.getProperty("server.error.include-stacktrace"));
        assertEquals("never", props.getProperty("server.error.include-message"));
        assertEquals("false", props.getProperty("server.error.include-exception"));

        // Test secrets hidden / env overridable
        assertTrue(props.getProperty("spring.datasource.password").contains("${DB_PASSWORD:"));
        assertTrue(props.getProperty("crowdfund.seed.demo-data").contains("false"));
    }
}
