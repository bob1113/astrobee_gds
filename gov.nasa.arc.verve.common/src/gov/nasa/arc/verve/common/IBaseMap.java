/*******************************************************************************
 * Copyright (c) 2013 United States Government as represented by the 
 * Administrator of the National Aeronautics and Space Administration. 
 * All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package gov.nasa.arc.verve.common;

import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;

public interface IBaseMap {
    float getHeightAt(float x, float y);
    
    /** get height at a specific resolution level */
    float getHeightAt(float x, float y, int level);
    /** maximum resolution level */
    int maxLevel();
    
    /** get intersection of ray with terrain */
    Vector3 getIntersection(Ray3 ray);
    
    //Terrain is ardor3d extension, this should not be in common
    //Terrain getTerrain();
}
