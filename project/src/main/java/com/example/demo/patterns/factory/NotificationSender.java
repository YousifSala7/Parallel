package com.example.demo.patterns.factory;

/**
 * ============================================================
 * DESIGN PATTERN: Factory Method
 * ============================================================
 * Intent: Define an interface for creating an object, but let
 * subclasses decide which class to instantiate.
 *
 * Real Use Here:
 * Sending notifications to employees (email, SMS, push).
 * The factory decides which sender to create based on the type.
 * ============================================================
 */
public interface NotificationSender {
    void send(String recipient, String message);
    String getType();
}
