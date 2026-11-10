package fr.mtpilates.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HtmxRequestTest {

    @Test
    void returnsTheFragmentForHtmxRequests() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");

        assertThat(HtmxRequest.view(request, "page", "page :: fragment")).isEqualTo("page :: fragment");
    }

    @Test
    void returnsTheFullPageForRegularAndBoostedRequests() {
        final MockHttpServletRequest regular = new MockHttpServletRequest();
        final MockHttpServletRequest boosted = new MockHttpServletRequest();
        boosted.addHeader("HX-Request", "true");
        boosted.addHeader("HX-Boosted", "true");

        assertThat(HtmxRequest.view(regular, "page", "page :: fragment")).isEqualTo("page");
        assertThat(HtmxRequest.view(boosted, "page", "page :: fragment")).isEqualTo("page");
    }
}
