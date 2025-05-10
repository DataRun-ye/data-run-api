package org.nmcpye.datarun.mongo.common;

import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;

import java.util.List;

/**
 * @author Hamza Assada, 26/03/2025
 */
public interface FormWithFields {
    String getName();

    String getUid();

    List<FormDataElementConf> getFields();

    List<FormSectionConf> getSections();

    Integer getVersion();
    FormWithFields version(Integer version);

    void setSections(List<FormSectionConf> sections);
    void setFields(List<FormDataElementConf> sections);
}
