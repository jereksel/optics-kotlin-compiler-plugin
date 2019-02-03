package com.ivianuu.debuglog

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClearableLazyValue
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.idea.caches.project.implementedDescriptors
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.maven.inspections.hasJavaFiles
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.load.kotlin.getContainingKotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsPackageFragmentImpl
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
        return MyPackageFragmentProvider(module)
    }

}

class MyPackageFragmentProvider(private val module: ModuleDescriptor) : PackageFragmentProvider {

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        println("get package fragments $fqName start")

        val myPackage = module.getPackage(fqName)

        return listOf(MyPackageFragmentDescriptor(module, fqName, myPackage))
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return emptyList()/*packages.asSequence()
            .map { it.fqName }
            .filter { !it.isRoot && it.parent() == fqName }
            .toList()*/
    }

}

class MyPackageFragmentDescriptor(
    val module: ModuleDescriptor,
    fqName: FqName,
    val packageViewDescriptor: PackageViewDescriptor
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val scope = MyMemberScope()

    override fun getMemberScope(): MemberScope = scope

    private inner class MyMemberScope : MemberScopeImpl() {

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

                    /*println("func ${func.name} -> " +
                            "member parameter $memberParameter " +
                            "clazz ${memberParameter.javaClass.name} " +
                            "return ${memberParameter.returnType} " +
                            "type ${memberParameter.type}")*/

                    println("func ${func.name} member jet type $jetType")

                    val memberClassDescriptor = module.findClassAcrossModuleDependencies(
                        ClassId.topLevel(FqName(jetType))
                    ) ?: return@mapNotNull null

                    println("func: ${func.name} " +
                            "member class descriptor $memberClassDescriptor " +
                            "name ${memberClassDescriptor.name} " +
                            "${memberClassDescriptor.thisAsReceiverParameter} ")

                    val memberExtension = MyMemberExtension(
                        this@MyPackageFragmentDescriptor, func.name,
                        memberClassDescriptor, func)

                    memberExtension.initialize(
                        func.extensionReceiverParameter,
                        null,//memberClassDescriptor.thisAsReceiverParameter,
                        func.typeParameters,
                        // drop the @Member annotated parameter
                        func.valueParameters.drop(1).map {
                            it.copy(memberExtension, it.name, it.index - 1)
                        },
                        func.returnType,
                        func.modality,
                        func.visibility
                    )

                    println(
                        "original func is $func member extensions is $memberExtension"
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
         // todo   lookupTracker.recordPackageLookup(location)
        }

        override fun printScopeStructure(p: Printer) {

        }
    }

}

class MyMemberExtension(
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