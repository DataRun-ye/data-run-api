package org.nmcpye.datarun.datatemplateversion;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

import java.util.List;

/**
 * @author Hamza Assada, 26/03/2025
 */
public interface DataTemplateVersionInterface {
//    String getName();

    String getUid();

    List<FormDataElementConf> getFields();

    List<FormSectionConf> getSections();

    void setSections(List<FormSectionConf> sections);
    void setFields(List<FormDataElementConf> fields);
}
