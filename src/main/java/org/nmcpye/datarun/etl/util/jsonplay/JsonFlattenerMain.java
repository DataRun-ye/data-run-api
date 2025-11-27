package org.nmcpye.datarun.etl.util.jsonplay;

import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;

import java.util.Map;

public class JsonFlattenerMain {
    public static void main(String[] args) {

        Map<String, Object> flattenJson = JsonFlattener.flattenAsMap(JsonSamples.SAMPLE_COMPLEX);

        System.out.println(flattenJson);

        String json = "{\"abc\":{\"def\":[1,2,{\"g\":{\"h\":[3]}}]}}";

// FlattenMode.NORMAL(default) - flatten everything
        System.out.println(new JsonFlattener(JsonSamples.SAMPLE_COMPLEX).withFlattenMode(FlattenMode.NORMAL).flatten());
// {"abc.def[0]":1,"abc.def[1]":2,"abc.def[2].g.h[0]":3}

// FlattenMode.KEEP_ARRAYS - flatten all except arrays
        System.out.println(new JsonFlattener(JsonSamples.SAMPLE_COMPLEX).withFlattenMode(FlattenMode.KEEP_ARRAYS).flatten());
// {"abc.def":[1,2,{"g.h":[3]}]}

// When the flattened outcome can NOT suit in a Java Map, it will still be put in the Map with "root" as its key.
        Map<String, Object> map = new JsonFlattener(JsonSamples.SAMPLE_COMPLEX).withFlattenMode(FlattenMode.KEEP_PRIMITIVE_ARRAYS).flattenAsMap();

        System.out.println(map.get("root"));
    }
}
