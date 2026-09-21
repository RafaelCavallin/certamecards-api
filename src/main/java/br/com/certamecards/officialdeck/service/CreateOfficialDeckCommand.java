package br.com.certamecards.officialdeck.service;

import java.util.UUID;

public record CreateOfficialDeckCommand(UUID id, UUID subjectId, String name, String description) {}
