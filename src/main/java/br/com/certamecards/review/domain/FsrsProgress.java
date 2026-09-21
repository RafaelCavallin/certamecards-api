package br.com.certamecards.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class FsrsProgress {

    @Column(nullable = false)
    private CardLearningState state;

    @Column(nullable = false)
    private double stability;

    @Column(nullable = false)
    private double difficulty;

    @Column(nullable = false)
    private Instant due;

    @Column(name = "last_review")
    private Instant lastReview;

    @Column(nullable = false)
    private int reps;

    @Column(nullable = false)
    private int lapses;

    @Column(name = "learning_steps", nullable = false)
    private int learningSteps;

    @Column(name = "scheduled_days", nullable = false)
    private int scheduledDays;

    protected FsrsProgress() {}

    public FsrsProgress(Instant due) {
        this.state = CardLearningState.NEW;
        this.due = due;
    }

    public CardLearningState getState() {
        return state;
    }

    public double getStability() {
        return stability;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public Instant getDue() {
        return due;
    }

    public Instant getLastReview() {
        return lastReview;
    }

    public int getReps() {
        return reps;
    }

    public int getLapses() {
        return lapses;
    }

    public int getLearningSteps() {
        return learningSteps;
    }

    public int getScheduledDays() {
        return scheduledDays;
    }

    public void resetToNew(Instant due) {
        this.state = CardLearningState.NEW;
        this.stability = 0;
        this.difficulty = 0;
        this.due = due;
        this.lastReview = null;
        this.reps = 0;
        this.lapses = 0;
        this.learningSteps = 0;
        this.scheduledDays = 0;
    }

    public void applySeed(FsrsProgressSeed seed) {
        this.state = seed.state();
        this.stability = seed.stability();
        this.difficulty = seed.difficulty();
        this.due = seed.due();
        this.lastReview = seed.lastReview();
        this.reps = seed.reps();
        this.lapses = seed.lapses();
        this.learningSteps = seed.learningSteps();
        this.scheduledDays = seed.scheduledDays();
    }
}
