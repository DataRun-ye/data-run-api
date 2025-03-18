package org.nmcpye.datarun.common.feedback;

import lombok.Getter;

@Getter
public enum ErrorCode {

    /* General */
    E1000("API query must be specified"),
    E1001("API query contains an illegal string"),
    E1002("API version is invalid"),
    E1003("Failed to update paths: `{0}`"),

    /* Basic metadata */
    E1100("Data element not found or not accessible: `{0}`"),
    E1101("Form element parent `{0}` not found: `{1}`"),
    E1102("Reference Type must be specified: `{0}`"),
    E1103("MetaDataSchema must be specified for Reference Type element: `{0}`"),
    E1104("Specified MetaDataSchema `{0}` not found: `{1}`"),
    E1105("field conf value type should match data element value type"),
    E1106("Property `{0}` not found: `{1}`"),
    E1130("System configuration Error: `{0}`"),

    /* Data */
    E2000("Query parameters cannot be null"),
    E2001("At least one data element must be specified"),

    E2014("Unable to parse filter `{0}`"),
    E2015("Unable to parse order param: `{0}`"),
    E2016("Invalid Query Format"),
    E2017("Unable to parse order param: `{0}`"),
    E2018("Value for `{0}` must be `{1}` "),

    E2034("Filter not supported: `{0}`"),
    E2035("Operator not supported: `{0}`"),
    E2050("Query parsing error: `{0}`"),

    /* Security */
    E3000("No currentUser available"),
    E3001("User `{0}` is not allowed to update object `{1}`"),
    E3002("User `{0}` is not allowed to delete object `{1}`"),
    E3003("User `{0}` is not allowed to create objects of type {1}"),
    E3004("User `{0}` is not allowed to access objects of type {1}"),

    /* Users */
    E6201("User account not found"),
    E6202("Username is already taken");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

}
