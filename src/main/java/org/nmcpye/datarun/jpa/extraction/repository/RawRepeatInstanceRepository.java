//package org.nmcpye.datarun.jpa.extraction.repository;
//
//import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
//import org.nmcpye.datarun.jpa.extraction.RawRepeatInstance;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.UUID;
//
///**
// * @author Hamza Assada
// * @since 21/09/2025
// */
//@Repository
//public interface RawRepeatInstanceRepository extends BaseJpaIdentifiableRepository<RawRepeatInstance, UUID> {
//    @Query(value = "SELECT rpi.* FROM raw_repeat_instance rpi " +
//        "WHERE rpi.processed = FALSE and rpi.repeat_uid = :repeatUid " +
//        "ORDER BY rpi.repeat_uid LIMIT :pageSize ", nativeQuery = true)
//    List<RawRepeatInstance> findTopUnprocessedByRepeatUid(@Param("repeatUid") String repeatUid, @Param("pageSize")  int pageSize);
//}
