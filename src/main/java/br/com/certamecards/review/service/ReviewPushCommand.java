package br.com.certamecards.review.service;

import java.util.List;
import java.util.UUID;

public record ReviewPushCommand(
        UUID deviceId, List<ReviewLogInput> reviews, List<ReviewVoidInput> voids, List<CardStatePushInput> states) {}
