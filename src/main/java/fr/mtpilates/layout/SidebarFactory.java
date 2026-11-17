package fr.mtpilates.layout;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.config.Labels;
import org.springframework.stereotype.Component;

/** Construit la barre latérale prête à afficher (libellés résolus, entrée active). */
@Component
public class SidebarFactory {

    public SidebarView build(final CurrentUser user, final Labels labels, final String requestPath) {
        return new SidebarView(NavigationBuilder.build(user.capacities()).stream()
                .map(zone -> new SidebarView.Zone(zone.title(), zone.groups().stream()
                        .map(group -> new SidebarView.Group(group.title(), group.items().stream()
                                .map(item -> new SidebarView.Link(item.path(), item.icon(),
                                        labels.label(item.labelKey(), item.labelFallback()), item.isActive(requestPath)))
                                .toList()))
                        .toList()))
                .toList());
    }
}
