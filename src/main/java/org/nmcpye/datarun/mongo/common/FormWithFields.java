package org.nmcpye.datarun.mongo.common;

import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;

import java.util.List;

/**
 * @author Hamza, 26/03/2025
 */
public interface FormWithFields {
    String getName();

    String getUid();

    List<FormDataElementConf> getFieldsConf();

    void setFieldsConf(List<FormDataElementConf> fields);

    List<FormSectionConf> getSections();

    FormWithFields version(Integer version);

    void setSections(List<FormSectionConf> sections);
}
