package org.nmcpye.datarun.utils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

public class JsonPathUtils {

    public static void main(String[] args) {

            Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonArrayString);

            Object author0 = JsonPath.read(document, "$.formData.mainSection.locationStatus.orgUnit");

            // Print the map to test
            System.out.println(author0);

    }

//   static String jsonArrayString = """
//    {
//      "_id": {
//        "$oid": "674e8275f560dd5f8eac0a4e"
//      },
//      "uid": "F0cHOc6OpcH",
//      "deleted": false,
//      "status": "DONE",
//      "form": "Tcf3Ks9ZRpB",
//      "activity": "lNfYSY0wNFA",
//      "team": "zFL6RtOgFeP",
//      "formData": {
//        "_deviceId": "RKQ1.201004.002",
//        "mainSection": {
//          "locationStatus": {
//            "distributionStatus": "DONE",
//            "orgUnit": "Bpgh46xfeiq",
//            "workDay": "1"
//          }
//        },
//        "_userInfo": "773190093",
//        "_phoneNumber": null,
//        "_form": "Tcf3Ks9ZRpB",
//        "_username": "773190093",
//        "_id": "frMktbT54dsTaYp",
//        "_team": "zFL6RtOgFeP",
//        "_version": 12,
//        "_activity": "lNfYSY0wNFA",
//        "households_information": {
//          "settlement": "resident",
//          "householdnames": [
//            {
//              "householdNumber": 1,
//              "householdName": "محمد حسين صالح علي",
//              "inputmethod": "addNew",
//              "population": 12,
//              "malePopulation": 8,
//              "femalePopulation": 4,
//              "pregnantWomen": 0,
//              "childrenMaleCount": 1,
//              "childrenFemaleCount": 0,
//              "displacedResidentsCount": 0,
//              "itns": 6,
//              "_parentId": "frMktbT54dsTaYp",
//              "_id": "fZxfhCfs5X9AqrN9",
//              "_index": 1
//            }
//          ]
//        }
//      },
//      "serialNumber": 11642,
//      "_class": "org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission",
//      "createdBy": "773190093",
//      "createdDate": {
//        "$date": "2024-12-03T04:00:53.225Z"
//      },
//      "currentVersion": 12,
//      "finishedEntryTime": {
//        "$date": "2024-12-03T03:59:06.968Z"
//      },
//      "lastModifiedBy": "system",
//      "lastModifiedDate": {
//        "$date": "2024-12-07T21:39:13.530Z"
//      },
//      "startEntryTime": {
//        "$date": "2024-12-03T03:57:52.348Z"
//      }
//    }
//    """;

    static String jsonArrayString = """
        {
          	   "mainSection": "mainSection",
               	"mainSection.workDay": {"path": "mainSection.locationStatus.workDay", "default": null},
               	"mainSection.status": {"path": "mainSection.locationStatus.status", "default": null},
               	"completion_details": {"path": "completion_details", "default": null},
               	"completion_details.completionStatus": {"path": "completion_details.completionStatus", "default": "FULL"},
               	"completion_details.completionPercentage": {"path": "completion_details.completionPercentage", "default": 100},
               	"status_reassigned_details": {"path": "status_reassigned_details"},
               	"status_reassigned_details.targetedByOtherTeamName": {"path": "mainSection.locationStatus.targetedByOtherTeamName"},
               	"status_reassigned_details.targetedByOtherTeamNum": {"path": "mainSection.locationStatus.targetedByOtherTeamNum"},
               	"status_cancelled_details": {"path": "status_cancelled_details"},
               	"status_cancelled_details.notargetingReasons": {"path": "mainSection.locationStatus.notargetingReasons"},
               	"status_cancelled_details.otherReasonForNotargeting": {"path": "mainSection.locationStatus.otherReasonForNotargeting" },
               	"status_merged_details": {"path": "status_merged_details", "default": null},
               	"status_merged_details.mergedWithOtherVillage": {"path": "mainSection.locationStatus.mergedWithOtherVillage"},
               	"status_rescheduled_details": {"path": "status_rescheduled_details"},
               	"status_rescheduled_details.rescheduledTo": {"path": "status_rescheduled_details.rescheduledTo"},
               	"households_information": {"path": "distributionData"},
               	"households_information.settlement": {"path": "distributionData.settlement"},
               	"households_information.householdnames": {"path": "households_information.householdnames"},
               	"households_information.householdnames.householdHeadSerialNumber": {"path": "households_information.householdnames.householdHeadSerialNumber", "default": null},
               	"households_information.householdnames.inputmethod": {"path": "households_information.householdnames.inputmethod" },
               	"households_information.householdnames.householdName": {"path": "households_information.householdnames.householdName" },
               	"households_information.householdnames.householdReference": {"path": "households_information.householdnames.householdReference" },
               	"households_information.householdnames.population": {"path": "households_information.householdnames.population", "default": 0},
               	"households_information.householdnames.malePopulation": {"path": "households_information.householdnames.malePopulation", "default": 0},
               	"households_information.householdnames.femalePopulation": {"path": "households_information.householdnames.femalePopulation", "default": 0},
               	"households_information.householdnames.pregnantWomen": {"path": "households_information.householdnames.pregnantWomen", "default": 0},
               	"households_information.householdnames.childrenMaleCount": {"path": "households_information.householdnames.childrenMaleCount", "default": 0},
               	"households_information.householdnames.childrenFemaleCount": {"path": "households_information.householdnames.childrenFemaleCount", "default": 0},
               	"households_information.householdnames.displacedResidentsCount": {"path": "households_information.householdnames.displacedResidentsCount", "default": 0},
               	"households_information.householdnames.itns": {"path": "households_information.householdnames.itns", "default": 0}
        }
        """;
}

