//package org.nmcpye.datarun.party;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.nmcpye.datarun.jpa.assignment.AssignmentPartyBinding;
//import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
//import org.nmcpye.datarun.party.dto.PartyResolutionRequest;
//import org.nmcpye.datarun.party.entities.*;
//import org.nmcpye.datarun.party.repository.PartyRepository;
//import org.nmcpye.datarun.party.repository.PartySetRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Transactional
//class PartyResolutionResourceIT {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    // Autowire repositories to set up test data
//     @Autowired private PartyRepository partyRepository;
//      @Autowired private PartySetRepository partySetRepository;
//     @Autowired private AssignmentRepository assignmentRepository;
//    // ... etc.
//
//    private static final String TEST_USER_ID = "user-test-123";
//    private static final String TEST_TEAM_ID = "team-alpha-456";
//    private static final String TEST_ASSIGNMENT_ID = "assign-kilo-789";
//
//    @BeforeEach
//    void setUp() {
//        // 1. Create Parties (Warehouses A, B, C)
//        Party warehouseA = partyRepository.persistAndFlush(new Party(...,"Warehouse A", ...));
//        Party warehouseB = partyRepository.persistAndFlush(new Party(...,"Warehouse B", ...));
//        Party warehouseC = partyRepository.persistAndFlush(new Party(...,"Warehouse C", ...));
//
//        // 2. Create PartySets
//        PartySet allWarehouses = partySetRepository.persistAndFlush(
//            new PartySet(...,"All Warehouses", PartySetKind.STATIC, ...));
//        PartySet teamAlphaList = partySetRepository.persistAndFlush(
//            new PartySet(...,"Team Alpha List", PartySetKind.STATIC, ...));
//
//        // 3. Populate PartySet Members
//        partySetMemberRepository.save(new PartySetMember(...,allWarehouses.getId(), warehouseA.getId()));
//        partySetMemberRepository.save(new PartySetMember(...,allWarehouses.getId(), warehouseB.getId()));
//
//        partySetMemberRepository.save(new PartySetMember(...,teamAlphaList.getId(), warehouseB.getId()));
//        partySetMemberRepository.save(new PartySetMember(...,teamAlphaList.getId(), warehouseC.getId()));
//
//        // 4. Create Assignment
//        assignmentRepository.save(new Assignment(TEST_ASSIGNMENT_ID, ...));
//
//        // 5. Create the Principal-Scoped Binding for Team Alpha
//        assignmentPartyBindingRepository.save(
//            new AssignmentPartyBinding(
//            ...,
//        "sender", // role_name
//            TEST_ASSIGNMENT_ID,
//            null, // vocabulary_id (null for simplicity)
//            teamAlphaList.getId(), // party_set_id
//            "TEAM", // principal_type
//            TEST_TEAM_ID, // principal_id
//            CombineMode.UNION
//        )
//    );
//
//        // 6. Set up Permissions: User can see A and B, but NOT C
//        userAllowedPartyRepository.save(
//            new UserAllowedParty(new UserAllowedPartyId(TEST_USER_ID, warehouseA.getId()), ...));
//        userAllowedPartyRepository.save(
//            new UserAllowedParty(new UserAllowedPartyId(TEST_USER_ID, warehouseB.getId()), ...));
//    }
//
//    @Test
//    void shouldResolvePartiesBasedOnPrincipalBindingAndPermissions() throws Exception {
//        // 1. Build the request payload
//        PartyResolutionRequest request = PartyResolutionRequest.builder()
//            .assignmentId(TEST_ASSIGNMENT_ID)
//            .role("sender")
//            .userId(TEST_USER_ID)
//            .limit(10)
//            .offset(0)
//            .build();
//
//        // Convert request object to JSON string
//        String requestJson = new ObjectMapper().writeValueAsString(request);
//
//        // 2. Perform the POST request and set assertions
//        mockMvc.perform(post("/api/parties/resolve")
//                // Simulate that our test user is making the request
//                .with(user(TEST_USER_ID))
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(requestJson))
//            .andExpect(status().isOk())
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//            // Assert the response is a JSON array with exactly one item
//            .andExpect(jsonPath("$", hasSize(1)))
//            .andExpect(jsonPath("$.login").value("Warehouse B"));
//    }
//}
