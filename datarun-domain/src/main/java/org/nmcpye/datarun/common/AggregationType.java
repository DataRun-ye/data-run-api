package org.nmcpye.datarun.common;

public enum AggregationType
{
    SUM( "sum", true ),
    AVERAGE( "avg", true ),
    AVERAGE_SUM_ORG_UNIT( "avg_sum_org_unit", true ),
    LAST( "last", true ), // Sum org unit
    LAST_AVERAGE_ORG_UNIT( "last_avg_org_unit", true ),
    LAST_IN_PERIOD( "last_analytics_period", true ), // Sum org unit
    LAST_IN_PERIOD_AVERAGE_ORG_UNIT( "last_analytics_period_avg_org_unit", true ),
    FIRST( "first", true ),
    FIRST_AVERAGE_ORG_UNIT( "first_avg_org_unit", true ),
    COUNT( "count", true ),
    STDDEV( "stddev", true ),
    VARIANCE( "variance", true ),
    MIN( "min", true ),
    MAX( "max", true ),
    NONE( "none", true ), // Aggregatable for text only
    CUSTOM( "custom", false ),
    DEFAULT( "default", false );

    private final String value;

    private boolean aggregatable;

    AggregationType(String value )
    {
        this.value = value;
    }

    AggregationType(String value, boolean aggregateable )
    {
        this.value = value;
        this.aggregatable = aggregateable;
    }

    public String getValue()
    {
        return value;
    }

    public boolean isAverage()
    {
        return this == AVERAGE_SUM_ORG_UNIT || this == AVERAGE;
    }

    public boolean isAggregatable()
    {
        return aggregatable;
    }

    public static AggregationType fromValue( String value )
    {
        for ( AggregationType type : AggregationType.values() )
        {
            if ( type.value.equalsIgnoreCase( value ) )
            {
                return type;
            }
        }

        return null;
    }
}
