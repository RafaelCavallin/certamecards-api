package br.com.certamecards.mail;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MailEventListener {

    private final MailService mailService;

    public MailEventListener(MailService mailService) {
        this.mailService = mailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmationEmail(SendConfirmationEmailEvent event) {
        mailService.sendConfirmationEmail(event.to(), event.displayName(), event.link());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetEmail(SendPasswordResetEmailEvent event) {
        mailService.sendPasswordResetEmail(event.to(), event.displayName(), event.link());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountExistsEmail(SendAccountExistsEmailEvent event) {
        mailService.sendAccountExistsEmail(event.to());
    }
}
