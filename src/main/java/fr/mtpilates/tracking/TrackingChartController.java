package fr.mtpilates.tracking;

import static fr.mtpilates.shared.web.FormValues.date;

import fr.mtpilates.auth.CurrentUserService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Export CSV et comparaison de deux dates du graphique d'évolution. */
@Controller
public class TrackingChartController {

    private final TrackingChartService chart;
    private final CurrentUserService currentUsers;
    private final Clock clock;

    public TrackingChartController(final TrackingChartService chart, final CurrentUserService currentUsers,
                                   final Clock clock) {
        this.chart = chart;
        this.currentUsers = currentUsers;
        this.clock = clock;
    }

    @GetMapping("/app/members/{memberId}/tracking.csv")
    public ResponseEntity<byte[]> csv(@PathVariable final UUID memberId) {
        final byte[] body = chart.csv(currentUsers.require(), memberId).getBytes(StandardCharsets.UTF_8);
        final String filename = "mensurations-" + LocalDate.now(clock) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }

    @GetMapping("/app/members/{memberId}/tracking/compare")
    public String compare(@PathVariable final UUID memberId, @RequestParam(defaultValue = "") final String a,
                          @RequestParam(defaultValue = "") final String b, final Model model) {
        model.addAttribute("comparison", chart.compare(currentUsers.require(), memberId, date(a), date(b)));
        return "fragments/tracking/chart :: comparison";
    }
}
