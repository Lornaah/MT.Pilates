package fr.mtpilates.shared.security;

import fr.mtpilates.auth.AccountUserDetailsService;
import fr.mtpilates.auth.Capacities;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.auth.LoginFailureHandler;
import fr.mtpilates.auth.LoginSuccessHandler;
import java.time.Duration;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Duration REMEMBER_ME_VALIDITY = Duration.ofDays(30);

    private final CurrentUserService currentUsers;

    public SecurityConfig(final CurrentUserService currentUsers) {
        this.currentUsers = currentUsers;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http,
                                                   final LoginSuccessHandler loginSuccess,
                                                   final LoginFailureHandler loginFailure,
                                                   final AccountUserDetailsService userDetails,
                                                   final PersistentTokenRepository rememberMeTokens,
                                                   final SecurityContextRepository securityContextRepository,
                                                   @Value("${APP_REMEMBER_ME_KEY:mt-pilates-remember-me}") final String rememberMeKey)
            throws Exception {
        // Seules /app et /onboarding exigent une session : une URL inconnue doit aboutir à la 404, comme l'original.
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AppPaths.OWNER).access(capacity(Capacities::isOwner))
                        .requestMatchers(AppPaths.STAFF).access(capacity(Capacities::isStaff))
                        .requestMatchers(AppPaths.AUTHENTICATED).authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/auth")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(loginSuccess)
                        .failureHandler(loginFailure))
                // L'original gardait la session dans le navigateur : on reste connecté d'une visite à l'autre.
                .rememberMe(remember -> remember
                        .alwaysRemember(true)
                        .key(rememberMeKey)
                        .tokenRepository(rememberMeTokens)
                        .userDetailsService(userDetails)
                        .tokenValiditySeconds((int) REMEMBER_ME_VALIDITY.toSeconds()))
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/"))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HtmxAwareEntryPoint("/auth"))
                        .accessDeniedHandler(new RedirectToAppHomeHandler()))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PersistentTokenRepository rememberMeTokens(final DataSource dataSource) {
        final JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Garde de route sur une capacité de l'utilisateur, évaluée sur ses rôles effectifs (« Voir comme » inclus). */
    private AuthorizationManager<RequestAuthorizationContext> capacity(final Predicate<Capacities> rule) {
        return (authentication, context) -> new AuthorizationDecision(
                currentUsers.current().map(user -> rule.test(user.capacities())).orElse(false));
    }
}
