package fr.mtpilates.layout;

import java.util.List;
import java.util.Optional;

public record NavGroup(Optional<String> title, List<NavItem> items) {

    static NavGroup untitled(final List<NavItem> items) {
        return new NavGroup(Optional.empty(), items);
    }

    static NavGroup titled(final String title, final List<NavItem> items) {
        return new NavGroup(Optional.of(title), items);
    }
}
