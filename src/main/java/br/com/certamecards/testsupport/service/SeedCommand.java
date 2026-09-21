package br.com.certamecards.testsupport.service;

import br.com.certamecards.testsupport.domain.SeedScenario;
import java.util.UUID;

public record SeedCommand(UUID ownerId, SeedScenario scenario, int count, String subjectName) {}
