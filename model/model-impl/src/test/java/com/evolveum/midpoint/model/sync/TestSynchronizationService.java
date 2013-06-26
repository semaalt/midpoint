/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.sync;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.util.mock.MockLensDebugListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSynchronizationService extends AbstractInternalModelIntegrationTest {
		
	@Autowired(required = true)
	SynchronizationService synchronizationService;
	
	@Autowired(required = true)
	Clockwork clockwork;
	
	private String accountShadowJackDummyOid = null;
	
	public TestSynchronizationService() throws JAXBException {
		super();
	}
		
	@Test
    public void test010AddedAccountJack() throws Exception {
		final String TEST_NAME = "test010AddedAccountJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME, ShadowType.class, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        provisioningService.applyDefinition(accountShadowJack, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
		dummyResource.addAccount(dummyAccount);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType, ShadowType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertEffectiveActivationDeltaOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), null);
		LensProjectionContext<ShadowType> accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		
		assertNull("Unexpected account primary delta", accCtx.getPrimaryDelta());
		assertNotNull("Missing account secondary delta", accCtx.getSecondaryDelta());
		
		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());
                  
	}
	
	@Test
    public void test020ModifyLootAbsolute() throws Exception {
		final String TEST_NAME = "test020ModifyLootAbsolute";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, "999");
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType, ShadowType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertEquals("Unexpected number of modifications in user secondary delta", 3, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_COST_CENTER, "999");
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), null);
		LensProjectionContext<ShadowType> accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		
		assertNull("Unexpected account primary delta", accCtx.getPrimaryDelta());
		assertNull("Unexpected account secondary delta", accCtx.getSecondaryDelta());
		
		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());
		
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertEquals("Unexpected used constCenter", "999", user.asObjectable().getCostCenter());
	}
	
	@Test
    public void test021ModifyLootAbsoluteEmpty() throws Exception {
		final String TEST_NAME = "test021ModifyLootAbsoluteEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
        
        display("SENDING CHANGE NOTIFICATION", change);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType, ShadowType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertEquals("Unexpected number of modifications in user secondary delta", 3, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_COST_CENTER);
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), null);
		LensProjectionContext<ShadowType> accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		
		assertNull("Unexpected account primary delta", accCtx.getPrimaryDelta());
		assertNull("Unexpected account secondary delta", accCtx.getSecondaryDelta());
		
		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());
		
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertEquals("Unexpected used constCenter", null, user.asObjectable().getCostCenter());
	}


}
