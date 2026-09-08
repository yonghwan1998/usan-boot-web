package com.usanmap.usan.service;

import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.repository.UserRegionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegionServiceTests {

    @Mock
    private UserRegionRepository userRegionRepository;

    @InjectMocks
    private UserRegionService userRegionService;

    @Test
    void saveOrUpdateCreatesRegionWhenUserHasNoRegion() {
        Long userId = 1L;
        when(userRegionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        userRegionService.saveOrUpdate(
                userId,
                "1111010100",
                "서울특별시",
                "종로구",
                "청운효자동",
                new BigDecimal("37.5840"),
                new BigDecimal("126.9707")
        );

        ArgumentCaptor<UserRegion> captor = ArgumentCaptor.forClass(UserRegion.class);
        verify(userRegionRepository).save(captor.capture());
        UserRegion saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAdmCd()).isEqualTo("1111010100");
        assertThat(saved.getSidoName()).isEqualTo("서울특별시");
        assertThat(saved.getSigunguName()).isEqualTo("종로구");
        assertThat(saved.getEmdName()).isEqualTo("청운효자동");
        assertThat(saved.getEmdLat()).isEqualByComparingTo("37.5840");
        assertThat(saved.getEmdLng()).isEqualByComparingTo("126.9707");
    }

    @Test
    void saveOrUpdateChangesExistingRegionWithoutSavingNewEntity() {
        Long userId = 1L;
        UserRegion existing = UserRegion.builder()
                .userId(userId)
                .admCd("1168010100")
                .sidoName("서울특별시")
                .sigunguName("강남구")
                .emdName("역삼동")
                .emdLat(new BigDecimal("37.5000"))
                .emdLng(new BigDecimal("127.0360"))
                .build();
        when(userRegionRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        userRegionService.saveOrUpdate(
                userId,
                "4113110200",
                "경기도",
                "성남시 수정구",
                "태평동",
                new BigDecimal("37.4400"),
                new BigDecimal("127.1300")
        );

        assertThat(existing.getAdmCd()).isEqualTo("4113110200");
        assertThat(existing.getSidoName()).isEqualTo("경기도");
        assertThat(existing.getSigunguName()).isEqualTo("성남시 수정구");
        assertThat(existing.getEmdName()).isEqualTo("태평동");
        assertThat(existing.getEmdLat()).isEqualByComparingTo("37.4400");
        assertThat(existing.getEmdLng()).isEqualByComparingTo("127.1300");
        verify(userRegionRepository, never()).save(existing);
    }
}
