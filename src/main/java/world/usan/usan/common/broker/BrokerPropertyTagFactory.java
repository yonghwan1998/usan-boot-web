package world.usan.usan.common.broker;

import world.usan.usan.dto.BrokerPropertyTagDto;
import world.usan.usan.entity.BrokerPropertyCount;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class BrokerPropertyTagFactory {

    private BrokerPropertyTagFactory() {}

    /**
     * @date    2026-01-27
     * @author  yongss
     * @param   {BrokerPropertyCount count}
     * @return  {List<BrokerPropertyTagDto>}
     *
     * 처리 과정:
     *  - 취급 매물 count가 담긴 객체를 매개변수로 받음
     *  - 전체 반환
     */
    public static List<BrokerPropertyTagDto> allSorted(BrokerPropertyCount count) {
        return sortedNonZero(count).toList();
    }

    /**
     * @date    2026-01-27
     * @author  yongss
     * @param   {BrokerPropertyCount count, int n}
     * @return  {List<BrokerPropertyTagDto>}
     *
     * 처리 과정:
     *  - 취급 매물 count가 담긴 객체와 보여줄 상위 n을 매개변수로 받음
     *  - 상위 n개 반환
     */
    public static List<BrokerPropertyTagDto> topN(BrokerPropertyCount count, int n) {
        return sortedNonZero(count).limit(n).toList();
    }

    /**
     * @date    2026-01-27
     * @author  yongss
     * @param   {BrokerPropertyCount c}
     * @return  {Stream<BrokerPropertyTagDto>}
     *
     * 처리 과정:
     *  - 취급 매물 count가 담긴 객체를 매개변수로 받음
     *  - baseItems()의 인자로 취급 매물 count가 담긱 객체 전달
     *  - 반환 된 데이터를 filtering, sorting 후 반환
     *
     * 예외/주의:
     *  - count가 1개 이상인 매물만 반환
     *  - count 기준 내림차순 정렬
     */
    private static Stream<BrokerPropertyTagDto> sortedNonZero(BrokerPropertyCount c) {
        return baseItems(c).stream()
                .filter(i -> i.getCount() > 0)
                .sorted(Comparator.comparingInt(BrokerPropertyTagDto::getCount).reversed());
    }

    /**
     * @date    2026-01-27
     * @author  yongss
     * @param   {BrokerPropertyCount count}
     * @return  {List<BrokerPropertyTagDto>}
     *
     * 처리 과정:
     *  - 취급 매물 count가 담긴 객체를 매개변수로 받음
     *  - 라벨, 카운트, cssClass를 매핑 후 반환
     */
    private static List<BrokerPropertyTagDto> baseItems(BrokerPropertyCount count) {
        return List.of(
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
    }
}
