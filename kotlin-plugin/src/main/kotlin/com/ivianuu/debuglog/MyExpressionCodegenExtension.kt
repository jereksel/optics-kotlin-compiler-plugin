package com.ivianuu.debuglog

import com.ivianuu.debuglog.OpticsConst.OPTICS_CLASS_NAME
import com.ivianuu.debuglog.OpticsConst.annotationClass
import com.ivianuu.debuglog.OpticsConst.lensClass
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyExpressionCodegenExtension : ExpressionCodegenExtension {

    override fun applyFunction(
        receiver: StackValue,
        resolvedCall: ResolvedCall<*>,
        c: ExpressionCodegenExtension.Context
    ): StackValue? {
        return super.applyFunction(receiver, resolvedCall, c)
    }

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        super.generateClassSyntheticParts(codegen)

        val descriptor = codegen.descriptor

        if (!descriptor.annotations.hasAnnotation(annotationClass)) {
            return
        }

        val containerAsmType = codegen.typeMapper.mapType(descriptor.defaultType)
        val creatorAsmType = Type.getObjectType("${containerAsmType.internalName}\$${OPTICS_CLASS_NAME}")

        val creatorClass = ClassDescriptorImpl(
            descriptor, Name.identifier(OPTICS_CLASS_NAME), Modality.FINAL, ClassKind.OBJECT, emptyList(),
            descriptor.source, false, LockBasedStorageManager.NO_LOCKS
        )

        creatorClass.initialize(
            MemberScope.Empty, emptySet(),
            DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source)
        )

        val classBuilderForCreator = codegen.state.factory.newVisitor(
            JvmDeclarationOrigin(JvmDeclarationOriginKind.SYNTHETIC, null, creatorClass),
            Type.getObjectType(creatorAsmType.internalName),
            codegen.myClass.containingKtFile
        )

        val classContextForCreator = ClassContext(
            codegen.typeMapper, creatorClass, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null
        )
        val codegenForCreator = ImplementationBodyCodegen(
            codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false
        )

        classBuilderForCreator.defineClass(
            null, Opcodes.V1_6, Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
            creatorAsmType.internalName, null, "java/lang/Object",
            emptyArray()
        )

        codegen.v.visitInnerClass(
            creatorAsmType.internalName,
            containerAsmType.internalName,
            OPTICS_CLASS_NAME,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )
        codegenForCreator.v.visitInnerClass(
            creatorAsmType.internalName,
            containerAsmType.internalName,
            OPTICS_CLASS_NAME,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )

        generateObjectBoilerplate(codegenForCreator, creatorClass, creatorAsmType, classBuilderForCreator)

        descriptor.unsubstitutedPrimaryConstructor!!.explicitParameters.forEach { parameter ->
            generateGetFunctionClass(parameter, creatorAsmType, creatorClass, codegenForCreator, codegen)
            generateSetFunctionClass(parameter, creatorAsmType, creatorClass, codegenForCreator, codegen)
        }

        descriptor.unsubstitutedPrimaryConstructor!!.explicitParameters.forEach { parameter ->
            generateGetExtensionMethod(parameter, creatorAsmType, creatorClass, codegenForCreator)
        }

        classBuilderForCreator.done()

    }

    private fun generateGetFunctionClass(
        parameter: ParameterDescriptor,
        creatorAsmType: Type,
        creatorClass: ClassDescriptorImpl,
        codegenForCreator: ImplementationBodyCodegen,
        codegen: ImplementationBodyCodegen
    ) {

        val function1 = codegen.descriptor.builtIns.getFunction(1)

        val parameterGetterName = "Get${parameter.name.asString().capitalize()}"

        val parameterGetterType = Type.getObjectType("${creatorAsmType.internalName}\$${parameterGetterName}")

        val getterClass = ClassDescriptorImpl(
            creatorClass,
            Name.identifier(parameterGetterName),
            Modality.FINAL,
            ClassKind.CLASS,
            listOf(function1.defaultType),
            creatorClass.source,
            false,
            LockBasedStorageManager.NO_LOCKS
        )

        getterClass.initialize(
            MemberScope.Empty, emptySet(),
            DescriptorFactory.createPrimaryConstructorForObject(getterClass, getterClass.source)
        )

        val classBuilderForGetter = codegenForCreator.state.factory.newVisitor(
            JvmDeclarationOrigin(JvmDeclarationOriginKind.SYNTHETIC, null, getterClass),
            parameterGetterType,
            codegenForCreator.myClass.containingKtFile
        )

        val classContextForGetter = ClassContext(
            codegenForCreator.typeMapper,
            getterClass,
            OwnerKind.IMPLEMENTATION,
            codegenForCreator.context.parentContext,
            null
        )
        val codegenForGetter = ImplementationBodyCodegen(
            codegenForCreator.myClass,
            classContextForGetter,
            classBuilderForGetter,
            codegenForCreator.state,
            codegenForCreator.parentCodegen,
            false
        )

        classBuilderForGetter.defineClass(
            null, Opcodes.V1_6, Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
            parameterGetterType.internalName, null, "java/lang/Object",
            arrayOf(function1.defaultType.asmType(codegen.typeMapper).internalName)
        )

        codegen.v.visitInnerClass(
            parameterGetterType.internalName,
            creatorAsmType.internalName,
            parameterGetterName,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )
        codegenForCreator.v.visitInnerClass(
            parameterGetterType.internalName,
            creatorAsmType.internalName,
            parameterGetterName,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )
        codegenForGetter.v.visitInnerClass(
            parameterGetterType.internalName,
            creatorAsmType.internalName,
            parameterGetterName,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )

        writeCreatorConstructor(codegenForGetter, getterClass, parameterGetterType)

        generateGetterFunction1(parameter, getterClass, codegenForGetter)

        //FIXME: Metadata is empty
        writeSyntheticClassMetadata(classBuilderForGetter, codegenForGetter.state)

        classBuilderForGetter.done()
    }

    private fun generateGetterFunction1(
        parameter: ParameterDescriptor,
        getterClass: ClassDescriptorImpl,
        codegenForGetter: ImplementationBodyCodegen
    ) {
        generateTypeSafeGet(parameter, getterClass, codegenForGetter)
        generateAnyGet(parameter, getterClass, codegenForGetter)
    }

    private fun generateTypeSafeGet(
        parameter: ParameterDescriptor,
        getterClass: ClassDescriptor,
        codegenForGetter: ImplementationBodyCodegen
    ) {
        val f = SimpleFunctionDescriptorImpl.create(
            getterClass, Annotations.EMPTY, Name.identifier("invoke"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        )

        val value = ValueParameterDescriptorImpl(
            containingDeclaration = f,
            original = null,
            index = 0,
            annotations = Annotations.EMPTY,
            name = Name.identifier("var1"),
            outType = (parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType,
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
        )

        f.initialize(
            null, getterClass.thisAsReceiverParameter, emptyList(), listOf(value),
            parameter.type.box(getterClass.builtIns, getterClass.module), Modality.FINAL, Visibilities.PUBLIC
        )

        val parameterType = parameter.type.asmType(codegenForGetter.typeMapper)
        val classType = (parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType.asmType(
            codegenForGetter.typeMapper
        )

        f.write(codegenForGetter) {
            v.load(1, classType)
            v.invokevirtual(
                classType.internalName,
                "get${parameter.name.asString().capitalize()}",
                "()${parameterType.descriptor}",
                false
            )
            v.boxIfRequired(parameterType)
            v.areturn(classType)
        }
    }

    private fun generateAnyGet(
        parameter: ParameterDescriptor,
        getterClass: ClassDescriptor,
        codegenForGetter: ImplementationBodyCodegen
    ) {
        val f = SimpleFunctionDescriptorImpl.create(
            getterClass, Annotations.EMPTY, Name.identifier("invoke"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        )

        val value = ValueParameterDescriptorImpl(
            containingDeclaration = f,
            original = null,
            index = 0,
            annotations = Annotations.EMPTY,
            name = Name.identifier("var1"),
            outType = parameter.builtIns.anyType,
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
        )

        f.initialize(
            null,
            getterClass.thisAsReceiverParameter,
            emptyList(),
            listOf(value),
            parameter.builtIns.anyType,
            Modality.FINAL,
            Visibilities.PUBLIC
        )

        val getterType = getterClass.defaultType.asmType(codegenForGetter.typeMapper)
        val parameterType = parameter.type.asmType(codegenForGetter.typeMapper)
        val boxedParameterType = AsmUtil.boxType(parameterType)
        val classType = (parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType.asmType(
            codegenForGetter.typeMapper
        )

        f.write(codegenForGetter) {
            v.load(0, getterType)
            v.load(1, classType)
            v.checkcast(classType)
            v.invokevirtual(
                getterType.internalName,
                "invoke",
                "(${classType.descriptor})${boxedParameterType.descriptor}",
                false
            )
            v.areturn(classType)
        }
    }

    private fun generateSetFunctionClass(
        parameter: ParameterDescriptor,
        creatorAsmType: Type,
        creatorClass: ClassDescriptorImpl,
        codegenForCreator: ImplementationBodyCodegen,
        codegen: ImplementationBodyCodegen
    ) {

        val function2 = codegen.descriptor.builtIns.getFunction(2)

        val parameterGetterName = "Set${parameter.name.asString().capitalize()}"

        val parameterGetterType = Type.getObjectType("${creatorAsmType.internalName}\$${parameterGetterName}")

        val getterClass = ClassDescriptorImpl(
            creatorClass,
            Name.identifier(parameterGetterName),
            Modality.FINAL,
            ClassKind.CLASS,
            listOf(function2.defaultType),
            creatorClass.source,
            false,
            LockBasedStorageManager.NO_LOCKS
        )

        getterClass.initialize(
            MemberScope.Empty, emptySet(),
            DescriptorFactory.createPrimaryConstructorForObject(getterClass, getterClass.source)
        )

        val classBuilderForGetter = codegenForCreator.state.factory.newVisitor(
            JvmDeclarationOrigin(JvmDeclarationOriginKind.SYNTHETIC, null, getterClass),
            parameterGetterType,
            codegenForCreator.myClass.containingKtFile
        )

        val classContextForGetter = ClassContext(
            codegenForCreator.typeMapper,
            getterClass,
            OwnerKind.IMPLEMENTATION,
            codegenForCreator.context.parentContext,
            null
        )
        val codegenForGetter = ImplementationBodyCodegen(
            codegenForCreator.myClass,
            classContextForGetter,
            classBuilderForGetter,
            codegenForCreator.state,
            codegenForCreator.parentCodegen,
            false
        )

        classBuilderForGetter.defineClass(
            null, Opcodes.V1_6, Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
            parameterGetterType.internalName, null, "java/lang/Object",
            arrayOf(function2.defaultType.asmType(codegen.typeMapper).internalName)
        )

        codegen.v.visitInnerClass(
            parameterGetterType.internalName,
            creatorAsmType.internalName,
            parameterGetterName,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )
        codegenForCreator.v.visitInnerClass(
            parameterGetterType.internalName,
            creatorAsmType.internalName,
            parameterGetterName,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )
        codegenForGetter.v.visitInnerClass(
            parameterGetterType.internalName,
            creatorAsmType.internalName,
            parameterGetterName,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        )

        writeCreatorConstructor(codegenForGetter, getterClass, parameterGetterType)

        generateSetterFunction2(parameter, getterClass, codegenForGetter)

        //FIXME: Metadata is empty
        writeSyntheticClassMetadata(classBuilderForGetter, codegenForGetter.state)

        classBuilderForGetter.done()

    }

    private fun generateSetterFunction2(
        parameter: ParameterDescriptor,
        getterClass: ClassDescriptorImpl,
        codegenForGetter: ImplementationBodyCodegen
    ) {
        generateTypeSafeSet(parameter, getterClass, codegenForGetter)
        generateAnySet(parameter, getterClass, codegenForGetter)
    }

    private fun generateTypeSafeSet(
        parameter: ParameterDescriptor,
        getterClass: ClassDescriptor,
        codegenForGetter: ImplementationBodyCodegen
    ) {

        val constructor = parameter.containingDeclaration as ConstructorDescriptor

        val f = SimpleFunctionDescriptorImpl.create(
            getterClass, Annotations.EMPTY, Name.identifier("invoke"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        )

        val parameters = listOf(
            (parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType,
            parameter.type
        ).mapIndexed { index: Int, kotlinType: KotlinType ->
            ValueParameterDescriptorImpl(
                containingDeclaration = f,
                original = null,
                index = index,
                annotations = Annotations.EMPTY,
                name = Name.identifier("var${index}"),
                outType = kotlinType,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = SourceElement.NO_SOURCE
            )
        }

        val classKotlinType = (parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType

        f.initialize(
            null, getterClass.thisAsReceiverParameter, emptyList(), parameters, classKotlinType, Modality.FINAL,
            Visibilities.PUBLIC
        )

        val getterType = getterClass.defaultType.asmType(codegenForGetter.typeMapper)
        val parameterType = parameter.type.asmType(codegenForGetter.typeMapper)
        val boxedParameterType = AsmUtil.boxType(parameterType)
        val classType = (parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType.asmType(
            codegenForGetter.typeMapper
        )

        var mask = 0

        val descriptor =
            (listOf((parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType) + constructor.explicitParameters.map { it.type } + constructor.builtIns.intType + constructor.builtIns.anyType)
                .joinToString(separator = "", prefix = "(", postfix = ")${classType.descriptor}") {
                    it.asmType(
                        codegenForGetter.typeMapper
                    ).descriptor
                }

        //FIXME: Data class with single parameter - it has different copy method
        f.write(codegenForGetter) {
            v.load(1, classType)

            constructor.explicitParameters.forEachIndexed { index, constructorParameter ->
                if (constructorParameter == parameter) {
                    v.load(2, parameterType)
                } else {
                    mask += 1 shl index
                    v.aconst(null)
                }
            }

            v.iconst(mask)
            v.aconst(null)
            v.invokestatic(classType.internalName, "copy\$default", descriptor, false)
            v.areturn(classType)
        }

    }

    private fun generateAnySet(
        parameter: ParameterDescriptor,
        getterClass: ClassDescriptor,
        codegenForGetter: ImplementationBodyCodegen
    ) {

        val f = SimpleFunctionDescriptorImpl.create(
            getterClass, Annotations.EMPTY, Name.identifier("invoke"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        )

        val parameters = listOf(0, 1).map { index ->
            ValueParameterDescriptorImpl(
                containingDeclaration = f,
                original = null,
                index = index,
                annotations = Annotations.EMPTY,
                name = Name.identifier("var${index}"),
                outType = parameter.builtIns.anyType,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = SourceElement.NO_SOURCE
            )
        }

        f.initialize(
            null,
            getterClass.thisAsReceiverParameter,
            emptyList(),
            parameters,
            parameter.builtIns.anyType,
            Modality.FINAL,
            Visibilities.PUBLIC
        )

        val getterType = getterClass.defaultType.asmType(codegenForGetter.typeMapper)
        val parameterType = Type.getType(parameter.type.asmType(codegenForGetter.typeMapper).descriptor)
        val boxedParameterType = AsmUtil.boxType(parameterType)
        val classType = Type.getType(
            (parameter.containingDeclaration.containingDeclaration as ClassDescriptor).defaultType.asmType(
                codegenForGetter.typeMapper
            ).descriptor
        )

        f.write(codegenForGetter) {
            v.load(0, getterType)
            v.load(1, classType)
            v.checkcast(classType)
            v.load(2, boxedParameterType)
            v.checkcast(boxedParameterType)
            v.invokevirtual(
                getterType.internalName,
                "invoke",
                "(${classType.descriptor}${boxedParameterType.descriptor})${classType.descriptor}",
                false
            )
            v.areturn(classType)
        }


    }

    private fun generateGetExtensionMethod(
        parameter: ParameterDescriptor,
        creatorAsmType: Type,
        creatorClass: ClassDescriptorImpl,
        codegenForCreator: ImplementationBodyCodegen
    ) {

        val methodName = "get${parameter.name.asString().capitalize()}"

        val getType =
            Type.getType("${creatorAsmType.descriptor.dropLast(1)}\$Get${parameter.name.asString().capitalize()};")
        val setType =
            Type.getType("${creatorAsmType.descriptor.dropLast(1)}\$Set${parameter.name.asString().capitalize()};")

        val genericType =
            creatorClass.module.findClassAcrossModuleDependencies(ClassId.topLevel(lensClass))?.defaultType!!

        val f = SimpleFunctionDescriptorImpl.create(
            creatorClass, Annotations.EMPTY,
            Name.identifier(methodName), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        )

        val param = ValueParameterDescriptorImpl(
            containingDeclaration = f,
            original = null,
            index = 0,
            annotations = Annotations.EMPTY,
            name = Name.identifier("var1"),
            outType = genericType,
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
        )

        f.initialize(
            null, null, emptyList(), listOf(param), genericType, Modality.FINAL,
            Visibilities.PUBLIC
        )

        val getterType = genericType.asmType(codegenForCreator.typeMapper)

        f.write(codegenForCreator) {

            v.load(1, Type.getType("Larrow/optics/PLens;"))

            v.getstatic("arrow/optics/PLens", "Companion", "Larrow/optics/PLens\$Companion;")

            v.anew(getType)
            v.dup()
            v.invokespecial(getType.internalName, "<init>", "()V", false)

            v.anew(setType)
            v.dup()
            v.invokespecial(setType.internalName, "<init>", "()V", false)

            v.invokevirtual(
                "arrow/optics/PLens\$Companion",
                "invoke",
                "(Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function2;)Larrow/optics/PLens;",
                false
            )

            v.invokeinterface("arrow/optics/PLens", "compose", "(Larrow/optics/PLens;)Larrow/optics/PLens;")

            v.areturn(getterType)
        }

    }

    //Generates private constructor and INSTANCE field
    private fun generateObjectBoilerplate(
        codegen: ImplementationBodyCodegen,
        creatorClass: ClassDescriptor,
        creatorAsmType: Type,
        classBuilderForCreator: ClassBuilder
    ) {
        writeCreatorConstructor(codegen, creatorClass, creatorAsmType)
        writeClassConstructor(codegen, creatorClass, creatorAsmType)
        classBuilderForCreator.newField(
            JvmDeclarationOrigin.NO_ORIGIN,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "INSTANCE",
            creatorAsmType.descriptor,
            null,
            null
        )
    }

    /**
     *   static <clinit>()V
    L0
    LINENUMBER 3 L0
    NEW com/ivianuu/debuglog/MyTest
    DUP
    INVOKESPECIAL com/ivianuu/debuglog/MyTest.<init> ()V
    ASTORE 0
    ALOAD 0
    PUTSTATIC com/ivianuu/debuglog/MyTest.INSTANCE : Lcom/ivianuu/debuglog/MyTest;
    RETURN
    MAXSTACK = 2
    MAXLOCALS = 1
     */
    private fun writeClassConstructor(
        codegen: ImplementationBodyCodegen,
        creatorClass: ClassDescriptor,
        creatorAsmType: Type
    ) {

        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            creatorClass, Annotations.EMPTY, Name.identifier("<clinit>"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        )

        functionDescriptor.initialize(
            null,
            creatorClass.thisAsReceiverParameter,
            emptyList(),
            emptyList(),
            creatorClass.builtIns.unitType,
            Modality.FINAL,
            Visibilities.PRIVATE
        )

        functionDescriptor.write(codegen) {
            v.anew(creatorAsmType)
            v.dup()
            v.invokespecial(creatorAsmType.internalName, "<init>", "()V", false)
            v.putstatic(creatorAsmType.internalName, "INSTANCE", creatorAsmType.descriptor)
            v.areturn(Type.VOID_TYPE)
        }

    }

    private fun writeCreatorConstructor(
        codegen: ImplementationBodyCodegen,
        creatorClass: ClassDescriptor,
        creatorAsmType: Type
    ) {
        DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source)
            .apply {
                returnType = creatorClass.defaultType
            }.write(codegen) {
                v.load(0, creatorAsmType)
                v.invokespecial("java/lang/Object", "<init>", "()V", false)
                v.areturn(Type.VOID_TYPE)
            }
    }

    private fun FunctionDescriptor.write(codegen: ImplementationBodyCodegen, code: ExpressionCodegen.() -> Unit) {
        val declarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, this)
        codegen.functionCodegen.generateMethod(
            declarationOrigin,
            this,
            object : FunctionGenerationStrategy.CodegenBased(codegen.state) {
                override fun doGenerateBody(e: ExpressionCodegen, signature: JvmMethodSignature) = e.code()
            })
    }

    private fun InstructionAdapter.boxIfRequired(parameterType: Type) {

        if (parameterType == Type.INT_TYPE) {
            invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer", false)
        }

    }

}

private fun KotlinType.box(builtIns: KotlinBuiltIns, moduleDescriptor: ModuleDescriptor): KotlinType {

    if (this == builtIns.intType) {
        return moduleDescriptor.findClassAcrossModuleDependencies(ClassId.fromString("java/lang/Integer"))?.defaultType!!
    }

    return this

}

