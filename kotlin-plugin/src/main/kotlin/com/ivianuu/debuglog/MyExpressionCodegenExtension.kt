package com.ivianuu.debuglog

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyExpressionCodegenExtension : ExpressionCodegenExtension {

    var messageCollector: MessageCollector? = null

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        super.generateClassSyntheticParts(codegen)

        println("generate class synthetic parts ${codegen} ${codegen.descriptor}")

        /*val delegates = codegen.myClass.superTypeListEntries
            .filterIsInstance<KtDelegatedSuperTypeEntry>()
            .mapNotNull { it.delegateExpression }
            .mapNotNull { CodegenUtil.getDelegatePropertyIfAny(it, codegen.descriptor, codegen.bindingContext) }
            .forEach {
                println("got delegate property desc -> $it")
            }*/

        /*val function = SimpleFunctionDescriptorImpl.create(codegen.descriptor,
            Annotations.EMPTY, Name.identifier("testFunction"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)
            .initialize(
                null,
                codegen.descriptor.thisAsReceiverParameter,
                emptyList(),
                emptyList(),
                codegen.descriptor.builtIns.unitType,
                Modality.FINAL,
                Visibilities.PUBLIC
            )

        codegen.functionCodegen.generateMethod()

        codegen.functionCodegen.generateMethod(
            JvmDeclarationOrigin.NO_ORIGIN, function,
            FunctionGenerationStrategy.FunctionDefault(

            )
        )*/
    }

    override fun applyProperty(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
    ): StackValue? {
        println("apply property resulting ${resolvedCall.resultingDescriptor} ${resolvedCall.resultingDescriptor.name}")
        return super.applyProperty(receiver, resolvedCall, c)
    }

    override fun applyFunction(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
    ): StackValue? {
        val resultingDescriptor = resolvedCall.resultingDescriptor

        println("apply function resulting $resultingDescriptor ${resultingDescriptor.name}")

        if (resultingDescriptor !is MyMemberFunctionDescriptor) return null

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