package org.nmcpye.datarun.web.rest.v1.pivot;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nmcpye.datarun.jpa.pivotdata.dto.PivotMetadataResponse;
import org.nmcpye.datarun.jpa.pivotdata.model.DimensionDefinition;
import org.nmcpye.datarun.jpa.pivotdata.model.MeasureDefinition;
import org.nmcpye.datarun.jpa.pivotdata.query.CardinalityService;
import org.nmcpye.datarun.jpa.pivotdata.query.InMemoryPivotRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public class PivotMetadataControllerTest {

    @Test
    void testMetadataControllerReturnsData() {
        InMemoryPivotRegistry registry = new InMemoryPivotRegistry();
        registry.registerDimension(new DimensionDefinition("assignment.org_unit", "Org Unit",
            DimensionDefinition.DataType.STRING, "ou.name", List.of("assignment", "org_unit"),
            DimensionDefinition.MultiSelectStrategy.EXPLODE, 120L), null);
        registry.registerMeasure(new MeasureDefinition("submission.count", "Submission Count", "1", List.of(MeasureDefinition.Aggregation.COUNT)), null);

        // mock cardinality service - cheap stub
        CardinalityService cardinalityService = Mockito.mock(CardinalityService.class);
        when(cardinalityService.getCardinalityHint("assignment.org_unit", null)).thenReturn(120L);

        PivotMetadataController controller = new PivotMetadataController(registry, cardinalityService);
        PivotMetadataResponse resp = controller.getMetadata(null);

        assertNotNull(resp);
        assertNotNull(resp.dimensions);
        assertNotNull(resp.measures);
    }
}
