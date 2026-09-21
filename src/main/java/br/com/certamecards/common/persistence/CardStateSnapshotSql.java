package br.com.certamecards.common.persistence;

public final class CardStateSnapshotSql {

    public static final String SYSTEM_DEVICE_UUID = "CAST('00000000-0000-0000-0000-000000000000' AS uuid)";
    private static final String INSTANT_FORMAT = "'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'";

    private CardStateSnapshotSql() {}

    public static String instantOf(String timestampExpression) {
        return "to_char(" + timestampExpression + " AT TIME ZONE 'UTC', " + INSTANT_FORMAT + ")";
    }

    public static String of(String alias) {
        return snapshot(
                alias + ".state",
                new String[] {alias + ".stability", alias + ".difficulty"},
                instantOf(alias + ".due"),
                new String[] {instantOf(alias + ".last_review"), alias + ".reps", alias + ".lapses"},
                new String[] {alias + ".learning_steps", alias + ".scheduled_days"});
    }

    public static String snapshot(String state, String[] curve, String due, String[] history, String[] scheduling) {
        return "jsonb_build_object('state', " + state
                + ", 'stability', " + curve[0]
                + ", 'difficulty', " + curve[1]
                + ", 'due', " + due
                + ", 'lastReview', " + history[0]
                + ", 'reps', " + history[1]
                + ", 'lapses', " + history[2]
                + ", 'learningSteps', " + scheduling[0]
                + ", 'scheduledDays', " + scheduling[1] + ")";
    }
}
