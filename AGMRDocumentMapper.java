
import edu.jhu.agiga.AgigaDocument;
import java.util.List;

public abstract class AGMRDocumentMapper {

	public abstract String name();

	/**
	 * emit a bunch of strings that you want to be counted
	 *
	 * for example, you might emit the words in a document
	 * to get regular counts of how common a word is
	 *
	 * alternatively you could emit words only once per document
	 * to get the document frequency of a word
	 */
	public abstract List<String> map(AgigaDocument document);

}

