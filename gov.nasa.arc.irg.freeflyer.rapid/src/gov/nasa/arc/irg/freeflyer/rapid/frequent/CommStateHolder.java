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
package gov.nasa.arc.irg.freeflyer.rapid.frequent;

import gov.nasa.arc.irg.freeflyer.rapid.state.CommStateGds;
import gov.nasa.rapid.idl.ext.astrobee.message.MessageTypeExtAstro;
import gov.nasa.rapid.v2.e4.agent.Agent;
import gov.nasa.rapid.v2.e4.message.MessageType;

import javax.inject.Inject;

import org.eclipse.e4.ui.model.application.MApplication;

import rapid.ext.astrobee.CommState;

public class CommStateHolder extends AbstractFrequentTelemetryHolder {
	protected CommStateGds commStateGds = new CommStateGds();

	@Inject
	public CommStateHolder(MApplication application) {
		sampleType = MessageTypeExtAstro.COMM_STATE_TYPE;

		topContext = application.getContext();

		init();
	}

	@Override
	protected void init() {
		super.init();
		topContext.set(CommStateHolder.class, this);
	}


	@Override
	public void onRapidMessageReceived(Agent agent, MessageType msgType,
			Object msgObj, Object cfgObj) {
		if(msgType.equals(MessageTypeExtAstro.COMM_STATE_TYPE)) { 
			CommState commState = (CommState) msgObj;
			ingestCommState(commState);
		}
	}

	public void ingestCommState(CommState commState) {
		if(commState == null) {
			return;
		}
		commStateGds.ingestCommState(commState);
	}

	public CommStateGds getCommStateGds() {
		return commStateGds;
	}
}
