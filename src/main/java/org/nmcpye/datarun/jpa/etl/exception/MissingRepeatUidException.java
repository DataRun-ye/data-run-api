package org.nmcpye.datarun.jpa.etl.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.List;

/**
 * @author Hamza Assada
 * @since 10/08/2025
 */
@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingRepeatUidException extends RuntimeException {
    private final List<MissingRepeatUid> details;

    public MissingRepeatUidException(List<MissingRepeatUid> details) {
        super("Missing _id for repeat items");
        this.details = details == null ? Collections.emptyList() : details;
    }

    @Getter
    public static class MissingRepeatUid {
        private final String repeatPath;
        private final int index;

        public MissingRepeatUid(String repeatPath, int index) {
            this.repeatPath = repeatPath;
            this.index = index;
        }

    }
}
