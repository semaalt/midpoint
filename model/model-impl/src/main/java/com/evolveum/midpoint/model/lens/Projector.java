/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAccountReferenceType;

import static com.evolveum.midpoint.model.ModelCompiletimeConfig.CONSISTENCY_CHECKS;

/**
 * @author semancik
 *
 */
@Component
public class Projector {
	
	@Autowired(required = true)
	private ContextLoader contextLoader;
	
	@Autowired(required = true)
    private UserPolicyProcessor userPolicyProcessor;

    @Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;

    @Autowired(required = true)
    private InboundProcessor inboundProcessor;
    
    @Autowired(required = true)
    private AccountValuesProcessor accountValuesProcessor;

    @Autowired(required = true)
    private ReconciliationProcessor reconciliationProcessor;

    @Autowired(required = true)
    private CredentialsProcessor credentialsProcessor;

    @Autowired(required = true)
    private ActivationProcessor activationProcessor;
	
	private static final Trace LOGGER = TraceManager.getTrace(Projector.class);
	
	public <F extends ObjectType, P extends ObjectType> void project(LensContext<F,P> context, String activityDescription, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		if (CONSISTENCY_CHECKS) context.checkConsistence();
		
		int originalWave = context.getWave();
		
		contextLoader.load(context, activityDescription, result);
		// Set the "fresh" mark now so following consistency check will be stricter
		context.setFresh(true);
		if (CONSISTENCY_CHECKS) context.checkConsistence();
		
		sortAccountsToWaves(context);
        // Let's do one extra wave with no accounts in it. This time we expect to get the results of the execution to the user
        // via inbound, e.g. identifiers generated by the resource, DNs and similar things. Hence the +2 instead of +1
        int maxWaves = context.getMaxWave() + 2;
        // Note that the number of waves may be recomputed as new accounts appear in the context (usually after assignment step).
                
        // Start the waves ....
        LOGGER.trace("Staring the waves. There will be {} waves (or so we think now)", maxWaves);
        context.setWave(0);
        while (context.getWave() < maxWaves) {
        	
        	if (CONSISTENCY_CHECKS) context.checkConsistence();
	        // Loop through the account changes, apply inbound expressions
	        inboundProcessor.processInbound(context, result);
	        if (CONSISTENCY_CHECKS) context.checkConsistence();
	        context.recomputeFocus();
	        LensUtil.traceContext(activityDescription, "inbound", context, false);
	        if (CONSISTENCY_CHECKS) context.checkConsistence();
	
	        userPolicyProcessor.processUserPolicy(context, result);
	        context.recomputeFocus();
	        LensUtil.traceContext(activityDescription,"user policy", context, false);
	        if (CONSISTENCY_CHECKS) context.checkConsistence();
	
	        assignmentProcessor.processAssignmentsProjections(context, result);
	        context.recompute();
	        sortAccountsToWaves(context);
	        maxWaves = context.getMaxWave() + 2;
	        LensUtil.traceContext(activityDescription,"assignments", context, true);
	        if (CONSISTENCY_CHECKS) context.checkConsistence();
	
	        for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
	        	if (projectionContext.getWave() != context.getWave()) {
	        		// Let's skip accounts that do not belong into this wave.
	        		continue;
	        	}
	        	
	        	if (CONSISTENCY_CHECKS) context.checkConsistence();
	        	
	        	accountValuesProcessor.process(context, projectionContext, activityDescription, result);
	        	
	        	if (CONSISTENCY_CHECKS) context.checkConsistence();
	        	
	        	projectionContext.recompute();
	        	//SynchronizerUtil.traceContext("values", context, false);
	        	if (CONSISTENCY_CHECKS) context.checkConsistence();
	        	
	        	credentialsProcessor.processCredentials(context, projectionContext, result);
	        	
	        	projectionContext.recompute();
	        	//SynchronizerUtil.traceContext("credentials", context, false);
	        	if (CONSISTENCY_CHECKS) context.checkConsistence();
	        	
	        	activationProcessor.processActivation(context, projectionContext, result);
		        
	        	context.recompute();
	        	LensUtil.traceContext(activityDescription, "values computation", context, false);
		        if (CONSISTENCY_CHECKS) context.checkConsistence();
		
		        reconciliationProcessor.processReconciliation(context, projectionContext, result);
		        context.recompute();
		        LensUtil.traceContext(activityDescription, "reconciliation", context, false);
		        if (CONSISTENCY_CHECKS) context.checkConsistence();
	        }
	        
	        if (CONSISTENCY_CHECKS) context.checkConsistence();
	        
	        context.incrementWave();
        }
        
        context.setWave(originalWave);
		
	}
	
	public <F extends ObjectType, P extends ObjectType> void sortAccountsToWaves(LensContext<F,P> context) throws PolicyViolationException {
		for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
			determineAccountWave(context, projectionContext);
		}
	}
	
	// TODO: check for circular dependencies
	private <F extends ObjectType, P extends ObjectType> void determineAccountWave(LensContext<F,P> context, LensProjectionContext<P> accountContext) throws PolicyViolationException {
		if (accountContext.getWave() >= 0) {
			// This was already processed
			return;
		}
		int wave = 0;
		for (ResourceAccountReferenceType dependency :accountContext.getDependencies()) {
			ResourceAccountType refRat = new ResourceAccountType(dependency);
			LensProjectionContext<P> dependencyAccountContext = context.findProjectionContext(refRat);
			if (dependencyAccountContext == null) {
				throw new PolicyViolationException("Unsatisdied dependency of account "+accountContext.getResourceAccountType()+
						" dependent on "+refRat+": Account not provisioned");
			}
			determineAccountWave(context, dependencyAccountContext);
			if (dependencyAccountContext.getWave() + 1 > wave) {
				wave = dependencyAccountContext.getWave() + 1;
			}
		}
//		LOGGER.trace("Wave for {}: {}", accountContext.getResourceAccountType(), wave);
		accountContext.setWave(wave);
	}


}
