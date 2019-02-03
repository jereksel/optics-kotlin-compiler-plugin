package com.ivianuu.debuglog

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class MyIndicesHelperExtension : KotlinIndicesHelperExtension {

    override fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean
    ) {
        println("append extension callables")
        moduleDescriptor.getPackage(FqName("com.ivianuu.application")).fragments
            .filterIsInstance<MyPackageFragmentDescriptor>()
            .flatMap { it.getMemberScope().getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) }
            .filterIsInstance<MyMemberFunctionDescriptor>()
            .filter {
                val receiverType = it.extensionReceiverParameter?.type ?: return@filter false
                receiverTypes.any { it.isSubtypeOf(receiverType) }
            }
            .forEach {
                println("add extension $it")
                consumer += it
            }
    }

}