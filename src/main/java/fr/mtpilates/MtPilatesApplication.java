package fr.mtpilates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MtPilatesApplication {

    public static void main(final String[] args) {
        SpringApplication.run(MtPilatesApplication.class, args);
    }
}
