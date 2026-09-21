package br.com.certamecards.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dados inválidos", "Revise os campos destacados."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Não autenticado", "Entre novamente para continuar."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha incorretos."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "E-mail não confirmado", "Confirme seu e-mail para entrar."),
    TERMS_REQUIRED(HttpStatus.FORBIDDEN, "Termos pendentes", "Aceite os termos de uso para continuar."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para esta ação."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Não encontrado", "O recurso solicitado não existe."),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "Conflito de versão", "Os dados mudaram. Recarregue e tente de novo."),
    SUBJECT_NAME_TAKEN(HttpStatus.CONFLICT, "Nome já existe", "Já existe uma matéria com esse nome."),
    SUBJECT_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Matéria desativada", "Escolha uma matéria ativa."),
    LAST_ADMIN(HttpStatus.UNPROCESSABLE_ENTITY, "Único administrador", "Não é possível retirar o único administrador."),
    DECK_CARD_LIMIT(HttpStatus.UNPROCESSABLE_ENTITY, "Limite do deck", "Este deck chegou a 5.000 cartões."),
    OFFICIAL_DECK_MIN_CARDS(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Poucos cartões",
            "Um deck oficial precisa de pelo menos 5 cartões para ser publicado."),
    OFFICIAL_DECK_HAS_SUBSCRIBERS(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Deck com inscritos",
            "Este deck tem inscritos. Descontinue-o em vez de despublicar ou excluir."),
    DECK_NOT_AVAILABLE(
            HttpStatus.UNPROCESSABLE_ENTITY, "Deck indisponível", "Este deck não está disponível na biblioteca."),
    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "Já inscrito", "Você já está inscrito neste deck."),
    NOT_SUBSCRIBED(HttpStatus.CONFLICT, "Sem inscrição", "Você não está inscrito neste deck."),
    USER_CARD_LIMIT(HttpStatus.UNPROCESSABLE_ENTITY, "Limite de cartões", "Você chegou a 50.000 cartões."),
    TOKEN_EXPIRED(HttpStatus.GONE, "Link expirado", "Peça um novo link."),
    TOKEN_USED(HttpStatus.GONE, "Link já usado", "Peça um novo link."),
    RESYNC_REQUIRED(HttpStatus.GONE, "Sincronização completa necessária", "Refaça a sincronização desde o início."),
    LOGIN_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas", "Aguarde para tentar de novo."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Limite de requisições", "Aguarde e tente de novo."),
    ORIGIN_REJECTED(HttpStatus.FORBIDDEN, "Origem rejeitada", "Requisição de origem não permitida.");

    private final HttpStatus status;
    private final String title;
    private final String detail;

    ErrorCode(HttpStatus status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    public String code() {
        return name().toLowerCase();
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String detail() {
        return detail;
    }
}
