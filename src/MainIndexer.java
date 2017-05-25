import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.htmlparser.Parser;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.*;
import org.htmlparser.NodeFilter;
import org.wltea.analyzer.lucene.IKAnalyzer;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.lucene.document.Field;
public class MainIndexer {
	private Analyzer analyzer; 
    private IndexWriter indexWriter;
    private float averageLength=1.0f;
    private String mainPath = "/Users/huanghuirong/news.tsinghua.edu.cn/publish/";
    //now split: hrefname, title, h1/2, h3-6, strong/b/i, p, title(attr), alt, meata(description), meta(keyword)
    private int featureNum = 10;
    private String tagOrAttr[] = {"t", "t", "t", "t", "t", "t", "a", "a", "v", "v"};
    private String fieldName[] = {"href", "title", "h12", "h36", "strong", "content", "titleattr", "alt", "description", "keywords"};
    private String keyName[]   = {"a", "title", "h1 h2", "h3 h4 h5 h6", "strong b i em", "p", "title", "alt", "description", "keywords"};
    //private int boost[] = {5, 10, 9, 6, 2, 7, 1, 1, 8, 8};
    
	public MainIndexer(String indexDir)
	throws Exception{
//		Directory dir=FSDirectory.open(Paths.get(indexDir));
//		IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		Directory dir=FSDirectory.open(new File(indexDir));
	    IndexWriterConfig iwc=new IndexWriterConfig(Version.LUCENE_40, new IKAnalyzer(true));
		iwc.setSimilarity(new BM25Similarity());
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		indexWriter = new IndexWriter(dir, iwc);
	}
	
	private String getTagText(String tag, String filecontent, String charset)
	throws Exception{
		Parser parser = MyParser.createParser(filecontent, charset);
		String splitResult[] = tag.split(" ");
		NodeFilter filter = new TagNameFilter(splitResult[0]);
		for (int i = 1;i < splitResult.length;i ++)
			filter = new OrFilter(filter, new TagNameFilter(splitResult[i]));
		NodeList nodes = parser.extractAllNodesThatMatch(filter);
		String totalStr = "";
		for (int index = 0;index < nodes.size();index ++){
			Node n = (Node)nodes.elementAt(index);
			totalStr += (n.toPlainTextString() + " ");
		}
		return totalStr;
	}
	
	private String getAttrText(String attr, String filecontent, String charset)
	throws Exception{
		Parser parser = MyParser.createParser(filecontent, charset);
		NodeFilter filter = new HasAttributeFilter(attr);
		NodeList nodes = parser.extractAllNodesThatMatch(filter);
		String totalStr = "";
		for (int i = 0;i < nodes.size();i ++){
			String text = ((TagNode)nodes.elementAt(i)).getAttribute(attr);
			if (text != null)
				totalStr += (text + " ");
		}
		return totalStr;
	}
	
	private String getValueText(String attrvalue, String filecontent, String charset)
	throws Exception{
		Parser parser = MyParser.createParser(filecontent, charset);
		NodeFilter filter = new HasAttributeFilter("name", attrvalue);
		NodeList nodes = parser.extractAllNodesThatMatch(filter);
		String totalStr = "";
		for (int i = 0;i < nodes.size();i ++){
			String text = ((TagNode)nodes.elementAt(i)).getAttribute("content");
			totalStr += (text + " ");
		}
		return totalStr;
	}
	
	private void readXML(String filePath)
	throws Exception{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		org.w3c.dom.Document doc = db.parse(new File(filePath));
		org.w3c.dom.NodeList nodeList = doc.getElementsByTagName("doc");
		for(int i=0;i<nodeList.getLength();i++){
			org.w3c.dom.Node node=nodeList.item(i);
			NamedNodeMap map=node.getAttributes();
			org.w3c.dom.Node id=map.getNamedItem("id");
			org.w3c.dom.Node locate=map.getNamedItem("locate");
			filePath = mainPath + locate.getNodeValue();
			String filecontent = new String(Files.readAllBytes(Paths.get(filePath)));
			filecontent = filecontent.replaceAll("<script[\\s\\S]*?</script>", "");
			String encoding = "utf-8";
			Pattern pp = Pattern.compile("charset\\s*=\"*\\s*([0-9a-zA-Z-_]+)\"*");
			Matcher m = pp.matcher(filecontent);
			if (m.find())
				encoding = m.group(1);
			Document document = new Document();
			Field idfield = new Field("id", id.getNodeValue(), Field.Store.YES, Field.Index.NO);
			Field pathfield = new Field("filepath", locate.getNodeValue(), Field.Store.YES, Field.Index.NO);
			document.add(idfield);
			document.add(pathfield);
			averageLength += locate.getNodeValue().length();
			for (int j = 0;j < featureNum;j ++){
				String resultText = "";
				if (tagOrAttr[j].equalsIgnoreCase("t"))
					resultText = getTagText(keyName[j], filecontent, encoding);
				else if (tagOrAttr[j].equalsIgnoreCase("a"))
					resultText = getAttrText(keyName[j], filecontent, encoding);
				else
					resultText = getValueText(keyName[j], filecontent, encoding);
				Field field = new Field(fieldName[j], resultText, Field.Store.YES, Field.Index.ANALYZED);
				//field.setBoost(boost[j]);
				document.add(field);
				averageLength += resultText.length();
			}
			indexWriter.addDocument(document);
			if(i%10000==0){
				System.out.println("process "+i);
			}
		}
		averageLength /= indexWriter.numDocs();
		indexWriter.close();
	}
	
	public void saveGlobals(String filename){
    	try{
    		PrintWriter pw=new PrintWriter(new File(filename));
    		pw.println(averageLength);
    		pw.close();
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
	
	public String splitWord(String keyword)
	throws Exception{
		IKAnalyzer ikanalyzer = new IKAnalyzer(true);
		StringReader reader = new StringReader(keyword);
		TokenStream ts = ikanalyzer.tokenStream("", reader);
		CharTermAttribute term=ts.getAttribute(CharTermAttribute.class);
		String splitStr = "";
		while(ts.incrementToken()){
			splitStr += term.toString() + " ";
        }
		return splitStr;
	}
	
	public static void main(String []args)
	throws Exception{
		MainIndexer mainindexer = new MainIndexer("forindex/index");
		mainindexer.readXML("news.xml");
		mainindexer.saveGlobals("forIndex/global.txt");
	}
}
