package com.planet_ink.coffee_mud.Commands;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.ExpertiseLibrary.CostType;
import com.planet_ink.coffee_mud.Libraries.interfaces.ExpertiseLibrary.CostManager;

import java.util.*;

/*
   Copyright 2004-2022 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class Train extends StdCommand
{
	public Train()
	{
	}

	protected Pair<String,Map<Trainable,Triad<Integer,Integer,CostType>>> trainCosts = null;

	private final String[] access=I(new String[]{"TRAIN","TR","TRA"});
	@Override
	public String[] getAccessWords()
	{
		return access;
	}

	private static enum Trainable
	{
		HITPOINTS("HIT POINTS"),
		MANA("MANA"),
		MOVES("MOVEMENT"),
		GAIN("GAIN"),
		PRACTICES("PRACTICES"),
		CCLASS(""),
		ATTRIBUTE("")
		;
		final String word;
		private Trainable(final String name)
		{
			this.word=name;
		}
	}

	public Triad<Integer,Integer,CostType> getTrainCost(final Trainable t)
	{
		final Map<Trainable,Triad<Integer,Integer,CostType>> cost = getTrainCosts();
		return cost.get(t);
	}

	public synchronized Map<Trainable,Triad<Integer,Integer,CostType>> getTrainCosts()
	{
		final String trainCostStr = CMProps.getVar(CMProps.Str.TRAINCOSTS);
		if((trainCosts == null)||(!trainCostStr.equals(trainCosts.first)))
		{
			final Map<Trainable,Triad<Integer,Integer,CostType>> costs = new Hashtable<Trainable,Triad<Integer,Integer,CostType>>();
			for(final String s : CMParms.parseCommas(trainCostStr.toUpperCase(), true))
			{
				final String[] split = CMParms.parse(s).toArray(new String[0]);
				if(split.length != 4)
					Log.errOut("Bad format on TRAINCOSTS entry in INI file: "+s);
				else
				{
					final Trainable t = (Trainable)CMath.s_valueOf(Trainable.class, split[0]);
					if(t==null)
						Log.errOut("Illegal entry type "+split[0]+" on TRAINCOSTS entry in INI file: "+s);
					else
					if(!CMath.isInteger(split[1]))
						Log.errOut("Illegal amount "+split[1]+" on TRAINCOSTS entry in INI file: "+s);
					else
					if(CMath.s_int(split[1])==0)
						continue;
					else
					if(!CMath.isInteger(split[2]))
						Log.errOut("Illegal amount "+split[2]+" on TRAINCOSTS entry in INI file: "+s);
					else
					{
						final CostType C = (CostType)CMath.s_valueOf(CostType.class, split[3]);
						if(C==null)
							Log.errOut("Illegal cost type "+split[3]+" on TRAINCOSTS entry in INI file: "+s);
						else
						{
							costs.put(t, new Triad<Integer,Integer,CostType>(
										Integer.valueOf(CMath.s_int(split[1])),
										Integer.valueOf(CMath.s_int(split[2])),
										C));
						}
					}
				}
			}
			trainCosts = new Pair<String,Map<Trainable,Triad<Integer,Integer,CostType>>>(trainCostStr,costs);
		}
		return trainCosts.second;
	}

	public List<String> getAllPossibleThingsToTrainFor()
	{
		final List<String> V=new Vector<String>();
		for(final Trainable t : getTrainCosts().keySet())
		{
			if(t.word.length()>0)
				V.add(t.word);
		}
		for(final int i: CharStats.CODES.BASECODES())
			V.add(CharStats.CODES.DESC(i));
		for(final Enumeration<CharClass> c=CMClass.charClasses();c.hasMoreElements();)
		{
			final CharClass C=c.nextElement();
			if(CMLib.login().isAvailableCharClass(C))
				V.add(C.name().toUpperCase().trim());
		}
		return V;
	}

	public Map<CharClass,Integer> getAvailableCharClasses(final MOB mob)
	{
		final Map<CharClass,Integer> classes = new HashMap<CharClass,Integer>();
		for(final Enumeration<CharClass> c=CMClass.charClasses();c.hasMoreElements();)
		{
			final CharClass C=c.nextElement();
			int classLevel=mob.charStats().getClassLevel(C);
			int trainCost = CMProps.getIntVar(CMProps.Int.CLASSSWITCHCOST);
			if(classLevel<0)
			{
				classLevel=0;
				trainCost = CMProps.getIntVar(CMProps.Int.CLASSTRAINCOST);
			}
			if((trainCost >= 0)
			&&(C.qualifiesForThisClass(mob,true))
			&&(CMLib.login().isAvailableCharClass(C)))
				classes.put(C, Integer.valueOf(trainCost));
		}
		return classes;
	}

	public String filter(final MOB mob, final String s)
	{
		return CMLib.coffeeFilter().fullOutFilter(null, mob, mob, mob, mob, s, false);
	}

	public String plural(final int amt, final String wd)
	{
		if(amt == 1)
			return wd;
		return CMLib.english().makePlural(wd);
	}
	@Override
	public boolean execute(final MOB mob, final List<String> commands, final int metaFlags)
		throws java.io.IOException
	{
		final List<String> origCmds=new StringXVector(commands);
		if(commands.size()<2)
		{
			final List<String> cols=new ArrayList<String>();
			for(final int i: CharStats.CODES.BASECODES())
			{
				final int costAmount=CMLib.login().getTrainingCost(mob, i, false);
				if(costAmount>=0)
				{
					cols.add("^H"+CMStrings.padRight(CMStrings.capitalizeAndLower(CharStats.CODES.DESC(i)),14)+"^N"
							+CMStrings.limit(L("@x1 "+plural(costAmount,"TRAIN"),""+costAmount),10));
				}
			}
			for(final Trainable t : getTrainCosts().keySet())
			{
				final Triad<Integer,Integer,CostType> cost = getTrainCosts().get(t);
				final int amt = cost.second.intValue();
				final int num = cost.first.intValue();
				cols.add("^H"+CMStrings.padRight(num+" "+CMStrings.capitalizeAndLower(t.word),14)+"^N"
						+CMStrings.limit(amt+" "+plural(amt,cost.third.name().toUpperCase()),10));
			}
			final Map<CharClass,Integer> map = getAvailableCharClasses(mob);
			for(final CharClass C : map.keySet())
			{
				final int amt = map.get(C).intValue();
				cols.add("^H"+CMStrings.padRight(C.name()+"^N",14)+"^N"
						+CMStrings.limit(amt+" "+plural(amt,"TRAIN"),10));
			}
			final String menu = CMLib.lister().buildNColTable(mob, cols, "", 3);
			CMLib.commands().postCommandFail(mob,origCmds,
					L("You have @x1 training sessions. Training costs:\n\r@x2",""+mob.getTrains(),menu));
			return false;
		}
		commands.remove(0);
		String teacherName=null;
		if(commands.size()>1)
		{
			teacherName=commands.get(commands.size()-1);
			if(teacherName.length()>1)
				commands.remove(commands.size()-1);
			else
				teacherName=null;
		}

		final String abilityName=CMParms.combine(commands,0).toUpperCase();
		final StringBuffer thingsToTrainFor=new StringBuffer("");
		for(final int i: CharStats.CODES.BASECODES())
			thingsToTrainFor.append(CharStats.CODES.DESC(i)+", ");
		for(final Trainable t : getTrainCosts().keySet())
			thingsToTrainFor.append(t.word+", ");

		int costAmount=1;
		int gainAmount=1;
		CostType costType=null;
		Trainable trainType = null;
		int curStat=-1;
		final int abilityCode=mob.baseCharStats().getStatCode(abilityName);
		if((abilityCode>=0)
		&&(CharStats.CODES.isBASE(abilityCode)))
		{
			trainType = Trainable.ATTRIBUTE;
			curStat=mob.baseCharStats().getRacialStat(mob, abilityCode);
			costAmount=CMLib.login().getTrainingCost(mob, abilityCode, false);
			if(costAmount<0)
				return false;
			costType=CostType.TRAIN;
		}
		CharClass theClass=null;
		if((!CMSecurity.isDisabled(CMSecurity.DisFlag.CLASSTRAINING))&&(abilityCode<0))
		{
			for(final Enumeration<CharClass> c=CMClass.charClasses();c.hasMoreElements();)
			{
				final CharClass C=c.nextElement();
				int classLevel=mob.charStats().getClassLevel(C);
				int trainCost = CMProps.getIntVar(CMProps.Int.CLASSSWITCHCOST);
				if(classLevel<0)
				{
					classLevel=0;
					trainCost = CMProps.getIntVar(CMProps.Int.CLASSTRAINCOST);
				}
				if((C.name().toUpperCase().startsWith(abilityName.toUpperCase()))
				||(C.name(classLevel).toUpperCase().startsWith(abilityName.toUpperCase())))
				{
					if((C.qualifiesForThisClass(mob,false))
					&&(CMLib.login().isAvailableCharClass(C)))
					{
						trainType = Trainable.CCLASS;
						theClass=C;
						costAmount=trainCost;
						if(costAmount<0)
							return false;
						costType=CostType.TRAIN;
					}
					break;
				}
				else
				if((C.qualifiesForThisClass(mob,true))
				&&(CMLib.login().isAvailableCharClass(C)))
					thingsToTrainFor.append(C.name()+", ");
			}
		}

		if(trainType==null)
		{
			for(final Trainable t : Trainable.values())
			{
				if((t.word.length()>0)
				&&(t.word.startsWith(abilityName.toUpperCase())))
				{
					final Triad<Integer,Integer,CostType> cost = getTrainCost(t);
					if(cost != null)
					{
						trainType = t;
						costAmount = cost.second.intValue();
						gainAmount = cost.first.intValue();
						costType = cost.third;
						break;
					}
				}
			}
			if(trainType==null)
			{
				CMLib.commands().postCommandFail(mob,origCmds,L("You can't train for '@x1'. Try: @x2.",abilityName,thingsToTrainFor.toString()));
				return false;
			}
		}
		final CostManager cost = CMLib.expertises().createCostManager(costType, Double.valueOf(costAmount));
		if(!cost.doesMeetCostRequirements(mob))
		{
			final String ofWhat=cost.costType(mob);
			mob.tell(L("You do not have enough @x1.  You need @x2.",ofWhat,cost.requirements(mob)));
			return false;
		}


		MOB teacher=null;
		if(teacherName!=null)
			teacher=mob.location().fetchInhabitant(teacherName);
		if(teacher==null)
		{
			for(int i=0;i<mob.location().numInhabitants();i++)
			{
				final MOB possTeach=mob.location().fetchInhabitant(i);
				if((possTeach!=null)&&(possTeach!=mob))
				{
					teacher=possTeach;
					break;
				}
			}
		}
		if((teacher==null)||(!CMLib.flags().canBeSeenBy(teacher,mob)))
		{
			if(teacher == null)
			{
				if(teacherName==null)
					mob.tell(teacher,null,null,L("There is no one here to train you!"));
				else
					mob.tell(teacher,null,null,L("'@x1' is no one here to train you!",teacherName));
			}
			else
				mob.tell(teacher,null,null,L("You can't see <S-NAME>!"));
			return false;
		}
		if(!CMLib.flags().canBeSeenBy(mob,teacher))
		{
			mob.tell(teacher,null,null,L("<S-NAME> can't see you!"));
			return false;
		}
		if(teacher==mob)
		{
			CMLib.commands().postCommandFail(mob,origCmds,L("You cannot train with yourself!"));
			return false;
		}
		if(teacher.isAttributeSet(MOB.Attrib.NOTEACH))
		{
			CMLib.commands().postCommandFail(mob,origCmds,L("@x1 is refusing to teach right now.",teacher.name()));
			return false;
		}
		if(mob.isAttributeSet(MOB.Attrib.NOTEACH))
		{
			CMLib.commands().postCommandFail(mob,origCmds,L("You are refusing training at this time."));
			return false;
		}
		if(CMLib.flags().isSleeping(mob)||CMLib.flags().isSitting(mob))
		{
			CMLib.commands().postCommandFail(mob,origCmds,L("You need to stand up for your training."));
			return false;
		}
		if(CMLib.flags().isSleeping(teacher)||CMLib.flags().isSitting(teacher))
		{
			if(teacher.isMonster())
				CMLib.commands().postStand(teacher,true, false);
			if(CMLib.flags().isSleeping(teacher)||CMLib.flags().isSitting(teacher))
			{
				CMLib.commands().postCommandFail(mob,origCmds,L("@x1 looks a bit too relaxed to train with you.",teacher.name()));
				return false;
			}
		}
		if(mob.isInCombat())
		{
			CMLib.commands().postCommandFail(mob,origCmds,L("Not while you are fighting!"));
			return false;
		}
		if(teacher.isInCombat())
		{
			CMLib.commands().postCommandFail(mob,origCmds,L("Your teacher seems busy right now."));
			return false;
		}

		if(trainType==Trainable.CCLASS)
		{
			boolean canTeach=false;
			for(int c=0;c<teacher.charStats().numClasses();c++)
			{
				if(teacher.charStats().getMyClass(c).baseClass().equals(mob.charStats().getCurrentClass().baseClass()))
					canTeach=true;
			}
			if((!canTeach)
			&&(teacher.charStats().getClassLevel(theClass)<1))
			{
				if((!CMProps.getVar(CMProps.Str.MULTICLASS).startsWith("MULTI"))
				&&(!CMProps.getVar(CMProps.Str.MULTICLASS).endsWith("MULTI")))
				{
					final CharClass C=CMClass.getCharClass(mob.charStats().getCurrentClass().baseClass());
					final String baseClassName=(C!=null)?C.name():mob.charStats().getCurrentClass().baseClass();
					CMLib.commands().postCommandFail(mob,origCmds,L("You can only learn that from another @x1.",baseClassName));
				}
				else
				if(theClass!=null)
				{
					int classLevel=mob.charStats().getClassLevel(theClass);
					if(classLevel<0)
						classLevel=0;
					CMLib.commands().postCommandFail(mob,origCmds,L("You can only learn that from another @x1.",theClass.name(classLevel)));
				}
				return false;
			}
		}

		if(trainType == Trainable.ATTRIBUTE)
		{
			final int teachStat=teacher.charStats().getStat(abilityCode);
			if(curStat>=teachStat)
			{
				CMLib.commands().postCommandFail(mob,origCmds,L("You can only train with someone whose score is higher than yours."));
				return false;
			}
			curStat=mob.baseCharStats().getStat(abilityCode);
		}

		final CMMsg msg=CMClass.getMsg(teacher,mob,null,CMMsg.MSG_NOISYMOVEMENT,L("<S-NAME> train(s) with <T-NAMESELF>."));
		if(!mob.location().okMessage(mob,msg))
			return false;
		mob.location().send(mob,msg);
		cost.spendSkillCost(mob);
		switch(trainType)
		{
		case MANA:
			mob.tell(L("You feel more powerful!"));
			mob.baseState().setMana(mob.baseState().getMana()+gainAmount);
			mob.maxState().setMana(mob.maxState().getMana()+gainAmount);
			mob.curState().setMana(mob.curState().getMana()+gainAmount);
			break;
		case MOVES:
			mob.tell(L("You feel more rested!"));
			mob.baseState().setMovement(mob.baseState().getMovement()+gainAmount);
			mob.maxState().setMovement(mob.maxState().getMovement()+gainAmount);
			mob.curState().setMovement(mob.curState().getMovement()+gainAmount);
			break;
		case GAIN:
			mob.tell(L("You feel more trainable!"));
			mob.setTrains(mob.getTrains()+gainAmount);
			break;
		case PRACTICES:
			mob.tell(L("You feel more educatable!"));
			mob.setPractices(mob.getPractices()+gainAmount);
			break;
		case ATTRIBUTE:
		default:
			switch(abilityCode)
			{
			case CharStats.STAT_STRENGTH:
				mob.tell(L("You feel stronger!"));
				mob.baseCharStats().setStat(CharStats.STAT_STRENGTH,curStat+gainAmount);
				mob.recoverCharStats();
				break;
			case CharStats.STAT_INTELLIGENCE:
				mob.tell(L("You feel smarter!"));
				mob.baseCharStats().setStat(CharStats.STAT_INTELLIGENCE,curStat+gainAmount);
				mob.recoverCharStats();
				break;
			case CharStats.STAT_DEXTERITY:
				mob.tell(L("You feel more dextrous!"));
				mob.baseCharStats().setStat(CharStats.STAT_DEXTERITY,curStat+gainAmount);
				mob.recoverCharStats();
				break;
			case CharStats.STAT_CONSTITUTION:
				mob.tell(L("You feel healthier!"));
				mob.baseCharStats().setStat(CharStats.STAT_CONSTITUTION,curStat+gainAmount);
				mob.recoverCharStats();
				break;
			case CharStats.STAT_CHARISMA:
				mob.tell(L("You feel more charismatic!"));
				mob.baseCharStats().setStat(CharStats.STAT_CHARISMA,curStat+gainAmount);
				mob.recoverCharStats();
				break;
			case CharStats.STAT_WISDOM:
				mob.tell(L("You feel wiser!"));
				mob.baseCharStats().setStat(CharStats.STAT_WISDOM,curStat+gainAmount);
				mob.recoverCharStats();
				break;
			default:
				if(CMParms.contains(CharStats.CODES.BASECODES(), abilityCode))
				{
					mob.tell(L("You feel more @x1!",CharStats.CODES.NAME(abilityCode)));
					mob.baseCharStats().setStat(abilityCode,curStat+gainAmount);
					mob.recoverCharStats();
				}
				break;
			}
			break;
		case HITPOINTS:
			mob.tell(L("You feel even healthier!"));
			mob.baseState().setHitPoints(mob.baseState().getHitPoints()+gainAmount);
			mob.maxState().setHitPoints(mob.maxState().getHitPoints()+gainAmount);
			mob.curState().setHitPoints(mob.curState().getHitPoints()+gainAmount);
			break;
		case CCLASS:
			if(theClass!=null)
			{
				int classLevel=mob.charStats().getClassLevel(theClass);
				if(classLevel<0)
					classLevel=0;
				mob.tell(L("You have undergone @x1 training!",theClass.name(classLevel)));
				mob.baseCharStats().getCurrentClass().endCharacter(mob);
				mob.baseCharStats().setCurrentClass(theClass);
				if((!mob.isMonster())&&(mob.soulMate()==null))
					CMLib.coffeeTables().bump(mob,CoffeeTableRow.STAT_CLASSCHANGE);
				mob.recoverCharStats();
				mob.charStats().getCurrentClass().startCharacter(mob,false,true);
			}
			break;
		}
		return false;
	}

	@Override
	public double combatActionsCost(final MOB mob, final List<String> cmds)
	{
		return CMProps.getCommandCombatActionCost(ID());
	}

	@Override
	public double actionsCost(final MOB mob, final List<String> cmds)
	{
		return CMProps.getCommandActionCost(ID());
	}

	@Override
	public boolean canBeOrdered()
	{
		return false;
	}
}
