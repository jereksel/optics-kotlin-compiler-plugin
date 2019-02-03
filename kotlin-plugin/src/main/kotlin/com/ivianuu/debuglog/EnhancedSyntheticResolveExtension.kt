package com.ivianuu.debuglog

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
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
            project, module, trace, lookupTracker, storageManager
        )
    }
}

class EnhancedSyntheticResolvingPackageFragmentProvider(
    private val project: Project,
    private val module: ModuleDescriptor,
    private val trace: BindingTrace,
    private val lookupTracker: LookupTracker,
    private val storageManager: StorageManager
) : PackageFragmentProvider {

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        println("get package fragments $fqName")
        val kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(project)

        return kotlinAsJavaSupport.findClassOrObjectDeclarationsInPackage(
            fqName,
            GlobalSearchScope.allScope(project)
        ).map {
            EnhancedSyntheticResolvingPackageFragmentDescriptor(
                project, it, fqName, module, module.getPackage(fqName),
                lookupTracker, trace
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
    private val classOrObject: KtClassOrObject,
    fqName: FqName,
    private val module: ModuleDescriptor,
    private val packageViewDescriptor: PackageViewDescriptor,
    private val lookupTracker: LookupTracker,
    private val trace: BindingTrace
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val descriptor by lazy {
        packageViewDescriptor.fragments
            .onEach { println("found fragment real $it ${it.javaClass} is this ${it == this}") }
            .filter { it !is EnhancedSyntheticResolvingPackageFragmentDescriptor }
            .flatMap { it.getMemberScope().getContributedDescriptors() }
            .onEach { println("contributed $it") }
            .filterIsInstance<ClassDescriptor>()
            .firstNotNullResult {
                println("check class $it name ${it.name} safe ${it.fqNameSafe} ${it.fqNameUnsafe} what we search fpr ${classOrObject.fqName}")

                it.takeIf { it.name.asString() == classOrObject.fqName!!.asString() }
            } ?: error("couldn't find descriptor for ${classOrObject.fqName} in $fqName")
    }

    /*// siblings can only be accessed from member scope
    // would give a recursive error otherwise
    private val siblings by lazy {
        packageViewDescriptor.fragments
            .filter {
                it.fqName.asString() == fqName.asString()
                        && it !is EnhancedSyntheticResolvingPackageFragmentDescriptor
            }
    }*/

    // todo check if its required to always recompute the value
    private val syntheticResolveExtensions
        get() =
            SyntheticResolveExtension.getInstances(project)
                .filterIsInstance<EnhancedSyntheticResolveExtension>()

    private val scope = SyntheticVariableMemberScope()

    override fun getMemberScope(): MemberScope = scope

    private val descriptors by lazy {
        syntheticResolveExtensions
            .flatMap { ext ->
                val fromSupertypes = ArrayList<PropertyDescriptor>()
                for (supertype in descriptor.typeConstructor.supertypes) {
                    fromSupertypes.addAll(
                        supertype.memberScope
                            .getContributedVariables(descriptor.name, NoLookupLocation.FOR_ALREADY_TRACKED)
                    )
                }

                val result = mutableSetOf<PropertyDescriptor>()

                val names = ext.getSyntheticVariableNames(descriptor)

                names.forEach { name ->
                    ext.generateSyntheticProperties(
                        descriptor,
                        name, trace.bindingContext,
                        fromSupertypes, result
                    )
                }

                result
            }
    }

    private val properties by lazy {
        syntheticResolveExtensions
            .flatMap { ext ->
                val fromSupertypes = ArrayList<PropertyDescriptor>()
                for (supertype in descriptor.typeConstructor.supertypes) {
                    fromSupertypes.addAll(
                        supertype.memberScope.getContributedVariables(
                            descriptor.name, NoLookupLocation.FOR_ALREADY_TRACKED
                        )
                    )
                }

                val result = mutableSetOf<PropertyDescriptor>()

                val names = ext.getSyntheticVariableNames(descriptor)

                names.forEach { name ->
                    ext.generateSyntheticProperties(
                        descriptor,
                        name, trace.bindingContext,
                        fromSupertypes, result
                    )
                }

                result
            }
    }

    private val variablesNames by lazy {
        syntheticResolveExtensions
            .flatMap { it.getSyntheticVariableNames(descriptor) }
            .toSet()
    }

    init {
        println("initialize descriptor in $fqName with ${classOrObject.fqName}")
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