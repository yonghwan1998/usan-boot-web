package com.usanmap.usan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.dto.ListingShareRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.*;

class ListingShareRequestTests {
    @Test
    void acceptsLegacyJsonAndValidatesOptionalSelectionSnapshot() throws Exception {
        var mapper = new ObjectMapper();
        var legacy = mapper.readValue("{\"listingId\":1,\"brokerCodes\":[]}", ListingShareRequest.class);
        assertThat(legacy.selectionCriteria()).isNull();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var invalid = new ListingShareRequest(1L, java.util.List.of(),
                    new ListingShareRequest.SelectionCriteria(-1, Arrays.asList(null, "")));
            assertThat(validator.validate(invalid)).hasSize(3);
            assertThat(validator.validate(legacy)).isEmpty();
        }
    }
}
