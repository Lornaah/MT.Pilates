package fr.mtpilates.member;

import fr.mtpilates.shared.files.FileAccessRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberBuckets {

    public static final String PHOTOS = "member-photos";
    public static final String DOCUMENTS = "member-documents";

    @Bean
    public FileAccessRule memberPhotosRule(final MemberGuard guard) {
        return new MemberFilesRule(PHOTOS, guard);
    }

    @Bean
    public FileAccessRule memberDocumentsRule(final MemberGuard guard) {
        return new MemberFilesRule(DOCUMENTS, guard);
    }
}
