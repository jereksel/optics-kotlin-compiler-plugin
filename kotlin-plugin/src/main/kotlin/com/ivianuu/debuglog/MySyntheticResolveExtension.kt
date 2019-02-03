package com.ivianuu.debuglog

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import java.util.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MySyntheticResolveExtension : EnhancedSyntheticResolveExtension {

    var messageCollector: MessageCollector? = null

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

        println("generate synthetic classes $name")
    }

    override fun getSyntheticVariableNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return emptyList()
        }
        println("received get synthetic variable names $thisDescriptor")

        return listOf(Name.identifier("testProperty"))
    }

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return emptyList()
        }
        println("received get synthetic function names $thisDescriptor")

        return listOf(Name.identifier("testFunction"), Name.identifier("testFunctionTwo"))
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) {
        super.generateSyntheticProperties(thisDescriptor, name, bindingContext, fromSupertypes, result)

        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return
        }

        println("generate synthetic properties $thisDescriptor $name")

        if (name.asString() == "testProperty") {
            val testProperty = PropertyDescriptorImpl.create(
                thisDescriptor,
                Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC,
                false, Name.identifier("testProperty"),
                CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false
            )

            val getter = PropertyGetterDescriptorImpl(
                testProperty, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC,
                false, false, false, CallableMemberDescriptor.Kind.SYNTHESIZED, null,
                SourceElement.NO_SOURCE
            )

            getter.initialize(null)

            testProperty.setType(
                thisDescriptor.builtIns.anyType,
                emptyList(),
                thisDescriptor.thisAsReceiverParameter,
                null
            )

            testProperty.initialize(getter, null)

            result += testProperty
        }
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        super.generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)

        println("generate synthetic methods $thisDescriptor")

        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return
        }

        println("generate synthetic methods $thisDescriptor $name")

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

        if (name.asString() == "testFunctionTwo") {
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