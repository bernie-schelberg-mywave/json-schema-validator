/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MinimumValidatorTest {
    private static JsonSchemaFactory factory = JsonSchemaFactory.getInstance();

    private static ObjectMapper mapper;
    private static ObjectMapper bigDecimalMapper;
    private static ObjectMapper bigIntegerMapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        bigDecimalMapper = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        bigIntegerMapper = new ObjectMapper().enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

    }

    @Test
    public void doubleValueOverflow() throws IOException {
        String[][] values = {
//            minimum,                           value
            {"-1.7976931348623157e+308",         "-1.7976931348623159e+308"},
            {"-1.7976931348623156e+308",         "-1.7976931348623157e+308"},
//          See a {@link #doubleValueCoarsing() doubleValueCoarsing} test notes below
//            {"-1.7976931348623157e+308",         "-1.7976931348623158e+308"},
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": %s }", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecing validation error with minimum %s and value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void documentParsedWithBigDecimal() throws IOException {
        String[][] values = {
//            minimum,                           value
            {"-1.7976931348623157e+308",         "-1.7976931348623159e+308"},
            {"-1.7976931348623156e+308",         "-1.7976931348623157e+308"},
//          See a {@link #doubleValueCoarsing() doubleValueCoarsing} test notes below
//            {"-1.7976931348623157e+308",         "-1.7976931348623158e+308"},
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": %s }", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigDecimalMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecing validation error with minimum %s and value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void documentAndSchemaParsedWithBigDecimal() throws IOException {
        String[][] values = {
//            minimum,                            value
            {"-1.7976931348623157e+308",         "-1.7976931348623159e+308"},
            {"-1.7976931348623156e+308",         "-1.7976931348623157e+308"},
//          See a {@link #doubleValueCoarsing() doubleValueCoarsing} test notes below
//            {"-1.7976931348623157e+308", "-1.7976931348623158e+308"},
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": %s }", minimum);

            JsonSchema v = factory.getSchema(bigDecimalMapper.readTree(schema));
            JsonNode doc = bigDecimalMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecing validation error with minimum %s and value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void negativeDoubleOverflowTest() throws IOException {
        String[][] values = {
//            minimum,                            value
            {"-1.79769313486231571E+308",        "-1.79769313486231572e+308"},
            {"-1.7976931348623157E+309",         "-1.7976931348623157e+309"},
            {"-1.000000000000000000000001E+308", "-1.000000000000000000000001E+308"},
            {"-1.000000000000000000000001E+400", "-1.000000000000000000000001E+401"},
            {"-1.000000000000000000000001E+400", "-1.000000000000000000000002E+400"},
            {"-1.000000000000000000000001E+400", "-1.0000000000000000000000011E+400"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": %s }", minimum);

            // Schema and document parsed with just double
            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);
            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Minimum %s and value %s are interpreted as Infinity, thus no schema violation should be reported", minimum, value), messages.isEmpty());

            // document parsed with BigDecimal
            doc = bigDecimalMapper.readTree(value);
            Set<ValidationMessage> messages2 = v.validate(doc);

            //when the schema and value are both using BigDecimal, the value should be parsed in same mechanism.
            if(Double.valueOf(minimum) == Double.NEGATIVE_INFINITY) {
                /**
                 * {"-1.000000000000000000000001E+308", "-1.000000000000000000000001E+308"} will be false
                 * because the different between two mappers, without using big decimal, it loses some precises.
                 */
                assertTrue(format("Minimum %s and value %s are equal, thus no schema violation should be reported", minimum, value), messages2.isEmpty());
            } else {
                assertFalse(format("Minimum %s is larger than value %s ,  should be validation error reported", minimum, value), messages2.isEmpty());
            }

            // schema and document parsed with BigDecimal
            v = factory.getSchema(bigDecimalMapper.readTree(schema));
            Set<ValidationMessage> messages3 = v.validate(doc);
            //when the schema and value are both using BigDecimal, the value should be parsed in same mechanism.
            if(minimum.toLowerCase().equals(value.toLowerCase()) || Double.valueOf(minimum) == Double.NEGATIVE_INFINITY) {
                assertTrue(format("Minimum %s and value %s are equal, thus no schema violation should be reported", minimum, value), messages3.isEmpty());
            } else {
                assertFalse(format("Minimum %s is larger than value %s ,  should be validation error reported", minimum, value), messages3.isEmpty());
            }
        }
    }

    /**
     *  value of -1.7976931348623158e+308 is not converted to NEGATIVE_INFINITY for some reason
     *  the only way to spot this is to use BigDecimal for schema (and for document)
     */
    @Test
    public void doubleValueCoarsing() throws IOException {
        String schema = "{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": -1.7976931348623157e+308 }";
        String content = "-1.7976931348623158e+308";

        JsonNode doc = mapper.readTree(content);
        JsonSchema v = factory.getSchema(mapper.readTree(schema));

        Set<ValidationMessage> messages = v.validate(doc);
        assertTrue("Validation should succeed as by default double values are used by mapper", messages.isEmpty());

        doc = bigDecimalMapper.readTree(content);
        messages = v.validate(doc);
        assertFalse("Validation should not succeed because content is using bigDecimalMapper, and smaller than the minimum", messages.isEmpty());

        /**
         * Note: technically this is where -1.7976931348623158e+308 rounding to -1.7976931348623157e+308 could be
         *       spotted, yet it requires a dedicated case of comparison BigDecimal to BigDecimal. Since values below
         *       -1.7976931348623158e+308 are parsed as Infinity anyways (jackson uses double as primary type with later
         *       "upcasting" to BigDecimal, if property is set) adding a dedicated code block just for this one case
         *       seems infeasible.
         */
        v = factory.getSchema(bigDecimalMapper.readTree(schema));
        messages = v.validate(doc);
        assertFalse("Validation should not succeed because content is using bigDecimalMapper, and smaller than the minimum", messages.isEmpty());
    }

    /**
     * BigDecimalMapper issue, it doesn't work as expected, it will treat -1.7976931348623157e+309 as INFINITY instead of as it is.
     */
    @Test
    public void doubleValueCoarsingExceedRange() throws IOException {
        String schema = "{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": -1.7976931348623159e+308 }";
        String content = "-1.7976931348623160e+308";

        JsonNode doc = mapper.readTree(content);
        JsonSchema v = factory.getSchema(mapper.readTree(schema));

        Set<ValidationMessage> messages = v.validate(doc);
        assertTrue("Validation should succeed as by default double values are used by mapper", messages.isEmpty());

        doc = bigDecimalMapper.readTree(content);
        messages = v.validate(doc);
        assertTrue("Validation should succeed due to the bug of BigDecimal option of mapper", messages.isEmpty());

        v = factory.getSchema(bigDecimalMapper.readTree(schema));
        messages = v.validate(doc);
        assertTrue("Validation should succeed due to the bug of BigDecimal option of mapper", messages.isEmpty());
    }

    @Test
    public void longUnderMinValueOverflow() throws IOException {
        String[][] values = {
//            minimum,                value
            {"-9223372036854775800",  "-9223372036854775855"},
            {"-9223372036854775808",  "-9223372036854775809"},
            {"-9223372036854775808",  new BigDecimal(String.valueOf(-Double.MAX_VALUE)).subtract(BigDecimal.ONE).toString()},
            {"-9223372036854775807",  new BigDecimal(String.valueOf(-Double.MAX_VALUE)).subtract(BigDecimal.ONE).toString()},
            {"-9223372036854776000",  "-9223372036854776001"}
        };
        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s }", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecing validation error with minimum %s and value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void longValueOverflowWithInverseEffect() throws IOException {
        String[][] values = {
//            minimum,                       value
            {"-9223372036854775000",         "-9223372036854774988"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": true}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecing no validation errors as minimum %s is lesser than value %s", minimum, value), messages.isEmpty());
        }
    }
    @Test
    public void BigIntegerBothWithinLongRangePositive() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"10",         "20"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": true}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecting no validation errors as minimum %s is lesser than value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void BigIntegerBothWithinLongRangeNegative() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"20",         "10"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": true}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecting validation error with minimum %s and value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void BigIntegerOverflow() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-9223372036854775807",         "-9223372036854775809"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": true}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecing validation error with minimum %s and value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void BigIntegerNotOverflow() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-9223372036854775809",         "-9223372036854775807"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": true}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecting no validation errors as minimum %s is lesser than value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void BigIntegerBothAboveLongRangePositive() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-9223372036854775810",         "-9223372036854775809"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": true}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecting no validation errors as minimum %s is lesser than value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void BigIntegerBothAboveLongRangeNegative() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-9223372036854775809",         "-9223372036854775810"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": true}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecing validation error with minimum %s and value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void BigIntegerNotOverflowOnLongRangeEdge() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-9223372036854775808",         "-9223372036854775808"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": false}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecing no validation errors as minimum %s is lesser than value %s", minimum, value), messages.isEmpty());
        }
    }

    @Test
    public void BigIntegerOverflowOnLongRangeEdge() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-9223372036854775809",         "-9223372036854775809"}
        };

        for(String[] aTestCycle : values) {
            String maximum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"maximum\": %s, \"exclusiveMaximum\": false}", maximum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = bigIntegerMapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecing no validation errors as maximum %s is greater than value %s", maximum, value), messages.isEmpty());
        }
    }

    @Test
    public void testMinimumDoubleValue() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-1E309",         "-1000"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": false}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecting no validation errors as value %s is greater than minimum %s", value, minimum), messages.isEmpty());
        }
    }

    @Test
    public void testMinimumDoubleValueNegative() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"-1000",         "-1E309"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\", \"minimum\": %s, \"exclusiveMinimum\": false}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecting  validation errors as value %s is smaller than minimum %s", value, minimum), messages.isEmpty());
        }
    }

    @Test
    public void testMinimumDoubleValueWithNumberType() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"1000",         "1000.1"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": %s, \"exclusiveMinimum\": false}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertTrue(format("Expecting no validation errors as value %s is greater than minimum %s", value, minimum), messages.isEmpty());
        }
    }

    @Test
    public void testMinimumDoubleValueWithNumberTypeNegative() throws IOException {
        String[][] values = {
//            minimum,                       value
                {"1000.1",         "1000"}
        };

        for(String[] aTestCycle : values) {
            String minimum = aTestCycle[0];
            String value = aTestCycle[1];
            String schema = format("{ \"$schema\":\"http://json-schema.org/draft-04/schema#\", \"type\": \"number\", \"minimum\": %s, \"exclusiveMinimum\": false}", minimum);

            JsonSchema v = factory.getSchema(mapper.readTree(schema));
            JsonNode doc = mapper.readTree(value);

            Set<ValidationMessage> messages = v.validate(doc);
            assertFalse(format("Expecting  validation errors as value %s is smaller than minimum %s", value, minimum), messages.isEmpty());
        }
    }
}


