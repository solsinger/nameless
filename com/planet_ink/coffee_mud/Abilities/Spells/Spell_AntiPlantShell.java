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
   Copyright 2002-2014 Bo Zimmerman

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
@SuppressWarnings("rawtypes")
public class Spell_AntiPlantShell extends Spell
{
	@Override public String ID() { return "Spell_AntiPlantShell"; }
	private final static String localizedName = CMLib.lang().L("Anti Plant Shell");
	@Override public String name() { return localizedName; }
	private final static String localizedStaticDisplay = CMLib.lang().L("(Anti Plant Shell)");
	@Override public String displayText() { return localizedStaticDisplay; }
	@Override public int abstractQuality(){ return Ability.QUALITY_BENEFICIAL_OTHERS;}
	@Override protected int canAffectCode(){return CAN_MOBS;}
	@Override protected int canTargetCode(){return 0;}
	@Override public int classificationCode(){return Ability.ACODE_SPELL|Ability.DOMAIN_ABJURATION;}

	protected int pointsRemaining = 5;
	
	@Override
	public void setAffectedOne(Physical affected)
	{
		if(super.affected != affected)
			pointsRemaining = 3 + (adjustedLevel(invoker(),0)/5);
		super.setAffectedOne(affected);
	}
	
	@Override
	public boolean okMessage(final Environmental myHost, final CMMsg msg)
	{
		if(!super.okMessage(myHost, msg))
			return false;

		if((msg.target() == affected) 
		&& msg.isTarget(CMMsg.MASK_MALICIOUS)
		&& CMLib.flags().isAPlant(msg.source())
		&&(affected instanceof MOB)
		&&(pointsRemaining >= 0))
		{
			final MOB mob=(MOB)affected;
			final Room R=msg.source().location();
			if((R!=null)&&(R==mob.location()))
			{
				if((msg.isSource(CMMsg.TYP_ADVANCE))
				&& (--pointsRemaining >=0))
				{
					R.show(msg.source(), affected, CMMsg.MSG_OK_ACTION, L("<S-NAME> struggle(s) against <T-YOUPOSS> anti-plant shell."));
					return false;
				}
				else
				if(msg.source().getVictim() == null)
				{
					msg.source().setVictim(mob);
					msg.source().setRangeToTarget(R.maxRange());
					R.show(msg.source(), affected, CMMsg.MSG_OK_ACTION, L("<S-NAME> <S-IS-ARE> repelled by <T-YOUPOSS> anti-plant shell."));
					return false;
				}
				else
				if((msg.source().getVictim() == affected) && (msg.source().rangeToTarget() <= 0))
				{
					msg.source().setRangeToTarget(R.maxRange());
					R.show(msg.source(), affected, CMMsg.MSG_OK_ACTION, L("<S-NAME> <S-IS-ARE> repelled by <T-YOUPOSS> anti-plant shell."));
					return false;
				}
				else
				if((mob.getVictim()==msg.source())&&(mob.rangeToTarget() <= 0))
				{
					mob.setRangeToTarget(R.maxRange());
					if(R!=null)
						R.show(msg.source(), affected, CMMsg.MSG_OK_ACTION, L("<S-NAME> <S-IS-ARE> repelled by <T-YOUPOSS> anti-plant shell."));
					return false;
				}
			}
		}
		else
		if(msg.isSource(CMMsg.TYP_ADVANCE)
		&&(msg.source() == affected)
		&& (msg.source().getVictim()==msg.target())
		&& (CMLib.flags().isAPlant((MOB)msg.target()))
		&& (pointsRemaining >=0)
		&& (msg.source().rangeToTarget() == 1))
		{
			final MOB plantM=msg.source().getVictim();
			if(plantM != null)
			{
				final Room R=plantM.location();
				if(R!=null)
				{
					final CMMsg msg2=CMClass.getMsg(plantM,msg.source(),CMMsg.MSG_RETREAT,L("<S-NAME> <S-IS-ARE> pushed back by <T-YOUPOSS> anti-plant shell."));
					if(R.okMessage(plantM,msg2))
						R.send(plantM,msg2);
				}
			}
		}
		if(pointsRemaining < 0)
		{
			unInvoke();
			return true;
		}
		return true;
	}

	@Override
	public void unInvoke()
	{
		final Physical affected=super.affected;
		super.unInvoke();
		if(canBeUninvoked())
		{
			if(affected instanceof MOB)
				((MOB)affected).tell(L("Your anti-plant shell fades."));
		}
	}

	@Override
	public boolean invoke(MOB mob, Vector commands, Physical givenTarget, boolean auto, int asLevel)
	{
		final MOB target=getTarget(mob,commands,givenTarget);
		if(target==null)
			return false;
		
		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		final boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.

			final CMMsg msg = CMClass.getMsg(mob, target, this,verbalCastCode(mob,target,auto),auto?L("An anti-plant shell surrounds <T-NAME>!"):L("^S<S-NAME> cast(s) the anti-plant shell around <T-NAMESELF>!^?"));
			if(mob.location().okMessage(mob,msg))
			{
				mob.location().send(mob,msg);
				beneficialAffect(mob,target,asLevel,0);
			}
		}
		else
			return beneficialWordsFizzle(mob,target,L("<S-NAME> cast(s) a shell at <T-NAMESELF>, but the magic fizzles."));

		// return whether it worked
		return success;
	}
}
