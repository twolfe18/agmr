
import edu.jhu.agiga.*;
import java.util.*;
import edu.stanford.nlp.trees.*;

class WordBeforeSBar extends AGMRSentenceMapper {

	@Override
	public String name() { return "WordBeforeSBAR"; }

	@Override
	public List<String> map(AgigaSentence sent) {

		List<String> emit = new ArrayList<String>();

		Tree root = sent.getStanfordContituencyTree();
		for(Tree node : root.subTreeList()) {
			Tree leftChild = null;
			for(Tree rightChild : node.getChildrenAsList()) {
				if(rightChild.value().equalsIgnoreCase("SBAR") && leftChild != null) {
					List<Tree> leftGrandChildren = leftChild.getLeaves();
					int n = leftGrandChildren.size();
					emit.add(leftGrandChildren.get(n-1).value());
				}
				leftChild = rightChild;
			}
		}

		return emit;
	}
}

