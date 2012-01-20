package com.jetbrains.python.codeInsight.dataflow.scope;

import com.intellij.codeInsight.dataflow.DFALimitExceededException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.jetbrains.python.psi.NameDefiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author oleg
 */
public interface Scope {
  /*
   * @return defined scope local/instance/class variables and parameters, using reaching defs
   */
  @Nullable
  ScopeVariable getDeclaredVariable(@NotNull PsiElement anchorElement, @NotNull String name) throws DFALimitExceededException;

  boolean isGlobal(String name);

  boolean isNonlocal(String name);

  boolean containsDeclaration(String name);

  @NotNull
  List<NameDefiner> getNameDefiners();

  @Nullable
  PsiNamedElement getNamedElement(String name);

  @Nullable
  PsiElement getImplicitElement(String name);
}
