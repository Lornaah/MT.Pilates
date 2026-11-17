package fr.mtpilates.profile;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OnboardingGateConfig implements WebMvcConfigurer {

    private final OnboardingGate gate;

    public OnboardingGateConfig(final OnboardingGate gate) {
        this.gate = gate;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        // « Voir comme » reste accessible : l'owner doit pouvoir revenir à son propre rôle.
        registry.addInterceptor(gate).addPathPatterns("/app", "/app/**").excludePathPatterns("/app/view-as");
    }
}
