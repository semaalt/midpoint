/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.path;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.CanonicalItemPathImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

public class ItemPathCanonicalizationTest {

    public ItemPathCanonicalizationTest() {
    }

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }


	@Test
	public void testCanonicalizationEmpty() {
		assertCanonical(null, null, "");
		assertCanonical(getPrismContext().emptyPath(), null, "");
	}

	private static final String COMMON = "${common}3";
	private static final String ICFS = "${icf}1/connector-schema-3";
	private static final String ICF = "${icf}1";
	private static final String ZERO = "${0}";
	private static final String ONE = "${1}";

	@Test
	public void testCanonicalizationSimple() {
		UniformItemPath path = getPrismContext().path(UserType.F_NAME);
		assertCanonical(path, null, "\\" + COMMON + "#name");
	}

	@Test
	public void testCanonicalizationSimpleNoNs() {
		UniformItemPath path = getPrismContext().path(UserType.F_NAME.getLocalPart());
		assertCanonical(path, null, "\\#name");
		assertCanonical(path, UserType.class, "\\" + COMMON + "#name");
	}

	@Test
	public void testCanonicalizationMulti() {
		UniformItemPath path = getPrismContext().path(UserType.F_ASSIGNMENT, 1234, AssignmentType.F_ACTIVATION,
				ActivationType.F_ADMINISTRATIVE_STATUS);
		assertCanonical(path, null, "\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
	}

	@Test
	public void testCanonicalizationMultiNoNs() {
		UniformItemPath path = getPrismContext().path(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_ACTIVATION.getLocalPart(),
				ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart());
		assertCanonical(path, null, "\\#assignment",
				"\\#assignment\\#activation", "\\#assignment\\#activation\\#administrativeStatus");
		assertCanonical(path, UserType.class, "\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation",
				"\\" + COMMON + "#assignment\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
	}

	@Test
	public void testCanonicalizationMixedNs() {
		UniformItemPath path = getPrismContext().path(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_EXTENSION,
				new QName("http://piracy.org/inventory", "store"),
				new QName("http://piracy.org/inventory", "shelf"),
				new QName("x"), ActivationType.F_ADMINISTRATIVE_STATUS);
		assertCanonical(path, null,
				"\\#assignment",
				"\\#assignment\\" + COMMON + "#extension",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x",
				"\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x\\" + ZERO + "#administrativeStatus");
		assertCanonical(path, UserType.class,
				"\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x\\" + ZERO + "#administrativeStatus");
	}

	@Test
	public void testCanonicalizationMixedNs2() {
		UniformItemPath path = getPrismContext().path(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_EXTENSION.getLocalPart(),
				new QName("http://piracy.org/inventory", "store"),
				new QName("http://piracy.org/inventory", "shelf"),
				AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
		assertCanonical(path, null,
				"\\#assignment",
				"\\#assignment\\#extension",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf\\" + COMMON + "#activation",
				"\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf\\" + COMMON + "#activation\\" + ONE + "#administrativeStatus");
		assertCanonical(path, UserType.class,
				"\\" + COMMON + "#assignment",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\" + ZERO + "#activation",
				"\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
	}

	// from IntegrationTestTools
	private static final String NS_RESOURCE_DUMMY_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
	private static final QName RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME = new QName(NS_RESOURCE_DUMMY_CONFIGURATION ,"uselessString");

	@Test
	public void testCanonicalizationLong() {
		UniformItemPath path = getPrismContext().path(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
				RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME);
		assertCanonical(path, null, "\\" + COMMON + "#connectorConfiguration",
				"\\" + COMMON + "#connectorConfiguration\\" + ICFS + "#configurationProperties",
				"\\" + COMMON + "#connectorConfiguration\\" + ICFS + "#configurationProperties\\" + ICF + "/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector#uselessString");
	}


	private void assertCanonical(UniformItemPath path, Class<? extends Containerable> clazz, String... representations) {
    	CanonicalItemPathImpl canonicalItemPath = CanonicalItemPathImpl.create(path, clazz, getPrismContext());
		System.out.println(path + " => " + canonicalItemPath.asString() + "  (" + clazz + ")");
		for (int i = 0; i < representations.length; i++) {
    		String c = canonicalItemPath.allUpToIncluding(i).asString();
    		assertEquals("Wrong string representation of length " + (i+1), representations[i], c);
		}
		assertEquals("Wrong string representation ", representations[representations.length-1], canonicalItemPath.asString());
	}

}