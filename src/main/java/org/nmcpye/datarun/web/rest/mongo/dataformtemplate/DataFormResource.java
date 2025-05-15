//package org.nmcpye.datarun.web.rest.mongo.dataformtemplate;
//
//import org.nmcpye.datarun.mongo.domain.DataForm;
//import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
//import org.nmcpye.datarun.mongo.repository.DataFormRepository;
//import org.nmcpye.datarun.mongo.service.DataFormService;
//import org.nmcpye.datarun.security.AuthoritiesConstants;
//import org.nmcpye.datarun.utils.FormTemplateSchema;
//import org.nmcpye.datarun.web.rest.common.ApiVersion;
//import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
//import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.net.URISyntaxException;
//import java.util.List;
//
//import static org.nmcpye.datarun.web.rest.mongo.dataformtemplate.DataFormResource.CUSTOM;
//import static org.nmcpye.datarun.web.rest.mongo.dataformtemplate.DataFormResource.V1;
//
///**
// * REST controller for managing {@link DataForm}.
// */
//@RestController
//@RequestMapping(value = {CUSTOM, V1})
//@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
//public class DataFormResource extends MongoBaseResource<DataForm> {
//    protected static final String NAME = "/dataForm";
//    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
//    protected static final String V1 = ApiVersion.API_V1 + NAME;
//
//    final DataFormService dataFormService;
//    private final FormTemplateSchema formTemplateSchema;
//    private final FormTemplateProcessor formTemplateProcessor;
//
//    public DataFormResource(DataFormService dataFormService,
//                            DataFormRepository dataFormRepository, FormTemplateSchema formTemplateSchema, FormTemplateProcessor formTemplateProcessor) {
//        super(dataFormService, dataFormRepository);
//        this.dataFormService = dataFormService;
//        this.formTemplateSchema = formTemplateSchema;
//        this.formTemplateProcessor = formTemplateProcessor;
//    }
//
//    @Override
//    protected String getName() {
//        return "dataForm";
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataForm> templates) {
//        log.debug("REST request to saveAll all {}", getName());
//        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
//        templates.stream().map(formTemplateProcessor::validate)
//            .map(formTemplateProcessor::processMetadata)
//            .forEach(t -> this.saveEntity((DataForm) t, summary));
//
//        return ResponseEntity.ok(summary);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataForm formTemplate) {
//
////        var sc = formTemplateSchema.generateSchema(entity);
////        Document document = sc.toDocument();
////        var bson = sc.toDocument().toBsonDocument();
////        var json = document.toJson();
////        var json2 = bson.toJson();
////        log.debug(document.toJson());
////        log.debug(json2);
//        log.debug("REST request to saveOne {}", getName());
//        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
//        final var processedTemplate = formTemplateProcessor.processMetadata(
//            formTemplateProcessor.validate(formTemplate));
//        this.saveEntity((DataForm) processedTemplate, summary);
//
//        return ResponseEntity.ok(summary);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<?> saveReturnSaved(DataForm entity) {
//        return super.saveReturnSaved(entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<Void> deleteByIdUid(String id) {
//        return super.deleteByIdUid(id);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<DataForm> updateEntity(String uid, DataForm entity) throws URISyntaxException {
//        return super.updateEntity(uid, entity);
//    }
//}
