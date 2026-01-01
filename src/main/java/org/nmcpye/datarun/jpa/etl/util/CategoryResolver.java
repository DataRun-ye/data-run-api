//package org.nmcpye.datarun.jpa.etl.util;
//
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
//import org.nmcpye.datarun.jpa.etl.model.CategoryResolutionResult;
//import org.springframework.stereotype.Service;
//
///**
// * Resolves a category raw value (id or code) to a canonical id + kind, and provides helper to map option codes.
// *
// * @author Hamza Assada - 7amza.it@gmail.com
// * @since 19/08/2025
// */
//@Service
//@RequiredArgsConstructor
//public class CategoryResolver {
//    private final ReferenceResolver referenceResolver;
//
//    /**
//     * Resolve a raw category candidate (String or domain object) into canonical id and kind.
//     * Returns a CategoryResolutionResult or throws InvalidCategoryValueException if cannot resolve.
//     */
//    public CategoryResolutionResult resolveCategory(Object rawValue, FormDataElementConf categoryElement) {
//        if (rawValue == null) return null;
//        final var resolveResult = referenceResolver
//            .resolveReference(rawValue, categoryElement);
//        return CategoryResolutionResult.builder()
//                .uid(resolveResult.getUid())
//                .name(resolveResult.getName())
//                .label(resolveResult.getLabel())
//                .kind(resolveResult.getKind())
//                .build();
//    }
//}
