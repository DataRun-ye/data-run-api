//package org.nmcpye.datarun.drun.mongo.event;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoOperations;
//import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
//import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
//
//public class UserCascadeSaveMongoEventListener extends AbstractMongoEventListener<Object> {
//
//    final private MongoOperations mongoOperations;
//
//    public UserCascadeSaveMongoEventListener(MongoOperations mongoOperations) {
//        this.mongoOperations = mongoOperations;
//    }
//
//    @Override
//    public void onBeforeConvert(final BeforeConvertEvent<Object> event) {
//        final Object source = event.getSource();
//        if ((source instanceof User) && (((User) source).getEmailAddress() != null)) {
//            mongoOperations.save(((User) source).getEmailAddress());
//        }
//    }
//}
