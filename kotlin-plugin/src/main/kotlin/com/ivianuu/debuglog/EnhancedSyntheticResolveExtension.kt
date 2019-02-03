package com.ivianuu.debuglog

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import java.util.*

interface EnhancedSyntheticResolveExtension : SyntheticResolveExtension {
    fun getSyntheticVariableNames(thisDescriptor: ClassDescriptor): List<Name>
}

class EnhancedSyntheticResolvingPackageFragmentProviderExtension : PackageFragmentProviderExtension {
    override fun getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider? {
        return EnhancedSyntheticResolvingPackageFragmentProvider(
            project, module, trace, lookupTracker
        )
    }
}

class EnhancedSyntheticResolvingPackageFragmentProvider(
    private val project: Project,
    private val module: ModuleDescriptor,
    private val trace: BindingTrace,
    private val lookupTracker: LookupTracker
) : PackageFragmentProvider {

    private val packages = mutableMapOf<FqName, List<PackageFragmentDescriptor>>()

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return packages.getOrPut(fqName) {
            val packageViewDescriptor = module.getPackage(fqName)
            listOf(
                EnhancedSyntheticResolvingPackageFragmentDescriptor(
                    project, fqName, module, packageViewDescriptor, lookupTracker, trace
                )
            )
        }
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        //println("get sub packages of $fqName")
        /*packages.values
            .flatten()
            .asSequence()
            .map { it.fqName }
            .filter { !it.isRoot && it.parent() == fqName }

           .toList()*/
        return emptyList()
    }
}

class EnhancedSyntheticResolvingPackageFragmentDescriptor(
    private val project: Project,
    fqName: FqName,
    private val module: ModuleDescriptor,
    private val packageViewDescriptor: PackageViewDescriptor,
    private val lookupTracker: LookupTracker,
    private val trace: BindingTrace
) : PackageFragmentDescriptorImpl(module, fqName) {

    // siblings can only be accessed from member scope
    // would give a recursive error otherwise
    private val siblings by lazy {
        packageViewDescriptor.fragments
            .filter {
                it.fqName.asString() == fqName.asString()
                        && it !is EnhancedSyntheticResolvingPackageFragmentDescriptor
            }
    }

    // todo check if its required to always recompute the value
    private val syntheticResolveExtensions
        get() =
            SyntheticResolveExtension.getInstances(project)
                .filterIsInstance<EnhancedSyntheticResolveExtension>()

    private val scope = SyntheticVariableMemberScope()

    override fun getMemberScope(): MemberScope = scope

    private val descriptors by lazy {
        siblings
            .flatMap { it.getMemberScope().getContributedDescriptors() }
            .filterIsInstance<ClassDescriptor>()
            .flatMap { classDescriptor ->
                syntheticResolveExtensions
                    .flatMap { ext ->
                        val fromSupertypes = ArrayList<PropertyDescriptor>()
                        for (supertype in classDescriptor.typeConstructor.supertypes) {
                            fromSupertypes.addAll(
                                supertype.memberScope
                                    .getContributedVariables(classDescriptor.name, NoLookupLocation.FOR_ALREADY_TRACKED)
                            )
                        }

                        val result = mutableSetOf<PropertyDescriptor>()

                        val names = ext.getSyntheticVariableNames(classDescriptor)

                        names.forEach { name ->
                            ext.generateSyntheticProperties(
                                classDescriptor,
                                name, trace.bindingContext,
                                fromSupertypes, result
                            )
                        }

                        result
                    }
            }
    }

    private val properties by lazy {
        siblings
            .flatMap { it.getMemberScope().getContributedDescriptors() }
            .filterIsInstance<ClassDescriptor>()
            .flatMap { classDescriptor ->
                syntheticResolveExtensions
                    .flatMap { ext ->
                        val fromSupertypes = ArrayList<PropertyDescriptor>()
                        for (supertype in classDescriptor.typeConstructor.supertypes) {
                            fromSupertypes.addAll(
                                supertype.memberScope.getContributedVariables(
                                    classDescriptor.name, NoLookupLocation.FOR_ALREADY_TRACKED
                                )
                            )
                        }

                        val result = mutableSetOf<PropertyDescriptor>()

                        val names = ext.getSyntheticVariableNames(classDescriptor)

                        names.forEach { name ->
                            ext.generateSyntheticProperties(
                                classDescriptor,
                                name, trace.bindingContext,
                                fromSupertypes, result
                            )
                        }

                        result
                    }
            }
    }

    private val variablesNames by lazy {
        siblings
            .flatMap { it.getMemberScope().getContributedDescriptors() }
            .filterIsInstance<ClassDescriptor>()
            .flatMap { classDescriptor ->
                syntheticResolveExtensions
                    .flatMap { it.getSyntheticVariableNames(classDescriptor) }
            }
            .toSet()
    }

    private inner class SyntheticVariableMemberScope : MemberScopeImpl() {

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> {
            return descriptors
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            recordLookup(name, location)
            return properties
        }

        override fun getVariableNames(): Set<Name> {
            return variablesNames
        }

        override fun recordLookup(name: Name, location: LookupLocation) {
            lookupTracker.record(
                location,
                this@EnhancedSyntheticResolvingPackageFragmentDescriptor, name
            )
        }

        override fun printScopeStructure(p: Printer) {
            // todo
        }
    }
}