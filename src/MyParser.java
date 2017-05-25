import org.htmlparser.Parser;  
import org.htmlparser.PrototypicalNodeFactory;  
import org.htmlparser.lexer.Lexer;  
import org.htmlparser.util.ParserFeedback;     
public class MyParser extends Parser{
	private static PrototypicalNodeFactory factory = null;  
	
    //注册自定义标签  
    static{  
        factory = new PrototypicalNodeFactory();  
        factory.registerTag(new StrongTag());  
        factory.registerTag(new bTag());
    }  
    public MyParser(){  
        super();  
        setNodeFactory(factory);  
    }  
      
    public MyParser(Lexer lexer, ParserFeedback fb) {  
        super(lexer, fb);  
        setNodeFactory(factory);  
    }
    
    public static Parser createParser(String html, String charset){
    	Parser res = Parser.createParser(html, charset);
    	res.setNodeFactory(factory);
    	return res;
    }
}
