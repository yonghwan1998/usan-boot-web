package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.dto.BrokerMarkerDetailDto;
import world.usan.usan.dto.BrokerMarkerDto;
import world.usan.usan.dto.BrokerPropertyTagDto;
import world.usan.usan.entity.BrokerPropertyCount;
import world.usan.usan.repository.BrokerPropertyCountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MapService {

    private final BrokerPropertyCountRepository brokerPropertyCountRepository;

    @Transactional(readOnly = true)
    public List<BrokerMarkerDto> getBrokersInBounds(double south, double north, double west, double east) {

        BigDecimal southBd = new BigDecimal(south);
        BigDecimal northBd = new BigDecimal(north);
        BigDecimal westBd = new BigDecimal(west);
        BigDecimal eastBd = new BigDecimal(east);

        List<BrokerPropertyCount> entities = brokerPropertyCountRepository.findByLatBetweenAndLngBetween(southBd, northBd, westBd, eastBd);

        return entities.stream()
                .map(b -> BrokerMarkerDto.builder()
                        .brokerCode(b.getBrokerCode())
                        .brokerName(b.getBrokerName())
                        .officeName(b.getOfficeName())
                        .lat(b.getLat())
                        .lng(b.getLng())
                        .build()
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public BrokerMarkerDetailDto getBrokerDetail(UUID brokerCode) {

        BrokerPropertyCount broker = brokerPropertyCountRepository.findById(brokerCode)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerCode));

        List<BrokerPropertyTagDto> items = getPropertiesTop5(broker);

        return BrokerMarkerDetailDto.builder()
                .brokerCode(broker.getBrokerCode())
                .brokerName(broker.getBrokerName())
                .officeName(broker.getOfficeName())
                .registrationNumber(broker.getRegistrationNumber())
                .tel(broker.getTel())
                .phone(broker.getPhone())
                .addrRoad(broker.getAddrRoad())
                .addrJibun(broker.getAddrJibun())
                .top5(items)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BrokerMarkerDetailDto> getBrokerDetails(List<UUID> brokerCodes) {

        if (brokerCodes == null || brokerCodes.isEmpty()) {
            return List.of();
        }

        List<BrokerPropertyCount> list = brokerPropertyCountRepository.findByBrokerCodeIn(brokerCodes);

        return list.stream()
                .map(b -> {
                    List<BrokerPropertyTagDto> top5 = getPropertiesTop5(b);
                    return BrokerMarkerDetailDto.builder()
                            .brokerCode(b.getBrokerCode())
                            .brokerName(b.getBrokerName())
                            .officeName(b.getOfficeName())
                            .registrationNumber(b.getRegistrationNumber())
                            .tel(b.getTel())
                            .phone(b.getPhone())
                            .sido(b.getSido())
                            .sigungu(b.getSigungu())
                            .emd(b.getEmd())
                            .addrRoad(b.getAddrRoad())
                            .addrJibun(b.getAddrJibun())
                            .top5(top5)
                            .build();
                })
                .toList();
    }

    private static List<BrokerPropertyTagDto> getPropertiesTop5(BrokerPropertyCount count) {
        List<BrokerPropertyTagDto> items = List.of(
                new BrokerPropertyTagDto("아파트", count.getAptCnt(), "listing__tag--apt"),
                new BrokerPropertyTagDto("오피스텔", count.getOfficetelCnt(), "listing__tag--officetel"),
                new BrokerPropertyTagDto("빌라/연립", count.getVillaCnt(), "listing__tag--villa"),
                new BrokerPropertyTagDto("원룸", count.getOneroomCnt(), "listing__tag--oneroom"),
                new BrokerPropertyTagDto("투룸", count.getTworoomCnt(), "listing__tag--tworoom"),
                new BrokerPropertyTagDto("단독/다가구", count.getDetachedCnt(), "listing__tag--detached"),
                new BrokerPropertyTagDto("전원주택", count.getRuralCnt(), "listing__tag--rural"),
                new BrokerPropertyTagDto("상가주택", count.getMixedhouseCnt(), "listing__tag--mixedhouse"),
                new BrokerPropertyTagDto("한옥주택", count.getHanokCnt(), "listing__tag--hanok"),
                new BrokerPropertyTagDto("상가", count.getStoreCnt(), "listing__tag--store"),
                new BrokerPropertyTagDto("사무실", count.getOfficeCnt(), "listing__tag--office"),
                new BrokerPropertyTagDto("건물", count.getBuildingCnt(), "listing__tag--building"),
                new BrokerPropertyTagDto("공장/창고", count.getFactoryCnt(), "listing__tag--factory"),
                new BrokerPropertyTagDto("지식산업센터", count.getKnowledgeCnt(), "listing__tag--knowledge"),
                new BrokerPropertyTagDto("토지", count.getLandCnt(), "listing__tag--land"),
                new BrokerPropertyTagDto("아파트분양권", count.getAptSaleCnt(), "listing__tag--apt-sale"),
                new BrokerPropertyTagDto("오피스텔분양권", count.getOfficetelSaleCnt(), "listing__tag--officetel-sale"),
                new BrokerPropertyTagDto("재개발", count.getRedevelopmentCnt(), "listing__tag--redevelopment"),
                new BrokerPropertyTagDto("재건축", count.getReconstructionCnt(), "listing__tag--reconstruction"),
                new BrokerPropertyTagDto("분양중/예정", count.getPresaleCnt(), "listing__tag--presale")
        );

        return items.stream()
                .filter(i -> i.getCount() > 0)
                .sorted((first, second) -> Integer.compare(second.getCount(), first.getCount()))
                .limit(5)
                .toList();
    }
}
