package org.nmcpye.datarun.jpa.etl.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * @author Hamza Assada 10/08/2025 (7amza.it@gmail.com)
 */
//@Entity
//@Table(name = "element_data_value"/*, uniqueConstraints = {
//    @UniqueConstraint(name = "ux_element_data_value_elem_repeat",
//        columnNames = {"submission_id", "element_id", "repeat_instance_id"})
//}*/,
//    indexes = {
//        @Index(name = "ux_element_data_value_elem", columnList = "submission_id,element_id,repeat_instance_id", unique = true),
//        @Index(name = "idx_ev_repeat_id", columnList = "repeat_instance_id"),
//        @Index(name = "idx_element_data_value_deleted_at", columnList = "deleted_at")
//    })
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@NoArgsConstructor
public class SubmissionValueRow implements Serializable {
    @Serial
    private static final long serialVersionUID = 2738519623273453182L;
//
//    @Id
//    @Setter
//    private Long id;

    @Column(name = "submission_id", nullable = false)
    private String submission;

    @Column(name = "repeat_instance_id")
    private String repeatInstance;

    @Column(name = "element_id", nullable = false)
    private String element;

    @Column(name = "value_text")
    private String valueText;
    @Column(name = "value_num")
    private BigDecimal valueNum;
    @Column(name = "value_bool")
    private Boolean valueBool;

    @Column(name = "option")
    private String option;

    @Column(name = "assignment_id")
    private String assignment;
    @Column(name = "template_id", nullable = false)
    private String template;

    @Setter
    @Column(name = "category_id")
    private String category;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_date", updatable = false)
    @Setter
    protected Instant createdDate = Instant.now();

    @Column(name = "last_modified_date")
    @Setter
    protected Instant lastModifiedDate = Instant.now();

    @Builder(toBuilder = true)
    public static SubmissionValueRow from(String submission, String repeatInstance, String category,
                                          String option, String template, String element,
                                          String assignment, Instant deletedAt, Instant createdDate,
                                          Instant lastModifiedDate) {
        final SubmissionValueRow s = new SubmissionValueRow();
        s.submission = submission;
        s.assignment = assignment;
        s.template = template;
        s.element = element;
        s.repeatInstance = repeatInstance;
        s.category = category;
        s.deletedAt = deletedAt;
        s.createdDate = createdDate;
        s.lastModifiedDate = lastModifiedDate;
        s.option = option;

        return s;
    }

    public void setValue(Object rawValue) {
        if (rawValue != null) {
            if (rawValue instanceof Number) {
                this.valueNum = new BigDecimal(rawValue.toString());
                this.valueText = null;
                this.valueBool = null;
            } else if (rawValue instanceof Boolean) {
                this.valueBool = (Boolean) rawValue;
                this.valueText = null;
            } else {
                this.valueText = rawValue.toString();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionValueRow that)) return false;
        return Objects.equals(submission, that.submission) &&
            Objects.equals(repeatInstance, that.repeatInstance) &&
            Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submission, repeatInstance, element);
    }
}
