## Trade-offs & Recommendations

**Trade-offs**:

| Approach                                      | Complexity      | Reporting Query Simplicity | Data Integrity          | JPA Friendliness                   |
|-----------------------------------------------|-----------------|----------------------------|-------------------------|------------------------------------|
| Current (single table)                        | Medium          | High (single table)        | Low (needs constraints) | Low (complex mapping)              |
| Alternative 1 (split tables)                  | Low (per table) | Low (union required)       | High                    | High                               |
| Alternative 2 (single table with constraints) | Medium          | High                       | High (with constraints) | Medium (with Hibernate extensions) |
| Alternative 3 (embedded root)                 | Low             | Medium (two sources)       | High                    | High                               |

**Recommendation**:

I recommend **Alternative 2 (single table with constraints)** because:

- It preserves the single join point for reporting, which is a key goal.
