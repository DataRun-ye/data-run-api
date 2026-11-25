package org.nmcpye.datarun.web.v2.problem;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProblemCodeTest {

    @Test
    void everyCodeIsUniqueKebabCaseWithAStandardErrorStatusAndATagTypeUri() {
        Set<String> seen = new HashSet<>();
        for (ProblemCode code : ProblemCode.values()) {
            assertThat(seen.add(code.code())).as("unique %s", code.code()).isTrue();
            assertThat(code.code()).matches("[a-z]+(-[a-z]+)*");
            assertThat(HttpStatus.resolve(code.status())).as("status of %s", code).isNotNull();
            assertThat(code.status()).isBetween(400, 599);
            assertThat(code.title()).isNotBlank().doesNotEndWith(".");
            assertThat(code.type()).hasToString("tag:nmcpye.org,2026:datarun/problem/" + code.code());
            assertThat(ProblemCode.byCode(code.code())).isSameAs(code);
        }
        assertThat(ProblemCode.codes()).hasSize(ProblemCode.values().length);
    }

    @Test
    void aStatusFallsBackOnItsGenericCodeOrOnTheFamilyGeneric() {
        assertThat(ProblemCode.forStatus(404)).isEqualTo(ProblemCode.NOT_FOUND);
        assertThat(ProblemCode.forStatus(405)).isEqualTo(ProblemCode.METHOD_NOT_ALLOWED);
        assertThat(ProblemCode.forStatus(422)).isEqualTo(ProblemCode.VALIDATION_FAILED);
        assertThat(ProblemCode.forStatus(418)).isEqualTo(ProblemCode.BAD_REQUEST);
        assertThat(ProblemCode.forStatus(502)).isEqualTo(ProblemCode.INTERNAL_ERROR);
    }

    @Test
    void anUnknownCodeIsAProgrammingError() {
        assertThatThrownBy(() -> ProblemCode.byCode("nope")).isInstanceOf(IllegalArgumentException.class);
    }
}
