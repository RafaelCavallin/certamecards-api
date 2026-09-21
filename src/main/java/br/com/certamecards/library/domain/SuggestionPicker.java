package br.com.certamecards.library.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SuggestionPicker {

    public List<LibraryDeckSummary> pick(List<LibraryDeckSummary> candidates, int limit) {
        Set<UUID> usedSubjects = new HashSet<>();
        List<LibraryDeckSummary> picked = new ArrayList<>();
        for (LibraryDeckSummary candidate : candidates) {
            if (picked.size() >= limit) {
                break;
            }
            if (!candidate.subscribed() && usedSubjects.add(candidate.subjectId())) {
                picked.add(candidate);
            }
        }
        return picked;
    }
}
