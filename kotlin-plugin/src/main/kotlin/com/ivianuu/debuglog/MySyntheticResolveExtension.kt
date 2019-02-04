package com.ivianuu.debuglog

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.MemberScope

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MySyntheticResolveExtension : SyntheticResolveExtension {

    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return emptyList()
        }

        return listOf(Name.identifier("TestClass"))
    }

    override fun generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        super.generateSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)

        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return
        }

        if (name.asString() == "TestClass") {
            val testClass = ClassDescriptorImpl(
                thisDescriptor,
                name,
                Modality.FINAL,
                ClassKind.OBJECT,
                emptyList(),
                SourceElement.NO_SOURCE,
                false,
                ctx.storageManager
            )

            testClass.initialize(
                MemberScope.Empty,
                emptySet(),
                DescriptorFactory.createPrimaryConstructorForObject(testClass, testClass.source)
            )

            result += testClass
        }
    }

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return emptyList()
        }

        return listOf(Name.identifier("testFunction"))
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        super.generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)

        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return
        }

        if (name.asString() == "testFunction") {
            result += SimpleFunctionDescriptorImpl.create(
                thisDescriptor,
                Annotations.EMPTY, name,
                CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
            )
                .initialize(
                    null,
                    thisDescriptor.thisAsReceiverParameter,
                    emptyList(),
                    emptyList(),
                    thisDescriptor.builtIns.unitType,
                    Modality.FINAL,
                    Visibilities.PUBLIC
                )
        }

    }

}