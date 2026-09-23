package br.com.certamecards.errorreport.domain;

public record ErrorReportFilter(ErrorReportStatus status, int page, int size) {}
