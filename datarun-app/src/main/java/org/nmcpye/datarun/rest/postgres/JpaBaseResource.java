package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.query.JpaQueryBuilder;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.common.QuerySpecification;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public abstract class JpaBaseResource<T extends JpaAuditableObject>
    extends BaseReadWriteResource<T, Long> implements QuerySpecification<T> {

    private final JpaAuditableObjectService<T> jpaAuditableObjectService;

    @Autowired
    private JpaQueryBuilder<T> jpaQueryBuilder;

    protected JpaBaseResource(JpaAuditableObjectService<T> jpaAuditableObjectService,
                              JpaAuditableRepository<T> repository) {
        super(jpaAuditableObjectService, repository);
        this.jpaAuditableObjectService = jpaAuditableObjectService;
    }

    @Override
    protected JpaQueryBuilder<T> getQueryBuilder() {
        return jpaQueryBuilder;
    }

    @Override
    protected JpaAuditableRepository<T> getRepository() {
        return (JpaAuditableRepository<T>) super.getRepository();
    }

    @Override
    protected Page<T> getList(QueryRequest queryRequest, String jsonQueryBody) {
        Specification<T> spec;
        try {
            spec = buildQuerySpecification(queryRequest);
        } catch (Exception e) {
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2050, e.getMessage()));
        }
        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
            try {
                FilterExpression filterExpression = UnifiedQueryParser.parse(jsonQueryBody);
                spec = spec.and(getQueryBuilder().buildQuery(List.of(filterExpression)));
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
            }
        }
        return jpaAuditableObjectService.findAllByUser(spec, queryRequest);
    }

    @GetMapping("entities")
    protected ResponseEntity<PagedResponse<?>> getEntities(
        QueryRequest queryRequest) {
        Pageable pageable = queryRequest.getPageable();

        Specification<T> spec;
        try {
            spec = buildQuerySpecification(queryRequest);
        } catch (Exception e) {
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2050, e.getMessage()));
        }

        Page<T> processedPage = getRepository().findAll(spec, pageable);

        String next = createNextPageLink(processedPage);

        PagedResponse<T> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }

//    protected Specification<T> buildSpecification(/*Map<String, Object>*/ String query) throws IOException {
//        Map<String, Object> filters = parseQuery(query);
//
//        Specification<T> spec = Specification.where(null); // Start with empty specification
//
//        for (Map.Entry<String, Object> entry : filters.entrySet()) {
//            String field = entry.getKey();
//            Object value = entry.getValue();
//
//            // Check if the value is a logical operation (AND/OR) or a comparison operator
//            if (field.equals("or")) {
//                spec = spec.or(buildLogicalSpecification((List<Map<String, Object>>) value, "or"));
//            } else if (field.equals("and")) {
//                spec = spec.and(buildLogicalSpecification((List<Map<String, Object>>) value, "and"));
//            } else {
//                spec = spec.and(buildFieldSpecification(field, value));
//            }
//        }
//
//        return spec;
//    }
//
//    private Specification<T> buildLogicalSpecification(List<Map<String, Object>> conditions, String logic) {
//        Specification<T> spec = Specification.where(null);
//
//        for (Map<String, Object> condition : conditions) {
//            for (Map.Entry<String, Object> entry : condition.entrySet()) {
//                if (logic.equals("or")) {
//                    spec = spec.or(buildFieldSpecification(entry.getKey(), entry.getValue()));
//                } else {
//                    spec = spec.and(buildFieldSpecification(entry.getKey(), entry.getValue()));
//                }
//            }
//        }
//
//        return spec;
//    }
//
////    private Specification<T> buildFieldSpecification(String field, Object value) {
////        if (value instanceof Map) {
////            Map<String, Object> operatorMap = (Map<String, Object>) value;
////            String operator = (String) operatorMap.get("operator");
////            Object val = operatorMap.get("value");
////
////            switch (operator) {
////                case "eq":
////                    return (root, query, criteriaBuilder) ->
////                        criteriaBuilder.equal(root.get(field), val.toString());
////                case "gt":
////                    return (root, query, criteriaBuilder) ->
////                        criteriaBuilder.greaterThan(root.get(field), val.toString());
////                case "lt":
////                    return (root, query, criteriaBuilder) ->
////                        criteriaBuilder.lessThan(root.get(field), val.toString());
////                case "in":
////                    return (root, query, criteriaBuilder) ->
////                        root.get(field).in((List<?>) val);
////                default:
////                    return (root, query, criteriaBuilder) ->
////                        criteriaBuilder.equal(root.get(field), val.toString());
////            }
////        } else {
////            return (root, query, criteriaBuilder) ->
////                criteriaBuilder.equal(root.get(field), value.toString());
////        }
////    }
//
//    private Specification<T> buildFieldSpecification(String field, Object value) {
//        // If value is just a raw value (no operator specified), assume "eq"
//        if (!(value instanceof Map)) {
//            return (root, query, criteriaBuilder) ->
//                criteriaBuilder.equal(root.get(field), value.toString());
//        }
//
//        // Otherwise, assume the value contains an operator
//        Map<String, Object> operatorMap = (Map<String, Object>) value;
//
//        String operator = operatorMap.keySet().iterator().next();
//        Object val = operatorMap.get(operator);
//
//        switch (operator) {
//            case "eq":
//                return (root, query, criteriaBuilder) ->
//                    criteriaBuilder.equal(root.get(field), val.toString());
//            case "gt":
//                return (root, query, criteriaBuilder) ->
//                    criteriaBuilder.greaterThan(root.get(field), val.toString());
//            case "lt":
//                return (root, query, criteriaBuilder) ->
//                    criteriaBuilder.lessThan(root.get(field), val.toString());
//            case "in":
//                return (root, query, criteriaBuilder) ->
//                    root.get(field).in((List<?>) val);
//            default:
//                return (root, query, criteriaBuilder) ->
//                    criteriaBuilder.equal(root.get(field), val.toString());
//        }
//    }
//
//    private Specification<T> buildFieldSpecificationWithPriority(Object value) {
//        return (root, query, criteriaBuilder) -> {
//            Predicate uidPredicate = criteriaBuilder.equal(root.get("uid"), value.toString());
//            Predicate idPredicate = criteriaBuilder.equal(root.get("id"), value);
//            Predicate codePredicate = criteriaBuilder.equal(root.get("code"), value.toString());
//
//            return criteriaBuilder.or(uidPredicate, idPredicate, codePredicate);
//        };
//    }
}
