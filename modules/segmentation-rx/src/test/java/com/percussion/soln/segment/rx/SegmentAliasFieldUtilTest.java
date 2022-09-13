package com.percussion.soln.segment.rx;

import static com.percussion.soln.segment.rx.SegmentAliasUtil.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class SegmentAliasFieldUtilTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testSimpleParseAliases() {
        List<String> aliases;
        aliases = parseAliases("a");
        assertEquals(asList("a"), aliases);
        
        aliases = parseAliases("a b");
        assertEquals(asList("a","b"), aliases);
        
        aliases = parseAliases("ant bee crawler ");
        assertEquals(asList("ant","bee","crawler"), aliases);
    }
    
    @Test
    public void testEmptyString() {
        List<String> aliases;
        aliases = parseAliases("");
        assertEquals(emptyList(), aliases);
    }
    
    @Test
    public void testQuotedString() {
        List<String> aliases;
        aliases = parseAliases("\"Adam Gent\" \"Steve Bolton\"    \"Again\"  ");
        assertEquals(asList("Adam Gent", "Steve Bolton", "Again"), aliases);
    }
    
    
    
    

}
