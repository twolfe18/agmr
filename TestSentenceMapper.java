
import edu.jhu.agiga.*;
import java.util.*;

class TestSentenceMapper extends AGMRSentenceMapper {

	public String name() { return "FirstPOS"; }

	public List<String> map(AgigaSentence sent) {
		List<String> l = new LinkedList<String>();
		String s = sent.getTokens().get(0).getPosTag();
		l.add(s);
		return l;
	}

}

