package br.com.certamecards.user.web;

public record DeleteAccountRequest(String password, String reauthToken) {}
