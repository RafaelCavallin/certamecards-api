package br.com.certamecards.officialdeck.service;

public record UpdateOfficialCardCommand(
        String front, String back, String source, Boolean contentChanged, String note, int expectedVersion) {}
