package fr.mtpilates.auth;

import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Page /auth (connexion, création de compte) et parcours de confirmation par email. */
@Controller
public class AuthController {

    static final String REGISTERED = "Inscription enregistrée. Vérifiez votre email pour confirmer votre adresse.";
    static final String RESENT = "Email de confirmation renvoyé.";
    static final String INVALID_LINK = "Lien de confirmation invalide ou expiré.";
    static final String MAIL_FAILED = "Envoi de l'email impossible, réessayez dans quelques minutes.";

    /** Ordre des champs du formulaire zod d'origine, pour concaténer les erreurs dans le même ordre. */
    private static final List<String> FIELD_ORDER = List.of("email", "password");

    private final RegistrationService registrations;
    private final AccountUserDetailsService userDetails;
    private final SecurityContextRepository securityContextRepository;
    private final ToastResponses toasts;

    public AuthController(final RegistrationService registrations, final AccountUserDetailsService userDetails,
                          final SecurityContextRepository securityContextRepository, final ToastResponses toasts) {
        this.registrations = registrations;
        this.userDetails = userDetails;
        this.securityContextRepository = securityContextRepository;
        this.toasts = toasts;
    }

    /** zod nettoyait l'email (trim) avant de le valider : on fait de même à la liaison du formulaire. */
    @InitBinder("signupForm")
    public void trimEmail(final WebDataBinder binder) {
        binder.registerCustomEditor(String.class, "email", new StringTrimmerEditor(false));
    }

    @GetMapping("/auth")
    public String page(final Authentication authentication) {
        return isSignedIn(authentication) ? "redirect:/app" : "pages/auth";
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<Void> signup(@Valid @ModelAttribute final SignupForm submitted, final BindingResult errors,
                                       final HttpServletRequest request, final HttpServletResponse response) {
        final SignupForm form = submitted.normalized();
        if (errors.hasErrors()) {
            return toast(response, Toast.error(joinedMessages(errors)), Map.of());
        }
        try {
            registrations.register(form.email(), form.password(), confirmationLink(request));
            return toast(response, Toast.success(REGISTERED), Map.of("registered", true));
        } catch (final EmailAlreadyRegisteredException e) {
            return toast(response, Toast.error(e.getMessage()), Map.of());
        } catch (final MailException e) {
            return toast(response, Toast.error(MAIL_FAILED), Map.of());
        }
    }

    @PostMapping("/auth/resend")
    public ResponseEntity<Void> resend(@RequestParam final String email, final HttpServletRequest request,
                                       final HttpServletResponse response) {
        try {
            registrations.resend(AccountUserDetailsService.normalize(email), confirmationLink(request));
            return toast(response, Toast.success(RESENT), Map.of());
        } catch (final MailException e) {
            return toast(response, Toast.error(MAIL_FAILED), Map.of());
        }
    }

    @GetMapping("/auth/confirm")
    public String confirm(@RequestParam final String token, final HttpServletRequest request,
                          final HttpServletResponse response, final RedirectAttributes redirect) {
        return registrations.confirm(token)
                .map(userId -> {
                    signIn(userId, request, response);
                    redirect.addFlashAttribute(ToastResponses.FLASH_ATTRIBUTE, Toast.success(LoginSuccessHandler.WELCOME));
                    return "redirect:/app";
                })
                .orElseGet(() -> {
                    redirect.addFlashAttribute(ToastResponses.FLASH_ATTRIBUTE, Toast.error(INVALID_LINK));
                    return "redirect:/auth";
                });
    }

    /** Comme Supabase après confirmation : l'utilisateur arrive connecté dans son espace. */
    private void signIn(final UUID userId, final HttpServletRequest request, final HttpServletResponse response) {
        final AuthenticatedUser user = (AuthenticatedUser) userDetails.loadUserById(userId);
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private ResponseEntity<Void> toast(final HttpServletResponse response, final Toast toast,
                                       final Map<String, Object> otherEvents) {
        toasts.trigger(response, toast, otherEvents);
        // Le formulaire reste tel quel : seul le toast (et l'éventuel changement d'onglet) change l'écran.
        return ResponseEntity.noContent().header("HX-Reswap", "none").build();
    }

    private static String joinedMessages(final BindingResult errors) {
        return errors.getFieldErrors().stream()
                .sorted(Comparator.comparingInt((FieldError e) -> FIELD_ORDER.indexOf(e.getField()))
                        .thenComparingInt(AuthController::ruleOrder))
                .map(FieldError::getDefaultMessage)
                .distinct()
                .reduce((a, b) -> a + " • " + b)
                .orElse("");
    }

    private static int ruleOrder(final FieldError error) {
        final List<String> order = List.of(EmailRules.REQUIRED, EmailRules.INVALID, EmailRules.TOO_LONG,
                PasswordPolicy.TOO_SHORT, PasswordPolicy.TOO_LONG, PasswordPolicy.NO_LOWER, PasswordPolicy.NO_UPPER,
                PasswordPolicy.NO_DIGIT, PasswordPolicy.NO_SPECIAL);
        return order.indexOf(error.getDefaultMessage());
    }

    private static Function<String, String> confirmationLink(final HttpServletRequest request) {
        final String base = ServletUriComponentsBuilder.fromContextPath(request).path("/auth/confirm").toUriString();
        return token -> base + "?token=" + token;
    }

    private static boolean isSignedIn(final Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser;
    }
}
