package natural.language.qa;

public class LuceneSearchResult {
	
	private int docId;
	private String path;
	private String content;
	
	public LuceneSearchResult(int docId, String path, String content) {
		super();
		this.docId = docId;
		this.path = path;
		this.content = content;
	}

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
