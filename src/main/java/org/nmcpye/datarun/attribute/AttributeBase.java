//package org.nmcpye.datarun.attribute;
//
//import com.google.common.base.CaseFormat;
//import lombok.Getter;
//import lombok.Setter;
//import org.nmcpye.datarun.common.PrimaryKeyObject;
//import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
//import org.nmcpye.datarun.dataelement.DataElement;
//import org.nmcpye.datarun.dataelementgroup.DataElementGroup;
//import org.nmcpye.datarun.dataelementgroupset.DataElementGroupSet;
//import org.nmcpye.datarun.datastage.StageDefinition;
//import org.nmcpye.datarun.entitydefinition.EntityDefinition;
//import org.nmcpye.datarun.optionset.OptionSet;
//import org.nmcpye.datarun.orgunit.OrgUnit;
//import org.nmcpye.datarun.orgunitgroup.OrgUnitGroup;
//import org.nmcpye.datarun.orgunitgroupset.OrgUnitGroupSet;
//import org.nmcpye.datarun.team.Team;
//import org.nmcpye.datarun.template.FormTemplate;
//import org.nmcpye.datarun.user.User;
//
//import java.util.EnumSet;
//
//import static java.util.Arrays.stream;
//
//@Getter
//@Setter
//public class AttributeBase
//    extends JpaAuditableObject {
//
//    @Getter
//    public enum ObjectType {
//        DATA_ELEMENT(DataElement.class),
//
//        DATA_ELEMENT_GROUP(DataElementGroup.class),
//
////        INDICATOR(Indicator.class),
////
////        INDICATOR_GROUP(IndicatorGroup.class),
////
////        DATA_SET(DataSet.class),
//
//        ORG_UNIT(OrgUnit.class),
//
//        ORGANISATION_UNIT_GROUP(OrgUnitGroup.class),
//
//        ORGANISATION_UNIT_GROUP_SET(OrgUnitGroupSet.class),
//
//        USER(User.class),
//
//        Team(Team.class),
//
//        TEMPLATE(FormTemplate.class),
//
//        DATA_STAGE(StageDefinition.class),
//
//        ENTITY_DEFINITION(EntityDefinition.class),
//        //        OPTION(Option.class),
//
//        OPTION_SET(OptionSet.class),
//
//        DATA_ELEMENT_GROUP_SET(DataElementGroupSet.class);
//
////        TRACKED_ENTITY_ATTRIBUTE(TrackedEntityAttribute.class),
//        //
////        CATEGORY_OPTION(CategoryOption.class),
//
////        CATEGORY_OPTION_GROUP(CategoryOptionGroup.class),
//
////        DOCUMENT(Document.class),
//
////        CONSTANT(Constant.class),
//
////        LEGEND_SET(LegendSet.class),
//
////        PROGRAM_INDICATOR(ProgramIndicator.class),
////
////        SQL_VIEW(SqlView.class),
////
////        SECTION(Section.class),
//
////        CATEGORY_OPTION_COMBO(CategoryOptionCombo.class),
////
////        CATEGORY_OPTION_GROUP_SET(CategoryOptionGroupSet.class),
//
////        VALIDATION_RULE(ValidationRule.class),
////
////        VALIDATION_RULE_GROUP(ValidationRuleGroup.class),
////
////        CATEGORY(Category.class),
////
////        VISUALIZATION(Visualization.class),
////
////        MAP(Map.class),
////
////        EVENT_REPORT(EventReport.class),
////
////        EVENT_CHART(EventChart.class),
////
////        RELATIONSHIP_TYPE(RelationshipType.class);
//
//        final Class<? extends PrimaryKeyObject<?>> type;
//
//        ObjectType(Class<? extends PrimaryKeyObject<?>> type) {
//            this.type = type;
//        }
//
//        public static ObjectType valueOf(Class<?> type) {
//            return stream(values()).filter(t -> t.type == type).findFirst().orElse(null);
//        }
//
//        public String getPropertyName() {
//            return CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL)
//                .convert(name()) + "Attribute";
//        }
//    }
//
//    protected final EnumSet<ObjectType> objectTypes = EnumSet.noneOf(ObjectType.class);
//
//}
