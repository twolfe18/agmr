
import edu.jhu.agiga.*;
import java.util.*;

class TestBigKeySentenceMapper extends AGMRSentenceMapper {

	public String name() { return "StupidLongKey"; }

	public List<String> map(AgigaSentence sent) {
		List<String> l = new LinkedList<String>();
		StringBuilder sb = new StringBuilder();
		for(AgigaToken t : sent.getTokens()) {
			String s = String.format("%s_%s_%s", t.getWord(), t.getPosTag(), t.getNerTag());
			if(sb.length() > 0)
				sb.append("-");
			sb.append(s);
		}
		l.add(sb.toString());
		return l;
	}

}

