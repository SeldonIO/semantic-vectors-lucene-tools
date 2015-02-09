package io.seldon.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public interface DocumentStore {

	public ArrayList<Long> getLatestUsers(Date d);
	public ArrayList<Long> getLatestComments(int itemType,Date d);
	public ArrayList<Long> getLatestItems(int itemType,Date d,int limit,String clientItemPattern,boolean useItemMapDatetime);
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
