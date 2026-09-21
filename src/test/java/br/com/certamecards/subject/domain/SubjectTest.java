package br.com.certamecards.subject.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubjectTest {

    @Test
    void givenNameAndNormalizedName_whenConstructing_thenActiveDefaultsToTrue() {
        Subject subject = new Subject("Direito Constitucional", "direito constitucional");
        assertThat(subject.getName()).isEqualTo("Direito Constitucional");
        assertThat(subject.getNormalizedName()).isEqualTo("direito constitucional");
        assertThat(subject.isActive()).isTrue();
        assertThat(subject.getId()).isNotNull();
        assertThat(subject.getChangeSeq()).isNull();
    }

    @Test
    void givenSubject_whenRenaming_thenNameAndNormalizedNameChange() {
        Subject subject = new Subject("Direito Penal", "direito penal");
        subject.rename("Direito Processual Penal", "direito processual penal");
        assertThat(subject.getName()).isEqualTo("Direito Processual Penal");
        assertThat(subject.getNormalizedName()).isEqualTo("direito processual penal");
    }

    @Test
    void givenActiveSubject_whenDeactivating_thenActiveIsFalse() {
        Subject subject = new Subject("Português", "portugues");
        subject.setActive(false);
        assertThat(subject.isActive()).isFalse();
    }
}
