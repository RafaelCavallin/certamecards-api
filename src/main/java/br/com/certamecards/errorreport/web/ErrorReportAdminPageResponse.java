package br.com.certamecards.errorreport.web;

import java.util.List;

public record ErrorReportAdminPageResponse(List<ErrorReportAdminResponse> items, int page, int size, long total) {}
