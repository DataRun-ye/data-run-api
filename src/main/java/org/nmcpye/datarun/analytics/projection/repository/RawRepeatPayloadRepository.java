package org.nmcpye.datarun.analytics.projection.repository;

import org.nmcpye.datarun.analytics.projection.dto.RawRepeatPayload;
import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawRepeatPayloadRepository
    extends BaseJpaIdentifiableRepository<RawRepeatPayload, String> {
    List<RawRepeatPayload> findByRepeatPath(String repeatPath, Limit limit);

    List<RawRepeatPayload> findByRepeatUid(String repeatUid, Limit limit);
}
