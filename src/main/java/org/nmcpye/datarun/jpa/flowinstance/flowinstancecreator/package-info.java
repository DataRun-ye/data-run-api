/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
package org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator;

// Todo:
//  1- UserContext Source: How do we reliably populate orgUnitId and other properties in UserContext?
//  2- Error Mapping: What HTTP status codes & payloads should FlowCreationException produce?
//  3- Testing: Coverage for invalid scope keys, unauthorized flows, and both planning modes.
//  4- UI Contracts: Confirm JSON shape of scopes for the front-end.
//  5- Error Handling: What retry strategy if ScopeInstanceService fails after saving FlowInstance?
