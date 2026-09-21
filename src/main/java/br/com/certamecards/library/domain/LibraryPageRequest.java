package br.com.certamecards.library.domain;

public record LibraryPageRequest(int page, int size) {

    public int offset() {
        return page * size;
    }
}
