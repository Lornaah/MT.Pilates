package fr.mtpilates.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/**
 * Attributs flash posés hors d'un controller (handlers Spring Security). Ces handlers
 * s'exécutent avant le DispatcherServlet, qui n'a donc pas encore préparé de FlashMap :
 * on passe directement par le gestionnaire en session, celui que Spring MVC relit.
 */
public final class Flash {

    private static final FlashMapManager MANAGER = new SessionFlashMapManager();

    private Flash() {
    }

    public static void put(final HttpServletRequest request, final HttpServletResponse response,
                           final String targetPath, final Map<String, ?> attributes) {
        final FlashMap flash = new FlashMap();
        flash.putAll(attributes);
        flash.setTargetRequestPath(request.getContextPath() + targetPath);
        MANAGER.saveOutputFlashMap(flash, request, response);
    }
}
