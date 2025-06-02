package org.nmcpye.datarun.importer.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.nmcpye.datarun.importer.util.RowError;

import java.util.*;

/**
 * Collects errors per row index (if known). For simplicity, rowIndex is passed by caller.
 *
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
public class ValidationContext {
    private final Map<Integer, List<String>> rowErrors = new HashMap<>();
    private final Validator validator;

    public ValidationContext() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public <T> void validateBean(T dto, int rowIndex) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        for (ConstraintViolation<T> v : violations) {
            addError(rowIndex, v.getPropertyPath() + " " + v.getMessage());
        }
    }

    public void addError(int rowIndex, String message) {
        rowErrors.computeIfAbsent(rowIndex, k -> new ArrayList<>()).add(message);
    }

    public boolean hasErrors() {
        return !rowErrors.isEmpty();
    }

    public List<RowError> getErrors() {
        List<RowError> list = new ArrayList<>();
        rowErrors.forEach((rowIndex, msgs) -> list.add(new RowError(rowIndex, msgs)));
        return list;
    }
}
