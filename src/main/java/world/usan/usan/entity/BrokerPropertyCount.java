package world.usan.usan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "broker_property_count")
@Getter
public class BrokerPropertyCount {

    @Id
    @Column(name = "broker_code", columnDefinition = "BINARY(16)")
    private UUID brokerCode;

    @Column(name = "broker_name")
    private String brokerName;

    @Column(name = "office_name")
    private String officeName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "tel")
    private String tel;

    @Column(name = "phone")
    private String phone;

    @Column(name = "sido")
    private String sido;

    @Column(name = "sigungu")
    private String sigungu;

    @Column(name = "dongmyun")
    private String dongmyun;

    @Column(name = "road_name")
    private String roadName;

    @Column(name = "addr_road")
    private String addrRoad;

    @Column(name = "addr_jibun")
    private String addrJibun;

    @Column(name = "lat", precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng",  precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "apt_cnt")
    private int aptCnt;

    @Column(name = "officetel_cnt")
    private int officetelCnt;

    @Column(name = "villa_cnt")
    private int villaCnt;

    @Column(name = "oneroom_cnt")
    private int oneroomCnt;

    @Column(name = "tworoom_cnt")
    private int tworoomCnt;

    @Column(name = "detached_cnt")
    private int detachedCnt;

    @Column(name = "rural_cnt")
    private int ruralCnt;

    @Column(name = "mixedhouse_cnt")
    private int mixedhouseCnt;

    @Column(name = "hanok_cnt")
    private int hanokCnt;

    @Column(name = "store_cnt")
    private int storeCnt;

    @Column(name = "office_cnt")
    private int officeCnt;

    @Column(name = "building_cnt")
    private int buildingCnt;

    @Column(name = "factory_cnt")
    private int factoryCnt;

    @Column(name = "knowledge_cnt")
    private int knowledgeCnt;

    @Column(name = "land_cnt")
    private int landCnt;

    @Column(name = "apt_sale_cnt")
    private int aptSaleCnt;

    @Column(name = "officetel_sale_cnt")
    private int officetelSaleCnt;

    @Column(name = "redevelopment_cnt")
    private int redevelopmentCnt;

    @Column(name = "reconstruction_cnt")
    private int reconstructionCnt;

    @Column(name = "presale_cnt")
    private int presaleCnt;

    @Column(name = "total_cnt")
    private int totalCnt;
}
