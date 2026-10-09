package com.crowdfund.config;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Initializes global Vaadin service hooks, including centralized error handling
 * that masks internal exceptions and prevents stack traces from leaking to the browser.
 */
@Component
public class VaadinAppServiceInitListener implements VaadinServiceInitListener {

    private static final Logger log = LoggerFactory.getLogger(VaadinAppServiceInitListener.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(sessionInitEvent -> {
            sessionInitEvent.getSession().setErrorHandler(errorEvent -> {
                Throwable t = errorEvent.getThrowable();
                Throwable root = t;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                if (root instanceof com.vaadin.flow.router.NotFoundException) {
                    return;
                }

                String errorRef = "ERR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                log.error("Unhandled Vaadin UI Exception [Ref: {}]: {}", errorRef, t.getMessage(), t);

                UI ui = UI.getCurrent();
                if (ui != null) {
                    ui.access(() -> {
                        Notification notification = Notification.show(
                                "An unexpected error occurred. Reference: " + errorRef,
                                5000,
                                Notification.Position.BOTTOM_CENTER
                        );
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
}
