package com.ivianuu.debuglog

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class MyGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        println("hello you there??")
        if (sourceElement is LeafPsiElement && sourceElement.parent is KtSimpleNameExpression) {
            println("go to declaration targets")
            val simpleNameExpression = sourceElement.parent as? KtSimpleNameExpression ?: return null

            println("simple name expression $simpleNameExpression")

            val resultingDescriptor = simpleNameExpression.resolveToCall()?.resultingDescriptor

            println("resulting descriptor $resultingDescriptor")

/*            if (resultingDescriptor is MySimpleFunctionDescriptor
                && resultingDescriptor.name.asString() == "toPx") {
                println("everything provided")
            }
*/
            /*val layoutManager = getLayoutManager(sourceElement) ?: return null



            val propertyDescriptor = resolvePropertyDescriptor(simpleNameExpression) ?: return null

            val psiElements = layoutManager.propertyToXmlAttributes(propertyDescriptor)
            val valueElements = psiElements.mapNotNull { (it as? XmlAttribute)?.valueElement as? PsiElement }
            if (valueElements.isNotEmpty()) return valueElements.toTypedArray()*/
        }

        return null
    }

}