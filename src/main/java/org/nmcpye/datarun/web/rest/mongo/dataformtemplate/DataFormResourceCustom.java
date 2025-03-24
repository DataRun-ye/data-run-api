package org.nmcpye.datarun.web.rest.mongo.dataformtemplate;

import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.repository.DataFormRepository;
import org.nmcpye.datarun.mongo.service.DataFormService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.utils.FormTemplateSchema;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping("/api/custom/dataForm")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormResourceCustom extends MongoBaseResource<DataForm> {

    final DataFormService dataFormService;
    private final FormTemplateSchema formTemplateSchema;

    public DataFormResourceCustom(DataFormService dataFormService,
                                  DataFormRepository dataFormRepository, FormTemplateSchema formTemplateSchema) {
        super(dataFormService, dataFormRepository);
        this.dataFormService = dataFormService;
        this.formTemplateSchema = formTemplateSchema;
    }

    @Override
    protected String getName() {
        return "dataForm";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataForm> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataForm entity) {

//        var sc = formTemplateSchema.generateSchema(entity);
//        Document document = sc.toDocument();
//        var bson = sc.toDocument().toBsonDocument();
//        var json = document.toJson();
//        var json2 = bson.toJson();
//        log.debug(document.toJson());
//        log.debug(json2);
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(DataForm entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<DataForm> updateEntity(String uid, DataForm entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }
}
