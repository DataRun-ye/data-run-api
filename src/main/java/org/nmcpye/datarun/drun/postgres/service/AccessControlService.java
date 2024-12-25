//package org.nmcpye.datarun.drun.postgres.service;
//
//import org.nmcpye.datarun.repository.UserRepository;
//import org.springframework.stereotype.Service;
//
//@Service
//public class AccessControlService {
//    final private UserRepository userRepository;
//
//    final private RoleRepository roleRepository;
//
//    public boolean hasPermission(String userId, String entity, String action) {
//        User user = userRepository.findById(userId).orElseThrow(...);
//        for (String roleId : user.getRoles()) {
//            Role role = roleRepository.findById(roleId).orElseThrow(...);
//            if (role.getPermissions().get(entity).contains(action)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public boolean hasConditionalPermission(String userId, String entity, String action, Map<String, Object> conditions) {
//        User user = userRepository.findById(userId).orElseThrow(...);
//        if (hasPermission(userId, entity, action)) {
//            if (user.getConditions().containsKey(action)) {
//                Map<String, Object> userConditions = user.getConditions().get(action);
//                for (String key : userConditions.keySet()) {
//                    if (!userConditions.get(key).equals(conditions.get(key))) {
//                        return false;
//                    }
//                }
//            }
//            return true;
//        }
//        return false;
//    }
//}
