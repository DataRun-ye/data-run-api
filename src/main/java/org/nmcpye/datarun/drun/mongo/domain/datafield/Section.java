package org.nmcpye.datarun.drun.mongo.domain.datafield;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

public class Section extends DefaultField {
    @Field("fields")
    @JsonDeserialize(contentUsing = FieldDeserializer.class)
//    @JsonDeserialize(using = FieldDeserializer.class)
    private List<AbstractField> fields = new ArrayList<>();

    public List<AbstractField> getFields() {
        return fields;
    }

    public void setFields(List<AbstractField> fields) {
        this.fields = fields;
    }

    public List<AbstractField> flattenFields() {
        List<AbstractField> flatList = new ArrayList<>();
        for (AbstractField field : this.fields) {
            flatList.add(field);
            Section section = ((Section) field);
            if (section.getFields() != null && !section.getFields().isEmpty()) {
                flatList.addAll(section.flattenFields());
            }
        }
        return flatList;
    }
}
