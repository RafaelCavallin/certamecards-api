package br.com.certamecards.sync.service;

public record CardMutationPayload(String front, String back, String source, Integer parentBaseVersion) {}
