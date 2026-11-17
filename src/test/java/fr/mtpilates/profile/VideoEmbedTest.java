package fr.mtpilates.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VideoEmbedTest {

    @Test
    void convertsYoutubeWatchAndShortLinks() {
        assertThat(VideoEmbed.of("https://www.youtube.com/watch?v=abc-123")).contains("https://www.youtube.com/embed/abc-123");
        assertThat(VideoEmbed.of("https://youtu.be/xyz_9")).contains("https://www.youtube.com/embed/xyz_9");
    }

    @Test
    void convertsVimeoLinks() {
        assertThat(VideoEmbed.of("https://vimeo.com/123456")).contains("https://player.vimeo.com/video/123456");
    }

    @Test
    void keepsOtherUrlsAndIgnoresEmptyValues() {
        assertThat(VideoEmbed.of("https://exemple.fr/video.mp4")).contains("https://exemple.fr/video.mp4");
        assertThat(VideoEmbed.of("")).isEmpty();
        assertThat(VideoEmbed.of(null)).isEmpty();
    }
}
