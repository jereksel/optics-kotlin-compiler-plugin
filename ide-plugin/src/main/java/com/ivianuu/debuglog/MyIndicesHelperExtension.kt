package com.ivianuu.debuglog

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

class MyIndicesHelperExtension : KotlinIndicesHelperExtension {

    override fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean
    ) {
        println("append ext callables")

        println("receiver types start")

        receiverTypes.forEach { receiverType ->
            println("receiver type $receiverType")
        }

        println("receiver types end")

        val fragments = moduleDescriptor.getPackage(FqName.ROOT)

        println("fragments start")

        fun handleScope(scope: MemberScope) {
            scope.getContributedDescriptors { nameFilter(it.asString()) }.forEach { contributedDescriptor ->
                println("contributed descriptor ${contributedDescriptor.name}")
            }
        }

        fragments.fragments.forEach { fragment ->
            println("fragment -> ${fragment.name}")
            handleScope(fragment.getMemberScope())
        }

        print("fragments end")

        /*
        println("append extension callables consumer: $consumer")

        receiverTypes.forEach { receiverType ->
            println("receiver type $receiverType")

            val contributedDescriptors = receiverType.memberScope.getContributedDescriptors(
                DescriptorKindFilter.FUNCTIONS) { nameFilter(it.asString()) }

            contributedDescriptors.forEach { contributedDescriptor ->
                println("contributed descriptor ${contributedDescriptor.name}")
            }

            /*receiverType.memberScope.getFunctionNames().forEach {
                print("function name -> $it")
                receiverType.memberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) {

                }
            }*/
        }*/
    }

    private fun createInContextDpToPx(
        classDescriptor: ClassDescriptor
    ): SimpleFunctionDescriptor {
        val functionDescriptor = object : SimpleFunctionDescriptorImpl(
            classDescriptor,
            null,
            Annotations.EMPTY,
            Name.identifier("toPx"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            classDescriptor.source
        ) {}

        functionDescriptor.initialize(
            DescriptorFactory.createExtensionReceiverParameterForCallable(functionDescriptor,
                classDescriptor.builtIns.intType, Annotations.EMPTY),
            classDescriptor.thisAsReceiverParameter,
            emptyList(), emptyList(),
            classDescriptor.builtIns.floatType, null, Visibilities.PUBLIC
        )

        return functionDescriptor
    }

}