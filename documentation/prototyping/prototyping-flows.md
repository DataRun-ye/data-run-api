Let's explore how the dynamic scope configuration model handles different domain scenarios. I'll provide complete
configurations and submissions for each use case, highlighting how the model adapts to diverse requirements.

### Scenario 1: ITNS Campaign Distribution (Household Level)

**Process**: Distribute insecticide-treated nets to households during a campaign

#### FlowType Configuration:

```json-sample
{
  "id": "ITNS_CAMPAIGN",
  "name": "ITNS Distribution Campaign",
  "flowScopeDefinition": {
    "elements": [
      {"key": "campaign", "type": "ENTITY", "entityTypeId": "CAMPAIGN", "required": true, "label": "Campaign"},
      {"key": "distribution-period", "type": "DATE", "required": true, "label": "Distribution Period"},
      {"key": "supervisor", "type": "ENTITY", "entityTypeId": "STAFF", "required": false, "label": "Supervisor"}
    ]
  },
  "stages": [
    {
      "id": "hh-registration",
      "name": "Household Registration",
      "repeatable": true,
      "stageScopeDefinition": {
        "elements": [
          {"key": "village", "type": "ORG_UNIT", "required": true, "label": "Village"},
          {"key": "household", "type": "ENTITY", "entityTypeId": "HOUSEHOLD", "required": true, "label": "Household"},
          {"key": "hh-size", "type": "NUMBER", "required": true, "label": "Household Size"}
        ]
      }
    },
    {
      "id": "net-distribution",
      "name": "Net Distribution",
      "repeatable": true,
      "stageScopeDefinition": {
        "elements": [
          {"key": "household", "type": "ENTITY", "entityTypeId": "HOUSEHOLD", "required": true, "label": "Household"},
          {"key": "distributor", "type": "ENTITY", "entityTypeId": "STAFF", "required": true, "label": "Distributor"},
          {"key": "distribution-date", "type": "DATE", "required": true, "label": "Distribution Date"}
        ]
      }
    }
  ]
}
```

#### Sample Submissions:

```json-sample
// Create Flow Instance
POST /api/flow-instances
{
  "flowTypeId": "ITNS_CAMPAIGN",
  "scope": {
    "coreElements": [
      {"key": "campaign", "entityRef": "CAMPAIGN_2024_MALARIA"},
      {"key": "distribution-period", "dateValue": "2024-06-01"},
      {"key": "supervisor", "entityRef": "STAFF_789"}
    ]
  }
}

// Household Registration Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_ITNS_001",
  "stageDefinitionId": "hh-registration",
  "scope": {
    "coreElements": [
      {"key": "village", "entityRef": "VILLAGE_123"},
      {"key": "household", "entityRef": "HH_456"},
      {"key": "hh-size", "numberValue": 5}
    ]
  },
  "data": {
    "hhHeadName": "John Doe",
    "gpsCoordinates": "-1.234,36.789"
  }
}

// Net Distribution Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_ITNS_001",
  "stageDefinitionId": "net-distribution",
  "scope": {
    "coreElements": [
      {"key": "household", "entityRef": "HH_456"},
      {"key": "distributor", "entityRef": "STAFF_101"},
      {"key": "distribution-date", "dateValue": "2024-06-15"}
    ]
  },
  "data": {
    "netsDistributed": 3,
    "batchNumbers": ["BATCH1", "BATCH2", "BATCH3"]
  }
}
```

### Scenario 2: Inventory Receiving (Warehouse Management)

**Process**: Receive medical supplies into warehouse inventory

#### FlowType Configuration:

```json-sample
{
  "id": "INVENTORY_RECEIVE",
  "name": "Inventory Receiving",
  "flowScopeDefinition": {
    "elements": [
      {"key": "warehouse", "type": "ORG_UNIT", "required": true, "label": "Warehouse"},
      {"key": "receiving-team", "type": "TEAM", "required": true, "label": "Receiving Team"},
      {"key": "invoice-number", "type": "STRING", "required": true, "label": "Invoice #"},
      {"key": "delivery-date", "type": "DATE", "required": true, "label": "Delivery Date"}
    ]
  },
  "stages": [
    {
      "id": "unpack-verify",
      "name": "Unpack & Verification",
      "repeatable": true,
      "stageScopeDefinition": {
        "elements": [
          {"key": "item", "type": "ENTITY", "entityTypeId": "MEDICAL_ITEM", "required": true, "label": "Item"},
          {"key": "batch-number", "type": "STRING", "required": true, "label": "Batch #"},
          {"key": "expiry-date", "type": "DATE", "required": true, "label": "Expiry Date"}
        ]
      }
    },
    {
      "id": "stock-entry",
      "name": "Stock Entry",
      "repeatable": false,
      "stageScopeDefinition": {
        "elements": [
          {"key": "storage-location", "type": "ENTITY", "entityTypeId": "STORAGE_LOC", "required": true, "label": "Storage Location"}
        ]
      }
    }
  ]
}
```

