package br.com.certamecards.mail;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final List<Duration> RETRY_DELAYS =
            List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15));

    private final JavaMailSender mailSender;
    private final MailTemplateRenderer renderer;
    private final MailProperties properties;
    private final TaskScheduler taskScheduler;
    private final MeterRegistry meterRegistry;

    public MailService(
            JavaMailSender mailSender,
            MailTemplateRenderer renderer,
            MailProperties properties,
            TaskScheduler taskScheduler,
            MeterRegistry meterRegistry) {
        this.mailSender = mailSender;
        this.renderer = renderer;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.meterRegistry = meterRegistry;
    }

    public void sendConfirmationEmail(String to, String displayName, String link) {
        String html = renderer.render("confirm-email.html", Map.of("displayName", displayName, "link", link));
        sendWithRetry(new Attempt(to, "Confirme seu e-mail", html, "confirm_email", 0));
    }

    public void sendPasswordResetEmail(String to, String displayName, String link) {
        String html = renderer.render("reset-password.html", Map.of("displayName", displayName, "link", link));
        sendWithRetry(new Attempt(to, "Redefina sua senha", html, "reset_password", 0));
    }

    public void sendAccountExistsEmail(String to) {
        String html = renderer.render("account-exists.html", Map.of());
        sendWithRetry(new Attempt(to, "Você já tem uma conta", html, "account_exists", 0));
    }

    private void sendWithRetry(Attempt attempt) {
        try {
            doSend(attempt.to(), attempt.subject(), attempt.html());
            meterRegistry
                    .counter("mail.send", "purpose", attempt.purpose(), "result", "sent")
                    .increment();
        } catch (MailException e) {
            handleFailure(attempt, e);
        }
    }

    private void handleFailure(Attempt attempt, MailException e) {
        if (attempt.number() >= RETRY_DELAYS.size()) {
            log.error("mail.send.failed purpose={}", attempt.purpose(), e);
            meterRegistry
                    .counter("mail.send", "purpose", attempt.purpose(), "result", "failed")
                    .increment();
            return;
        }
        meterRegistry
                .counter("mail.send", "purpose", attempt.purpose(), "result", "retry")
                .increment();
        Instant nextAttempt = Instant.now().plus(RETRY_DELAYS.get(attempt.number()));
        taskScheduler.schedule(() -> sendWithRetry(attempt.next()), nextAttempt);
    }

    private void doSend(String to, String subject, String html) throws MailException {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setSubject(subject);
            helper.setText(html, true);
        } catch (Exception e) {
            throw new org.springframework.mail.MailParseException(e);
        }
        mailSender.send(message);
    }

    private record Attempt(String to, String subject, String html, String purpose, int number) {
        Attempt next() {
            return new Attempt(to, subject, html, purpose, number + 1);
        }
    }
}
