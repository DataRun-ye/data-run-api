package org.nmcpye.datarun.sequencegenerator.repository;

import org.nmcpye.datarun.mongo.domain.Counter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CounterRepository extends MongoRepository<Counter, String> {
}
