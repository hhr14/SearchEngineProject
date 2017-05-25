import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class MainSearcher {
	private IndexReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	private float avgLength=1.0f;
	private int featurenum = 10;
	private String field[] = {"href", "title", "h12", "h36", "strong", "content", "titleattr", "alt", "description", "keywords"};
	private float boostvalue[] = {1, 1, 1, 1, 10, 1, 1, 1, 8, 8};
	private Map<String , Float> boosts;
	
	public MainSearcher(String indexdir)
	throws Exception{
		reader = IndexReader.open(FSDirectory.open(new File(indexdir)));
		searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());
		analyzer = new IKAnalyzer(true);
		boosts = new HashMap<String, Float>();
		for (int i = 0;i < featurenum;i ++)
			boosts.put(field[i], boostvalue[i]);
	}
	
	public TopDocs searchQuery(String queryString, int maxnum)
	throws Exception{
		MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_40, field, analyzer, boosts);
		parser.setDefaultOperator(QueryParser.AND_OPERATOR);
		Query query = parser.parse(queryString);
		TopDocs results = searcher.search(query, maxnum);
		System.out.println(results);
		return results;
	}
	
	public Document getDoc(int docID){
		try{
			return searcher.doc(docID);
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args)
	throws Exception{
		MainSearcher main = new MainSearcher("forindex/index");
		TopDocs results = main.searchQuery("校园交通", 50);
		ScoreDoc[] hits = results.scoreDocs;
		for (int i = 0; i < hits.length; i++) { // output raw format
			Document doc = main.getDoc(hits[i].doc);
			System.out.println("doc=" + hits[i].doc + " score="
					+ hits[i].score+" id= "+doc.get("id") + " filepath= " +doc.get("filepath"));
		}
	}
}
