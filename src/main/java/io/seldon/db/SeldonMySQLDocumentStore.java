package io.seldon.db;

import io.seldon.util.CollectionUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class SeldonMySQLDocumentStore implements DocumentStore {

	Connection conn;

	public SeldonMySQLDocumentStore(String jdbcUrl)
	{
		try
	    {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(jdbcUrl);			
	    }
	    catch (ClassNotFoundException ex)
	    {
	    	System.out.println("Can't create db connection:"+ex.getMessage());
	    }
	    catch (SQLException ex)
	    {
	    	System.out.println("Can't create db connection:"+ex.getMessage());
	    }
	}
	
	public String getComments(long id) {
		try
		{
			StringBuffer buf = new StringBuffer();
			PreparedStatement stmt = conn.prepareStatement("select comment from action_comment ac,actions a where a.item_id = ? and ac.action_id=a.action_id");
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next())
			{
				String c = rs.getString(1).trim();
				//System.out.println(""+count+":"+c);
				buf.append(c);
				buf.append("\n");
				count++;
			}
			//System.out.println("found " + count +" entries for " + id);
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	

	/**
	 * Get item ids if a certain type from db
	 */
	
	public ArrayList<Long> getLatestItems(int itemType, Date d,int limit,String clientItemPattern,boolean useItemMapDatetime) {
		System.out.println("GetLatestItems with itemType"+itemType+" limit "+limit+" useItemMapDatetime:"+useItemMapDatetime);
		ArrayList<Long> ids = new ArrayList<Long>();
		try
		{
			PreparedStatement stmt;
			if (d != null)
			{
				String sql;
				if (useItemMapDatetime)
				{
					sql = "select i.item_id from items i join item_map_datetime imd on (i.item_id=imd.item_id) where type=? and imd.value>?";
					if (clientItemPattern != null)
						sql = sql + " and client_item_id like \""+clientItemPattern+"\" ";
					if (limit > 0)
						sql = sql + " order by item_id desc limit "+limit;					
				}
				else
				{
					sql = "select item_id from items where type=? and first_op>=?";
					if (clientItemPattern != null)
						sql = sql + " and client_item_id like \""+clientItemPattern+"\" ";
					if (limit > 0)
						sql = sql + " order by item_id desc limit "+limit;
				}
				System.out.println("Running sql "+sql);
				stmt= conn.prepareStatement(sql);
				stmt.setInt(1, itemType);
				stmt.setDate(2, new java.sql.Date(d.getTime()));
			}
			else
			{
				String sql = "select item_id from items where type=?";
				if (clientItemPattern != null)
					sql = sql + " and client_item_id like \""+clientItemPattern+"\" ";
				if (limit > 0)
					sql = sql + " order by item_id desc limit "+limit;
				System.out.println("Running sql "+sql);
				stmt= conn.prepareStatement(sql);
				stmt.setInt(1, itemType);
			}
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				ids.add(rs.getLong(1));
			}
			stmt.close();
		} catch (SQLException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			
		}
		return ids;
	}
	
	
	public ArrayList<Long> getUserDim(Set<Integer> attrIds) 
	{
		ArrayList<Long> ids = new ArrayList<Long>();
		try
		{
			PreparedStatement stmt;
			
			if (attrIds == null || attrIds.size() == 0)
				stmt= conn.prepareStatement("select dim_id from dimension where exists (select * from user_dim where user_dim.dim_id=dimension.dim_id)");
			else
				stmt= conn.prepareStatement("select dim_id from dimension where attr_id in ("+CollectionUtils.join(attrIds, ",")+") and exists (select * from user_dim where user_dim.dim_id=dimension.dim_id)");
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				ids.add(rs.getLong(1));
			}
			stmt.close();
		} catch (SQLException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			
		}
		return ids;
	}
	
	
	
	
	public ArrayList<Long> getLatestComments(int itemType,Date d) 
	{
		ArrayList<Long> ids = new ArrayList<Long>();
		try
		{
			PreparedStatement stmt;
			if (d != null)
			{
				stmt = conn.prepareStatement("select distinct a.item_id from actions a natural join action_comment join items on (a.item_id=items.item_id) where date>=? and items.type=?");
				stmt.setDate(1, new java.sql.Date(d.getTime()));
				stmt.setInt(2, itemType);
			}
			else
			{
				stmt = conn.prepareStatement("select distinct a.item_id from actions a natural join action_comment join items on (a.item_id=items.item_id) where items.type=?");
				stmt.setInt(1, itemType);
			}
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				ids.add(rs.getLong(1));
			}
			stmt.close();
		} catch (SQLException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			
		}
		return ids;
	}
	
	
	public Long getIdFromName(String name) {
		Long id=null;
		try
		{
			PreparedStatement stmt = conn.prepareStatement("select item_id from items where name like '"+name+"%'");
			ResultSet rs = stmt.executeQuery();
			if(rs.next())
			{
				id = rs.getLong(1);
			}
			stmt.close();
		} catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return id;
	}
	
	public String getItemTextual(long id) {
		try
		{
			StringBuffer buf = new StringBuffer();
			PreparedStatement stmt = conn.prepareStatement("SELECT CASE WHEN imi.value IS NOT NULL THEN cast(imi.value as char) WHEN imd.value IS NOT NULL THEN cast(imd.value as char) WHEN imb.value IS NOT NULL THEN cast(imb.value as char) WHEN imboo.value IS NOT NULL THEN cast(imboo.value as char) WHEN imt.value IS NOT NULL THEN imt.value WHEN imdt.value IS NOT NULL THEN cast(imdt.value as char) WHEN imv.value IS NOT NULL THEN imv.value WHEN e.value_name IS NOT NULL THEN e.value_name END value_id FROM  items i INNER JOIN item_attr a ON i.item_id=? and a.semantic = true and i.type=a.item_type LEFT JOIN item_map_int imi ON i.item_id=imi.item_id AND a.attr_id=imi.attr_id LEFT JOIN item_map_double imd ON i.item_id=imd.item_id AND a.attr_id=imd.attr_id LEFT JOIN item_map_enum ime ON i.item_id=ime.item_id AND a.attr_id=ime.attr_id LEFT JOIN item_map_bigint imb ON i.item_id=imb.item_id AND a.attr_id=imb.attr_id LEFT JOIN item_map_boolean imboo ON i.item_id=imboo.item_id AND a.attr_id=imboo.attr_id LEFT JOIN item_map_text imt ON i.item_id=imt.item_id AND a.attr_id=imt.attr_id LEFT JOIN item_map_datetime imdt ON i.item_id=imdt.item_id AND a.attr_id=imdt.attr_id LEFT JOIN item_map_varchar imv ON i.item_id=imv.item_id AND a.attr_id=imv.attr_id LEFT JOIN item_attr_enum e ON ime.attr_id =e.attr_id AND ime.value_id=e.value_id order by imv.pos");
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next())
			{
				String c = rs.getString(1);
				if (c != null)
				{
					c = c.trim();
					//System.out.println(""+count+":"+c);
					buf.append(c);
					buf.append("\n");
					count++;
				}
				
			}
			//System.out.println("found " + count +" entries for " + id);
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	public String getItemTextualById(long id,Set<Integer> attrIds) {
		try
		{
			
			StringBuffer buf = new StringBuffer();
			PreparedStatement stmt = conn.prepareStatement("SELECT CASE WHEN imi.value IS NOT NULL THEN cast(imi.value as char) WHEN imd.value IS NOT NULL THEN cast(imd.value as char) WHEN imb.value IS NOT NULL THEN cast(imb.value as char) WHEN imboo.value IS NOT NULL THEN cast(imboo.value as char) WHEN imt.value IS NOT NULL THEN imt.value WHEN imdt.value IS NOT NULL THEN cast(imdt.value as char) WHEN imv.value IS NOT NULL THEN imv.value WHEN e.value_name IS NOT NULL THEN e.value_name END value_id FROM  items i INNER JOIN item_attr a ON i.item_id=? and a.attr_id in ("+CollectionUtils.join(attrIds,",")+") and i.type=a.item_type LEFT JOIN item_map_int imi ON i.item_id=imi.item_id AND a.attr_id=imi.attr_id LEFT JOIN item_map_double imd ON i.item_id=imd.item_id AND a.attr_id=imd.attr_id LEFT JOIN item_map_enum ime ON i.item_id=ime.item_id AND a.attr_id=ime.attr_id LEFT JOIN item_map_bigint imb ON i.item_id=imb.item_id AND a.attr_id=imb.attr_id LEFT JOIN item_map_boolean imboo ON i.item_id=imboo.item_id AND a.attr_id=imboo.attr_id LEFT JOIN item_map_text imt ON i.item_id=imt.item_id AND a.attr_id=imt.attr_id LEFT JOIN item_map_datetime imdt ON i.item_id=imdt.item_id AND a.attr_id=imdt.attr_id LEFT JOIN item_map_varchar imv ON i.item_id=imv.item_id AND a.attr_id=imv.attr_id LEFT JOIN item_attr_enum e ON ime.attr_id =e.attr_id AND ime.value_id=e.value_id order by imv.pos");
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next())
			{
				String c = rs.getString(1);
				if (c != null)
				{
					c = c.trim();
					//System.out.println(""+count+":"+c);
					buf.append(c);
					buf.append("\n");
					count++;
				}
				
			}
			//System.out.println("found " + count +" entries for " + id);
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public String getItemTextualByName(long id,Set<String> attrNames) {
		try
		{
			
			StringBuffer buf = new StringBuffer();
			String attrNamesStr = CollectionUtils.join(attrNames,"\" , \"");
			if (attrNamesStr.length() > 1)
				attrNamesStr = "\"" + attrNamesStr + "\"";
			PreparedStatement stmt = conn.prepareStatement("SELECT CASE WHEN imi.value IS NOT NULL THEN cast(imi.value as char) WHEN imd.value IS NOT NULL THEN cast(imd.value as char) WHEN imb.value IS NOT NULL THEN cast(imb.value as char) WHEN imboo.value IS NOT NULL THEN cast(imboo.value as char) WHEN imt.value IS NOT NULL THEN imt.value WHEN imdt.value IS NOT NULL THEN cast(imdt.value as char) WHEN imv.value IS NOT NULL THEN imv.value WHEN e.value_name IS NOT NULL THEN e.value_name END value_id FROM  items i INNER JOIN item_attr a ON i.item_id=? and a.name in ("+attrNamesStr+") and i.type=a.item_type LEFT JOIN item_map_int imi ON i.item_id=imi.item_id AND a.attr_id=imi.attr_id LEFT JOIN item_map_double imd ON i.item_id=imd.item_id AND a.attr_id=imd.attr_id LEFT JOIN item_map_enum ime ON i.item_id=ime.item_id AND a.attr_id=ime.attr_id LEFT JOIN item_map_bigint imb ON i.item_id=imb.item_id AND a.attr_id=imb.attr_id LEFT JOIN item_map_boolean imboo ON i.item_id=imboo.item_id AND a.attr_id=imboo.attr_id LEFT JOIN item_map_text imt ON i.item_id=imt.item_id AND a.attr_id=imt.attr_id LEFT JOIN item_map_datetime imdt ON i.item_id=imdt.item_id AND a.attr_id=imdt.attr_id LEFT JOIN item_map_varchar imv ON i.item_id=imv.item_id AND a.attr_id=imv.attr_id LEFT JOIN item_attr_enum e ON ime.attr_id =e.attr_id AND ime.value_id=e.value_id order by imv.pos");
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next())
			{
				String c = rs.getString(1);
				if (c != null)
				{
					c = c.trim();
					//System.out.println(""+count+":"+c);
					buf.append(c);
					buf.append("\n");
					count++;
				}
				
			}
			//System.out.println("found " + count +" entries for " + id);
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	public ArrayList<Long> getLatestUsers(Date d) {
		ArrayList<Long> ids = new ArrayList<Long>();
		try
		{
			PreparedStatement stmt;
			if (d != null)
			{
				stmt = conn.prepareStatement("select user_id from users where active and first_op>=?");
				stmt.setDate(1, new java.sql.Date(d.getTime()));
			}
			else
			{
				stmt = conn.prepareStatement("select user_id from users where active");
			}
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				ids.add(rs.getLong(1));
			}
			stmt.close();
		} catch (SQLException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			
		}
		return ids;
	}
	
	private String getUserItemsUsingItemIds(long userId)
	{
		try
		{
			StringBuffer buf = new StringBuffer();
			PreparedStatement stmt = conn.prepareStatement("select item_id from actions where user_id=?");
			stmt.setLong(1, userId);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next())
			{
				Long itemId = rs.getLong(1);
				//System.out.println(""+count+":"+c);
				buf.append(itemId);
				buf.append(" ");
				count++;
			}
			//System.out.println("found " + count +" entries for " + id);
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	public String getUserActionAttrs(long userId,Set<Integer> attrIds) {
		try
		{
			StringBuffer buf = new StringBuffer();
			PreparedStatement stmt = conn.prepareStatement("select item_id,times from actions where user_id=?");
			stmt.setLong(1, userId);
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				Long itemId = rs.getLong(1);
				Integer times = rs.getInt(2);
				String itemText = getItemTextualById(itemId,attrIds);
				itemText = itemText.replaceAll(",", " ");
				for(int i=0;i<times;i++)
					buf.append(itemText).append("\n");
			}
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	private String getUserItemsUsingItemNames(long userId)
	{
		try
		{
			StringBuffer buf = new StringBuffer();
			PreparedStatement stmt = conn.prepareStatement("select name from actions a join items i on a.item_id=i.item_id where a.user_id=?");
			stmt.setLong(1, userId);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next())
			{
				String token = rs.getString(1);
				if (token != null)
				{
					token = token.toLowerCase().trim();
					//System.out.println(""+count+":"+c);
					buf.append(token);
					buf.append(" \n");
					count++;
				}
			}
			//System.out.println("found " + count +" entries for " + id);
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	public String getUserItems(long userId,boolean useItemIds) 
	{
		if (useItemIds)
			return getUserItemsUsingItemIds(userId);
		else
			return getUserItemsUsingItemNames(userId);
	}
	
	public String getDimTextual(long id, Set<Integer> textAttrIds,int maxItems) {
		try
		{
			StringBuffer buf = new StringBuffer();
			String sql = "select imt.value from dimension d join item_map_enum ime on d.attr_id=ime.attr_id and d.value_id=ime.value_id and d.dim_id=? join item_map_text imt on imt.item_id=ime.item_id and imt.attr_id in ("+CollectionUtils.join(textAttrIds,",")+") ";
			if (maxItems > 0)
				sql = sql + " order by imt.item_id desc limit "+maxItems;
			System.out.println("Running "+sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next())
			{
				String token = rs.getString(1);
				if (token != null)
				{
					token = token.trim();
					System.out.println(""+count+" for dim "+id);
					buf.append(token);
					buf.append(" \n");
					count++;
				}
			}
			//System.out.println("found " + count +" entries for " + id);
			stmt.close();
			return buf.toString();
		} catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
}

