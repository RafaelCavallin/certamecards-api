package br.com.certamecards.library.web;

import java.util.List;

public record ItemsResponse<T>(List<T> items) {}
