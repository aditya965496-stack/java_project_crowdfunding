package com.crowdfund.ui.contributor;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.CampaignUpdate;
import com.crowdfund.entity.Message;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.ContributionService;
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
 * Feed of progress updates and messages from campaigns the contributor has backed.
 */
@Route(value = "contributor/updates", layout = MainLayout.class)
@PageTitle("Campaign Updates | CrowdFund")
@RolesAllowed("CONTRIBUTOR")
public class CampaignUpdatesView extends VerticalLayout {

    public CampaignUpdatesView(CampaignService campaignService,
                               ContributionService contributionService,
                               AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);

        User contributor = authenticatedUser.get().orElseThrow();
        H2 heading = new H2("Updates from Your Campaigns");

        List<Campaign> supported = contributionService.campaignsSupportedBy(contributor);

        if (supported.isEmpty()) {
            add(heading, new Span("No updates yet. Support a campaign to receive updates!"));
            return;
        }

        add(heading);

        // Progress updates
        List<CampaignUpdate> updates = campaignService.getUpdatesForCampaigns(supported);
        if (!updates.isEmpty()) {
            H3 updatesHeader = new H3("Progress Updates");
            add(updatesHeader);
            for (CampaignUpdate update : updates) {
                Div card = createUpdateCard(
                        update.getCampaign().getTitle(),
                        update.getContent(),
                        update.getPostedBy().getName(),
                        update.getPostedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                        "var(--lumo-primary-color-10pct)"
                );
                add(card);
            }
        }

        // Messages
        List<Message> messages = campaignService.getMessagesForCampaigns(supported);
        if (!messages.isEmpty()) {
            H3 msgHeader = new H3("Messages from Creators");
            add(msgHeader);
            for (Message msg : messages) {
                Div card = createUpdateCard(
                        msg.getCampaign().getTitle(),
                        msg.getContent(),
                        msg.getSender().getName(),
                        msg.getSentAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                        "var(--lumo-success-color-10pct)"
                );
                add(card);
            }
        }

        if (updates.isEmpty() && messages.isEmpty()) {
            add(new Span("No updates available yet for your supported campaigns."));
        }
    }

    private Div createUpdateCard(String campaignTitle, String content,
                                  String author, String time, String bgColor) {
        Div card = new Div();
        card.getStyle()
                .set("background", bgColor)
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-s)");

        Span titleSpan = new Span(campaignTitle);
        titleSpan.getStyle()
                .set("font-weight", "bold")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");

        Span contentSpan = new Span(content);
        contentSpan.getStyle().set("display", "block");

        Span metaSpan = new Span("by " + author + " • " + time);
        metaSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("display", "block")
                .set("margin-top", "var(--lumo-space-xs)");

        card.add(titleSpan, contentSpan, metaSpan);
        return card;
    }
}
