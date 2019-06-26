/******************************************************************************
 * Copyright © 2019, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration. All 
 * rights reserved.
 * 
 * The Astrobee Control Station platform is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0. 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 *****************************************************************************/
package gov.nasa.arc.verve.freeflyer.workbench.plantrace;

import gov.nasa.arc.irg.freeflyer.rapid.FreeFlyerStrings;
import gov.nasa.arc.irg.freeflyer.rapid.runningplan.AbstractGuestScienceRunningPlanInfo;
import gov.nasa.arc.irg.freeflyer.rapid.runningplan.RunningPlanInfo;
import gov.nasa.rapid.v2.e4.agent.Agent;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;

public class AbstractGuestSciencePlanTrace extends RunPlanTrace {
	int ordinal;
	
	public AbstractGuestSciencePlanTrace(int number) {
		super("GuestSciencePlanTrace" + number);
//		System.out.println("*** Creating GuestSciencePlanTrace" + number);
		ordinal = number;
	}
	
	public void setGuestScienceRunningPlanInfo(AbstractGuestScienceRunningPlanInfo gsrpi) {
		ingestRunningPlanInfo(gsrpi);
	}

	@Inject
	@Optional
	@Override
	public void onAgentSelected(@Named(FreeFlyerStrings.PRIMARY_BEE) Agent primaryBee) {
		// do nothing
	}

	@Inject @Optional
	public void acceptRunningPlanInfo(@Named(FreeFlyerStrings.RUNNING_PLAN_INFO)RunningPlanInfo rpi) {
		// do nothing
	}
	
	public String toString() {
		return "GuestSciencePlanTrace" + ordinal;
	}
}
