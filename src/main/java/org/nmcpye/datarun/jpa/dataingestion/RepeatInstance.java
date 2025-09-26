//package org.nmcpye.datarun.jpa.dataingestion;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//
//import java.time.Instant;
//import java.util.UUID;
//
//@Entity
//@Table(name = "repeat_instance")
//public class RepeatInstance {
//    @Id
//    @Column(name = "repeat_instance_id")
//    private UUID repeatInstanceId;
//
//    @Column(name = "submission_id")
//    private UUID submissionId;
//
//    @Column(name = "repeat_path")
//    private String repeatPath;
//
//    @Column(name = "client_rid")
//    private String clientRid;
//
//    @Column(name = "idx")
//    private Integer idx;
//
//    @Column(name = "created_at")
//    private Instant createdAt = Instant.now();
//
//    public static RepeatInstance create(UUID id, UUID submissionId, String repeatPath, String clientRid, int idx) {
//        RepeatInstance r = new RepeatInstance();
//        r.repeatInstanceId = id;
//        r.submissionId = submissionId;
//        r.repeatPath = repeatPath;
//        r.clientRid = clientRid;
//        r.idx = idx;
//        r.createdAt = Instant.now();
//        return r;
//    }
//
//    public UUID getRepeatInstanceId() { return repeatInstanceId; }
//}
