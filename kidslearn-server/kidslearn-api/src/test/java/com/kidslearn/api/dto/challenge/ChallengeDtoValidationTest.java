package com.kidslearn.api.dto.challenge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ChallengeDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createChallengeAllowsAutoMatchedOpponent() {
        CreateChallengeDTO dto = new CreateChallengeDTO();
        dto.setType("RANKED");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void submitChallengeAllowsSimulatedOpponentScore() {
        SubmitChallengeDTO dto = new SubmitChallengeDTO();
        dto.setChallengeId(1L);
        dto.setUserScore(80);

        assertTrue(validator.validate(dto).isEmpty());
    }
}
