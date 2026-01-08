I need you to read the attached code and my notes carefully. and review for any spots in the current code that needs
clean up before moving forward, this is the core components, for brevity, I omitted others that also was implemented. 
if needed we need to have the exact minimal changes to existing services.

I hava the `BindingResolver` this was created early on, and never reviewd it with the point of view of the overall
recent updated decisions we made along the way after it.

```java

@Service
@RequiredArgsConstructor
public class BindingResolver {

    private final DSLContext dsl;

    /**
     * Resolves the list of PartySets to apply based on precedence.
     *
     * @param templateFallbackPartySetId The 'partySetRef' from the vocabulary/template (Level 3 fallback).
     */
    public List<BindingResult> resolveBindings(
        String assignmentId,
        String vocabularyId,
        String role,
        String userId,
        UUID templateFallbackPartySetId
    ) {
        // 1. Resolve User's Principals (User ID + Team IDs + Group IDs)
        // We do this first to check "Level 1" precedence.
        Set<String> principalIds = getUserPrincipals(userId);

        // 2. LEVEL 1: Check Principal-Scoped Bindings
        // (Assignment + Role + [Vocab?] + Principal)
        List<BindingResult> principalBindings = fetchBindings(assignmentId, vocabularyId, role, principalIds, true);
        if (!principalBindings.isEmpty()) {
            return principalBindings;
        }

        // 3. LEVEL 2: Check Assignment-Global Bindings
        // (Assignment + Role + [Vocab?] + No Principal)
        List<BindingResult> globalBindings = fetchBindings(assignmentId, vocabularyId, role, null, false);
        if (!globalBindings.isEmpty()) {
            return globalBindings;
        }

        // 4. LEVEL 3: Template Fallback
        if (templateFallbackPartySetId != null) {
            return List.of(new BindingResult(
                templateFallbackPartySetId,
                CombineMode.UNION,
                "Template Fallback (partySetRef)"
            ));
        }

        // 5. LEVEL 4: Assignment Default
        UUID assignmentDefaultId = dsl.select(ASSIGNMENT.DEFAULT_PARTY_SET_ID)
            .from(ASSIGNMENT)
            .where(ASSIGNMENT.ID.eq(assignmentId))
            .fetchOneInto(UUID.class);

        if (assignmentDefaultId != null) {
            return List.of(new BindingResult(
                assignmentDefaultId,
                CombineMode.UNION,
                "Assignment Default"
            ));
        }

        // 6. No bindings found (return empty list, effectively no parties)
        return Collections.emptyList();
    }

    public List<BindingResult> resolveBindings(String assignmentId, String vocabularyId, String role) {
        return fetchBindings(assignmentId, vocabularyId, role, null, false);
    }

    private List<BindingResult> fetchBindings(
        String assignmentId,
        String vocabularyId,
        String role,
        Set<String> principalIds,
        boolean usePrincipals
    ) {
        var query = dsl.select(
                ASSIGNMENT_PARTY_BINDING.PARTY_SET_ID,
                ASSIGNMENT_PARTY_BINDING.COMBINE_MODE,
                ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID,
                ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID
            )
            .from(ASSIGNMENT_PARTY_BINDING)
            .where(ASSIGNMENT_PARTY_BINDING.ASSIGNMENT_ID.eq(assignmentId))
            .and(ASSIGNMENT_PARTY_BINDING.NAME.eq(role));

        // Filter by Principals or NULL
        if (usePrincipals && principalIds != null && !principalIds.isEmpty()) {
            query = query.and(ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID.in(principalIds));
        } else {
            query = query.and(ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID.isNull());
        }

        // Handle Vocabulary Specificity
        // We want rows that match our Vocab OR are NULL.
        if (vocabularyId != null) {
            query = query.and(
                ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID.eq(vocabularyId)
                    .or(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID.isNull())
            );
        } else {
            query = query.and(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID.isNull());
        }

        // Fetch all candidates
        var results = query.fetch();

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // In-Memory Sorting for Specificity (avoiding complex SQL sorting for now)
        // Rule: Specific Vocab > Null Vocab.
        // If we have Mixed Specificity (some match vocab, some are null),
        // the "Specific Vocabulary" rule usually strictly wins in config systems.
        // Strategy: If *any* row matches the specific Vocabulary, discard the Null-Vocab rows.
        boolean hasSpecificVocabMatch = results.stream()
            .anyMatch(r -> vocabularyId != null &&
                vocabularyId.equals(r.get(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID)));

        return results.stream()
            .filter(r -> {
                if (!hasSpecificVocabMatch) return true; // Keep all if no specific winner
                return vocabularyId != null && vocabularyId.equals(r.get(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID));
            })
            .map(r -> new BindingResult(
                r.get(ASSIGNMENT_PARTY_BINDING.PARTY_SET_ID),
                // Ensure DB stores 'UNION'/'INTERSECT'
                CombineMode.valueOf(r.get(ASSIGNMENT_PARTY_BINDING.COMBINE_MODE) != null ?
                    r.get(ASSIGNMENT_PARTY_BINDING.COMBINE_MODE) : "UNION"),
                determineProvenance(r, usePrincipals)
            ))
            .collect(Collectors.toList());
    }

    private String determineProvenance(Record r, boolean isPrincipal) {
        if (isPrincipal) return "Principal Binding: " + r.get(ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID);
        return "Assignment Global Binding";
    }

    /**
     * Helper to gather all IDs representing the user (User ID + Team IDs + Group IDs).
     */
    private Set<String> getUserPrincipals(String userId) {
        Set<String> principals = new java.util.HashSet<>();
        principals.add(userId);

        final var p = dsl.select(TEAM_USER.TEAM_ID)
            .from(TEAM_USER)
            .join(TEAM).on(TEAM_USER.TEAM_ID.eq(TEAM.ID))
            .where(TEAM_USER.USER_ID.eq(userId))
            .fetchInto(String.class);
        // Add Teams
        principals.addAll(dsl.select(TEAM_USER.TEAM_ID)
            .from(TEAM_USER)
            .join(TEAM).on(TEAM_USER.TEAM_ID.eq(TEAM.ID))
            .where(TEAM_USER.USER_ID.eq(userId).and(TEAM.DISABLED.isFalse().or(TEAM.DISABLED.isNull())))
            .fetchInto(String.class));

        // Add User Groups
        principals.addAll(dsl.select(USER_GROUP_USERS.USER_GROUP_ID)
            .from(USER_GROUP_USERS)
            .join(USER_GROUP).on(USER_GROUP_USERS.USER_GROUP_ID.eq(USER_GROUP.ID))
            .where(USER_GROUP_USERS.USER_ID.eq(userId).and(USER_GROUP.DISABLED.isFalse()
                .or(USER_GROUP.DISABLED.isNull())))
            .fetchInto(String.class));

        return principals;
    }
}
```

