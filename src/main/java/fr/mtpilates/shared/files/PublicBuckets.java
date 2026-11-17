package fr.mtpilates.shared.files;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicBuckets {

    @Bean
    public FileAccessRule avatarsArePublic() {
        return new PublicBucketRule("avatars");
    }

    @Bean
    public FileAccessRule announcementImagesArePublic() {
        return new PublicBucketRule("announcement-images");
    }
}
