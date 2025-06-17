package org.nmcpye.datarun.common.uidgenerate;

import de.huxhorn.sulky.ulid.ULID;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for ULID <-> BINARY(16)
 *
 * @author Hamza Assada 09/06/2025 <7amza.it@gmail.com>
 */
@Converter(autoApply = true)
public class UlidBinaryConverter implements AttributeConverter<ULID.Value, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn(ULID.Value attribute) {
        return attribute != null ? attribute.toBytes() : null;
    }

    @Override
    public ULID.Value convertToEntityAttribute(byte[] dbData) {
        return dbData != null ? ULID.fromBytes(dbData) : null;
    }
}

