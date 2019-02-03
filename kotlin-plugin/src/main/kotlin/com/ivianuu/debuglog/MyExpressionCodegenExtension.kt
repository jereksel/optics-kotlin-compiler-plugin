package com.ivianuu.debuglog

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.codeInsight.collectSyntheticStaticMembersAndConstructors
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceivers
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyExpressionCodegenExtension : ExpressionCodegenExtension {

    var messageCollector: MessageCollector? = null

    override fun applyFunction(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
    ): StackValue? {
        val resultingDescriptor = resolvedCall.resultingDescriptor

        println("apply function resulting $resultingDescriptor ${resultingDescriptor.name}")

        if (resultingDescriptor !is MyMemberExtension) return null

        println("is my member extension ${resultingDescriptor.name}")

        val typeMapper = c.typeMapper

        val actualReceiver = StackValue.receiver(resolvedCall, receiver, c.codegen, null)

        return object : StackValue(typeMapper.mapType(resultingDescriptor.returnType!!)) {
            override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
                val callableMethod = typeMapper.mapToCallableMethod(resultingDescriptor, false)
                println("func ${resultingDescriptor.name} " +
                        "callable method $callableMethod " +
                        "owner ${callableMethod.owner} " +
                        "owner internal ${callableMethod.owner.internalName}")

                val asmMethod = typeMapper.mapAsmMethod(resultingDescriptor)
                println("func ${resultingDescriptor.name} asm method -> $asmMethod desc ${asmMethod.descriptor}")

                val receiverType = resultingDescriptor.extensionReceiverParameter!!.type

                actualReceiver.put(c.typeMapper.mapType(receiverType), v)

                val dispatchType = resultingDescriptor.dispatchReceiverParameter!!.type
                v.load(0, typeMapper.mapType(dispatchType))

                v.checkcast(typeMapper.mapClass(resultingDescriptor.memberReceiverType))

                v.invokestatic(
                    callableMethod.owner.internalName,
                    resultingDescriptor.name.asString(),
                    asmMethod.descriptor,
                    false
                )
            }
        }
    }

}