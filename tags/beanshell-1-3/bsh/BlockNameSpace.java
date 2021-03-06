/*****************************************************************************
 *                                                                           *
 *  This file is part of the BeanShell Java Scripting distribution.          *
 *  Documentation and updates may be found at http://www.beanshell.org/      *
 *                                                                           *
 *  Sun Public License Notice:                                               *
 *                                                                           *
 *  The contents of this file are subject to the Sun Public License Version  *
 *  1.0 (the "License"); you may not use this file except in compliance with *
 *  the License. A copy of the License is available at http://www.sun.com    * 
 *                                                                           *
 *  The Original Code is BeanShell. The Initial Developer of the Original    *
 *  Code is Pat Niemeyer. Portions created by Pat Niemeyer are Copyright     *
 *  (C) 2000.  All Rights Reserved.                                          *
 *                                                                           *
 *  GNU Public License Notice:                                               *
 *                                                                           *
 *  Alternatively, the contents of this file may be used under the terms of  *
 *  the GNU Lesser General Public License (the "LGPL"), in which case the    *
 *  provisions of LGPL are applicable instead of those above. If you wish to *
 *  allow use of your version of this file only under the  terms of the LGPL *
 *  and not to allow others to use your version of this file under the SPL,  *
 *  indicate your decision by deleting the provisions above and replace      *
 *  them with the notice and other provisions required by the LGPL.  If you  *
 *  do not delete the provisions above, a recipient may use your version of  *
 *  this file under either the SPL or the LGPL.                              *
 *                                                                           *
 *  Patrick Niemeyer (pat@pat.net)                                           *
 *  Author of Learning Java, O'Reilly & Associates                           *
 *  http://www.pat.net/~pat/                                                 *
 *                                                                           *
 *****************************************************************************/


package	bsh;

import java.util.Vector;

/**
    A specialized namespace	for Blocks, e.g. the body of a "for" statement.
	The Block acts like a child namespace but only for typed variables 
	declared within it.  Elsewhere variable assignment (including untyped
	variable usage) acts like it is part of the containing block.  
	<p>

	Note: It *must* remain possible for a BlockNameSpace to be a child of
	another BlockNameSpace and have variable propogation pass all the way
	through.  (This happens naturally and simply here). This is used in 
	BSHForStatement (see notes there).
*/
class BlockNameSpace extends NameSpace 
{
	/** When true, capture all variable assignment locally */
	boolean initMode;

    public BlockNameSpace( NameSpace parent ) 
		throws EvalError
	{
		super( parent, parent.name + "/BlockNameSpace" );
    }

	/**
		Override the standard namespace behavior.
		If the variables exists in our namespace assign it there,
		otherwise in the parent space.
		i.e. only allow typed var declaration to happen in this namespace.
		Typed vars are handled in the ordinary way... local scope.
	*/
    public void	setVariable( String name, Object o, boolean strictJava ) 
		throws UtilEvalError 
	{
		if ( weHaveVar( name ) || initMode ) 
			super.setVariable( name, o, strictJava );
		else
			getParent().setVariable( name, o, strictJava );
    }

	/**
		When set to true, handle all variable assignment in this local
		scope - don't delegate to parent.  This is used in catch blocks
		to initialize local parameters to the block then turned off to allow 
		the normal BlockNameSpace behavior (which is to keep only locally 
		declared typed variables local and pass the rest to the parent).
	*/
	public void setInitMode( boolean b ) {
		initMode = b;
	}

	boolean weHaveVar( String name ) {
		return super.getVariableImpl( name, false ) != null;
	}

	/**
		super is our parent's super
	*/
    public NameSpace getSuper() {
		return getParent().getSuper();
	}

	/**
		this is our parent's this
	*/
    This getThis( Interpreter declaringInterpreter ) {
		return getParent().getThis( declaringInterpreter );
	}

	/**
		delegate import to our parent
	*/
    public void	importClass(String name) {
		getParent().importClass( name );
	}

	/**
		delegate import to our parent
	*/
    public void	importPackage(String name) {
		getParent().importPackage( name );
	}

	/**
		The block namespace acts like part of the enclosing block for most
		vars.  So we need to add our locals to the enclosing when someone
		asks for the full list.
	// We should do this for getMethodNames() and getMethods() as well.
    public String [] getVariableNames() {
		String [] v1=super.getVariableNames();
		String [] v2 = getParent().getVariableNames();
		Vector v = new Vector();
		for(int i=0; i<v1.length; i++)
			v.addElement( v1[i] );
		for(int i=0; i<v2.length; i++)
			v.addElement( v2[i] );
		String [] sa = new String [ v.size() ];
		v.copyInto( sa );
		return sa;
	}
	*/

}

