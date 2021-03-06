/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.time.LocalDate;
import java.util.Collections;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.lang.Nullable;

/**
 * Tests for the mapping of {@link CriteriaQuery} by a
 * {@link org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter}. In the same package as
 * {@link CriteriaQueryProcessor} as this is needed to get the String representation to assert.
 * 
 * @author Peter-Josef Meisch
 */
public class CriteriaQueryMappingTests {

	MappingElasticsearchConverter mappingElasticsearchConverter;

	@BeforeEach
	void setUp() {
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(Collections.singleton(Person.class));
		mappingContext.afterPropertiesSet();

		mappingElasticsearchConverter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
		mappingElasticsearchConverter.afterPropertiesSet();

	}

	@Test // DATAES-716
	void shouldMapNamesAndConvertValuesInCriteriaQuery() throws JSONException {

		// use POJO properties and types in the query building
		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				new Criteria("birthDate") //
						.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9)) //
						.or("birthDate").is(LocalDate.of(2019, 12, 28)) //
		);

		// mapped field name and converted parameter
		String expected = '{' + //
				"  \"bool\" : {" + //
				"    \"should\" : [" + //
				"      {" + //
				"        \"range\" : {" + //
				"          \"birth-date\" : {" + //
				"            \"from\" : \"09.11.1989\"," + //
				"            \"to\" : \"09.11.1990\"," + //
				"            \"include_lower\" : true," + //
				"            \"include_upper\" : true" + //
				"          }" + //
				"        }" + //
				"      }," + //
				"      {" + //
				"        \"query_string\" : {" + //
				"          \"query\" : \"28.12.2019\"," + //
				"          \"fields\" : [" + //
				"            \"birth-date^1.0\"" + //
				"          ]" + //
				"        }" + //
				"      }" + //
				"    ]" + //
				"  }" + //
				'}'; //

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldMapNamesAndValuesInSubCriteriaQuery() throws JSONException {

		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				new Criteria("firstName").matches("John") //
						.subCriteria(new Criteria("birthDate") //
								.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9)) //
								.or("birthDate").is(LocalDate.of(2019, 12, 28))));

		String expected = "{\n" + //
				"  \"bool\": {\n" + //
				"    \"must\": [\n" + //
				"      {\n" + //
				"        \"match\": {\n" + //
				"          \"first-name\": {\n" + //
				"            \"query\": \"John\"\n" + //
				"          }\n" + //
				"        }\n" + //
				"      },\n" + //
				"      {\n" + //
				"        \"bool\": {\n" + //
				"          \"should\": [\n" + //
				"            {\n" + //
				"              \"range\": {\n" + //
				"                \"birth-date\": {\n" + //
				"                  \"from\": \"09.11.1989\",\n" + //
				"                  \"to\": \"09.11.1990\",\n" + //
				"                  \"include_lower\": true,\n" + //
				"                  \"include_upper\": true\n" + //
				"                }\n" + //
				"              }\n" + //
				"            },\n" + //
				"            {\n" + //
				"              \"query_string\": {\n" + //
				"                \"query\": \"28.12.2019\",\n" + //
				"                \"fields\": [\n" + //
				"                  \"birth-date^1.0\"\n" + //
				"                ]\n" + //
				"              }\n" + //
				"            }\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}\n"; //

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	static class Person {
		@Nullable @Id String id;
		@Nullable @Field(name = "first-name") String firstName;
		@Nullable @Field(name = "last-name") String lastName;
		@Nullable @Field(name = "birth-date", type = FieldType.Date, format = DateFormat.custom,
				pattern = "dd.MM.uuuu") LocalDate birthDate;
	}
}
