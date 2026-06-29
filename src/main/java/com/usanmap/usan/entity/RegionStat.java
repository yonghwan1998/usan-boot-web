package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "region_stat")
@Getter
public class RegionStat {

    @Id
    @Column(name = "adm_cd", length = 10)
    private String admCd;

    @Column(name = "adm_level", length = 10)
    private String admLevel;

    @Column(name = "broker_count") private int brokerCount;
    @Column(name = "apt_cnt") private int aptCnt;
    @Column(name = "officetel_cnt") private int officetelCnt;
    @Column(name = "villa_cnt") private int villaCnt;
    @Column(name = "oneroom_cnt") private int oneroomCnt;
    @Column(name = "tworoom_cnt") private int tworoomCnt;
    @Column(name = "detached_cnt") private int detachedCnt;
    @Column(name = "rural_cnt") private int ruralCnt;
    @Column(name = "mixedhouse_cnt") private int mixedhouseCnt;
    @Column(name = "hanok_cnt") private int hanokCnt;
    @Column(name = "store_cnt") private int storeCnt;
    @Column(name = "office_cnt") private int officeCnt;
    @Column(name = "building_cnt") private int buildingCnt;
    @Column(name = "factory_cnt") private int factoryCnt;
    @Column(name = "knowledge_cnt") private int knowledgeCnt;
    @Column(name = "land_cnt") private int landCnt;
    @Column(name = "apt_sale_cnt") private int aptSaleCnt;
    @Column(name = "officetel_sale_cnt") private int officetelSaleCnt;
    @Column(name = "redevelopment_cnt") private int redevelopmentCnt;
    @Column(name = "reconstruction_cnt") private int reconstructionCnt;
    @Column(name = "presale_cnt") private int presaleCnt;
    @Column(name = "total_cnt") private int totalCnt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
