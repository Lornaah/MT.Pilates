package fr.mtpilates.layout;

import java.util.List;
import java.util.Optional;

/** Barre latérale prête à afficher : libellés résolus et entrée active calculée. */
public record SidebarView(List<Zone> zones) {

    public record Zone(String title, List<Group> groups) {
    }

    public record Group(Optional<String> title, List<Link> links) {
    }

    public record Link(String path, String icon, String label, boolean active) {
    }
}
