import org.htmlparser.tags.CompositeTag;

public class bTag extends CompositeTag {  
    
    private static final String mIds[] = {     
        "b"
    };     
    private static final String mEndTagEnders[] = {     
        "b"
    };     
     
    public bTag()     
    {     
    }     
     
    public String[] getIds()     
    {     
        return mIds;     
    }     
    public String[] getEndTagEnders()     
    {     
        return mEndTagEnders;     
    }     
}  