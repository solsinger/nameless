package com.planet_ink.coffee_mud.system;

import java.sql.*;
import java.util.*;
import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;

public class StatLoader
{
	public static CoffeeTables DBRead(long startTime)
	{
		if(Log.debugChannelOn()&&(CMSecurity.isDebugging("CMSTAT")))
			Log.debugOut("StatLoader","Reading content of Stat  "+new IQCalendar(startTime).d2String());
		DBConnection D=null;
		CoffeeTables T=null;
		try
		{
			D=DBConnector.DBFetch();
			ResultSet R=D.query("SELECT * FROM CMSTAT WHERE CMSTRT="+startTime);
			T=new CoffeeTables();
			if(R.next())
			{
				long endTime=DBConnections.getLongRes(R,"CMENDT");
				String data=DBConnections.getRes(R,"CMDATA");
				T.populate(startTime,endTime,data);
			}
		}
		catch(Exception sqle)
		{
			Log.errOut("DataLoader",sqle);
		}
		if(D!=null) DBConnector.DBDone(D);
		// log comment 
		return T;
	}
	
	public static Vector DBReadAfter(long startTime)
	{
		if(Log.debugChannelOn()&&(CMSecurity.isDebugging("CMSTAT")))
			Log.debugOut("StatLoader","Reading content of Stats since "+new IQCalendar(startTime).d2String());
		DBConnection D=null;
		CoffeeTables T=null;
		Vector rows=new Vector();
		try
		{
			D=DBConnector.DBFetch();
			ResultSet R=D.query("SELECT * FROM CMSTAT WHERE CMSTRT > "+startTime);
			while(R.next())
			{
				T=new CoffeeTables();
				long strTime=DBConnections.getLongRes(R,"CMSTRT");
				long endTime=DBConnections.getLongRes(R,"CMENDT");
				String data=DBConnections.getRes(R,"CMDATA");
				T.populate(strTime,endTime,data);
				rows.addElement(T);
			}
		}
		catch(Exception sqle)
		{
			Log.errOut("DataLoader",sqle);
		}
		if(D!=null) DBConnector.DBDone(D);
		// log comment 
		return rows;
	}
	
	public static void DBDelete(long startTime)
	{
		if(Log.debugChannelOn()&&(CMSecurity.isDebugging("CMSTAT")))
			Log.debugOut("StatLoader","Deleting Stat  "+new IQCalendar(startTime).d2String());
		try
		{
			DBConnector.update("DELETE FROM CMSTAT WHERE CMSTRT="+startTime);
		}
		catch(Exception sqle)
		{
			Log.errOut("DataLoader",sqle);
		}
	}
	public static void DBUpdate(long startTime, String data)
	{
		if(Log.debugChannelOn()&&(CMSecurity.isDebugging("CMSTAT")))
			Log.debugOut("StatLoader","Updating Stat  "+new IQCalendar(startTime).d2String());
		try
		{
			DBConnector.update("UPDATE CMSTAT SET CMDATA='"+data+"' WHERE CMSTRT="+startTime);
		}
		catch(Exception sqle)
		{
			Log.errOut("DataLoader",sqle);
		}
	}
	public static void DBCreate(long startTime, long endTime, String data)
	{
		if(Log.debugChannelOn()&&(CMSecurity.isDebugging("CMSTAT")))
			Log.debugOut("StatLoader","Creating Stat  "+new IQCalendar(startTime).d2String());
		DBConnector.update(
		 "INSERT INTO CMSTAT ("
		 +"CMSTRT, "
		 +"CMENDT, "
		 +"CMDATA "
		 +") values ("
		 +""+startTime+","
		 +""+endTime+","
		 +"'"+data+"'"
		 +")");
	}
}