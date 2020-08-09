package com.ivianuu.debuglog

import com.ivianuu.debuglog.OpticsConst.OPTICS_CLASS_NAME
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer
import java.util.ArrayList

class MySyntheticResolveExtension : SyntheticResolveExtension {

    private val annotation = OpticsConst.annotationClass
    private val lens = OpticsConst.lensClass

    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!thisDescriptor.annotations.hasAnnotation(annotation)) {
            return emptyList()
        }

        return listOf(Name.identifier(OPTICS_CLASS_NAME))
    }

    override fun generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        if (!thisDescriptor.annotations.hasAnnotation(annotation)) {
            return
        }

        if (name.asString() == OPTICS_CLASS_NAME) {

            val opticsObject = ClassDescriptorImpl(
                thisDescriptor,
                name,
                Modality.FINAL,
                ClassKind.OBJECT,
                emptyList(),
                thisDescriptor.source,
                false,
                ctx.storageManager
            )

            val functions: List<SimpleFunctionDescriptorImpl> = emptyList()

            val parameters = thisDescriptor.unsubstitutedPrimaryConstructor?.explicitParameters ?: return

            val properties: List<PropertyDescriptorImpl> = parameters.map { parameter ->

                PropertyDescriptorImpl.create(
                    opticsObject,
                    Annotations.EMPTY,
                    Modality.FINAL,
                    Visibilities.DEFAULT_VISIBILITY,
                    false,
                    parameter.name,
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    parameter.source,
                    false, false, false, false, false, false
                ).apply {

                    val getter = PropertyGetterDescriptorImpl(
                        this,
                        Annotations.EMPTY,
                        Modality.FINAL,
                        visibility,
                        false,
                        false,
                        false,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        null,
                        parameter.source
                    )

                    val genericType = ctx.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(lens))?.defaultType ?: return

                    val typeParameterDescriptor = TypeParameterDescriptorImpl.createWithDefaultBound(
                        this,
                        Annotations.EMPTY,
                        false,
                        Variance.INVARIANT,
                        Name.identifier("A"),
                        0,
                        LockBasedStorageManager.NO_LOCKS
                    )

                    val left = KotlinTypeFactory.simpleType(
                        genericType,
                        arguments = listOf(
                            TypeProjectionImpl(typeParameterDescriptor.defaultType),
                            TypeProjectionImpl(typeParameterDescriptor.defaultType),
                            TypeProjectionImpl(thisDescriptor.defaultType),
                            TypeProjectionImpl(thisDescriptor.defaultType)
                        )
                    )

                    val right = KotlinTypeFactory.simpleType(
                        genericType,
                        arguments = listOf(
                            TypeProjectionImpl(typeParameterDescriptor.defaultType),
                            TypeProjectionImpl(typeParameterDescriptor.defaultType),
                            TypeProjectionImpl(parameter.type),
                            TypeProjectionImpl(parameter.type)
                        )
                    )

                    val extensionReceiver = ExtensionReceiver(this, left, null)
                    val receiverParameterDescriptor =
                        ReceiverParameterDescriptorImpl(this, extensionReceiver, Annotations.EMPTY)

                    getter.initialize(left)
                    initialize(getter, null)
                    setType(
                        right,
                        listOf(typeParameterDescriptor),
                        opticsObject.thisAsReceiverParameter,
                        receiverParameterDescriptor
                    )
                }

            }

            val memberScope = object: MemberScopeImpl() {

                override fun getContributedFunctions(
                    name: Name,
                    location: LookupLocation
                ): Collection<SimpleFunctionDescriptor> {
                    return functions.filter { it.name == name }
                }

                override fun getContributedVariables(
                    name: Name,
                    location: LookupLocation
                ): Collection<PropertyDescriptor> {
                    return properties.filter { it.name == name }
                }

                override fun getContributedDescriptors(
                    kindFilter: DescriptorKindFilter,
                    nameFilter: (Name) -> Boolean
                ): Collection<DeclarationDescriptor> {
                    return (functions + properties)
                        .filter { kindFilter.accepts(it) && nameFilter(it.name) }
                }

                override fun printScopeStructure(p: Printer) {
                    p.println(this::class.java.simpleName)
                }

            }

            result += opticsObject.apply {
                initialize(
                    memberScope,
                    setOf(),
                    DescriptorFactory.createPrimaryConstructorForObject(opticsObject, opticsObject.source)
                )
            }

        }

        super.generateSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)
    }

    private fun propertyDescriptor(
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

}
