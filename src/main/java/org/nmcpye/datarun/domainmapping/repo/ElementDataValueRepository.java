package org.nmcpye.datarun.domainmapping.repo;

import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElementDataValueRepository {
    List<ElementDataValue> findByEtlRunId(String etlRunId);
}
