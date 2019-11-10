package com.ivianuu.debuglog

import com.ivianuu.debuglog.OpticsConst.OPTICS_CLASS_NAME
import com.ivianuu.debuglog.OpticsConst.LENS_CLASS_NAME
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

class MyKotlinIndicesHelperExtension : KotlinIndicesHelperExtension {

    private val lens = OpticsConst.lensClass

    override fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean,
        lookupLocation: LookupLocation) {

        //Return collection with multiple elements
       val receiverType = receiverTypes.singleOrNull() ?: return

        val location = (lookupLocation as? KotlinLookupLocation)?.element ?: return

//        (lookupLocation.location as KotlinLookupLocation).element.

        //FIXME
        if (receiverType.constructor.declarationDescriptor?.fqNameSafe != lens) {
            return
        }

        val dataClass = receiverType.arguments.last().type

        val clzName = dataClass.toClassDescriptor?.importableFqName ?: return

        val declarationDescriptor = dataClass.constructor.declarationDescriptor ?: return

        println((declarationDescriptor as? ClassDescriptor)?.constructors?.first()?.allParameters)

        val parameters = (declarationDescriptor as? ClassDescriptor)?.constructors?.first()?.explicitParameters ?: return

        consumer.addAll(
            parameters
                .map { parameter ->

                    PropertyDescriptorImpl.create(
//                        moduleDescriptor,
//                        moduleDescriptor.getPackage(location.containingKtFile.packageFqName),
                        EmptyPackageFragmentDescriptor(moduleDescriptor, clzName.child(Name.identifier(OPTICS_CLASS_NAME))),
                        Annotations.EMPTY,
                        Modality.FINAL,
                        Visibilities.PUBLIC,
                        false,
                        Name.identifier(parameter.name.asString()),
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

                        val typeParameterDescriptor = TypeParameterDescriptorImpl.createWithDefaultBound(
                            this,
                            Annotations.EMPTY,
                            false,
                            Variance.INVARIANT,
                            Name.identifier("A"),
                            0
                        )

                        val genericType = moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(lens))?.defaultType ?: return

                        val left = KotlinTypeFactory.simpleType(
                            genericType,
                            arguments = listOf(
                                TypeProjectionImpl(typeParameterDescriptor.defaultType),
                                TypeProjectionImpl(typeParameterDescriptor.defaultType),
                                receiverType.arguments.last(),
                                receiverType.arguments.last()
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
                        val receiverParameterDescriptor = ReceiverParameterDescriptorImpl(this, extensionReceiver, Annotations.EMPTY)

                        getter.initialize(left)
                        initialize(getter, null)
                        setType(right, listOf(typeParameterDescriptor), null, receiverParameterDescriptor)
                    }

                }
                .filter { nameFilter(it.name.asString()) }

        )

        println(dataClass)
        println(receiverType)
        println(moduleDescriptor)
        println(receiverTypes)
        println(nameFilter)
        println(lookupLocation)
    }

    override fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}