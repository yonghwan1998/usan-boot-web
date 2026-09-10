package com.usanmap.usan.controller;

import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.entity.enums.ListingStatus;
import com.usanmap.usan.repository.ListingRepository;
import com.usanmap.usan.repository.UserRegionRepository;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.ListingService;
import com.usanmap.usan.service.UserRegionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MapQueryTests {

    @Mock private SecurityUtils securityUtils;
    @Mock private ListingRepository listingRepository;
    @Mock private UserRegionRepository userRegionRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // 실제 조회 Service를 연결하고 DB와 인증 사용자 조회만 대체한다.
        MapApiController controller = new MapApiController(
                null, securityUtils, listingRepository,
                new ListingService(listingRepository), new UserRegionService(userRegionRepository),
                null, null, null, null);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void anonymousQueriesKeepDifferentResponsesWithoutReadingData() throws Exception {
        when(securityUtils.currentUserId()).thenReturn(null);
        mvc.perform(get("/map/api/user-region"))
                .andExpect(status().isOk()).andExpect(content().string(""));
        mvc.perform(get("/map/api/listings/my-listings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        verifyNoInteractions(listingRepository, userRegionRepository);
    }

    @Test
    void missingRegionReturnsEmptyBody() throws Exception {
        when(securityUtils.currentUserId()).thenReturn(1L);
        when(userRegionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        mvc.perform(get("/map/api/user-region"))
                .andExpect(status().isOk()).andExpect(content().string(""));
    }

    @Test
    void regionReturnsOnlyCoordinatesOfFirstRegionInRepositoryOrder() throws Exception {
        when(securityUtils.currentUserId()).thenReturn(1L);
        UserRegion latest = UserRegion.builder().emdLat(new BigDecimal("37.584"))
                .emdLng(new BigDecimal("126.9707")).build();
        UserRegion older = UserRegion.builder().emdLat(BigDecimal.ZERO).emdLng(BigDecimal.ZERO).build();
        when(userRegionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(latest, older));
        mvc.perform(get("/map/api/user-region"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"lat\":37.584,\"lng\":126.9707}", JsonCompareMode.STRICT));
    }

    @Test
    void missingListingsReturnEmptyArray() throws Exception {
        when(securityUtils.currentUserId()).thenReturn(1L);
        when(listingRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(1L, ListingStatus.ACTIVE))
                .thenReturn(List.of());
        mvc.perform(get("/map/api/listings/my-listings"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void listingsKeepFieldsAndRepositoryOrder() throws Exception {
        when(securityUtils.currentUserId()).thenReturn(1L);
        Listing latest = mock(Listing.class);
        when(latest.getId()).thenReturn(2L);
        when(latest.getPublicId()).thenReturn("abc123");
        when(latest.getAddressName()).thenReturn("서울특별시 종로구");
        when(latest.getType()).thenReturn("APARTMENT");
        when(latest.getTradeType()).thenReturn("SALE");
        when(latest.getLat()).thenReturn(new BigDecimal("37.584"));
        when(latest.getLng()).thenReturn(new BigDecimal("126.9707"));
        Listing older = mock(Listing.class);
        when(older.getId()).thenReturn(1L);
        when(listingRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(1L, ListingStatus.ACTIVE))
                .thenReturn(List.of(latest, older));
        mvc.perform(get("/map/api/listings/my-listings"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"listingId":2,"publicId":"abc123","addressName":"서울특별시 종로구",
                          "type":"APARTMENT","tradeType":"SALE","lat":37.584,"lng":126.9707},
                         {"listingId":1,"publicId":null,"addressName":null,
                          "type":null,"tradeType":null,"lat":null,"lng":null}]
                        """, JsonCompareMode.STRICT));
    }
}
