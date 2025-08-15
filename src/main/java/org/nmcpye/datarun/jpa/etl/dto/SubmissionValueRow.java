package org.nmcpye.datarun.jpa.etl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Canonical SubmissionValueRow DTO used by the normalizer and DAO.
 * Field names and getter names must match those used by SubmissionValuesJdbcDao.
 *
 * @author Hamza Assada 10/08/2025 (7amza.it@gmail.com)
 */
@Getter
@Setter
@Builder
public class SubmissionValueRow {
    private Long id;

    // "submission_id"
    private String submission;

    // "repeat_instance_id"
    private String repeatInstance;

    // "element_id"
    private String element;

    // "option_id" (for multi-select)
    private String option;

    // "value_text"
    private String valueText;

    // "value_num"
    private BigDecimal valueNum;

    // "value_bool"
    private Boolean valueBool;

    // "assignment_id"
    private String assignment;

    // "template_id"
    private String template;

    // "category_id"
    private String category;

    // timestamps
    private Instant createdDate;
    private Instant lastModifiedDate;
    private Instant deletedAt;

    // convenience setters for numeric/boolean
    public void setValueNumber(BigDecimal v) {
        this.valueNum = v;
    }

    public void setValueBoolean(Boolean b) {
        this.valueBool = b;
    }

    // convenience for text value
    public void setValueTextValue(String t) {
        this.valueText = t;
    }

//    public void setValue(Object rawValue) {
//        if (rawValue != null) {
//            if (type.isNumeric()) {
//                this.valueNum = new BigDecimal(rawValue.toString());
//                this.valueText = null;
//                this.valueBool = null;
//            } else if (type.isBoolean()) {
//                this.valueBool = (Boolean) rawValue;
//                this.valueText = null;
//            } else {
//                this.valueText = rawValue.toString();
//            }
//        }
//    }

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

//@Getter
//@NoArgsConstructor
//@Builder
//public class SubmissionValueRow implements Serializable {

//    @Builder(toBuilder = true)
//    public static SubmissionValueRow from(String submission, String repeatInstance, String category,
//                                          String option, String template, String element,
//                                          String assignment, Instant deletedAt, Instant createdDate,
//                                          Instant lastModifiedDate) {
//        final SubmissionValueRow s = new SubmissionValueRow();
//        s.submission = submission;
//        s.assignment = assignment;
//        s.template = template;
//        s.element = element;
//        s.repeatInstance = repeatInstance;
//        s.category = category;
//        s.deletedAt = deletedAt;
//        s.createdDate = createdDate;
//        s.lastModifiedDate = lastModifiedDate;
//        s.option = option;
//
//        return s;
//    }
//public void setValue(Object rawValue) {
//    if (rawValue != null) {
//        if (rawValue instanceof Number) {
//            this.valueNum = new BigDecimal(rawValue.toString());
//            this.valueText = null;
//            this.valueBool = null;
//        } else if (rawValue instanceof Boolean) {
//            this.valueBool = (Boolean) rawValue;
//            this.valueText = null;
//        } else {
//            this.valueText = rawValue.toString();
//        }
//    }
//}
//}
