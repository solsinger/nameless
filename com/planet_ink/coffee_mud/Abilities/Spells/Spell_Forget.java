package com.planet_ink.coffee_mud.Abilities.Spells;
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
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2002-2022 Bo Zimmerman

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
public class Spell_Forget extends Spell
{

	@Override
	public String ID()
	{
		return "Spell_Forget";
	}

	private final static String localizedName = CMLib.lang().L("Forget");

	@Override
	public String name()
	{
		return localizedName;
	}

	private final static String localizedStaticDisplay = CMLib.lang().L("(Forgetful)");

	@Override
	public String displayText()
	{
		return localizedStaticDisplay;
	}

	@Override
	public int abstractQuality()
	{
		return Ability.QUALITY_MALICIOUS;
	}

	@Override
	protected int canAffectCode()
	{
		return CAN_MOBS;
	}

	@Override
	public int classificationCode()
	{
		return Ability.ACODE_SPELL|Ability.DOMAIN_ENCHANTMENT;
	}

	@Override
	public long flags()
	{
		return super.flags() | Ability.FLAG_MINDALTERING;
	}

	public HashSet<Ability> forgotten=new HashSet<Ability>();
	public HashSet<Ability> remember=new HashSet<Ability>();

	@Override
	public boolean okMessage(final Environmental myHost, final CMMsg msg)
	{
		if(!(affected instanceof MOB))
			return true;

		final MOB mob=(MOB)affected;
		if((msg.amISource(mob))
		&&(msg.tool() instanceof Ability))
		{
			if(remember.contains(msg.tool()))
				return true;
			if(forgotten.contains(msg.tool()))
			{
				mob.tell(L("You still can't remember @x1!",msg.tool().name()));
				return false;
			}
			if(mob.fetchAbility(msg.tool().ID())==msg.tool())
			{
				if(CMLib.dice().rollPercentage()>(mob.charStats().getSave(CharStats.STAT_SAVE_MIND)+25))
				{
					forgotten.add((Ability)msg.tool());
					mob.tell(L("You can't remember @x1!",msg.tool().name()));
					return false;
				}
				else
					remember.add((Ability)msg.tool());
			}
		}
		return super.okMessage(myHost,msg);
	}

	@Override
	public void unInvoke()
	{
		// undo the affects of this spell
		if(!(affected instanceof MOB))
			return;
		final MOB mob=(MOB)affected;

		super.unInvoke();

		if(canBeUninvoked())
			mob.tell(L("You start remembering things again."));
	}

	@Override
	public boolean invoke(final MOB mob, final List<String> commands, final Physical givenTarget, final boolean auto, final int asLevel)
	{
		final MOB target=this.getTarget(mob,commands,givenTarget);
		if(target==null)
			return false;

		int levelDiff=target.phyStats().level()-(mob.phyStats().level()+(2*getXLEVELLevel(mob)));
		if(levelDiff<0)
			levelDiff=0;

		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		// now see if it worked
		boolean success=proficiencyCheck(mob,-((target.charStats().getStat(CharStats.STAT_INTELLIGENCE)*2)+(levelDiff*5)),auto);
		if(success)
		{
			final String str=auto?"":L("^S<S-NAME> incant(s) confusingly at <T-NAMESELF>^?");
			final CMMsg msg=CMClass.getMsg(mob,target,this,verbalCastCode(mob,target,auto),str);
			final CMMsg msg2=CMClass.getMsg(mob,target,this,CMMsg.MSK_CAST_MALICIOUS_VERBAL|CMMsg.TYP_MIND|(auto?CMMsg.MASK_ALWAYS:0),null);
			if((mob.location().okMessage(mob,msg))&&(mob.location().okMessage(mob,msg2)))
			{
				mob.location().send(mob,msg);
				mob.location().send(mob,msg2);
				if((msg.value()<=0)&&(msg2.value()<=0))
				{
					success=maliciousAffect(mob,target,asLevel,-levelDiff,-1)!=null;
					if(success)
						mob.location().show(target,null,CMMsg.MSG_OK_VISUAL,L("<S-NAME> seem(s) forgetful!"));
				}
			}
		}
		if(!success)
			return maliciousFizzle(mob,target,L("<S-NAME> incant(s) confusingly at <T-NAMESELF>, but nothing happens."));

		// return whether it worked
		return success;
	}
}
