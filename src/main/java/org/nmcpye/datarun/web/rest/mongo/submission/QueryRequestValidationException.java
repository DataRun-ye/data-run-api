package org.nmcpye.datarun.web.rest.mongo.submission;

import org.springframework.http.HttpStatus;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;

public class QueryRequestValidationException extends RuntimeException {

    private final String field;
    private final String operator;
    private final String value;

    public QueryRequestValidationException(String message, String field) {
        this(message, field, null, null);
    }

    public QueryRequestValidationException(String message, String field, String operator, String value) {
        super(message);
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public ProblemDetailWithCause toProblemDetail() {
        ProblemDetailWithCause problem = ProblemDetailWithCauseBuilder.instance()
            .withStatus(HttpStatus.BAD_REQUEST.value())
            .withTitle("Query Request Validation Error")
            .withDetail(getMessage())
            .build();

        problem.setProperty("field", field);
        problem.setProperty("operator", operator);
        problem.setProperty("value", value);
        return problem;
    }
}

