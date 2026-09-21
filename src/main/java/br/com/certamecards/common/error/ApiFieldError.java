package br.com.certamecards.common.error;

public record ApiFieldError(String field, String code, String message) {}
