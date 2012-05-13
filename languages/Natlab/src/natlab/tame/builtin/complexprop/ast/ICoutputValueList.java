package natlab.tame.builtin.isComplexInfoProp.ast;

import java.util.ArrayList;
import java.util.List;

import natlab.tame.builtin.isComplexInfoProp.isComplexInfoPropMatch;
import natlab.tame.builtin.shapeprop.ShapePropMatch;
import natlab.tame.valueanalysis.value.Args;
import natlab.tame.valueanalysis.value.Value;

public class ICoutputValueList extends ICNode{

	 ICoutputValueList ovl;
	 ICAbstractValue ov;
	 
	 public ICoutputValueList(ICoutputValueList ovl, ICAbstractValue ov)
	 {
		 this.ovl = ovl;
		 this.ov = ov;
	 }
	 
	 public String toString()
	 {
		 return (ovl==null?"":ovl.toString()+",")+ov.toString();
	 }

	@Override
	public isComplexInfoPropMatch match(boolean isPatternSide,
			isComplexInfoPropMatch previousMatchResult, List<Integer> argValues) {
		// TODO Auto-generated method stub
		return null;
	}
}
