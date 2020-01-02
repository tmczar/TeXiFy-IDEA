// This is a generated file. Not intended for manual editing.
package nl.hannahsten.texifyidea.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static nl.hannahsten.texifyidea.psi.LatexTypes.*;
import nl.hannahsten.texifyidea.psi.LatexCommandsImplMixin;
import nl.hannahsten.texifyidea.psi.*;
import com.intellij.psi.PsiReference;
import nl.hannahsten.texifyidea.index.stub.LatexCommandsStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;

public class LatexCommandsImpl extends LatexCommandsImplMixin implements LatexCommands {

  public LatexCommandsImpl(@NotNull LatexCommandsStub stub, @NotNull IStubElementType type) {
    super(stub, type);
  }

  public LatexCommandsImpl(@NotNull ASTNode node) {
    super(node);
  }

  public LatexCommandsImpl(LatexCommandsStub stub, IElementType type, ASTNode node) {
    super(stub, type, node);
  }

  public void accept(@NotNull LatexVisitor visitor) {
    visitor.visitCommands(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LatexVisitor) accept((LatexVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<LatexParameter> getParameterList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, LatexParameter.class);
  }

  @Override
  @NotNull
  public PsiElement getCommandToken() {
    return notNullChild(findChildByType(COMMAND_TOKEN));
  }

  @Override
  @NotNull
  public PsiReference[] getReferences() {
    return LatexPsiImplUtil.getReferences(this);
  }

  @Override
  public List<String> getOptionalParameters() {
    return LatexPsiImplUtil.getOptionalParameters(this);
  }

  @Override
  public List<String> getRequiredParameters() {
    return LatexPsiImplUtil.getRequiredParameters(this);
  }

  @Override
  public boolean hasLabel() {
    return LatexPsiImplUtil.hasLabel(this);
  }

}
