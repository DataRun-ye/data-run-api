package org.nmcpye.etl.inventory;

import org.nmcpye.etl.translation.DomainHandler;
import org.nmcpye.etl.translation.FlowConfig;
import org.nmcpye.etl.translation.SubmissionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryDomainHandler implements DomainHandler {

    @Override
    public String getDomainName() { return "INVENTORY"; }

    @Override
    @Transactional
    public void process(SubmissionContext ctx, FlowConfig config) {
        // 1. Resolve Actors based on Config
        Actor source = resolveActor(ctx, config.getMappingRules().getSource());
        Actor dest = resolveActor(ctx, config.getMappingRules().getDestination());

        // 2. Parse Items (using jOOQ or Jackson)
        List<StockItem> items = parseItems(ctx.getJson(), config);

        // 3. Write to CLEAN 'inventory' schema
        // This is pure, specific business data. No JSON blobs.
        inventoryRepository.saveMovement(source, dest, items, ctx.getDate());
    }

    // Helper to abstract the "Rules" you mentioned
    private Actor resolveActor(SubmissionContext ctx, String rule) {
        if ("CURRENT_TEAM".equals(rule)) return ctx.getTeam();
        if ("ASSIGNED_ORG_UNIT".equals(rule)) return ctx.getOrgUnit();
        if (rule.startsWith("json_field:")) return extractFromJson(ctx, rule);
        // ... handle other complex logic here
    }
}