#### Sample Submissions:

```json-sample
// Create Flow Instance
POST /api/flow-instances
{
  "flowTypeId": "INVENTORY_RECEIVE",
  "scope": {
    "coreElements": [
      {"key": "warehouse", "entityRef": "WH_MAIN"},
      {"key": "receiving-team", "entityRef": "TEAM_RECEIVE_1"},
      {"key": "invoice-number", "stringValue": "INV-2024-001"},
      {"key": "delivery-date", "dateValue": "2024-06-10"}
    ]
  }
}

// Unpack & Verification Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_RECEIVE_001",
  "stageDefinitionId": "unpack-verify",
  "scope": {
    "coreElements": [
      {"key": "item", "entityRef": "ITEM_MED_A"},
      {"key": "batch-number", "stringValue": "BATCH-001"},
      {"key": "expiry-date", "dateValue": "2025-12-31"}
    ]
  },
  "data": {
    "quantityReceived": 100,
    "condition": "GOOD",
    "temperatureCheck": "23.5°C"
  }
}

// Stock Entry Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_RECEIVE_001",
  "stageDefinitionId": "stock-entry",
  "scope": {
    "coreElements": [
      {"key": "storage-location", "entityRef": "LOC_SHELF_B3"}
    ]
  },
  "data": {
    "storageConditions": "Ambient",
    "items": [
      {"itemId": "ITEM_MED_A", "quantity": 100}
    ]
  }
}
```

### Scenario 3: Patient Intake (Health Facility)

**Process**: Patient registration and initial assessment at health facility

#### FlowType Configuration:

```json-sample
{
  "id": "PATIENT_INTAKE",
  "name": "Patient Intake Process",
  "flowScopeDefinition": {
    "elements": [
      {"key": "facility", "type": "ORG_UNIT", "required": true, "label": "Health Facility"},
      {"key": "intake-date", "type": "DATE", "required": true, "label": "Intake Date"},
      {"key": "provider", "type": "ENTITY", "entityTypeId": "STAFF", "required": true, "label": "Provider"}
    ]
  },
  "stages": [
    {
      "id": "registration",
      "name": "Patient Registration",
      "repeatable": false,
      "stageScopeDefinition": {
        "elements": [
          {"key": "patient", "type": "ENTITY", "entityTypeId": "PATIENT", "required": true, "label": "Patient"}
        ]
      }
    },
    {
      "id": "vital-signs",
      "name": "Vital Signs Capture",
      "repeatable": false,
      "stageScopeDefinition": {
        "elements": [
          {"key": "patient", "type": "ENTITY", "entityTypeId": "PATIENT", "required": true, "label": "Patient"}
        ]
      }
    },
    {
      "id": "initial-assessment",
      "name": "Initial Assessment",
      "repeatable": false,
      "stageScopeDefinition": {
        "elements": [
          {"key": "patient", "type": "ENTITY", "entityTypeId": "PATIENT", "required": true, "label": "Patient"},
          {"key": "complaint", "type": "STRING", "required": true, "label": "Chief Complaint"}
        ]
      }
    }
  ]
}
```

#### Sample Submissions:

```json-sample
// Create Flow Instance
POST /api/flow-instances
{
  "flowTypeId": "PATIENT_INTAKE",
  "scope": {
    "coreElements": [
      {"key": "facility", "entityRef": "CLINIC_A"},
      {"key": "intake-date", "dateValue": "2024-06-15"},
      {"key": "provider", "entityRef": "DR_SMITH"}
    ]
  }
}

// Patient Registration Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_INTAKE_001",
  "stageDefinitionId": "registration",
  "scope": {
    "coreElements": [
      {"key": "patient", "entityRef": "PATIENT_123"}
    ]
  },
  "data": {
    "name": "Mary Johnson",
    "dob": "1985-04-12",
    "gender": "F",
    "contact": "+1234567890"
  }
}

// Vital Signs Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_INTAKE_001",
  "stageDefinitionId": "vital-signs",
  "scope": {
    "coreElements": [
      {"key": "patient", "entityRef": "PATIENT_123"}
    ]
  },
  "data": {
    "bp": "120/80",
    "temp": "98.6",
    "pulse": "72",
    "resp": "16"
  }
}

// Initial Assessment Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_INTAKE_001",
  "stageDefinitionId": "initial-assessment",
  "scope": {
    "coreElements": [
      {"key": "patient", "entityRef": "PATIENT_123"},
      {"key": "complaint", "stringValue": "Fever and headache for 3 days"}
    ]
  },
  "data": {
    "symptoms": ["fever", "headache", "nausea"],
    "duration": "3 days",
    "severity": "MODERATE"
  }
}
```

### Scenario 4: Field Survey (Agriculture)

**Process**: Crop assessment survey across multiple farms

#### FlowType Configuration:

