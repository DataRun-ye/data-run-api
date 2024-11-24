package org.nmcpye.datarun.web.rest.postgres;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.nmcpye.datarun.web.rest.errors.RequestQueryParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/custom")
public abstract class
AbstractRelationalResource<T extends IdentifiableObject<Long>>
    extends AbstractResource<T, Long> {

    private final Logger log = LoggerFactory.getLogger(AbstractRelationalResource.class);

    protected AbstractRelationalResource(IdentifiableRelationalService<T> identifiableService,
                                         IdentifiableRelationalRepository<T> repository) {
        super(identifiableService, repository);
    }

    @Override
    protected IdentifiableRelationalRepository<T> getRepository() {
        return (IdentifiableRelationalRepository<T>) super.getRepository();
    }

    protected Specification<T> buildSpecification(Map<String, Object> params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            params.forEach((key, value) -> {
                if (key.contains(".")) {
                    // Handle nested properties, for example: parent.uid
                    String[] parts = key.split("\\.");
                    Path<Object> path = root.get(parts[0]);
                    for (int i = 1; i < parts.length; i++) {
                        path = path.get(parts[i]);
                    }
                    predicates.add(cb.equal(path, value));
                } else {
                    // Handle simple properties
                    predicates.add(cb.equal(root.get(key), value));
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @GetMapping("entities")
    public ResponseEntity<Page<T>> getEntities(@ParameterObject Pageable pageable,
                                               @RequestParam(name = "paging", required = false, defaultValue = "true")
                                               boolean paging,
                                               @RequestParam(required = false) Map<String, Object> query) {
        query.remove("paging");
        if (!paging) {
            pageable = Pageable.unpaged();
        }

        Specification<T> spec;
        try {
            spec = buildSpecification(query);
        } catch (Exception e) {
            throw new RequestQueryParsingException();
        }
        Page<T> results = getRepository().findAll(spec, pageable);
        return ResponseEntity.ok(results);
    }
//    @GetMapping("")
//    public ResponseEntity<Page<T>> getEntities(@ParameterObject Pageable pageable,
//                                               @RequestParam(name = "paging", required = false, defaultValue = "true")
//                                               boolean paging,
//                                               @RequestParam(required = false) String query,
//                                               HttpServletRequest request,
//                                               HttpServletResponse response/*,
//                                               @RequestParam(required = false) Map<String, Object> query*/) {
//        if (!paging) {
//            pageable = Pageable.unpaged();
//        }
//
//        Specification<T> spec;
//        try {
//            spec = buildSpecification(query);
//        } catch (Exception e) {
//            throw new RequestQueryParsingException();
//        }
//        Page<T> results = getRepository().findAll(spec, pageable);
//        return ResponseEntity.ok(results);
//    }
//
//    protected Map<String, Object> parseQuery(String query) throws IOException {
//        if (query == null) {
//            return new HashMap<>();
//        }
//        ObjectMapper objectMapper = new ObjectMapper();
//        return objectMapper.readValue(query, new TypeReference<Map<String, Object>>() {
//        });
//    }
//
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
