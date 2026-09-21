package br.com.certamecards.user.service;

public record DeleteAccountCommand(String password, String reauthToken) {}