```json-sample
{
  "id": "CROP_SURVEY",
  "name": "Agricultural Crop Survey",
  "flowScopeDefinition": {
    "elements": [
      {"key": "region", "type": "ORG_UNIT", "required": true, "label": "Region"},
      {"key": "survey-team", "type": "TEAM", "required": true, "label": "Survey Team"},
      {"key": "season", "type": "STRING", "required": true, "label": "Season"}
    ]
  },
  "stages": [
    {
      "id": "farm-assessment",
      "name": "Farm Assessment",
      "repeatable": true,
      "stageScopeDefinition": {
        "elements": [
          {"key": "farm", "type": "ENTITY", "entityTypeId": "FARM", "required": true, "label": "Farm"},
          {"key": "assessment-date", "type": "DATE", "required": true, "label": "Assessment Date"}
        ]
      }
    },
    {
      "id": "summary-report",
      "name": "Regional Summary",
      "repeatable": false,
      "stageScopeDefinition": {
        "elements": [
          {"key": "analyst", "type": "ENTITY", "entityTypeId": "STAFF", "required": true, "label": "Analyst"}
        ]
      }
    }
  ]
}
```

#### Sample Submissions:

```json-sample
// Create Flow Instance
POST /api/flow-instances
{
  "flowTypeId": "CROP_SURVEY",
  "scope": {
    "coreElements": [
      {"key": "region", "entityRef": "REGION_NORTH"},
      {"key": "survey-team", "entityRef": "TEAM_AGRO_1"},
      {"key": "season", "stringValue": "2024-LR"}
    ]
  }
}

// Farm Assessment Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_SURVEY_001",
  "stageDefinitionId": "farm-assessment",
  "scope": {
    "coreElements": [
      {"key": "farm", "entityRef": "FARM_789"},
      {"key": "assessment-date", "dateValue": "2024-06-20"}
    ]
  },
  "data": {
    "cropType": "Maize",
    "growthStage": "Tasseling",
    "pestIncidence": "LOW",
    "yieldEstimate": 4.2
  }
}

// Summary Report Stage
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_SURVEY_001",
  "stageDefinitionId": "summary-report",
  "scope": {
    "coreElements": [
      {"key": "analyst", "entityRef": "AGRO_SPEC_1"}
    ]
  },
  "data": {
    "avgYield": 3.8,
    "diseasePrevalence": "15%",
    "recommendations": "Increase fertilizer application"
  }
}
```

### Advantages of the Model:

1. **Uniform Configuration**:
    - Same pattern works for healthcare, inventory, surveys, campaigns
    - Configuration defines both core elements and data templates

2. **Dynamic Scope Binding**:
    - Flow-level: Campaigns, warehouses, facilities
    - Stage-level: Households, items, patients, farms
    - Mixed entity/value handling

3. **Query Efficiency**:
   ```sql
   -- Find all ITNS distributions in Village 123
   SELECT * FROM scope_core_element
   WHERE key = 'village' AND entity_ref = 'VILLAGE_123'
   
   -- Find all inventory receipts for a specific batch
   SELECT * FROM scope_core_element
   WHERE key = 'batch-number' AND string_value = 'BATCH-001'
   ```

4. **Domain-Specific Extensions**:
   ```java
   @Entity
   @DiscriminatorValue("HEALTH")
   public class HealthFlowScope extends FlowScope {
       @Column(name = "emergency_level")
       private String emergencyLevel;
   }
   ```

5. **Validation Integration**:
   ```java
   validator.checkRequired(flowType, submission);
   ```

### Potential Challenges & Mitigations:

1. **Complex Reporting**:
    - **Solution**: Create materialized views for common queries
      ```sql
      CREATE MATERIALIZED VIEW mv_inventory_receipts AS
      SELECT 
          f.id AS flow_id,
          MAX(CASE WHEN ce.key = 'warehouse' THEN ce.entity_ref END) AS warehouse,
          MAX(CASE WHEN ce.key = 'invoice-number' THEN ce.string_value END) AS invoice,
          s.data->>'quantityReceived' AS quantity
      FROM flow_instance f
      JOIN scope_core_element ce ON ce.scope_id = f.scope_id
      JOIN stage_submission s ON s.flow_instance_id = f.id
      GROUP BY f.id, s.id
      ```

2. **Performance at Scale**:
    - **Solution**: Partition scope tables by date/type
      ```sql
      PARTITION BY RANGE (scope_date)
      ```

3. **Complex Configuration UI**:
    - **Solution**: Domain-specific configuration templates
      ```json
      "template": "INVENTORY_RECEIVE"
      ```

4. **Data Migration**:
    - **Solution**: Flyway migrations for new core elements
      ```sql
      ALTER TABLE scope_core_element ADD COLUMN string_value2 VARCHAR;
      ```

The model demonstrates excellent flexibility across domains while maintaining query efficiency through:

1. Configurable core elements instead of fixed columns
2. Type-safe value storage
3. Entity reference support
4. Inheritable scope properties
5. Domain-specific extensions

All scenarios were easily implemented with consistent patterns, proving the model's adaptability to different workflow
requirements while preserving data integrity and query performance.
