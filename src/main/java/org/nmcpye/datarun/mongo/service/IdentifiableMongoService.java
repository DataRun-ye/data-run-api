package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.common.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;

public interface IdentifiableMongoService
    <T extends IdentifiableEntity<String>> extends IdentifiableService<T, String> {
}
