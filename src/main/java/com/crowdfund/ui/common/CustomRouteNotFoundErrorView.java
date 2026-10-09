package com.crowdfund.ui.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Clean, branded 404 Not Found error view without exposing internal paths or framework traces.
 */
@PageTitle("Page Not Found | CrowdFund")
@AnonymousAllowed
public class CustomRouteNotFoundErrorView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    private final Paragraph errorMessage;

    public CustomRouteNotFoundErrorView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 header = new H1("404 - Page Not Found");
        header.getStyle().set("color", "var(--lumo-error-color)");

        errorMessage = new Paragraph("The page or resource you requested could not be found.");
        errorMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button homeBtn = new Button("Go to Home", e -> UI.getCurrent().navigate(""));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(header, errorMessage, homeBtn);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
