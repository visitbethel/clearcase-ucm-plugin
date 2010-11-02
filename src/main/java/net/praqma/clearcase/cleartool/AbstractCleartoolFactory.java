package net.praqma.clearcase.cleartool;

import net.praqma.debug.Debug;



public abstract class AbstractCleartoolFactory implements CleartoolInterface
{
	public static AbstractCleartoolFactory cfInstance = null;
	
	protected static final String linesep = System.getProperty( "line.separator" );
	
	protected static Debug logger = Debug.GetLogger();
	protected static boolean hudson = false;
	
	public AbstractCleartoolFactory( )
	{
		
	}
}