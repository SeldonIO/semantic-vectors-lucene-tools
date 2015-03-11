/*
 * Seldon -- open source prediction engine
 * =======================================
 * Copyright 2011-2015 Seldon Technologies Ltd and Rummble Ltd (http://www.seldon.io/)
 *
 **********************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at       
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************************** 
*/
package io.seldon.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public interface DocumentStore {

	public ArrayList<Long> getLatestUsers(Date d);
	public ArrayList<Long> getLatestComments(int itemType,Date d);
	public ArrayList<Long> getLatestItems(int itemType,Date d,int limit,String clientItemPattern,boolean useItemMapDatetime,String filterAttrEnumId);
	public ArrayList<Long> getUserDim(Set<Integer> attrIds);
	public String getComments(long id);
	public String getItemTextual(long id);
	public String getDimTextual(long id,Set<Integer> textAttrIds,int maxItems);
	public String getItemTextualById(long id,Set<Integer> attrIds);
	public String getItemTextualByName(long id,Set<String> attrNames);
	public String getUserItems(long userId,boolean useItemIds);
	public String getUserActionAttrs(long userId,Set<Integer> attrIds);
	public Long getIdFromName(String name);
	
}
