package com.ctbe.eventflow.service;

import com.ctbe.eventflow.model.Event;
import com.ctbe.eventflow.model.OrganizerRequest;
import com.ctbe.eventflow.model.Registration;
import com.ctbe.eventflow.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Sends transactional emails asynchronously so the HTTP response
 * is never delayed by SMTP latency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' HH:mm");

    // ── Booking confirmation ──────────────────────────────────

    @Async
    public void sendBookingConfirmation(Registration reg) {
        Event event = reg.getEvent();
        User user  = reg.getUser();
        int count  = reg.getAttendeeCount();

        String subject = "Booking Confirmed – " + event.getTitle();
        String body = String.format("""
                Hi %s,

                Your booking is confirmed! Here are the details:

                  Event   : %s
                  Date    : %s
                  Location: %s
                  Seats   : %d

                See your ticket and manage your bookings at:
                  %s/my-registrations

                Enjoy the event!
                — EventFlow
                """,
                user.getName(),
                event.getTitle(),
                event.getDateTime().format(FMT),
                event.getLocation(),
                count,
                baseUrl);

        send(user.getEmail(), subject, body);
    }

    // ── Cancellation confirmation ─────────────────────────────

    @Async
    public void sendCancellationConfirmation(User user, Event event, int attendeeCount) {
        String subject = "Booking Cancelled – " + event.getTitle();
        String body = String.format("""
                Hi %s,

                Your booking has been cancelled:

                  Event   : %s
                  Date    : %s
                  Location: %s
                  Seats released: %d

                If this was a mistake, you can re-book while slots are still available:
                  %s/events/%d

                — EventFlow
                """,
                user.getName(),
                event.getTitle(),
                event.getDateTime().format(FMT),
                event.getLocation(),
                attendeeCount,
                baseUrl,
                event.getId());

        send(user.getEmail(), subject, body);
    }

    // ── Event rescheduled notification ────────────────────────

    @Async
    public void sendRescheduledNotification(User user, Event event, String oldDateTime) {
        String subject = "📅 Date Changed – " + event.getTitle();
        String body = String.format("""
                Hi %s,

                The date of an event you have booked has changed:

                  Event       : %s
                  Old date    : %s
                  New date    : %s
                  Location    : %s

                Please update your calendar. If the new date doesn't work for you,
                you can cancel your booking at:
                  %s/my-registrations

                — EventFlow
                """,
                user.getName(),
                event.getTitle(),
                oldDateTime,
                event.getDateTime().format(FMT),
                event.getLocation(),
                baseUrl);

        send(user.getEmail(), subject, body);
    }

    // ── Slot available notification (waitlist) ────────────────

    @Async
    public void sendSlotAvailableNotification(User user, Event event) {
        String subject = "🎟️ A slot just opened – " + event.getTitle();
        String body = String.format("""
                Hi %s,

                Great news! A slot has opened up in an event you were waiting for:

                  Event   : %s
                  Date    : %s
                  Location: %s

                Book now before it fills up again:
                  %s/events/%d

                This notification was sent only to you. Act fast!

                — EventFlow
                """,
                user.getName(),
                event.getTitle(),
                event.getDateTime().format(FMT),
                event.getLocation(),
                baseUrl,
                event.getId());

        send(user.getEmail(), subject, body);
    }

    // ── Waitlist confirmation ─────────────────────────────────

    @Async
    public void sendWaitlistConfirmation(User user, Event event) {
        String subject = "You're on the waitlist – " + event.getTitle();
        String body = String.format("""
                Hi %s,

                You're on the waitlist for:

                  Event   : %s
                  Date    : %s
                  Location: %s

                We'll email you the moment a slot opens up. You can remove yourself
                from the waitlist at any time at:
                  %s/my-registrations

                — EventFlow
                """,
                user.getName(),
                event.getTitle(),
                event.getDateTime().format(FMT),
                event.getLocation(),
                baseUrl);

        send(user.getEmail(), subject, body);
    }

    @Async
    public void sendOrganizerRequestNotificationToStaff(
            String staffEmail, String staffName,
            OrganizerRequest req) {

        String subject = "📋 New Organizer Request – " + req.getName();
        String body = String.format("""
                Hi %s,

                A user has submitted a request to become an organizer:

                  Name   : %s
                  Email  : %s
                  Phone  : %s
                  Account: %s

                Their message:
                ─────────────
                %s
                ─────────────

                Review and act on this request at:
                  %s/admin/organizer-requests/%d

                — EventFlow
                """,
                staffName,
                req.getName(),
                req.getEmail(),
                req.getPhone(),
                req.getUser().getEmail(),
                req.getMessage(),
                baseUrl,
                req.getId());

        send(staffEmail, subject, body);
    }

    // ── Organizer request approved ────────────────────────────

    @Async
    public void sendRequestApproved(User user, OrganizerRequest req) {
        String subject = "🎉 Your organizer request has been approved!";
        String body = String.format("""
                Hi %s,

                Great news — your request to become an organizer has been APPROVED.

                You can now create and manage events on EventFlow.

                Log in and get started:
                  %s/events/create

                %s

                — EventFlow
                """,
                user.getName(),
                baseUrl,
                req.getReviewNote() != null && !req.getReviewNote().isBlank()
                        ? "Note from staff:\n" + req.getReviewNote()
                        : "");

        send(user.getEmail(), subject, body);
    }

    // ── Organizer request declined ────────────────────────────

    @Async
    public void sendRequestDeclined(User user, OrganizerRequest req) {
        String subject = "Your organizer request – update";
        String body = String.format("""
                Hi %s,

                Thank you for your interest in becoming an organizer on EventFlow.

                After review, your request has not been approved at this time.

                %s

                You're welcome to submit a new request in the future.

                — EventFlow
                """,
                user.getName(),
                req.getReviewNote() != null && !req.getReviewNote().isBlank()
                        ? "Reason from staff:\n" + req.getReviewNote()
                        : "No additional reason was provided.");

        send(user.getEmail(), subject, body);
    }

    // ── Internal helper ───────────────────────────────────────

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {} – {}", to, subject);
        } catch (MailException ex) {
            // Log and swallow: email failure must never break a business transaction
            log.error("Failed to send email to {} – {}: {}", to, subject, ex.getMessage());
        }
    }
}