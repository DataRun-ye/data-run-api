//package org.nmcpye.datarun.web.rest.exception;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
//public class ErrorResponse {
//    private String message;
//    private String details;
//    private LocalDateTime timestamp;
//
//    public Map<String, String> getErrors() {
//        return errors;
//    }
//
//    public void setErrors(Map<String, String> errors) {
//        this.errors = errors;
//    }
//
//    private Map<String, String> errors;
//
//    public ErrorResponse(String message, String details) {
//        this.message = message;
//        this.details = details;
//        this.timestamp = LocalDateTime.now();
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getDetails() {
//        return details;
//    }
//
//    public void setDetails(String details) {
//        this.details = details;
//    }
//
//    public LocalDateTime getTimestamp() {
//        return timestamp;
//    }
//
//    public void setTimestamp(LocalDateTime timestamp) {
//        this.timestamp = timestamp;
//    }
//}