I have a `PartyResolutionEngine` check out its current implementation:

```java

@Service
public class PartyResolutionEngine {

    private final Map<PartySetKind, PartySetStrategy> strategies;

    public PartyResolutionEngine(List<PartySetStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(PartySetStrategy::getKind, Function.identity()));
    }

    public List<ResolvedParty> executeStrategy(PartySetKind kind,
                                               UUID partySetId,
                                               String spec,
                                               boolean isMaterialized,
                                               PartyResolutionRequest request) {
        PartySetStrategy strategy = strategies.get(kind);

        if (strategy == null) {
            throw new UnsupportedOperationException("No strategy found for kind: " + kind);
        }

        return strategy.resolve(partySetId, spec, isMaterialized, request);
    }
}
```

I have `NamedQueryProvider` it is static hardcoded for now, they're not saved externally to make things initially
simple:

```java

@Component
public class NamedQueryProvider {

    private final DSLContext dsl;
    private final Map<String, BiFunction<DSLContext, Map<String, Object>, SelectConditionStep<?>>> queries;

    public NamedQueryProvider(DSLContext dsl) {
        this.dsl = dsl;
        this.queries = Map.of(
            // Query 1: Find children of a specific Parent Org Unit
            // Spec: { "sqlKey": "FIND_ORG_CHILDREN", "params": { "parentId": "context_region_id" } }
            "FIND_ORG_CHILDREN", (ctx, params) -> {
                UUID parentId = getUuid(params, "parentId");
                if (parentId == null) return null; // Or throw, or return empty condition

                // We join Party to OrgUnit to filter by parent
                return ctx.select(PARTY.asterisk())
                    .from(PARTY)
                    .where(PARTY.PARENT_ID.eq(parentId));
            },

            // Query 2: Find Teams managing a specific Team (e.g. for escalation)
            "FIND_MANAGING_TEAMS", (ctx, params) -> {
                UUID teamId = getUuid(params, "targetTeamId");
                // ... logic ...
                return ctx.select(PARTY.asterisk())
                    .from(PARTY)
                    .where(DSL.falseCondition()); // stub
            },
            // Query 2: Example of finding parties of a certain type with a name like a parameter.
            // Spec Params: { "partyType": "TEAM", "nameQuery": "search-term" }
            "FIND_PARTIES_BY_TYPE_AND_NAME", (ctx, params) -> {
                String partyType = getString(params, "partyType");
                String nameQuery = getString(params, "nameQuery");
                if (partyType == null || nameQuery == null) return null;

                return dsl.selectFrom(PARTY)
                    .where(PARTY.TYPE.eq(partyType))
                    .and(PARTY.NAME.likeIgnoreCase("%" + nameQuery + "%"));
            }
        );
    }

    public SelectConditionStep<?> getQuery(String queryName, Map<String, Object> resolvedParams) {
        if (!queries.containsKey(queryName)) {
            throw new IllegalArgumentException("Unknown named query: " + queryName);
        }
        return queries.get(queryName).apply(dsl, resolvedParams);
    }

    // Helper methods to safely extract and cast parameters
    private UUID getUuid(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val instanceof String s && !s.isBlank()) return UUID.fromString(s);
        if (val instanceof UUID u) return u;
        return null;
    }

    private String getString(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return (val != null) ? val.toString() : null;
    }
}
```

