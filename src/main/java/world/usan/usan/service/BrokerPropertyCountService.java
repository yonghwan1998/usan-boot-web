package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.common.broker.BrokerPropertyTagFactory;
import world.usan.usan.dto.BrokerPropertyTagDto;
import world.usan.usan.entity.BrokerPropertyCount;
import world.usan.usan.repository.BrokerPropertyCountRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrokerPropertyCountService {

    private final BrokerPropertyCountRepository brokerPropertyCountRepository;

    /**
     * @date    2026-01-27
     * @author  yongss
     * @param   {UUID brokerCode}
     * @return  {List<BrokerPropertyTagDto>}
     *
     * 처리 과정:
     *  - 매개변수로 brokerCode를 받음
     *  - brokerCode로 broker_property_count 테이블에서 전체 취급 매물 조회
     *  - 전체 취급 매물 mapping, sorting 후 반환
     */
    @Transactional(readOnly = true)
    public List<BrokerPropertyTagDto> getBrokerTagsAll(UUID brokerCode) {

        BrokerPropertyCount count = brokerPropertyCountRepository.findById(brokerCode)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerCode));

        return BrokerPropertyTagFactory.allSorted(count);

    }
}
