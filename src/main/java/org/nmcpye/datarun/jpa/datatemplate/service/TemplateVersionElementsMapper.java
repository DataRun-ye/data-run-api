package org.nmcpye.datarun.jpa.datatemplate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 25/08/2025
 */
@Service
@RequiredArgsConstructor
public class TemplateVersionElementsMapper {
    private final ObjectMapper mapper;

    @SneakyThrows
    public List<FormDataElementConf> fieldsNodeToList(JsonNode node) {
        if (node == null || !node.isArray()) return Collections.emptyList();
        List<FormDataElementConf> out = new ArrayList<>();
        for (JsonNode n : node) {
//            try {
            out.add(mapper.treeToValue(n, FormDataElementConf.class));
//            } catch (JsonProcessingException e) {
            // log + handle: malformed element -> decide to skip or fail
//            }
        }
        return out;
    }

    public JsonNode listToFieldsNode(List<FormDataElementConf> fields) {
        return mapper.valueToTree(fields);
    }
}
