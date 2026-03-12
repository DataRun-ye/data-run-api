package org.nmcpye.datarun.common.uidgenerate;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;

public class FlexibleUuidConverter implements Converter<String, UUID> {

    @Override
    public UUID convert(String source) {
        return CodeGenerator.resolveToUuid(source);
    }
}
