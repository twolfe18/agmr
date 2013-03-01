
import edu.jhu.agiga.AgigaDocument;
import java.util.List;

public abstract class AGMRDocumentMapper {
	public abstract String name();
	public abstract List<String> map(AgigaDocument sentence);
}

