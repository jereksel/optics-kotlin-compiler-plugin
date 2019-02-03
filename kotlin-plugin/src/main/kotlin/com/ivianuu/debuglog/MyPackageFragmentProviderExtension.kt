package com.ivianuu.debuglog

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

class MyPackageFragmentProviderExtension : PackageFragmentProviderExtension {

    var messageCollector: MessageCollector? = null

    override fun getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider? {
        return null//MyPackageFragmentProvider(module, lookupTracker, trace)
    }

}

class MyPackageFragmentProvider(
    private val module: ModuleDescriptor,
    private val lookupTracker: LookupTracker,
    private val trace: BindingTrace
) : PackageFragmentProvider {

    private val packages = mutableListOf<MyPackageFragmentDescriptor>()

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        val myPackage = module.getPackage(fqName)

        val descriptor = MyPackageFragmentDescriptor(module, fqName, myPackage, lookupTracker, trace)

        packages.add(descriptor)

        return listOf(descriptor)
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return packages.asSequence()
            .map { it.fqName }
            .filter { !it.isRoot && it.parent() == fqName }
            .toList()
    }

}

class MyPackageFragmentDescriptor(
    private val module: ModuleDescriptor,
    fqName: FqName,
    private val packageViewDescriptor: PackageViewDescriptor,
    private val lookupTracker: LookupTracker,
    private val trace: BindingTrace
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val scope = MyMemberScope()

    override fun getMemberScope(): MemberScope = scope

    private inner class MyMemberScope : MemberScopeImpl() {

        /*private val functions: List<SimpleFunctionDescriptorImpl> by lazy {
            packageViewDescriptor.fragments
                .filter { it.fqName.asString() == fqName.asString() }
                .filter { it !is MyPackageFragmentDescriptor }
                .flatMap { it.getMemberScope().getContributedDescriptors() }
                .filterIsInstance<LazyClassDescriptor>()
                .onEach { println("got descriptor in $fqName $it ${it.name} type ${it.javaClass}") }
                .flatMap { type ->
                    // i don't know a better way for now..
                    val classOrObject = type.javaClass.let {
                        it.getDeclaredField("classOrObject").let {
                            it.isAccessible = true
                            it.get(type) as KtClassOrObject
                        }
                    }

                    classOrObject.superTypeListEntries
                        .onEach { println("$type .. process super type $it") }
                        .filterIsInstance<KtDelegatedSuperTypeEntry>()
                        .mapNotNull {
                            // how the f*ck do we get "real" type
                            trace.get(BindingContext.TYPE, it.typeReference)
                        }
                        .mapNotNull { it.toClassDescriptor }
                        .map { superType ->
                            val function = MyDelegateFunctionDescriptor(
                                this@MyPackageFragmentDescriptor,
                                Name.identifier(
                                    superType.fqNameSafe.shortName().asString().decapitalize()
                                ))

                            function.initialize(
                                null,
                                null,//memberClassDescriptor.thisAsReceiverParameter,
                                emptyList(),
                                // drop the @Member annotated parameter
                                emptyList(),
                                superType.defaultType,
                                Modality.FINAL,
                                Visibilities.PUBLIC
                            )

                            function
                        }
                }
        }*/

        private val functions by lazy {
            packageViewDescriptor.fragments
                .filter { it.fqName.asString() == fqName.asString() }
                .filter { it !is MyPackageFragmentDescriptor }
                .flatMap { it.getMemberScope().getContributedDescriptors() }
                .filterIsInstance<SimpleFunctionDescriptor>()
                .filter { func ->
                    func.valueParameters.any {
                        it.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Member"))
                    }
                }
                .mapNotNull { func ->
                    val memberParameter = func.valueParameters.first {
                        it.annotations.hasAnnotation(FqName("com.ivianuu.myapplication.Member"))
                    }

                    val type = memberParameter.type

                    val jetType = type.getJetTypeFqName(false)

                    val memberClassDescriptor = module.findClassAcrossModuleDependencies(
                        ClassId.topLevel(FqName(jetType))
                    ) ?: return@mapNotNull null

                    val memberExtension = MyMemberFunctionDescriptor(
                        memberClassDescriptor, func.name,
                        memberClassDescriptor, func
                    )

                    memberExtension.initialize(
                        func.extensionReceiverParameter,
                        memberClassDescriptor.thisAsReceiverParameter,
                        func.typeParameters,
                        // drop the @Member annotated parameter
                        func.valueParameters.drop(1).map {
                            it.copy(memberExtension, it.name, it.index - 1)
                        },
                        func.returnType,
                        func.modality,
                        func.visibility
                    )

                    memberExtension
                }
        }

        override fun getContributedFunctions(name: Name, location: LookupLocation): List<SimpleFunctionDescriptor> {
            return functions.filter { it.name == name }
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> {
            return functions.filter { kindFilter.acceptsKinds(
                DescriptorKindFilter.FUNCTIONS_MASK) && nameFilter(it.name) }
        }

        override fun recordLookup(name: Name, location: LookupLocation) {
            super.recordLookup(name, location)
            lookupTracker.record(location, this@MyPackageFragmentDescriptor, name)
        }

        override fun printScopeStructure(p: Printer) {

        }
    }

}

class MyMemberFunctionDescriptor(
    containingDeclarationDescriptor: DeclarationDescriptor,
    name: Name,
    val memberReceiverType: ClassDescriptor,
    val sourceFunction: SimpleFunctionDescriptor
) : SimpleFunctionDescriptorImpl(
    containingDeclarationDescriptor,
    null,
    Annotations.EMPTY,
    name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    sourceFunction.source
)


class MyDelegateFunctionDescriptor(
    containingDeclarationDescriptor: DeclarationDescriptor,
    name: Name
) : SimpleFunctionDescriptorImpl(
    containingDeclarationDescriptor,
    null,
    Annotations.EMPTY,
    name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    SourceElement.NO_SOURCE
)