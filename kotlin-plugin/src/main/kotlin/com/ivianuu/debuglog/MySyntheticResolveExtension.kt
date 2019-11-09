package com.ivianuu.debuglog

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MySyntheticResolveExtension : SyntheticResolveExtension {

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return emptyList()
        }

        return listOf(Name.identifier("variable"), Name.identifier("function"), Name.identifier("MyInternalTest"))
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

        if (name.asString() == "variable") {
            val (a) =  propertyDescriptor(
                    thisDescriptor,
                    name.asString(),
                    thisDescriptor.builtIns.intType,
                    source = thisDescriptor.source,
                    isVar = false,
                    visibility = Visibilities.PUBLIC
            )
            result += a
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

        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return
        }

        if (name.asString() == "function") {

            val methodDescriptor = SimpleFunctionDescriptorImpl.create(
                    thisDescriptor,
                    Annotations.EMPTY, name,
                    CallableMemberDescriptor.Kind.SYNTHESIZED, thisDescriptor.source
            )
                    .initialize(
                            null,
                            thisDescriptor.thisAsReceiverParameter,
                            emptyList(),
                            emptyList(),
                            thisDescriptor.builtIns.intType,
                            Modality.FINAL,
                            Visibilities.PUBLIC
                    )

            result += methodDescriptor

        }

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

        if (name.asString() == "MyInternalTest") {
            val c = thisDescriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("com.jereksel.TestInterface")))?.defaultType!!
            val testClass = ClassDescriptorImpl(
                    thisDescriptor,
                    name,
                    Modality.FINAL,
                    ClassKind.CLASS,
                    listOf(c),
                    thisDescriptor.source,
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

}

/*class SimpleSyntheticPropertyDescriptor(
        owner: ClassDescriptor,
        name: String,
        type: KotlinType,
        source: SourceElement,
        isVar: Boolean = false,
        visibility: Visibility = Visibilities.PRIVATE
) : PropertyDescriptorImpl(
        owner,
        null,
        Annotations.EMPTY,
        Modality.FINAL,
        visibility,
        isVar,
        Name.identifier(name),
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        owner.source,
        false, false, false, false, false, false
) {

    private val _getter = PropertyGetterDescriptorImpl(
            this,
            Annotations.EMPTY,
            Modality.FINAL,
            visibility,
            false,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            source
    )

    init {
        _getter.initialize(type)
        super.initialize(_getter, null)
        super.setType(type, emptyList(), owner.thisAsReceiverParameter, null)
    }
}*/

/*
class MySyntheticResolveExtension : SyntheticResolveExtension {

    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
//        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
//        }

//        return emptyList()
        return emptyList()
    }



    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return emptyList()
        }

//        return listOf(Name.identifier("testFunction"), Name.identifier("variable"))
        return listOf(Name.identifier("testFunction"), Name.identifier("abc"))
    }

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
        if (!thisDescriptor.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Synthetics"))) {
            return null
        }
        return SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
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

            val methodDescriptor = SimpleFunctionDescriptorImpl.create(
                thisDescriptor,
                Annotations.EMPTY, name,
                CallableMemberDescriptor.Kind.SYNTHESIZED, thisDescriptor.source
            )
                .initialize(
                    null,
                    thisDescriptor.thisAsReceiverParameter,
                    emptyList(),
                    emptyList(),
                    thisDescriptor.builtIns.intType,
                    Modality.FINAL,
                    Visibilities.PUBLIC
                )

            result += methodDescriptor

        }

        if (name.asString() == "variable") {

            val (_, a) = propertyDescriptor(
                    thisDescriptor,
                    name.asString(),
                    thisDescriptor.builtIns.intType,
                    source = thisDescriptor.source,
                    isVar = false,
                    visibility = Visibilities.PUBLIC
            )


        }

//        if (name.asString() == "testFunction1") {
////
//            result += SimpleFunctionDescriptorImpl.create(
//                    thisDescriptor.companionObjectDescriptor!!.containingDeclaration,
//                    Annotations.EMPTY, name,
//                    CallableMemberDescriptor.Kind.SYNTHESIZED, thisDescriptor.source
//            )
//                    .initialize(
//                            null,
//                            thisDescriptor.companionObjectDescriptor!!.thisAsReceiverParameter,
//                            emptyList(),
//                            emptyList(),
//                            thisDescriptor.builtIns.intType,
//                            Modality.FINAL,
//                            Visibilities.PUBLIC
//                    )
//
//        }

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

        if (name.asString() == "abc") {
            val (a) = propertyDescriptor(
                    thisDescriptor,
                    name.asString(),
                    thisDescriptor.builtIns.intType,
                    source = thisDescriptor.source,
                    isVar = false,
                    visibility = Visibilities.PUBLIC
            )
            result += a
        }

    }

}

*/

fun propertyDescriptor(
        owner: ClassDescriptor,
        name: String,
        type: KotlinType,
        source: SourceElement,
        isVar: Boolean,
        visibility: Visibility
): Pair<PropertyDescriptorImpl, PropertyGetterDescriptorImpl> {

    val propertyDescriptor = PropertyDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            Modality.FINAL,
            visibility,
            isVar,
            Name.identifier(name),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            owner.source,
            false, false, false, false, false, false
    )

    val getter = PropertyGetterDescriptorImpl(
            propertyDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            visibility,
            false,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            source
    )

    getter.initialize(type)
    propertyDescriptor.initialize(getter, null)
    propertyDescriptor.setType(type, emptyList(), owner.thisAsReceiverParameter, null)

    return propertyDescriptor to getter

}

/*

class SimpleSyntheticPropertyDescriptor(
        owner: ClassDescriptor,
        name: String,
        type: KotlinType,
        source: SourceElement,
        isVar: Boolean = false,
        visibility: Visibility = Visibilities.PRIVATE
) : PropertyDescriptorImpl(
        owner,
        null,
        Annotations.EMPTY,
        Modality.FINAL,
        visibility,
        isVar,
        Name.identifier(name),
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        owner.source,
        false, false, false, false, false, false
) {

    private val _getter = PropertyGetterDescriptorImpl(
            this,
            Annotations.EMPTY,
            Modality.FINAL,
            visibility,
            false,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            source
    )

    init {
        _getter.initialize(type)
        super.initialize(_getter, null, FieldDescriptorImpl(Annotations.EMPTY, this), FieldDescriptorImpl(Annotations.EMPTY, this))
        super.setType(type, emptyList(), owner.thisAsReceiverParameter, owner.thisAsReceiverParameter)
    }
}*/
