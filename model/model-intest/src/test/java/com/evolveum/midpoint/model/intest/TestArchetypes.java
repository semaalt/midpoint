/*
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestArchetypes extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/archetypes");
	
	public static final File SYSTEM_CONFIGURATION_ARCHETYPES_FILE = new File(TEST_DIR, "system-configuration-archetypes.xml");
	
	public static final File ARCHETYPE_EMPLOYEE_FILE = new File(TEST_DIR, "archetype-employee.xml");
	protected static final String ARCHETYPE_EMPLOYEE_OID = "7135e68c-ee53-11e8-8025-170b77da3fd6";
	private static final String ARCHETYPE_EMPLOYEE_DISPLAY_LABEL = "Employee";
	private static final String ARCHETYPE_EMPLOYEE_DISPLAY_PLURAL_LABEL = "Employees";
	
	public static final File ARCHETYPE_TEST_FILE = new File(TEST_DIR, "archetype-test.xml");
	protected static final String ARCHETYPE_TEST_OID = "a8df34a8-f6f0-11e8-b98e-eb03652d943f";
	
	public static final File ROLE_EMPLOYEE_BASE_FILE = new File(TEST_DIR, "role-employee-base.xml");
	protected static final String ROLE_EMPLOYEE_BASE_OID = "e869d6c4-f6ef-11e8-b51f-df3e51bba129";

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        
        repoAddObjectFromFile(ROLE_EMPLOYEE_BASE_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_TEST_FILE, initResult);
        
        addObject(SHADOW_GROUP_DUMMY_TESTERS_FILE, initTask, initResult);
    }
	
	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_ARCHETYPES_FILE;
	}

    @Test
    public void test050AddArchetypeEmployee() throws Exception {
		final String TEST_NAME = "test050SetupJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        addObject(ARCHETYPE_EMPLOYEE_FILE, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<ArchetypeType> archetypeEmployee = modelService.getObject(ArchetypeType.class, ARCHETYPE_EMPLOYEE_OID, null, task, result);
        display("Archetype employee", archetypeEmployee);
    }
    

	@Test
    public void test100AssignJackArchetypeEmployee() throws Exception {
		final String TEST_NAME = "test100AssignJackArchetypeEmployee";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignArchetype(USER_JACK_OID, ARCHETYPE_EMPLOYEE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = assertUserAfter(USER_JACK_OID)
        	.assignments()
        		.assertAssignments(1)
        		.assertArchetype(ARCHETYPE_EMPLOYEE_OID)
        		.end()
        	.assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID)
        	.roleMembershipRefs()
        		.assertRoleMemberhipRefs(1)
        		.assertArchetype(ARCHETYPE_EMPLOYEE_OID)
        		.end()
        	.getObject();
        
        assertArchetypeSpec(userAfter)
	        .archetypePolicy()
	        	.displayType()
	        		.assertLabel(ARCHETYPE_EMPLOYEE_DISPLAY_LABEL)
	        		.assertPluralLabel(ARCHETYPE_EMPLOYEE_DISPLAY_PLURAL_LABEL);
    }
	
	@Test
    public void test109UnassignJackArchetypeEmployee() throws Exception {
		final String TEST_NAME = "test109UnassignJackArchetypeEmployee";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignArchetype(USER_JACK_OID, ARCHETYPE_EMPLOYEE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = assertUserAfter(USER_JACK_OID)
        	.assignments()
        		.assertAssignments(0)
        		.end()
        	.assertNoArchetypeRef()
        	.roleMembershipRefs()
        		.assertRoleMemberhipRefs(0)
        		.end()
        	.getObject();
        
        assertArchetypeSpec(userAfter)
        	.assertNull();
    }
	
	@Test
    public void test110AssignJackRoleEmployeeBase() throws Exception {
		final String TEST_NAME = "test110AssignJackRoleEmployeeBase";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignRole(USER_JACK_OID, ROLE_EMPLOYEE_BASE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
        	.assignments()
        		.assertAssignments(1)
        		.assertRole(ROLE_EMPLOYEE_BASE_OID)
        		.end()
        	.assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID)
        	.roleMembershipRefs()
        		.assertRoleMemberhipRefs(2)
        		.assertArchetype(ARCHETYPE_EMPLOYEE_OID)
        		.assertRole(ROLE_EMPLOYEE_BASE_OID);
    }
	
	@Test
    public void test119UnassignJackRoleEmployeeBase() throws Exception {
		final String TEST_NAME = "test119UnassignJackRoleEmployeeBase";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignRole(USER_JACK_OID, ROLE_EMPLOYEE_BASE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
	    	.assignments()
	    		.assertAssignments(0)
	    		.end()
	    	.assertNoArchetypeRef()
	    	.roleMembershipRefs()
	    		.assertRoleMemberhipRefs(0);
    }
	
	@Test
    public void test120AssignJackArchetypeTest() throws Exception {
		final String TEST_NAME = "test120AssignJackArchetypeTest";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignArchetype(USER_JACK_OID, ARCHETYPE_TEST_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
        	.assignments()
        		.assertAssignments(1)
        		.assertArchetype(ARCHETYPE_TEST_OID)
        		.end()
        	.assertArchetypeRef(ARCHETYPE_TEST_OID)
        	.roleMembershipRefs()
        		.assertRoleMemberhipRefs(1)
        		.assertArchetype(ARCHETYPE_TEST_OID)
        		.end()
        	.singleLink()
        		.target()
        			.assertResource(RESOURCE_DUMMY_OID)
        			.assertKind(ShadowKindType.ACCOUNT)
        			.assertIntent(INTENT_TEST)
        			.end()
        		.end();
    }
	
	@Test
    public void test129UnassignJackArchetypeTest() throws Exception {
		final String TEST_NAME = "test129UnassignJackArchetypeTest";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignArchetype(USER_JACK_OID, ARCHETYPE_TEST_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
    	.assignments()
    		.assertAssignments(0)
    		.end()
    	.assertNoArchetypeRef()
    	.roleMembershipRefs()
    		.assertRoleMemberhipRefs(0)
    		.end()
    	.links()
    		.assertNone();
    }
	
	// TODO: search by archetypeRef
	
	// TODO: object template in archetype
	// TODO: correct application of object template for new object (not yet stored)
	
	// TODO: assertArchetypeSpec() for new object (not yet stored)
	
	// TODO: assignmentRelation (assertArchetypeSpec)

}
