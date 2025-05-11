package org.nmcpye.datarun.sequencegenerator;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "counters")
public class Counter {
    @Id
    private String id;
    private long sequenceValue;
}
