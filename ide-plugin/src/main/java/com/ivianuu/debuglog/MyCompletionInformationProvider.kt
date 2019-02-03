package com.ivianuu.debuglog

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.completion.CompletionInformationProvider

class MyCompletionInformationProvider : CompletionInformationProvider {

    override fun getContainerAndReceiverInformation(descriptor: DeclarationDescriptor): String? {
    //    logger.warn("getting container and receiver info for $descriptor")

        //if (descriptor !is MySimpleFunctionDescriptor) return null

        if (descriptor !is MyMemberExtension) return null

        return null
    }
}