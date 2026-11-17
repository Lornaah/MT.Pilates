package fr.mtpilates.video;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class YouTubeIdsTest {

    @Test
    void acceptsBareIdsAndEveryUrlShape() {
        assertThat(YouTubeIds.extract(" abcdefghijk ")).contains("abcdefghijk");
        assertThat(YouTubeIds.extract("https://www.youtube.com/watch?v=abc-EFG_123&t=4")).contains("abc-EFG_123");
        assertThat(YouTubeIds.extract("https://youtu.be/abc-EFG_123")).contains("abc-EFG_123");
        assertThat(YouTubeIds.extract("https://www.youtube.com/embed/abc-EFG_123")).contains("abc-EFG_123");
    }

    @Test
    void rejectsAnythingElse() {
        assertThat(YouTubeIds.extract("pas une url")).isEmpty();
        assertThat(YouTubeIds.extract("https://vimeo.com/123")).isEmpty();
        assertThat(YouTubeIds.extract("")).isEmpty();
    }
}
