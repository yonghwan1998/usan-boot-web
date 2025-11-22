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

    @Column(name = "lat", precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng",  precision = 10, scale = 7)
    private BigDecimal lng;
}
