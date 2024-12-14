//package org.nmcpye.datarun.drun.postgres.attribute;
//
//import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
//import com.google.common.base.CaseFormat;
//import org.nmcpye.am.common.BaseNameableObject;
//import org.nmcpye.am.common.DxfNamespaces;
//import org.nmcpye.am.common.IdentifiableObject;
//import org.nmcpye.am.common.MetadataObject;
//import org.nmcpye.am.constant.Constant;
//import org.nmcpye.am.dataelement.DataElement;
//import org.nmcpye.am.dataelement.DataElementGroup;
//import org.nmcpye.am.dataelement.DataElementGroupSet;
//import org.nmcpye.am.document.Document;
//import org.nmcpye.am.legend.LegendSet;
//import org.nmcpye.am.option.Option;
//import org.nmcpye.am.option.OptionSet;
//import org.nmcpye.am.organisationunit.OrganisationUnit;
//import org.nmcpye.am.organisationunit.OrganisationUnitGroup;
//import org.nmcpye.am.organisationunit.OrganisationUnitGroupSet;
//import org.nmcpye.am.program.Program;
//import org.nmcpye.am.program.ProgramIndicator;
//import org.nmcpye.am.program.ProgramStage;
//import org.nmcpye.am.relationship.RelationshipType;
//import org.nmcpye.am.sqlview.SqlView;
//import org.nmcpye.am.trackedentity.TrackedEntityAttribute;
//import org.nmcpye.am.trackedentity.TrackedEntityType;
//import org.nmcpye.am.user.User;
//import org.nmcpye.am.user.UserGroup;
//import org.nmcpye.datarun.domain.AbstractAuditingEntity;
//import org.nmcpye.datarun.domain.User;
//import org.nmcpye.datarun.drun.postgres.common.Identifiable;
//import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
//import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
//import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet;
//import org.nmcpye.datarun.drun.postgres.domain.Team;
//
//import java.util.EnumSet;
//
//import static java.util.Arrays.stream;
//
//@JacksonXmlRootElement(localName = "attributeBase", namespace = DxfNamespaces.DXF_2_0)
//public class AttributeBase
//    extends AbstractAuditingEntity<Long> {
//
//    public enum ObjectType {
////        DATA_ELEMENT(DataElement.class),
//
////        DATA_ELEMENT_GROUP(DataElementGroup.class),
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
////
////        PROGRAM(Program.class),
////
////        PROGRAM_STAGE(ProgramStage.class),
////
////        TRACKED_ENTITY_TYPE(TrackedEntityType.class),
////
////        TRACKED_ENTITY_ATTRIBUTE(TrackedEntityAttribute.class),
//////
////        CATEGORY_OPTION(CategoryOption.class),
////
////        CATEGORY_OPTION_GROUP(CategoryOptionGroup.class),
//
////        DOCUMENT(Document.class),
////
////        OPTION(Option.class),
////
////        OPTION_SET(OptionSet.class),
////
////        CONSTANT(Constant.class),
////
////        LEGEND_SET(LegendSet.class),
////
////        PROGRAM_INDICATOR(ProgramIndicator.class),
////
////        SQL_VIEW(SqlView.class),
//
////        SECTION(Section.class),
//
//        //        CATEGORY_OPTION_COMBO(CategoryOptionCombo.class),
////
////        CATEGORY_OPTION_GROUP_SET(CategoryOptionGroupSet.class),
////
////        DATA_ELEMENT_GROUP_SET(DataElementGroupSet.class),
////
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
//
//        RELATIONSHIP_TYPE(RelationshipType.class);
//
//        final Class<? extends Identifiable<Long>> type;
//
//        ObjectType(Class<? extends IdentifiableObject> type) {
//            this.type = type;
//        }
//
//        public Class<? extends IdentifiableObject> getType() {
//            return type;
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
