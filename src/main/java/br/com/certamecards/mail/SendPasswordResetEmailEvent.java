package br.com.certamecards.mail;

public record SendPasswordResetEmailEvent(String to, String displayName, String link) {}
