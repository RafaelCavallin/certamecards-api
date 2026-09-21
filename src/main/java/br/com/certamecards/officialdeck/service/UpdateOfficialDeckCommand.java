package br.com.certamecards.officialdeck.service;

import java.util.UUID;

public record UpdateOfficialDeckCommand(UUID subjectId, String name, String description, int expectedVersion) {}
