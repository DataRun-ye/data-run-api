//package org.nmcpye.datarun.repository.timezone;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.io.Serializable;
//import java.time.*;
//import java.util.Objects;
//
//@Setter
//@Getter
//@Entity
//@Table(name = "app_date_time_wrapper")
//public class DateTimeWrapper implements Serializable {
//
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    private Long id;
//
//    @Column(name = "instant")
//    private Instant instant;
//
//    @Column(name = "local_date_time")
//    private LocalDateTime localDateTime;
//
//    @Column(name = "offset_date_time")
//    private OffsetDateTime offsetDateTime;
//
//    @Column(name = "zoned_date_time")
//    private ZonedDateTime zonedDateTime;
//
//    @Column(name = "local_time")
//    private LocalTime localTime;
//
//    @Column(name = "offset_time")
//    private OffsetTime offsetTime;
//
//    @Column(name = "local_date")
//    private LocalDate localDate;
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) {
//            return true;
//        }
//        if (o == null || getClass() != o.getClass()) {
//            return false;
//        }
//
//        DateTimeWrapper dateTimeWrapper = (DateTimeWrapper) o;
//        return !(dateTimeWrapper.getId() == null || getId() == null) && Objects.equals(getId(), dateTimeWrapper.getId());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hashCode(getId());
//    }
//
//    // prettier-ignore
//    @Override
//    public String toString() {
//        return "TimeZoneTest{" +
//            "id=" + id +
//            ", instant=" + instant +
//            ", localDateTime=" + localDateTime +
//            ", offsetDateTime=" + offsetDateTime +
//            ", zonedDateTime=" + zonedDateTime +
//            '}';
//    }
//}