I also have multiple strategies fo `PartySetStrategy` i.e `StaticPartySetStrategy`, `TagFilterPartySetStrategy`,
`OrgTreePartySetStrategy`, `QueryPartySetStrategy`:

```java
public interface PartySetStrategy {
    PartySetKind getKind();

    List<ResolvedParty> resolve(UUID partySetId,
                                String spec,
                                boolean isMaterialized,
                                PartyResolutionRequest request);

    default <R extends org.jooq.Record> SelectConditionStep<R> applySinceFilter(SelectConditionStep<R> query,
                                                                                Instant since) {
        if (since == null) return query;
        return query
            .and(PARTY.LAST_MODIFIED_DATE.greaterThan(LocalDateTime.ofInstant(since, ZoneId.systemDefault())));
    }
}

@Component
@RequiredArgsConstructor
public class StaticPartySetStrategy implements PartySetStrategy {

    private final DSLContext dsl;
    private final PartySecurityFilter securityFilter; // Injected
    private final ObjectMapper objectMapper;
    private final JooqMapper jooqMapper;

    @Override
    public PartySetKind getKind() {
        return PartySetKind.STATIC;
    }

    @Override
    public List<ResolvedParty> resolve(UUID partySetId,
                                       String spec,
                                       boolean isMaterialized,
                                       PartyResolutionRequest request) {

        // 1. Base Condition: Must belong to this specific set
        Condition whereCondition = PARTY_SET_MEMBER.PARTY_SET_ID.eq(partySetId);

        // 2. Apply Search (if provided)
        // We check name or code via case-insensitive search
        if (request.getSearchQuery() != null && !request.getSearchQuery().isBlank()) {
            String term = "%" + request.getSearchQuery().trim() + "%";
            whereCondition = whereCondition.and(
                PARTY.NAME.likeIgnoreCase(term)
                    .or(PARTY.CODE.likeIgnoreCase(term))
            );
        }

        var query = dsl.select(
                PARTY.ID,
                PARTY.UID,
                PARTY.TYPE,
                PARTY.NAME,
                PARTY.CODE,
                PARTY.PROPERTIES_MAP,
                PARTY.SOURCE_TYPE
            )
            .from(PARTY)
            .join(PARTY_SET_MEMBER).on(PARTY.ID.eq(PARTY_SET_MEMBER.PARTY_ID))
            .where(whereCondition);

        // --- APPLY modified since FILTER ---
        query = applySinceFilter(query, request.getSince());

        // --- APPLY SECURITY FILTER ---
        var securedQuery = securityFilter.apply(query, request.getUserId(), isMaterialized);

        // 3. Execute Query
        return securedQuery
            .orderBy(PARTY.NAME.asc())
            .limit(request.getLimit())
            .offset(request.getOffset())
            .fetch(jooqMapper::mapPartyRecord);
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class TagFilterPartySetStrategy implements PartySetStrategy {

    private final DSLContext dsl;
    private final PartySecurityFilter securityFilter;
    private final ObjectMapper objectMapper;
    private final JooqMapper jooqMapper;

    @Override
    public PartySetKind getKind() {
        return PartySetKind.TAG_FILTER;
    }

    @Override
    public List<ResolvedParty> resolve(UUID partySetId,
                                       String specJson,
                                       boolean isMaterialized,
                                       PartyResolutionRequest request) {
        try {
            // 1. Parse Spec
            // Expected Spec: { "tags": ["tag1", "tag2"], "types": ["ORG_UNIT", "TEAM"] }
            JsonNode spec = objectMapper.readTree(specJson);

            List<String> requiredTags = new ArrayList<>();
            JsonNode tagsNode = spec.path("tags");
            if (tagsNode.isArray()) {
                tagsNode.forEach(node -> requiredTags.add(node.asText()));
            }

            List<String> requiredTypes = new ArrayList<>();
            JsonNode typesNode = spec.path("types");
            if (typesNode.isArray()) {
                typesNode.forEach(node -> requiredTypes.add(node.asText()));
            }

            // 2. Build the base query
            var query = dsl.selectFrom(PARTY)
                // 3. Apply Tag Filter Condition
                // This query uses the JSONB @> operator, which is highly efficient with a GIN index.
                .where(!requiredTags.isEmpty() ? PARTY.TAGS.contains(
                    JSONB.valueOf(objectMapper.writeValueAsString(requiredTags))) :
                    DSL.falseCondition());

            // 4. Apply Type Filter Condition (if specified)
            if (!requiredTypes.isEmpty()) {
                query = query.and(PARTY.SOURCE_TYPE.in(requiredTypes));
            }

            // 5. Apply Search Filter (if provided)
            if (request.getSearchQuery() != null && !request.getSearchQuery().isBlank()) {
                String term = "%" + request.getSearchQuery().trim() + "%";
                query = query.and(PARTY.NAME.likeIgnoreCase(term).or(PARTY.CODE.likeIgnoreCase(term)));
            }

            // --- APPLY modified since FILTER ---
            query = applySinceFilter(query, request.getSince());

            // 6. Apply Security Filter
            var securedQuery = securityFilter.apply(query, request.getUserId(),
                isMaterialized);

            // 7. Execute, Paginate, and Map
            return securedQuery
                .orderBy(PARTY.NAME.asc())
                .limit(request.getLimit())
                .offset(request.getOffset())
                .fetch(jooqMapper::mapPartyRecord);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid JSON spec for TAG_FILTER PartySet " + partySetId, e);
        }
    }
}

// and similarly the other strategies `OrgTreePartySetStrategy`, `QueryPartySetStrategy`, omitted for brevity
```

