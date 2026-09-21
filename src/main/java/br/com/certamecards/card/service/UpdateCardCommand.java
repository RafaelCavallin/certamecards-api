package br.com.certamecards.card.service;

public record UpdateCardCommand(CardContent content, int expectedVersion) {}
