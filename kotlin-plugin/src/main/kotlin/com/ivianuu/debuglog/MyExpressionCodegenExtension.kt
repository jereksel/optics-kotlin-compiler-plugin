package com.ivianuu.debuglog

import com.ivianuu.debuglog.OpticsConst.LENS_CLASS_NAME
import com.ivianuu.debuglog.OpticsConst.OPTICS_CLASS_NAME
import com.ivianuu.debuglog.OpticsConst.annotationClass
import com.ivianuu.debuglog.OpticsConst.lensClass
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
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
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyExpressionCodegenExtension : ExpressionCodegenExtension {

  override fun applyFunction(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
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

    // From ParcelableCodegenExtension
//        val c = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("com.jereksel.TestInterface")))?.defaultType!!

    val creatorClass = ClassDescriptorImpl(
        descriptor, Name.identifier(OPTICS_CLASS_NAME), Modality.FINAL, ClassKind.OBJECT, emptyList(),
        descriptor.source, false, LockBasedStorageManager.NO_LOCKS)

    creatorClass.initialize(
        MemberScope.Empty, emptySet(),
        DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source))

    val classBuilderForCreator = codegen.state.factory.newVisitor(
        JvmDeclarationOrigin(JvmDeclarationOriginKind.SYNTHETIC, null, creatorClass),
        Type.getObjectType(creatorAsmType.internalName),
        codegen.myClass.containingKtFile)

    val classContextForCreator = ClassContext(
        codegen.typeMapper, creatorClass, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
    val codegenForCreator = ImplementationBodyCodegen(
        codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

    classBuilderForCreator.defineClass(null, Opcodes.V1_6, Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
        creatorAsmType.internalName, null, "java/lang/Object",
        emptyArray())

    codegen.v.visitInnerClass(creatorAsmType.internalName, containerAsmType.internalName, OPTICS_CLASS_NAME, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)
    codegenForCreator.v.visitInnerClass(creatorAsmType.internalName, containerAsmType.internalName, OPTICS_CLASS_NAME, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)

    generateObjectBoilerplate(codegenForCreator, creatorClass, creatorAsmType, classBuilderForCreator)

    val function1 = codegen.descriptor.builtIns.getFunction(1)

    descriptor.unsubstitutedPrimaryConstructor!!.explicitParameters.forEach { parameter ->

      val parameterGetterName = "Get${parameter.name.asString().capitalize()}"

      val parameterGetterType = Type.getObjectType("${creatorAsmType.internalName}\$${parameterGetterName}")

      val getterClass = ClassDescriptorImpl(
          creatorClass, Name.identifier(parameterGetterName), Modality.FINAL, ClassKind.CLASS, listOf(function1.defaultType),
          creatorClass.source, false, LockBasedStorageManager.NO_LOCKS)

      getterClass.initialize(
          MemberScope.Empty, emptySet(),
          DescriptorFactory.createPrimaryConstructorForObject(getterClass, getterClass.source)
      )

      val classBuilderForGetter = codegenForCreator.state.factory.newVisitor(
          JvmDeclarationOrigin(JvmDeclarationOriginKind.SYNTHETIC, null, getterClass),
          parameterGetterType,
          codegenForCreator.myClass.containingKtFile)

      val classContextForGetter = ClassContext(
          codegenForCreator.typeMapper, getterClass, OwnerKind.IMPLEMENTATION, codegenForCreator.context.parentContext, null)
      val codegenForGetter = ImplementationBodyCodegen(
          codegenForCreator.myClass, classContextForGetter, classBuilderForGetter, codegenForCreator.state, codegenForCreator.parentCodegen, false)

      classBuilderForGetter.defineClass(null, Opcodes.V1_6, Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
          parameterGetterType.internalName, null, "java/lang/Object",
          arrayOf(function1.defaultType.asmType(codegen.typeMapper).internalName)
      )

      codegen.v.visitInnerClass(parameterGetterType.internalName, creatorAsmType.internalName, parameterGetterName, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)
      codegenForCreator.v.visitInnerClass(parameterGetterType.internalName, creatorAsmType.internalName, parameterGetterName, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)
      codegenForGetter.v.visitInnerClass(parameterGetterType.internalName, creatorAsmType.internalName, parameterGetterName, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)

      val f = SimpleFunctionDescriptorImpl.create(getterClass, Annotations.EMPTY, Name.identifier("invoke"),
          CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)

      val genericType = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(lensClass))?.defaultType!!

      val value = ValueParameterDescriptorImpl(
          containingDeclaration = f,
          original = null,
          index = 0,
          annotations = Annotations.EMPTY,
          name = Name.identifier("a"),
          outType = genericType,
          declaresDefaultValue = false,
          isCrossinline = false,
          isNoinline = false,
          varargElementType = null,
          source = SourceElement.NO_SOURCE
      )

      f.initialize(null, getterClass.thisAsReceiverParameter, emptyList(), listOf(value), genericType, Modality.FINAL,
          Visibilities.PUBLIC)

      f.write(codegenForGetter) {
        v.aconst(null)
        v.areturn(Type.getType("Larrow/optics/PLens;"))
      }

      codegenForGetter.functionCodegen.generateBridges(f)

      classBuilderForGetter.done()

    }


//            writeCreatorConstructor(codegenForCreator, creatorClass, creatorAsmType)
//            writeNewArrayMethod(codegenForCreator, parcelableClass, creatorClass, parcelerObject)
//            writeCreateFromParcel(codegenForCreator, parcelableClass, creatorClass, parcelClassType, parcelAsmType, parcelerObject, properties)

//            writeSyntheticClassMetadata(classBuilderForCreator, codegen.state)

//            codegenForCreator.generate()

//            codegenForCreator.generate()

//            codegenForCreator.generate()

    classBuilderForCreator.done()

  }

/*    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        super.generateClassSyntheticParts(codegen)

        val descriptor = codegen.descriptor

        if (descriptor.annotations.hasAnnotation(
                FqName("com.ivianuu.myapplication.Synthetics")
            )
        ) {

            run {

                val name = Name.identifier("testFunction")

                val methodDescriptor = SimpleFunctionDescriptorImpl.create(
                        descriptor,
                        Annotations.EMPTY, name,
                        CallableMemberDescriptor.Kind.SYNTHESIZED, descriptor.source
                )
                        .initialize(
                                null,
                                descriptor.thisAsReceiverParameter,
                                emptyList(),
                                emptyList(),
                                descriptor.builtIns.intType,
                                Modality.FINAL,
                                Visibilities.PUBLIC
                        )

                codegen.generateMethod(methodDescriptor) { _, _ ->
                    iconst(1)
                    areturn(INT_TYPE)
                }

            }

            run {

                val name = Name.identifier("getAbc")

                val methodDescriptor = SimpleFunctionDescriptorImpl.create(
                        descriptor,
                        Annotations.EMPTY, name,
                        CallableMemberDescriptor.Kind.SYNTHESIZED, descriptor.source
                )
                        .initialize(
                                null,
                                descriptor.thisAsReceiverParameter,
                                emptyList(),
                                emptyList(),
                                descriptor.builtIns.intType,
                                Modality.FINAL,
                                Visibilities.PUBLIC
                        )

                codegen.generateMethod(methodDescriptor) { _, _ ->
                    iconst(1)
                    areturn(INT_TYPE)
                }

            }

            val containerAsmType = codegen.typeMapper.mapType(descriptor.defaultType)
            val creatorAsmType = Type.getObjectType(containerAsmType.internalName + "\$MyInternalTest")

            val parcelableClass = descriptor

            // From ParcelableCodegenExtension


            val c = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("com.jereksel.TestInterface")))?.defaultType!!

            val creatorClass = ClassDescriptorImpl(
                    parcelableClass, Name.identifier("MyInternalTest"), Modality.FINAL, ClassKind.CLASS, listOf(c),
                    parcelableClass.source, false, LockBasedStorageManager.NO_LOCKS)

            creatorClass.initialize(
                    MemberScope.Empty, emptySet(),
                    DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source))

            val classBuilderForCreator = codegen.state.factory.newVisitor(
                    JvmDeclarationOrigin(JvmDeclarationOriginKind.SYNTHETIC, null, creatorClass),
                    Type.getObjectType(creatorAsmType.internalName),
                    codegen.myClass.containingKtFile)

            val classContextForCreator = ClassContext(
                    codegen.typeMapper, creatorClass, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
            val codegenForCreator = ImplementationBodyCodegen(
                    codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

            classBuilderForCreator.defineClass(null, Opcodes.V1_6, Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
                    creatorAsmType.internalName, null, "java/lang/Object",
                    arrayOf("com/jereksel/TestInterface"))
//                    emptyArray())

            codegen.v.visitInnerClass(creatorAsmType.internalName, containerAsmType.internalName, "MyInternalTest", Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)
            codegenForCreator.v.visitInnerClass(creatorAsmType.internalName, containerAsmType.internalName, "MyInternalTest", Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)

            run {
                val clz = ImplementationBodyCodegen::class.java
                val m = clz.getDeclaredMethod("generateDefaultImplsIfNeeded")
                m.isAccessible = true
                m.invoke(codegenForCreator)
            }

//            writeCreatorConstructor(codegenForCreator, creatorClass, creatorAsmType)
//            writeNewArrayMethod(codegenForCreator, parcelableClass, creatorClass, parcelerObject)
//            writeCreateFromParcel(codegenForCreator, parcelableClass, creatorClass, parcelClassType, parcelAsmType, parcelerObject, properties)

//            writeSyntheticClassMetadata(classBuilderForCreator, codegen.state)

//            codegenForCreator.generate()

//            codegenForCreator.generate()

//            codegenForCreator.generate()

            classBuilderForCreator.done()

            return
        }

    }*/

  //Generates private constructor and INSTANCE field
  private fun generateObjectBoilerplate(codegen: ImplementationBodyCodegen, creatorClass: ClassDescriptor, creatorAsmType: Type, classBuilderForCreator: ClassBuilder) {
    writeCreatorConstructor(codegen, creatorClass, creatorAsmType)
    writeClassConstructor(codegen, creatorClass, creatorAsmType)
    classBuilderForCreator.newField(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "INSTANCE", creatorAsmType.descriptor, null, null)
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
  private fun writeClassConstructor(codegen: ImplementationBodyCodegen, creatorClass: ClassDescriptor, creatorAsmType: Type) {

    val functionDescriptor = SimpleFunctionDescriptorImpl.create(creatorClass, Annotations.EMPTY, Name.identifier("<clinit>"),
        CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)

    functionDescriptor.initialize(null, creatorClass.thisAsReceiverParameter, emptyList(), emptyList(), creatorClass.builtIns.unitType, Modality.FINAL,
        Visibilities.PRIVATE)

    functionDescriptor.write(codegen) {
      v.anew(creatorAsmType)
      v.dup()
      v.invokespecial(creatorAsmType.internalName, "<init>", "()V", false)
      v.putstatic(creatorAsmType.internalName, "INSTANCE", creatorAsmType.descriptor)
      v.areturn(Type.VOID_TYPE)
    }

  }

  private fun writeCreatorConstructor(codegen: ImplementationBodyCodegen, creatorClass: ClassDescriptor, creatorAsmType: Type) {
    DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source)
        .apply {
          returnType = creatorClass.defaultType
        }.write(codegen) {
          v.load(0, creatorAsmType)
          v.invokespecial("java/lang/Object", "<init>", "()V", false)
          v.areturn(Type.VOID_TYPE)
        }
  }

  private inline fun FunctionDescriptor.write(codegen: ImplementationBodyCodegen, crossinline code: ExpressionCodegen.() -> Unit) {
    val declarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, this)
    codegen.functionCodegen.generateMethod(declarationOrigin, this, object : FunctionGenerationStrategy.CodegenBased(codegen.state) {
      override fun doGenerateBody(e: ExpressionCodegen, signature: JvmMethodSignature) = e.code()
    })
  }

}