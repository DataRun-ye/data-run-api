package org.nmcpye.datarun.importer.handler;

import org.nmcpye.datarun.importer.config.IdentifierResolver;
import org.nmcpye.datarun.importer.config.KeyType;
import org.nmcpye.datarun.importer.dto.DataInstanceDto;
import org.nmcpye.datarun.importer.service.ValidationContext;
import org.nmcpye.datarun.jpa.datainstance.DataInstance;
import org.nmcpye.datarun.jpa.datainstance.repository.DataInstanceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */

@Component
public class DataInstanceImportHandler implements EntityImportHandler<DataInstanceDto, DataInstance> {
    private final DataInstanceRepository dataInstanceRepository;
    private final IdentifierResolver<DataInstance> identifierResolver;

    public DataInstanceImportHandler(DataInstanceRepository dataInstanceRepository,
                                     IdentifierResolver<DataInstance> identifierResolver) {
        this.dataInstanceRepository = dataInstanceRepository;
        this.identifierResolver = identifierResolver;
    }

    @Override
    public String getEntityName() {
        return "product";
    }

    @Override
    public Class<DataInstanceDto> getDtoClass() {
        return DataInstanceDto.class;
    }

    @Override
    public void validate(DataInstanceDto dto, ValidationContext ctx, int rowIndex) {
        // 1) Bean Validation (via ValidationContext)
        ctx.validateBean(dto, rowIndex);

        // 2) Custom checks: code non-empty is covered by @NotBlank; verify price positive covered.
        // 3) Key-based lookup (if updating rather than creating)
        if (dto.getKeyType() != null && dto.getKeyValue() != null) {
            KeyType kt;
            try {
                kt = KeyType.valueOf(dto.getKeyType().toUpperCase());
                Optional<DataInstance> existing = identifierResolver.resolve(kt, dto.getKeyValue());
                if (existing.isEmpty()) {
                    ctx.addError(rowIndex, "No existing DataInstance found for " + dto.getKeyType() + " = " + dto.getKeyValue());
                }
            } catch (IllegalArgumentException e) {
                ctx.addError(rowIndex, "Invalid keyType: " + dto.getKeyType());
            }
        }

        // 4) Uniqueness check on code (for new products)
        if (dto.getKeyType() == null && dataInstanceRepository.findByUid(dto.getUid()).isPresent()) {
            ctx.addError(rowIndex, "DataInstance with code '" + dto.getUid() + "' already exists");
        }
    }

    @Override
    public DataInstance toEntity(DataInstanceDto dto, Map<String, Object> resolvedRefs) {
        DataInstance entity;
        if (dto.getKeyType() != null && dto.getKeyValue() != null) {
            KeyType kt = KeyType.valueOf(dto.getKeyType().toUpperCase());
            entity = identifierResolver.resolve(kt, dto.getKeyValue()).orElse(new DataInstance());
        } else {
            entity = new DataInstance();
        }
        entity.setUid(dto.getUid());
        entity.setOrgUnitUid(dto.getOrgUnitUid());
//        entity.setDefaultPriceCents(dto.getDefaultPriceCents());
        return entity;
    }

    @Override
    public void postProcess(DataInstance entity) {
        // e.g., generate a UID if missing (for demonstration)
        if (entity.getUid() == null) {
            entity.setUid(UUID.randomUUID().toString());
        }
    }

    @Override
    public void persistAll(List<DataInstance> entities, boolean dryRun) {
        if (!dryRun) {
            dataInstanceRepository.saveAll(entities);
        }
    }
}
