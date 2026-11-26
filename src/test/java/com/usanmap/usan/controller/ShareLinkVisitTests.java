package com.usanmap.usan.controller;

import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.repository.*;
import com.usanmap.usan.service.ListingLinkVisitService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShareLinkVisitTests {
    @Test
    void supportsOldLinksRecordsGetOnlyAndSurvivesTrackingDatabaseFailure() throws Exception {
        var listings = mock(ListingRepository.class);
        var visits = mock(ListingLinkVisitService.class);
        var listing = mock(Listing.class);
        when(listing.getId()).thenReturn(9L);
        when(listings.findByPublicId("public")).thenReturn(Optional.of(listing));
        var mvc = MockMvcBuilders.standaloneSetup(new SharePageController(visits, listings,
                mock(BrokerPropertyCountRepository.class))).build();
        mvc.perform(get("/l/public")).andExpect(status().isOk()).andExpect(view().name("pages/share/listing"));
        mvc.perform(get("/l/public?t=Ab3x_K")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
        verify(visits).record(9L, "Ab3x_K");
        clearInvocations(visits);
        mvc.perform(head("/l/public?t=Ab3x_K")).andExpect(status().isOk());
        verifyNoInteractions(visits);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"))
                .when(visits).record(9L, "Ab3x_K");
        mvc.perform(get("/l/public?t=Ab3x_K")).andExpect(status().isOk());
    }
}
