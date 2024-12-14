package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;

public interface IdentifiableMongoService
    <T extends Identifiable<String>> extends IdentifiableService<T, String> {
}
