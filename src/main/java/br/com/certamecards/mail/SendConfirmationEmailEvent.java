package br.com.certamecards.mail;

public record SendConfirmationEmailEvent(String to, String displayName, String link) {}
