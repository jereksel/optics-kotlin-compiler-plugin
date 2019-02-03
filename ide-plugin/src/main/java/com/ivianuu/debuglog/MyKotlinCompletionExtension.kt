package com.ivianuu.debuglog

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.CompletionBindingContextProvider
import org.jetbrains.kotlin.idea.completion.KotlinCompletionExtension
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf

class MyKotlinCompletionExtension : KotlinCompletionExtension() {

    override fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
        println("perform completion params $parameters result $result")

        val position = parameters.position
        val file = position.containingFile as? KtFile ?: return false
        val project = position.project

        val resolutionFacade = file.getResolutionFacade()

        val bindingContext = CompletionBindingContextProvider
            .getInstance(project).getBindingContext(position, resolutionFacade)
        val inDescriptor = position.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor

        println("typing in descriptor $inDescriptor")

        val classDescriptor = inDescriptor.parentsWithSelf
            .filterIsInstance<ClassDescriptor>()
            .firstOrNull()
            ?: return false

        println("class is $classDescriptor")

        return false
    }

}