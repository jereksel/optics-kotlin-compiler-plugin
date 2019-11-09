package com.ivianuu.debuglog

import com.ivianuu.debuglog.OpticsConst.OPTICS_CLASS_NAME
import com.ivianuu.debuglog.OpticsConst.annotationClass
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
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

    classBuilderForCreator.newField(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "INSTANCE", creatorAsmType.descriptor, null, null)

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

//      classBuilderForGetter.newField(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "INSTANCE", parameterGetterType.descriptor, null, null)

//      codegenForGetter.functionCodegen.generateMeth

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
  private fun generateObjectBoilerplate(codegen: ImplementationBodyCodegen, creatorClass: ClassDescriptor, creatorAsmType: Type) {

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

  private fun FunctionDescriptor.write(codegen: ImplementationBodyCodegen, code: ExpressionCodegen.() -> Unit) {
    val declarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, this)
    codegen.functionCodegen.generateMethod(declarationOrigin, this, object : FunctionGenerationStrategy.CodegenBased(codegen.state) {
      override fun doGenerateBody(e: ExpressionCodegen, signature: JvmMethodSignature) = with(e) {
        e.code()
      }
    })
  }

}