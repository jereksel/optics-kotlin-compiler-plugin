package com.ivianuu.debuglog

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.searches.AllClassesSearch
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens.DATA_KEYWORD
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

class MyPackageFragmentProviderExtension : PackageFragmentProviderExtension {

  private val topLevel = FqName.topLevel(Name.identifier("arrowx"))

  override fun getPackageFragmentProvider(
      project: Project,
      module: ModuleDescriptor,
      storageManager: StorageManager,
      trace: BindingTrace,
      moduleInfo: ModuleInfo?,
      lookupTracker: LookupTracker
  ): PackageFragmentProvider? {

    println("Module: $moduleInfo")

    val classes = if (moduleInfo == null) {
//      //CLI
      val psiManager = PsiManager.getInstance(project)
      val fileManager = VirtualFileManager.getInstance()

      println()
      println(psiManager)
      println(fileManager)

//
//      FileTypeIndex.getFiles(KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project))
//
//      GlobalSearchScope.ALL
//
////      fileManager.
//
//      AllClassesSearch.search(GlobalSearchScope.allScope(project), project)
//          .findAll()
//          .forEach {
//            println(it)
//          }
//
//      println(psiManager)
//      println(fileManager)
//
//      AllClassesSearch.search(GlobalSearchScope.allScope(project), project)
//          .findAll()
//          .asSequence()
//          .filterIsInstance<KtLightClassBase>()
//          .filter { it.kotlinOrigin?.hasModifier(DATA_KEYWORD) == true }
//          .toList()


      return object: PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
          // module.getPackage(FqName("com.github.ajalt.clikt.output")).memberScope.getContributedClassifier(Name.identifier("HelpFormatter"), NoLookupLocation.FROM_BACKEND)
//          println()
//          module.resolveClassByFqName()
//          println(project)
//          println(module)
//          println(psiManager)
//          println(fileManager)
//          return emptyList()

          val originalFqName = fqName

          if (fqName.isRoot || fqName.parent().isRoot) {
            return emptyList()
          }

          if (!fqName.isSubpackageOf(topLevel)) {
            return emptyList()
          }

          val fqName = FqName.fromSegments(fqName.pathSegments().drop(1).map { it.asString() })

          var className = fqName.shortName()
          var tempFqName = fqName.parent()

          val segments = mutableListOf<String>()

          val segmentsMap = mutableMapOf<FqName, Name>()

//          val segments = mutableListOf<String>()

          while (!tempFqName.isRoot) {
            segmentsMap += tempFqName to className
            className = Name.identifier("${tempFqName.shortName().asString()}\$${className.asString()}")
            tempFqName = tempFqName.parent()
//            className = tempFqName.shortName()
//            seg

//            segments.add(0, tempFqName.shortName().asString())
//            tempFqName = tempFqName.parent()
          }

//          val clz = module.getPackage(FqName.fromSegments).memberScope.getContributedClassifier(Name.identifier("HelpFormatter"), NoLookupLocation.FROM_BACKEND)

          val pckg = FqName.fromSegments(segments)

          val clz = segmentsMap.mapNotNull { (p, c) ->
            val memberScope = module.getPackage(p).memberScope

            memberScope.getContributedClassifier(c, NoLookupLocation.FROM_BACKEND)
                as? DeserializedClassDescriptor
          }
//              .firstOrNull() ?: return emptyList()

          println(clz)

//          val memberScope =  ?: return emptyList()

//          val clz = (memberScope.getClassifierNames() ?: emptySet())
//              .mapNotNull { className ->
//                memberScope.getContributedClassifier(className, NoLookupLocation.FROM_BACKEND) as? DeserializedClassDescriptor
//              }
//              .firstOrNull {
//                it.name.asString() == className.asString()
//              } ?: return emptyList()

          return listOf(
              object: PackageFragmentDescriptorImpl(
                  module,
                  originalFqName
              ) {
                override fun getMemberScope(): MemberScope {
                  val t = this

                  val functions: List<SimpleFunctionDescriptorImpl> = emptyList()

                  val properties = listOf(
                      OpticsPropertyDescriptor(
                          t,
                          null,
                          Annotations.EMPTY,
                          Modality.FINAL,
                          Visibilities.PUBLIC,
                          false,
                          Name.identifier("myAwesomeProperty"),
                          CallableMemberDescriptor.Kind.SYNTHESIZED,
                          SourceElement.NO_SOURCE,
                          false, false, false, false, false, false
                      ).apply {

                        val getter = object: OpticsSyntheticFunction, PropertyGetterDescriptorImpl(
                            this,
                            Annotations.EMPTY,
                            Modality.FINAL,
                            visibility,
                            false,
                            false,
                            false,
                            CallableMemberDescriptor.Kind.SYNTHESIZED,
                            null,
                            SourceElement.NO_SOURCE
                        ) {}

                        getter.initialize(module.builtIns.intType)
                        initialize(getter, null)
                        setType(module.builtIns.intType, emptyList(), null, module.builtIns.string.thisAsReceiverParameter)
                      }

                  )

                  return object: MemberScopeImpl() {

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

                }
              }
          )

        }

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
//          println()
//          println(project)
//          println(module)
//          println(psiManager)
//          println(fileManager)
          return emptyList()
        }

      }

    } else if (moduleInfo is LibraryInfo) {
      //IDE
      AllClassesSearch.search(moduleInfo.contentScope(), project)
          .findAll()
          .asSequence()
          .filterIsInstance<KtLightClassBase>()
          .filter { it.kotlinOrigin?.hasModifier(DATA_KEYWORD) == true }
          .toList()

    } else {
      return null
    }

    println("Module: $moduleInfo")

//    val classes = AllClassesSearch.search(moduleInfo.contentScope(), project)
//        .findAll()
//        .asSequence()
//        .filterIsInstance<KtLightClassBase>()
//        .filter { it.kotlinOrigin?.hasModifier(DATA_KEYWORD) == true }
//        .toList()
//
    classes.forEach {
      println("${it.name}")
    }

    return MyPackageFragmentProvider(classes, module)

//    FileTypeIndex.getFiles(KotlinFileType.INSTANCE, moduleInfo.contentScope())
//        .asSequence()
//        .mapNotNull { PsiManager.getInstance(project).findFile(it) }
//        .filterIsInstance<KtFile>()
//        .flatMap { it.classes.asSequence() }
//        .filter { it.qualifiedName != null }
//        .forEach { psiClass ->
//          println("File: ${psiClass.qualifiedName}")
//      }

//    return null

  }
}