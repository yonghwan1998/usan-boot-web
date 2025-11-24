package world.usan.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class BrokerMarkerDetailDto {

    private UUID brokerCode;
    private String brokerName;
    private String officeName;
    private String registrationNumber;
    private String tel;
    private String phone;
    private String addrRoad;
    private String addrJibun;

    private int aptCnt;
    private int officetelCnt;
    private int villaCnt;
    private int oneroomCnt;
    private int tworoomCnt;
    private int detachedCnt;
    private int ruralCnt;
    private int mixedhouseCnt;
    private int hanokCnt;
    private int storeCnt;
    private int officeCnt;
    private int buildingCnt;
    private int factoryCnt;
    private int knowledgeCnt;
    private int landCnt;
    private int aptSaleCnt;
    private int officetelSaleCnt;
    private int redevelopmentCnt;
    private int reconstructionCnt;
    private int presaleCnt;
    private int totalCnt;
}
