
import edu.jhu.agiga.*;
import java.util.*;

public abstract class AGMRSentenceMapper extends AGMRDocumentMapper {

	public abstract List<String> map(AgigaSentence sentence);

	@Override
	public final List<String> map(AgigaDocument doc) {
		List<String> all = new ArrayList<String>();
		for(AgigaSentence s : doc.getSents()) {
			List<String> forS = map(s);
			all.addAll(forS);
		}
		return all;
	}

}

