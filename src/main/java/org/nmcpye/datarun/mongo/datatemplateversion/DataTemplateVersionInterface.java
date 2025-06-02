package org.nmcpye.datarun.mongo.datatemplateversion;

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

    DataTemplateVersionInterface sections(List<FormSectionConf> sections);

    DataTemplateVersionInterface fields(List<FormDataElementConf> fields);
}
