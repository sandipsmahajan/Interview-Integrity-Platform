package com.interviewintegrity.notification.service;

/** Signals that an email could not be handed to the transport provider. */
public class EmailDispatchException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates a dispatch exception for a failed send. */
  public EmailDispatchException(String to, String subject, Throwable cause) {
    super("Failed to dispatch email to " + to + " subject '" + subject + "'", cause);
  }
}