I also have ``:

```java

@Service
@RequiredArgsConstructor
public class PartyResolutionService {

    private final BindingResolver bindingResolver;
    private final PartyResolutionEngine engine;
    private final DSLContext dsl; // Used for the simple config lookup

    @Transactional(readOnly = true)
    public List<ResolvedParty> resolveParties(PartyResolutionRequest request) {

        // 1. Resolve Bindings (The "Brain")
        // Note: You might need to look up templateFallbackPartySetId from the Vocabulary ID first.
        // For MVP, passing null for fallback.
        List<BindingResult> bindings = bindingResolver.resolveBindings(
            request.getAssignmentId(),
            request.getVocabularyId(),
            request.getRole(),
            request.getUserId(),
            null // TODO: Look up data_template.party_set_ref if needed later
        );

        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Execute Strategies & Combine Results
        // We start with the results of the first binding, then merge subsequent ones.
        List<ResolvedParty> accumulatedParties = new ArrayList<>();
        boolean firstPass = true;

        for (BindingResult binding : bindings) {
            // A. Fetch Config (Kind + Spec)
            // In a real app, cache this lookup.
            var config = dsl.select(PARTY_SET.KIND, PARTY_SET.SPEC, PARTY_SET.IS_MATERIALIZED)
                .from(PARTY_SET)
                .where(PARTY_SET.ID.eq(binding.getPartySetId()))
                .fetchOne();

            if (config == null) continue; // Should not happen given referential integrity

            PartySetKind kind = PartySetKind.valueOf(config.value1()); // Enum mapping
            String spec = config.value2().data(); // JSONB string

            // Default to TRUE (secure) if null, for safety
            boolean isMaterialized = config.value3() == null || config.value3();

            // B. Execute Strategy
            List<ResolvedParty> currentSetParties = engine.executeStrategy(
                kind,
                binding.getPartySetId(),
                spec,
                isMaterialized,
                request
            );

            // C. Combine (Union / Intersect)
            if (firstPass) {
                accumulatedParties.addAll(currentSetParties);
                firstPass = false;
            } else {
                accumulatedParties = applyCombination(
                    accumulatedParties,
                    currentSetParties,
                    binding.getCombineMode()
                );
            }
        }

        // 3. Final Polish (Deduplication)
        // UNION usually implies uniqueness. INTERSECT definitely does.
        // We assume ResolvedParty.equals/hashCode relies on ID.
        return accumulatedParties.stream().distinct().collect(Collectors.toList());
    }

    private List<ResolvedParty> applyCombination(
        List<ResolvedParty> existing,
        List<ResolvedParty> newParties,
        CombineMode mode
    ) {
        if (mode == CombineMode.INTERSECT) {
            // Only keep parties present in both lists
            // O(N*M) naive, but lists are usually page-sized (small).
            // Optimizable with Sets if lists are large.
            Set<UUID> newIds = newParties.stream()
                .map(ResolvedParty::getId)
                .collect(Collectors.toSet());

            return existing.stream()
                .filter(p -> newIds.contains(p.getId()))
                .collect(Collectors.toList());
        }

        // Default: UNION
        List<ResolvedParty> result = new ArrayList<>(existing);
        result.addAll(newParties);
        return result;
    }
}


@Service
@RequiredArgsConstructor
@Slf4j
public class JooqMapper {
    private final ObjectMapper objectMapper;

    public ResolvedParty mapPartyRecord(org.jooq.Record r) {
        Map<String, Object> properties = new HashMap<>();
        try {
            properties = objectMapper.readValue(r.get(PARTY.PROPERTIES_MAP).data(),
                new TypeReference<Map<String, Object>>() {
                });
        } catch (JsonProcessingException e) {
            log.error("error reading properties map", e);
        }

        return ResolvedParty.builder()
            .id(r.get(PARTY.ID))
            .uid(r.get(PARTY.UID))
            .code(r.get(PARTY.CODE))
            .type(r.get(PARTY.TYPE))
            .name(r.get(PARTY.NAME))
            .properties(properties)
            .source(r.get(PARTY.SOURCE_TYPE))
            .build();
    }
}
```

