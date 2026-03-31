package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import com.usanmap.usan.common.jpa.UuidToBytesConverter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "broker_property_count")
@Getter
public class BrokerPropertyCount {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    @Convert(converter = UuidToBytesConverter.class)
    private UUID brokerCode;

    @Column()
    private String brokerName;

    @Column()
    private String officeName;

    @Column()
    private String registrationNumber;

    @Column()
    private String tel;

    @Column()
    private String phone;

    @Column()
    private String sido;

    @Column()
    private String sigungu;

    @Column()
    private String emd;

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
