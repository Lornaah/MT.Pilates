package fr.mtpilates.shared.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Toasts reçus en flash (redirection), à afficher dès le chargement de la page. */
@ControllerAdvice
public class InitialToastsAdvice {

    @ModelAttribute("initialToasts")
    public List<Map<String, String>> initialToasts(final Model model) {
        final List<Toast> toasts = new ArrayList<>();
        if (model.getAttribute(ToastResponses.FLASH_ATTRIBUTE) instanceof Toast toast) {
            toasts.add(toast);
        }
        if (model.getAttribute(ToastResponses.FLASH_LIST_ATTRIBUTE) instanceof List<?> list) {
            list.stream().filter(Toast.class::isInstance).map(Toast.class::cast).forEach(toasts::add);
        }
        return toasts.stream()
                .map(t -> Map.of("message", t.message(), "category", t.category().name().toLowerCase(Locale.ROOT)))
                .toList();
    }
}
