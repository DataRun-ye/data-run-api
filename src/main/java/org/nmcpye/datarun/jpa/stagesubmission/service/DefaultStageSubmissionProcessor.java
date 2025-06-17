package org.nmcpye.datarun.jpa.stagesubmission.service;

import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.entityinstance.service.EntityHistoryService;
import org.nmcpye.datarun.jpa.entityinstance.service.EntityInstanceService;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.repository.FlowInstanceRepository;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.scopeinstance.service.ScopeInstanceService;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;
import org.nmcpye.datarun.jpa.stagesubmission.exception.SubmissionCreationException;
import org.nmcpye.datarun.jpa.stagesubmission.repository.StageSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
@Service
public class DefaultStageSubmissionProcessor implements StageSubmissionProcessor {
    private final StageSubmissionRepository submissionRepo;
    private final FlowInstanceRepository flowRepo;
    private final EntityInstanceService entitySvc;
    private final EntityHistoryService historySvc;
    private final ScopeInstanceService scopeSvc;

    public DefaultStageSubmissionProcessor(StageSubmissionRepository submissionRepo,
                                           FlowInstanceRepository flowRepo,
                                           EntityInstanceService entitySvc,
                                           EntityHistoryService historySvc,
                                           ScopeInstanceService scopeSvc) {
        this.submissionRepo = submissionRepo;
        this.flowRepo = flowRepo;
        this.entitySvc = entitySvc;
        this.historySvc = historySvc;
        this.scopeSvc = scopeSvc;
    }

    @Transactional
    @Override
    public StageInstance processSubmission(String flowInstanceId,
                                           String stageId,
                                           Map<String, Object> data,
                                           boolean isRepeatable,
                                           String boundEntityTypeId) {
        FlowInstance fi = flowRepo.findById(flowInstanceId)
            .orElseThrow(() -> new IllegalStateException("Unknown flow instance"));
        StageDefinition sd = fi.getFlowType().getStages().stream()
            .filter((s) -> Objects.equals(s.getId(), stageId))
            .findFirst()
            .orElseThrow(() -> new SubmissionCreationException("not found stage definition: " + stageId));
        // 1. Persist Submission
        StageInstance ss = new StageInstance();
        ss.setFlowInstance(fi);
        ss.setStageDefinition(sd);
        ss.setDataTemplateId(sd.getDataTemplateId());
        ss.setInstanceData(data);
        ss.setStatus(StageInstance.SubmissionStatus.SUBMITTED);
        submissionRepo.save(ss);

        // 2. Handle entity binding if configured
        if (boundEntityTypeId != null) {
            // Decide create vs update
            String entityId = entitySvc.upsertEntity(boundEntityTypeId, data);
            // Record history
            historySvc.recordHistory(entityId, fi.getId(), ss.getId(), "STAGE_" + stageId.toUpperCase(), data);
            // Create or update scope instance for this submission
            scopeSvc.createForStage(fi.getId(), ss.getId(), fi.getScopes(), entityId);
        }

        // 3. Update FlowInstance.stageStates
        fi.getStageStates().computeIfAbsent(stageId, k -> new java.util.ArrayList<>()).add(ss.getId());

        // 4. If single mode or last stage, complete
        if (fi.getFlowType().getSubmissionMode() == FlowType.SubmissionMode.SINGLE ||
            /* check last stage condition */ false) {
            fi.setStatus(FlowStatus.DONE);
        }
        flowRepo.save(fi);
        return ss;
    }
}
