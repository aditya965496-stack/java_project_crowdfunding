package com.crowdfund.ui.creator;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.Message;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Shows all sent messages grouped by campaign for the creator.
 */
@Route(value = "creator/messages", layout = MainLayout.class)
@PageTitle("Messages | CrowdFund")
@RolesAllowed("CREATOR")
public class CreatorMessagesView extends VerticalLayout {

    public CreatorMessagesView(CampaignService campaignService,
                               AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);

        User creator = authenticatedUser.get().orElseThrow();
        H2 heading = new H2("Sent Messages");

        List<Campaign> campaigns = campaignService.findByCreator(creator);

        if (campaigns.isEmpty()) {
            add(heading, new Span("You have no campaigns yet."));
            return;
        }

        add(heading);

        for (Campaign campaign : campaigns) {
            List<Message> messages = campaignService.getMessages(campaign.getId());
            if (messages.isEmpty()) continue;

            H3 campaignTitle = new H3(campaign.getTitle());
            add(campaignTitle);

            for (Message msg : messages) {
                Div card = new Div();
                card.getStyle()
                        .set("background", "var(--lumo-contrast-5pct)")
                        .set("border-radius", "var(--lumo-border-radius-m)")
                        .set("padding", "var(--lumo-space-m)")
                        .set("margin-bottom", "var(--lumo-space-xs)");

                Span content = new Span(msg.getContent());
                Span time = new Span(" — " + msg.getSentAt()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
                time.getStyle()
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("font-size", "var(--lumo-font-size-xs)");

                card.add(content, time);
                add(card);
            }
        }
    }
}
