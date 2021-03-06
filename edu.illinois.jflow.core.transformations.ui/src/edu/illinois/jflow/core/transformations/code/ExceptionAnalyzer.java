/**
 * This file is a direct copy of
 * {@link org.eclipse.jdt.internal.corext.refactoring.code.ExceptionAnalyzer} and is licensed under
 * the Eclipse Public License.
 * 
 * The original file was declared to be package-accessible and we wanted to access it from this
 * plug-in without modifying the original JDT plug-in.
 */
package edu.illinois.jflow.core.transformations.code;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.internal.corext.refactoring.util.AbstractExceptionAnalyzer;

@SuppressWarnings("restriction")
class ExceptionAnalyzer extends AbstractExceptionAnalyzer {

	public static ITypeBinding[] perform(ASTNode[] statements) {
		ExceptionAnalyzer analyzer= new ExceptionAnalyzer();
		for (int i= 0; i < statements.length; i++) {
			statements[i].accept(analyzer);
		}
		List<ITypeBinding> exceptions= analyzer.getCurrentExceptions();
		return exceptions.toArray(new ITypeBinding[exceptions.size()]);
	}

	@Override
	public boolean visit(ThrowStatement node) {
		ITypeBinding exception= node.getExpression().resolveTypeBinding();
		if (exception == null) // Safety net for null bindings when compiling fails.
			return true;

		addException(exception);
		return true;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		return handleExceptions((IMethodBinding)node.getName().resolveBinding());
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		return handleExceptions((IMethodBinding)node.getName().resolveBinding());
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		return handleExceptions(node.resolveConstructorBinding());
	}

	private boolean handleExceptions(IMethodBinding binding) {
		if (binding == null)
			return true;
		addExceptions(binding.getExceptionTypes());
		return true;
	}
}
