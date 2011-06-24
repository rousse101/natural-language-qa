package natural.language.qa;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LuceneSearch {

	public List<LuceneSearchResult> search(String queryString, int maxRes) throws Exception {
		IndexSearcher searcher = null;
		List<LuceneSearchResult> results = new ArrayList<LuceneSearchResult>();
		try {
			Properties indexConf = new Properties();
			FileInputStream fis = new FileInputStream("index.properties");
			indexConf.load(fis);
			
			String index = indexConf.getProperty("index");
			String field = "contents";

			Directory indexDir = FSDirectory.open(new File(index));

			searcher = new IndexSearcher(indexDir);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);

			QueryParser parser = new QueryParser(Version.LUCENE_31, field, analyzer);

			queryString = queryString.trim();
			if (queryString.length() == 0) {
				return results;
			}

			Query query = parser.parse(queryString);
			System.out.println("Searching for: " + query.toString(field));

			// ================================================
			Formatter f = new SimpleHTMLFormatter("", "");
			Encoder e = new DefaultEncoder();
			QueryScorer fs = new QueryScorer(query);
			Fragmenter fragmenter = new SimpleSpanFragmenter(fs, 50);// new SentenceFragmenter();
			Highlighter h = new Highlighter(f, e, fs);
			h.setTextFragmenter(fragmenter);
			h.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

			// ================================================

			// Collect docs
			TopDocs res = searcher.search(query, maxRes);
			int numTotalHits = res.totalHits;
			ScoreDoc[] scoreDocs = res.scoreDocs;

			for (ScoreDoc scoreDoc : scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc);
				String path = doc.get("path");
				String content = readDocument(path);
				String bestFragment = h.getBestFragment(analyzer, field,
				 content);
				String frag = bestFragment;
				//System.out.println(frag);
				LuceneSearchResult hit = new LuceneSearchResult(scoreDoc.doc, path, frag);
				results.add(hit);
			}
			System.out.println(numTotalHits + " total matching documents");
		} finally {
			if (searcher != null) {
				searcher.close();
			}
		}
		return results;
	}

	private String readDocument(String path) throws Exception {
		StringBuffer strFileContents = new StringBuffer();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				strFileContents.append(line).append("\n");
				line = reader.readLine();
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return strFileContents.toString();
	}

	public static void main(String[] args) throws Exception {
		LuceneSearch ls = new LuceneSearch();
		List<LuceneSearchResult> searchRes = ls.search("computer",
				10);

		for (LuceneSearchResult res : searchRes) {
			System.out.println("============================[" + res.getDocId()
					+ "]================================");
			System.out.println(res.getPath());
			System.out.println("------------------------------------------------------------");
			System.out.println(res.getContent());
		}
	}
}