```java

@Service
@RequiredArgsConstructor
public class ManifestService {

    private final AssignmentRepository assignmentRepo;
    private final AssignmentMemberRepository memberRepo;
    /**
     * vocabRepo
     */
    private final DataTemplateRepository templateRepository;
    private final AssignmentPartyBindingRepository bindingRepo;

    // In a real app, you'd inject a "UserContext" to get the current user's ID
    // You would inject a service to get user's teams/groups, or resolve them here.
    // For now, we'll assume they are passed in.
    @Transactional(readOnly = true)
    public Page<AssignmentManifestDto> buildManifest(String userId, Collection<String> teamIds,
                                                     Collection<String> userGroupIds, PagedRequest pagedRequest) {

        // 1. Gather all principals for the user
        Set<String> principalIds = new HashSet<>(teamIds);
        principalIds.add(userId);
        principalIds.addAll(userGroupIds);


        // 2. Find all active Assignment IDs in a single query
        Page<String> assignmentIds = (pagedRequest.getSince() == null)
            ? memberRepo
            .findActiveAssignmentIdsForPrincipalsAndTeams(principalIds, teamIds, pagedRequest.getPageable())
            : memberRepo
            .findActiveAssignmentIdsForPrincipalsAndTeams(principalIds, teamIds, pagedRequest.getSince(), pagedRequest.getPageable());
        if (assignmentIds.isEmpty()) {
            return Page.empty(pagedRequest.getPageable());
        }

        // 3. Fetch all required data in bulk to avoid N+1 queries
        List<Assignment> assignments = assignmentRepo.findAllById(assignmentIds.getContent());

        List<AssignmentPartyBinding> bindings = bindingRepo.findByAssignmentIdIn(assignmentIds.getContent());

        // Collect all unique vocabulary/template IDs from the bindings
        Set<String> vocabularyIds = bindings.stream()
            .map(AssignmentPartyBinding::getVocabulary)
            .map(DataTemplate::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<String, DataTemplate> templatesById = templateRepository.findAllById(vocabularyIds).stream()
            .collect(Collectors.toMap(DataTemplate::getId, Function.identity()));

        // Group bindings by assignment for efficient mapping
        Map<String, List<AssignmentPartyBinding>> bindingsByAssignmentId = bindings.stream()
            .collect(Collectors.groupingBy(b -> b.getAssignment().getId()));

        // 4. Build the final DTOs in memory
        final var bindingsManifest = assignments.stream().map(assign -> {
            Set<String> vocabUids = Optional.ofNullable(assign.getForms()).orElse(Collections.emptySet());
            List<AssignmentPartyBinding> assignBindings = bindingsByAssignmentId.getOrDefault(assign.getId(), Collections.emptyList());

            return AssignmentManifestDto.builder()
                .assignmentUid(assign.getUid())
                .label(assign.getOrgUnit().getName()) // Or other meaningful label
                .status(AssignmentStatus.getAssignmentStatus(assign.getStatus()))
                .templateUids(vocabUids)
                .bindings(mapBindings(assignBindings, templatesById))
                .build();
        }).toList();


        return new PageImpl<>(bindingsManifest, assignmentIds.getPageable(), bindingsManifest.size());
    }

    private List<AssignmentManifestDto.BindingDto> mapBindings(List<AssignmentPartyBinding> source,
                                                               Map<String, DataTemplate> templatesById) {
        return source.stream().map(b -> {
            DataTemplate template = (b.getVocabulary() != null) ? templatesById.get(b.getVocabulary().getId()) : null;
            return AssignmentManifestDto.BindingDto.builder()
                .roleName(b.getName())
                .templateUid(template != null ? template.getUid() : null) // Null for global-assignment roles
                .partySetId(b.getPartySet().getId())
                .combineMode(b.getCombineMode())
                .provenance("Role Binding")
                .build();
        }).toList();
    }
}
```

