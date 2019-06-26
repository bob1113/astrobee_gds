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
package gov.nasa.arc.irg.freeflyer.utils.converters;

import gov.nasa.arc.irg.plan.model.modulebay.Module.ModuleName;

import org.eclipse.core.databinding.conversion.Converter;

public class IntToModuleConverter extends Converter {

	public IntToModuleConverter() {
		super(Integer.TYPE, ModuleName.class);
	}

	public Object convert(Object fromObject) {
		// TODO Check if fromObject is int
		int asInt = (int) fromObject;
		
		if(asInt > -1 && asInt < ModuleName.values().length) {
			return ModuleName.values()[asInt];
		}
		
		return null;
	}

}
