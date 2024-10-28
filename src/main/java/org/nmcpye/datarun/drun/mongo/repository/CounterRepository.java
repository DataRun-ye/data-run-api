package org.nmcpye.datarun.drun.mongo.repository;

import org.nmcpye.datarun.drun.mongo.domain.Counter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CounterRepository extends MongoRepository<Counter, String> {
}