```java

@Service
@RequiredArgsConstructor
@Slf4j
public class PartySyncService {

    private final PartyRepository partyRepo;

    @Data
    @Builder
    @Accessors(fluent = true)
    public static class ToSyncParty {
        private String id;
        private String uid;
        private String code;
        private SourceType sourceType;
        private String name;
        private Map<String, String> label;
        private String parentId;
        private List<String> tags;
        private Map<String, Object> meta;
    }

    /**
     * Generic upsert method.
     * We use the ID as the source of truth. If it exists, we update label/meta.
     */
    @Transactional
    public void syncParty(ToSyncParty toSyncParty) {
        UUID partyParentId;
        if (toSyncParty.sourceType == SourceType.ORG_UNIT && toSyncParty.parentId != null) {
            final var partyParent = partyRepo.findBySourceId(toSyncParty.parentId)
                .orElse(null);
            partyParentId = partyParent != null ? partyParent.getId() : null;
        }

        partyRepo.findByUid(toSyncParty.uid).ifPresentOrElse(
            // Case A: Update existing
            existing -> {
                boolean changed = !Objects.equals(existing.getParentId(), partyParentId)
                    || !Objects.equals(existing.getLabel(), toSyncParty.label);
                log.debug("Updating Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                if (changed && Objects.equals(existing.getSourceType(), SourceType.ORG_UNIT)) {
                    existing.setName(toSyncParty.name);
                    existing.setCode(toSyncParty.code);
                    existing.setLabel(toSyncParty.label);
                    existing.setType(PartyType.INTERNAL); // we'll focus on internal for now
                    existing.setSourceType(toSyncParty.sourceType);
                    existing.setParentId(partyParentId);
//                    existing.setTags(toSyncParty.tags); // because they are frequently created, i am thinking of moving them
//                    existing.setProperties(toSyncParty.meta); // because they are frequently created, i am thinking of moving them
                    existing.setLastModifiedDate(Instant.now());
                    log.debug("Updated Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                }
            },
            // Case B: Create new
            () -> {
                Party newParty = Party.builder()
                    .id(UUID.nameUUIDFromBytes(toSyncParty.id.getBytes(StandardCharsets.UTF_8)))      // Keeps the same ULID as the source
                    .uid(toSyncParty.uid)    // Keeps the same UID as the source
                    .code(toSyncParty.code)
                    .type(PartyType.INTERNAL)
                    .name(toSyncParty.name)
                    .parentId(partyParentId)
                    .sourceType(toSyncParty.sourceType)
                    .sourceId(toSyncParty.id)
                    .createdDate(Instant.now())
                    .lastModifiedDate(Instant.now())
                    .build();
                newParty.setLabel(toSyncParty.label);

                partyRepo.persistAndFlush(newParty);

                log.info("Indexed new Party: {} ({})", toSyncParty.name, toSyncParty.uid);
            }
        );
    }

    public void updateUserPartyTags(String s) {
        // not implemented yet
    }
}
```

