package natlab.tame.builtin.isComplexInfoProp.ast;

import java.util.ArrayList;
import java.util.List;

import natlab.tame.builtin.isComplexInfoProp.isComplexInfoPropMatch;
import natlab.tame.builtin.shapeprop.ShapePropMatch;
import natlab.tame.valueanalysis.value.Args;
import natlab.tame.valueanalysis.value.Value;

public class ICTypeA extends ICType{
	
	
	public ICTypeA()
	{
		
	}

	
	public String toString()
	{
		return "A";
	}


	@Override
	public isComplexInfoPropMatch match(boolean isPatternSide,
			isComplexInfoPropMatch previousMatchResult, List<Integer> argValues) {
		// TODO Auto-generated method stub
		return null;
	}
}
