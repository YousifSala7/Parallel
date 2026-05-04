package com.example.demo.patterns.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// ============================================================
// Concrete Implementation 1: Email
// ============================================================
@Component
class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public void send(String recipient, String message) {
        log.info("[EMAIL] To: {} | Message: {}", recipient, message);
        // Real implementation: connect to SMTP, JavaMailSender, etc.
    }

    @Override
    public String getType() { return "email"; }
}

// ============================================================
// Concrete Implementation 2: SMS
// ============================================================
@Component
class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Override
    public void send(String recipient, String message) {
        log.info("[SMS] To: {} | Message: {}", recipient, message);
        // Real implementation: Twilio, Vonage, etc.
    }

    @Override
    public String getType() { return "sms"; }
}

// ============================================================
// Concrete Implementation 3: In-App Push
// ============================================================
@Component
class PushNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationSender.class);

    @Override
    public void send(String recipient, String message) {
        log.info("[PUSH] To: {} | Message: {}", recipient, message);
        // Real implementation: Firebase FCM, etc.
    }

    @Override
    public String getType() { return "push"; }
}