PartySet service:

```java

@Service
@Transactional
@RequiredArgsConstructor
public class PartySetService {

    private final PartySetRepository partySetRepository;
    private final PartyResolutionEngine engine;
    private final PartySetMapper partySetMapper;
    private final AssignmentPartyBindingRepository bindingRepository;

    public PartySetDto save(PartySetDto partySetDto) {
        PartySet partySet = partySetMapper.toEntity(partySetDto);

        partySet = partySetRepository.persistAndFlush(partySet);
        return partySetMapper.toDto(partySet);
    }

    @Transactional(readOnly = true)
    public List<PartySetDto> findAll() {
        return partySetRepository.findAll().stream()
            .map(partySetMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PartySetDto> findOne(UUID id) {
        return partySetRepository.findById(id).map(partySetMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ResolvedParty> findPartiesBySetId(String optionSetId, PagedRequest pagedRequest,
                                                  CurrentUserDetails user) {
        final var pageable = pagedRequest.getPageable();
        final var partySet = partySetRepository.findByUid(optionSetId)
            .orElseThrow(() -> new NotFoundObjectException("PartySet with optionSetId: " + optionSetId + " not found"));

        final List<ResolvedParty> parties;
        try {
            parties = engine.executeStrategy(partySet.getKind(), partySet.getId(),
                partySet.getSpec() != null ? PagedRequest.specToString(partySet.getSpec()) : null, true, PartyResolutionRequest.builder()
                    .userId(user.getId())
                    .limit(pagedRequest.getSize())
                    .offset(pagedRequest.getPage() * pagedRequest.getSize())
                    .since(pagedRequest.getSince())
                    .build());
        } catch (IOException e) {
            throw new InvalidRequestException("invalid PartySet specs:\n" + e.getMessage());
        }

        return new PageImpl<>(parties, pageable, parties.size());
    }

    public void delete(String id) {
        boolean isUUid = UuidUtils.isUuid(id);
        // **VALIDATION STEP**
        // Check if any bindings are currently using this PartySet.
        if (bindingRepository.existsByPartySetUid(id) || (isUUid &&
            bindingRepository.existsByPartySetId(UuidUtils.toUuid(id)))) {
            throw new DeletionConflictException(
                "Cannot delete PartySet with ID " + id + ". It is currently in use by one or more bindings."
            );
        }

        // If the check passes, proceed with the deletion.
        if (isUUid) {
            partySetRepository.deleteById(UuidUtils.toUuid(id));
        } else {
            partySetRepository.deleteByUid(id);
        }
    }
}
```

and last:

```java
package org.nmcpye.datarun.party.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.party.dto.PagedRequest;
import org.nmcpye.datarun.party.dto.UserAllowedPartyDto;
import org.nmcpye.datarun.party.entities.UserAllowedParty;
import org.nmcpye.datarun.party.mapper.UserAllowedPartyMapper;
import org.nmcpye.datarun.party.repository.UserAllowedPartyRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPermissionSyncService {
    private final UserAllowedPartyRepository userAllowedPartyRepository;
    private final UserAllowedPartyMapper userAllowedPartyMapper;

    public Page<UserAllowedPartyDto> getAllowedPartiesForUser(String userId, PagedRequest pageable) {

        Page<UserAllowedParty> allowedParties;
        if (userId != null) {
            allowedParties = pageable.getSince() != null ?
                userAllowedPartyRepository.findById_UserIdAndLastUpdatedAfter(userId, pageable.getSince(), pageable.getPageable()) :
                userAllowedPartyRepository.findById_UserId(userId, pageable.getPageable());
        } else {
            allowedParties = userAllowedPartyRepository.findAll(pageable.getPageable());
        }

        return allowedParties.map(userAllowedPartyMapper::toDto);
    }
}
```
