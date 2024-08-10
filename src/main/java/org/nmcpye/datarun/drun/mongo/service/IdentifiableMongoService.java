package org.nmcpye.datarun.drun.mongo.service;

import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;

public interface IdentifiableMongoService
    <T extends IdentifiableObject<String>> extends IdentifiableService<T, String> {
}
