package com.planet_ink.coffee_mud.Abilities.Spells;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import com.planet_ink.coffee_mud.Abilities.Spells.interfaces.*;
import java.util.*;

public class Spell_MirrorImage extends Spell
	implements IllusionistDevotion
{
	private	Random randomizer = new Random(System.currentTimeMillis());
	private int numberOfImages = 0;
	private boolean notAgain=false;

	public Spell_MirrorImage()
	{
		super();
		myID=this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
		name="Mirror Image";

		// what the affected mob sees when they
		// bring up their affected list.
		displayText="(Mirror Image spell)";

		quality=Ability.BENEFICIAL_SELF;

		canBeUninvoked=true;
		isAutoinvoked=false;

		baseEnvStats().setLevel(11);

		addQualifyingClass("Mage",11);
		addQualifyingClass("Ranger",baseEnvStats().level()+4);

		uses=Integer.MAX_VALUE;
		recoverEnvStats();
	}

	public boolean okAffect(Affect affect)
	{
		if((affected==null)||(!(affected instanceof MOB)))
			return true;

		MOB mob=(MOB)affected;

		if((affect.amITarget(mob))&&(affect.targetMinor()==Affect.TYP_WEAPONATTACK))
		{
			if(invoker()!=null)
			{
				if(numberOfImages <= 0)
				{
					unInvoke();
					return true;
				}
				int numberOfTargets = numberOfImages + 1;
				if(randomizer.nextInt() % numberOfTargets == 0)
				{
					FullMsg msg=new FullMsg(mob,affect.source(),null,Affect.MSG_OK_ACTION,"<T-NAME> attack(s) a mirrored image!");
					if(mob.location().okAffect(msg))
						mob.location().send(mob,msg);
					numberOfImages--;
					return false;
				}
			}
		}
		return true;
	}
	public void affect(Affect affect)
	{
		super.affect(affect);

		if((affected==null)||(!(affected instanceof MOB)))
			return;

		if(notAgain) return;

		MOB mob=(MOB)affected;
		if(affect.amISource(mob))
		{
			if((
				(Util.bset(affect.othersCode(),Affect.OTH_SEE_SEEING))
				||(Util.bset(affect.othersCode(),Affect.OTH_SENSE_MOVEMENT))
				||(Util.bset(affect.othersCode(),Affect.OTH_SENSE_LISTENING))
				||(Util.bset(affect.othersCode(),Affect.OTH_SENSE_CONSUMPTION))
				||(Util.bset(affect.othersCode(),Affect.OTH_SENSE_TOUCHING)))
			&&(affect.othersMessage()!=null)
			&&(affect.othersMessage().length()>0))
			{
				notAgain=true;
				for(int x=0;x<numberOfImages;x++)
					affect.addTrailerMsg(new FullMsg(mob,affect.target(),Affect.MSG_OK_VISUAL,affect.othersMessage()));
			}
		}
		notAgain=false;
	}

	public Environmental newInstance()
	{
		return new Spell_MirrorImage();
	}

	public void affectEnvStats(Environmental affected, EnvStats affectableStats)
	{
		super.affectEnvStats(affected,affectableStats);
		affectableStats.setArmor(affectableStats.armor() - 10);
	}


	public void unInvoke()
	{
		// undo the affects of this spell
		if((affected==null)||(!(affected instanceof MOB)))
			return;
		MOB mob=(MOB)affected;

		super.unInvoke();

		mob.tell("Your mirror images fade away.");
	}



	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto)
	{
		MOB target=mob;

		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto))
			return false;

		boolean success=profficiencyCheck(0,auto);

		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.
			invoker=mob;
			numberOfImages = 2 + (randomizer.nextInt() % (int)(Math.round(Util.div(mob.envStats().level(),2.0))));
			FullMsg msg=new FullMsg(mob,target,this,affectType,(auto?"A spell forms around":"<S-NAME> incant(s) the reflective spell of")+" <T-NAME>, and suddenly " + numberOfImages + " copies appear.");
			if(mob.location().okAffect(msg))
			{
				mob.location().send(mob,msg);
				beneficialAffect(mob,target,0);
			}
		}
		else
		{
			numberOfImages = 0;
			return beneficialWordsFizzle(mob,target,"<S-NAME> chant(s) reflectively, but nothing more happens.");
		}
		// return whether it worked
		return success;
	}
}
