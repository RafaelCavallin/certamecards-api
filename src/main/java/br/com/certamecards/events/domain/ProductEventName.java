package br.com.certamecards.events.domain;

public enum ProductEventName {
    SIGNUP_COMPLETED("signup_completed"),
    DECK_CREATED("deck_created"),
    CARD_CREATED("card_created"),
    SESSION_STARTED("session_started"),
    SESSION_ENDED("session_ended"),
    REVIEW_UNDONE("review_undone"),
    PWA_INSTALLED("pwa_installed"),
    SYNC_FLUSHED("sync_flushed"),
    CLIENT_ERROR("client_error"),
    LIBRARY_OPENED("library_opened"),
    LIBRARY_SEARCHED("library_searched"),
    DECK_PREVIEW_OPENED("deck_preview_opened"),
    DECK_SUBSCRIBED("deck_subscribed"),
    DECK_UNSUBSCRIBED("deck_unsubscribed"),
    DECK_DUPLICATED("deck_duplicated"),
    CARD_ERROR_REPORTED("card_error_reported"),
    OFFLINE_CHANGE_SAVED("offline_change_saved"),
    SYNC_CYCLE_STARTED("sync_cycle_started"),
    SYNC_CYCLE_COMPLETED("sync_cycle_completed"),
    SYNC_ACTION_REQUIRED("sync_action_required"),
    SYNC_CONFLICT_AVAILABLE("sync_conflict_available"),
    SYNC_CONFLICT_RESTORED("sync_conflict_restored"),
    SYNC_LOGOUT_PENDING("sync_logout_pending"),
    SYNC_STORAGE_FAILED("sync_storage_failed");

    private final String code;

    ProductEventName(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ProductEventName fromCode(String code) {
        for (ProductEventName name : values()) {
            if (name.code.equals(code)) {
                return name;
            }
        }
        throw new IllegalArgumentException("Unknown ProductEventName code: " + code);
    }
}
