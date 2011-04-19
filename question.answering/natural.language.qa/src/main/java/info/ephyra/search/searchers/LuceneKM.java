package info.ephyra.search.searchers;

import info.ephyra.search.Result;

import java.util.ArrayList;
import java.util.List;

import natural.language.qa.LuceneSearch;
import natural.language.qa.LuceneSearchResult;

public class LuceneKM extends KnowledgeMiner {
	private static final int MAX_RESULTS_TOTAL = 100;
	private static final int MAX_RESULTS_PERQUERY = 100;
	private static final int RETRIES = 5;

	protected int getMaxResultsTotal() {
		return MAX_RESULTS_TOTAL;
	}

	protected int getMaxResultsPerQuery() {
		return MAX_RESULTS_PERQUERY;
	}

	protected Result[] doSearch() {
		LuceneSearch searcher = new LuceneSearch();
		List<LuceneSearchResult> luceneRes = new ArrayList<LuceneSearchResult>();
		String queryString = query.getQueryString();

		List<String> search = null;
		// perform search
		try {
			luceneRes = searcher.search(queryString, MAX_RESULTS_TOTAL);
			LuceneKM.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// get snippets and URLs of the corresponding websites
		String[] paragraphs = new String[luceneRes.size()];
		String[] docIds = new String[luceneRes.size()];
		String[] cacheIds = new String[luceneRes.size()];
		for (int i = 0; i < luceneRes.size(); i++) {
			paragraphs[i] = luceneRes.get(i).getContent();
			docIds[i] = luceneRes.get(i).getPath();
			cacheIds[i] = String.valueOf(luceneRes.get(i).getDocId());
		}

		// set cache URLs and return results
		return getResults(paragraphs, docIds, cacheIds, true);
	}

	public KnowledgeMiner getCopy() {
		return new LuceneKM();
	}
}
