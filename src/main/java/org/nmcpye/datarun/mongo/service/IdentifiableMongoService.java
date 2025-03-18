package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;

public interface IdentifiableMongoService
    <T extends IdentifiableEntity<String>> extends IdentifiableService<T, String> {
}
